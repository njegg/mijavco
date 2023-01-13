package parser;

import compiler.Main;
import scanner.Scanner;
import scanner.Token;
import scanner.TokenKind;

import java.util.*;
import java.util.stream.Collectors;

import static scanner.TokenKind.*;

public class Parser {
    private static SymbolTable symbolTable;

    private static TokenKind kind;
    private static Token token;
    private static Token prevToken;

    private static final int ERROR_IGNORE_DISTANCE = 4;
    private static int lastError = ERROR_IGNORE_DISTANCE;

    private static EnumSet<TokenKind> firstStatement;

    public static void parse() {
        symbolTable = new SymbolTable();
        firstStatement = EnumSet.of(IDENT, IF, WHILE, BREAK, RETURN, READ, PRINT, LBRACE, SEMICOLON);

        scan();
        mijava(); /* Checks the whole program file */

        /* Check if main function is declared */
        Symbol main = symbolTable.find("main");
        if (main == null || main.symbolKind != SymbolKind.FUNCTION) {
            error("Program needs to have a function called main()");
        } else if (main.parameters.size() > 0) {
            error("Main must not have parameters");
        }
    }

    public static void error(String message) {
        if (lastError >= ERROR_IGNORE_DISTANCE) {
            System.err.printf("%s:%d:%d : %s\n", Scanner.getFilePath(), prevToken.line, prevToken.column, message);
            Main.error();
        }

        lastError = 0;
    }

    private static void scan() {
        prevToken = token;
        token = Scanner.nextToken();

        kind = token.kind;

        if (kind == ERROR) {
            error(token.text);
            scan();
        }
    }

    private static void check(TokenKind expected) {
        if (expected == kind) {
            scan();
            lastError++;
        } else {
            error(expected + " expected, got " + kind);
        }
    }

    private static void mijava() {
        check(PROGRAM);
        check(IDENT);

        while (true) {
            if      (kind == IDENT)  varDeclaration();
            else if (kind == CONST)  constDeclaration();
            else if (kind == STRUCT) structDeclaration();
            else {
                if (kind != LBRACE) error(kind + " not expected here");
                break;
            }
        }

        /* If there was an error, skip to start of program block*/
        while (kind != LBRACE && kind != EOF)
            scan();

        check(LBRACE);

        while (kind == IDENT || kind == VOID)
            function();

        check(RBRACE);

        check(EOF);
    }

    private static Symbol varDeclaration() {
        Type varType = type();
        Symbol var = null;

        // If block start is reached, exit immediately, so it's not skipped
        // with rest of check()'s
        // After this method there is usually a loop that jumps to
        // start of the block on error and will not find it if it is entered now
        if (kind == LBRACE) return null;

        do {
            if (kind == COMMA) scan();

            if (kind != IDENT) {
                error("Variable name expected, got: " + kind);
            } else if (varType != null && varType.typeKind != TypeKind.NOTYPE) {
                var = symbolTable.insert(token.text, SymbolKind.VAR, varType);
            }

            scan();
        } while (kind == COMMA);

        if (kind == ASSIGN) {
            error("You can't assign values here");
        } else {
            check(SEMICOLON);
        }

        return var;
    }

    private static Type type() {
        Type type = null;
        String name = token.text;

        if (kind != IDENT) {
            error("Type name expected");
        } else {
            Symbol typeSymbol = symbolTable.find(name);
            if (typeSymbol == null || typeSymbol.symbolKind != SymbolKind.TYPE) {
                error('\'' + name + '\'' + " is not a type");
            } else {
                type = typeSymbol.symbolType;
            }
        }

        scan();

        if (kind == LBRACK) {
            scan();
            if (type != null) {
                String typeName = name + "[]";
                Symbol symbol = symbolTable.find(typeName);
                if (symbol == null) {
                    symbol = symbolTable.insert(
                                typeName,
                                SymbolKind.TYPE,
                                new Type(TypeKind.REFERENCE));

                    symbol.symbolType.arrayType = type;
                    symbol.symbolType.name = typeName;
                }

                type = symbol.symbolType;
            }

            check(RBRACK);
        }

        return type;
    }

    /**
     *  ConstDecl = "final" Type ident "=" (number | charConst) ";".
     */
    private static Symbol constDeclaration() {
        check(CONST);
        Type type = type();
        Symbol symbol = null;

        if (kind != IDENT) {
            error(IDENT + " expected");
        } else if (type.typeKind != TypeKind.CHAR && type.typeKind != TypeKind.INT) {
            error(type.typeKind + " cannot be constant");
        } else {
            symbol = symbolTable.insert(token.text, SymbolKind.CONST, type);
        }

        scan();
        if (kind != ASSIGN) error("You forgot to initialize a constant");

        scan();
        if (kind != NUMBER && kind != CHARACTER) error("Value expected");

        scan();
        check(SEMICOLON);

        return symbol;
    }

    private static void block() {
        /* Print new errors on block enter */
        lastError = ERROR_IGNORE_DISTANCE;

        check(LBRACE);

        symbolTable.openScope(null);

        while (kind != RBRACE && kind != EOF) {
            if  (firstStatement.contains(kind)) {
                statement();
            } else {
                error("Illegal start of statement with " + kind);

                while (!firstStatement.contains(kind) && kind != EOF && kind != RBRACE) {
                    scan();
                }
            }
        }

        check(RBRACE);

        symbolTable.closeScope();

        /* After exiting the block, print new errors */
        lastError = ERROR_IGNORE_DISTANCE;
    }

    private static void statement() {
        switch (kind) {
            case IDENT:
                String designatorName = token.text;
                Symbol designator = designator();

                switch (kind) {
                    case ASSIGN:
                        scan();

                        if (designator.symbolKind != SymbolKind.VAR) {
                            error(designator.symbolKind + " cannot be used here");
                        }

                        Symbol expression = expression();
                        if (!expression.assignableTo(designator)) {
                            error(String.format("Expression of type '%s' not assignable to '%s'",
                                    expression.symbolType, designator));
                        }

                        check(SEMICOLON);
                        break;

                    case LPAREN:
                        scan();

                        if (designator.symbolKind != SymbolKind.FUNCTION) {
                            error(designator.symbolKind + " is not a " + SymbolKind.FUNCTION);
                        }

                        actualParameters(symbolTable.find(designatorName));
                        check(RPAREN);
                        check(SEMICOLON);
                        break;

                    case INC:
                    case DEC:
                        scan();

                        if (designator.symbolKind != SymbolKind.VAR) {
                            error("Cant do arithmetic operations on a " + designator.symbolKind);
                        }

                        check(SEMICOLON);
                        break;

                    default:
                        error(kind + " not expected right after identifier");
                }
                break;

            case RETURN:
                scan();
                Symbol scopeFunction = symbolTable.getScopeFunction();

                if (kind == SEMICOLON) { // No return value
                    if (scopeFunction.symbolType != null && scopeFunction.symbolType.typeKind != TypeKind.NOTYPE) {
                        error("Return value expected");
                    }
                    break;
                }

                if (scopeFunction.symbolType.typeKind == TypeKind.NOTYPE) {
                    error("Return value not expected, function is of type void");
                } else {
                    Symbol expression = expression();
                    if (!expression.assignableTo(scopeFunction)) {
                        error(String.format("Return type %s expected, got %s",
                                scopeFunction.symbolType.name,
                                expression.symbolType.name));
                    }
                }

                check(SEMICOLON);
                break;

            case LBRACE:
                block();
                break;

            case IF:
                scan();
                check(LPAREN);
                condition();
                check(RPAREN);
                statement();
                if (kind == ELSE) {
                    scan();
                    statement();
                }
                break;

            case WHILE:
                scan();
                check(LPAREN);
                condition();
                check(RPAREN);
                symbolTable.setNextScopeIsLoop();
                statement();
                break;

            case READ:
                scan();
                check(LPAREN);
                designator();
                check(RPAREN);
                check(SEMICOLON);
                break;

            case PRINT:
                scan();
                check(LPAREN);
                expression();
                while (kind == COMMA) {
                    scan();
                    check(NUMBER);
                }
                check(RPAREN);
                check(SEMICOLON);
                break;

            case BREAK:
                scan();
                check(SEMICOLON);
                if (!symbolTable.isScopeLoop()) {
                    error(BREAK + " can only be used inside loops");
                }
                break;

            case SEMICOLON: scan(); break;

            default:
                error(kind + " not expected here");
        }
    }

    private static void actualParameters(Symbol function) {
        if (function.symbolKind == SymbolKind.FUNCTION && function.parameters.size() > 0 && kind == RPAREN) {
            error("Parameters expected: " +
                    function.parameters.stream().map(s -> s.symbolType.name).collect(Collectors.toList()));
            return;
        } else if (kind == RPAREN) {
            return;
        }

        Iterator<Symbol> formalParameters = function.parameters.iterator();
        Symbol parameter, expression;

        do {
            if (kind == COMMA) scan();

            expression = expression();

            if (formalParameters.hasNext()) {
                parameter = formalParameters.next();

                if (!expression.assignableTo(parameter)) {
                    error(String.format("Parameter types do not match, expected %s but got %s",
                            parameter.symbolType.name,
                            expression.symbolType.name));
                }
            } else {
                error(String.format("Too manny parameters provided, function %s requires %d with types: %s",
                        function.name, function.parameters.size(), function.parameters));
            }

        } while(kind == COMMA);
    }

    private static LinkedList<Symbol> formalParameters() {
        LinkedList<Symbol> params = new LinkedList<>();

        if (kind == RPAREN) return params;

        do {
            if (kind == COMMA) scan();

            Type paramType = type();

            if (kind == IDENT) {
                Symbol param = symbolTable.insert(token.text, SymbolKind.VAR, paramType);

                if (param != null) {
                    params.addLast(param);
                }
            } else if (kind != COMMA && kind != RPAREN) {
                error(String.format("%s not expected here, expected tokens: %s, %s",
                        kind, COMMA, RPAREN));
            }

            scan();
        } while (kind == COMMA);

        return params;
    }

    private static Symbol function() {
        Type functionType = new Type(TypeKind.NOTYPE);
        Symbol function = new Symbol();

        if      (kind == VOID)  scan();
        else if (kind == IDENT) functionType = type();
        else                    error("Function return type expected, got: " + kind);

        if (kind != IDENT) {
            error("Function name expected, got" + kind);
        } else {
            function = symbolTable.insert(token.text, SymbolKind.FUNCTION, functionType);
        }

        scan();

        check(LPAREN);

        symbolTable.openScope(function);

        function.parameters = formalParameters();

        check(RPAREN);

        if (kind != IDENT && kind != LBRACE) {
            error(kind + " not allowed in here");
        }

        while (kind == IDENT)
            varDeclaration();

        /* If there was an error, skip to start of the block */
        while (kind != LBRACE && kind != EOF)
            scan();

        block();

        symbolTable.closeScope();

        return function;
    }

    private static Symbol structDeclaration() {
        Symbol symbol = null;

        check(STRUCT);

        if (kind != IDENT) {
            error("struct identifier expected");
        } else {
            Type newType = new Type(TypeKind.REFERENCE);
            newType.name = token.text;
            symbol = symbolTable.insert(token.text, SymbolKind.TYPE, newType);
        }

        scan();
        check(LBRACE);

        HashMap<String, Symbol> fields = new HashMap<>();

        while (kind == IDENT) {
            Symbol field = varDeclaration();
            if (field != null) {
                fields.put(field.name, field);
            }
        }

        if (symbol != null) {
            symbol.symbolType.fields = fields;
        }

        check(RBRACE);

        return symbol;
    }

    private static Symbol designator() {
        Symbol designator = null;

        check(IDENT);
        if (prevToken.kind == IDENT) {
            designator = symbolTable.find(prevToken.text);
            if (designator == null) {
                error(prevToken.text + " not in scope");
                designator = new Symbol();
            }
        }

        while (kind == PERIOD || kind == LBRACK) {
            if (kind == PERIOD) {
                scan();
                check(IDENT);

                if (designator != null) {
                    if (!designator.symbolType.isStruct()) {
                        error(designator + " has no accessible fields");
                    } else if (prevToken.kind == IDENT) {
                        designator = designator.symbolType.fields.get(prevToken.text);
                        if (designator == null) {
                            error(String.format("Cannot find field " + token.text));
                        }
                    }
                }
            } else {
                scan();
                Symbol expression = expression();

                if (designator != null) {
                    if (!designator.symbolType.isArray()) {
                        error("Array type expected, found " + expression.symbolType);
                    } else if (expression.symbolType.typeKind != TypeKind.INT) {
                        error("Expression of type " + TypeKind.INT + " expected");
                    } else {
                        Type arrayType = designator.symbolType.arrayType;
                        designator = designator.copy();
                        designator.symbolType = arrayType;
                    }
                }

                check(RBRACK);
            }
        }

        return designator;
    }

    private static Symbol factor() {
        Symbol symbol = new Symbol();
        symbol.symbolType = new Type(TypeKind.NOTYPE);

        switch (kind) {
            case IDENT:
                symbol = designator();
                if (symbol.symbolKind == SymbolKind.FUNCTION) {
                    check(LPAREN);
                    actualParameters(symbolTable.find(symbol.name));
                    check(RPAREN);
                }
                break;

            case CHARACTER:
                symbol.symbolType = new Type(TypeKind.CHAR);
                scan();
                break;

            case NUMBER:
                symbol.symbolType = new Type(TypeKind.INT);
                scan();
                break;

            case NEW:
                scan();
                check(IDENT);
                if (prevToken.kind == IDENT) {
                    symbol = symbolTable.find(prevToken.text);
                    if (symbol == null) {
                        error(prevToken.text + " not in scope");
                        symbol = new Symbol();
                    } else if (symbol.symbolKind != SymbolKind.TYPE) {
                        error(prevToken.text + " is not a type");
                    }
                }

                if (kind == LBRACK) {
                    scan();
                    symbol = symbolTable.find(symbol.symbolType.name + "[]");

                    Symbol arraySize = expression();
                    if (arraySize.symbolType.typeKind != TypeKind.INT) {
                        error("Expression of type " + TypeKind.INT + " expected");
                    }

                    check(RBRACK);
                }
                break;

            case LPAREN:
                scan();
                expression();
                check(RPAREN);
                break;

            default:
                error(kind + " not expected in an expression here");
        }

        return symbol;
    }

    private static Symbol term() {
        Symbol factor1 = factor();
        Symbol factorN;

        if (kind == ASTERISK || kind == MOD || kind == SLASH) {
            if (!factor1.symbolType.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + factor1 + "'");
            }
        }

        while (kind == ASTERISK || kind == MOD || kind == SLASH) {
            scan();
            factorN = factor();

            if (!factorN.symbolType.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + factorN + "'");
            }
        }

        return factor1;
    }

    private static Symbol expression() {
        if (kind == MINUS) scan();

        Symbol term1 = term();
        Symbol termN;

        if (kind == PLUS || kind == MINUS) {
            if (!term1.symbolType.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + term1 + "'");
            }
        }

        while (kind == PLUS || kind == MINUS) {
            scan();
            termN = term();

            if (termN != null && !termN.symbolType.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + termN + "'");
            }
        }

        return term1;
    }

    private static void conditionFactor() {
        expression();

        if (kind == EQ  ||
            kind == NEQ ||
            kind == GRE ||
            kind == GEQ ||
            kind == LES ||
            kind == LEQ ) {
            scan();
            expression();
        }
    }

    private static void conditionTerm() {
        conditionFactor();

        while (kind == AND) {
            scan();
            conditionFactor();
        }
    }

    private static void condition() {
        conditionTerm();

        while (kind == OR) {
            scan();
            conditionTerm();
        }
    }
}
