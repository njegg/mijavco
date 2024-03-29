package parser;

import codegen.*;
import compiler.Mijavco;
import scanner.Scanner;
import scanner.Token;
import scanner.TokenKind;

import java.util.*;
import java.util.stream.Collectors;

import static scanner.TokenKind.*;

public class Parser {
    public static SymbolTable symbolTable;

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
            Mijavco.error();
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
            if      (kind == IDENT)  varDeclaration(true);
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
                            new Type(TypeKind.REFERENCE),
                            token
                    );

                    symbol.symbolType.arrayType = type;
                    symbol.symbolType.name = typeName;
                }

                type = symbol.symbolType;
            }

            check(RBRACK);
        }

        return type;
    }

    private static void varDeclaration(boolean isGlobal) {
        Type varType = type();
        Symbol var = null;

        // If block start is reached, exit immediately, so it's not skipped
        // with rest of check()'s
        // After this method there is usually a loop that jumps to
        // start of the block on error and will not find it if it is entered now
        if (kind == LBRACE) return;

        do {
            if (kind == COMMA) scan();

            if (kind != IDENT) {
                error("Variable name expected, got: " + kind);
            } else if (varType != null && varType.typeKind != TypeKind.NOTYPE) {
                var = symbolTable.insert(token.text, SymbolKind.VAR, varType, token);
            }

            scan();
        } while (kind == COMMA);

        if (kind == ASSIGN) {
            error("You can't assign values here");
        } else {
            check(SEMICOLON);
        }

        if (var != null) var.isGlobal = isGlobal;
    }

    private static void constDeclaration() {
        check(CONST);
        Type type = type();
        Symbol symbol = new Symbol();

        if (kind != IDENT) {
            error(IDENT + " expected");
        } else if (type.typeKind != TypeKind.CHAR && type.typeKind != TypeKind.INT) {
            error(type.typeKind + " cannot be constant");
        } else {
            symbol = symbolTable.insert(token.text, SymbolKind.NOSYM, type, token);
            symbol.symbolKind = SymbolKind.CONST;
        }

        scan();
        if (kind != ASSIGN) error("You forgot to initialize a constant");

        scan();
        if (kind != NUMBER && kind != CHARACTER) {
            error("Value expected");
        } else {
            symbol.value = token.value;
        }

        scan();
        check(SEMICOLON);
    }

    private static void structDeclaration() {
        Symbol symbol = null;

        check(STRUCT);

        if (kind != IDENT) {
            error("struct identifier expected");
        } else {
            Type newType = new Type(TypeKind.REFERENCE);
            newType.name = token.text;
            symbol = symbolTable.insert(token.text, SymbolKind.TYPE, newType, token);

            Type newArrType = new Type(TypeKind.REFERENCE);
            newArrType.name = newType.name + "[]";
            newArrType.arrayType = newType;
            symbolTable.insert(newArrType.name, SymbolKind.TYPE, newArrType, null);
        }

        scan();
        check(LBRACE);

        HashMap<String, Symbol> fields = new HashMap<>();

        while (kind == IDENT) {
            Symbol field = fieldDeclaration();
            if (field != null) {
                fields.put(field.name, field);
            }
        }

        if (symbol != null) {
            int address = 0;
            for (Symbol field : fields.values()) {
                field.address = address++;
            }

            symbol.symbolType.fields = fields;
            symbol.symbolType.sizeInBytes = fields.size() * 4;

            if (fields.size() > 256) {
                error(String.format("%s has too many fields, it has %d but max is 256 %n",
                    symbol.name, fields.size()));
            }
        }

        check(RBRACE);
    }

    private static Symbol fieldDeclaration() {
        Type fieldType = type();
        Symbol field = null;

        if (kind == LBRACE) return null;

        if (kind != IDENT) {
            error("Variable name expected, got: " + kind);
        } else if (fieldType != null && fieldType.typeKind != TypeKind.NOTYPE) {
            field = new Symbol(token);
            field.name = token.text;
            field.symbolKind = SymbolKind.VAR;
            field.symbolType =  fieldType;
        }

        scan();

        if (kind == ASSIGN) {
            error("You can't assign values here");
        } else {
            check(SEMICOLON);
        }

        return field;
    }


    private static void function() {
        Type functionType = new Type(TypeKind.NOTYPE);
        Symbol function = new Symbol(token);

        if      (kind == VOID)  scan();
        else if (kind == IDENT) functionType = type();
        else                    error("Function return type expected, got: " + kind);

        if (kind != IDENT) {
            error("Function name expected, got" + kind);
        } else {
            function = symbolTable.insert(token.text, SymbolKind.FUNCTION, functionType, token);
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
            varDeclaration(false);

        /* If there was an error, skip to start of the block */
        while (kind != LBRACE && kind != EOF)
            scan();

        if (function.name.equals("main")) {
            CodeBuffer.mainStart = CodeBuffer.pc;
        }

        function.address = CodeBuffer.pc;

        int parameterSize = function.parameters.size();
        CodeBuffer.putByte(Instruction.ENTER);
        CodeBuffer.putByte(parameterSize);
        CodeBuffer.putByte(symbolTable.numberOfLocals() - parameterSize);

        block();

        if (function.symbolType.typeKind == TypeKind.NOTYPE) {
            CodeBuffer.putByte(Instruction.EXIT);
            CodeBuffer.putByte(Instruction.RETURN);
        } else {
            CodeBuffer.putByte(Instruction.TRAP); // A trap for forgotten return
            CodeBuffer.putByte(1);
        }

        symbolTable.closeScope();
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
        Symbol parameter;
        Operand expression;

        do {
            if (kind == COMMA) scan();

            expression = expression(); // Also loads the value to eStack

            if (formalParameters.hasNext()) {
                parameter = formalParameters.next();

                if (!expression.type.assignableTo(parameter.symbolType)) {
                    error(String.format("Parameter types do not match, expected %s but got %s",
                            parameter.symbolType.name,
                            expression.type.name));
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
                Symbol param = symbolTable.insert(token.text, SymbolKind.VAR, paramType, token);

                if (param != null) {
                    params.addLast(param);
                } else {
                    Symbol dummy = new Symbol(); // For error checking later in the program
                    dummy.symbolType = paramType;
                    params.addLast(dummy);
                }
            } else if (kind != COMMA && kind != RPAREN) {
                error(String.format("%s not expected here, expected tokens: %s, %s",
                        kind, COMMA, RPAREN));
            }

            scan();
        } while (kind == COMMA);

        return params;
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
                Operand designatorOperand = designator();
                Symbol designator = designatorOperand.symbol;

                switch (kind) {
                    case ASSIGN:
                        scan();

                        if (designator.symbolKind != SymbolKind.VAR) {
                            error(designator.symbolKind + " cannot be used here");
                        }

                        Operand expression = expression();
                        if (!expression.type.assignableTo(designator.symbolType)) {
                            error(String.format("Expression of type '%s' not assignable to '%s'",
                                    expression.type, designator));
                        } else {
                            CodeBuffer.store(designatorOperand, expression);
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


                        if (designator.name.equals("len")) {
                            CodeBuffer.putByte(Instruction.LENGTH);
                        } else if (designator.name.equals("rand")) {
                            CodeBuffer.putByte(Instruction.RAND);
                        } else if (!designator.name.equals("ctoi") && !designator.name.equals("itoc")) {
                            CodeBuffer.putByte(Instruction.CALL);
                            CodeBuffer.putShort(designator.address);
                        }

                        if (designator.symbolType.typeKind != TypeKind.NOTYPE)
                            CodeBuffer.putByte(Instruction.POP); // Get return value

                        break;

                    case INC:
                    case DEC:
                        scan();

                        if (designator.symbolKind != SymbolKind.VAR) {
                            error("Cant do arithmetic operations on a " + designator.symbolKind);
                        }

                        OperandKind designatorKind = designatorOperand.kind;
                        CodeBuffer.load(designatorOperand);
                        CodeBuffer.putByte(prevToken.kind == INC ? Instruction.CONST_1 : Instruction.CONST_M1);
                        CodeBuffer.putByte(Instruction.ADD);
                        designatorOperand.kind = designatorKind;
                        CodeBuffer.store(designatorOperand, null);

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
                } else if (scopeFunction.symbolType.typeKind == TypeKind.NOTYPE) {
                    error("Cannot return value when function is void");
                } else {
                    Operand expression = expression();

                    if (!expression.type.assignableTo(scopeFunction.symbolType)) {
                        error(String.format("Return type %s expected, got %s",
                                scopeFunction.symbolType.name,
                                expression.type.name));
                    }

                    CodeBuffer.load(expression);
                }

                CodeBuffer.putByte(Instruction.EXIT);
                CodeBuffer.putByte(Instruction.RETURN);

                check(SEMICOLON);
                break;

            case LBRACE:
                block();
                break;

            case IF:
                scan();
                check(LPAREN);
                Operand ifCondition = condition();
                check(RPAREN);

                CodeBuffer.falseJump(ifCondition);
                ifCondition.trueLabel.here();

                statement();

                ifCondition.falseLabel.here();

                if (kind == ELSE) {
                    scan();
                    statement();
                }

                break;

            case WHILE:
                scan();
                Label whileTop = new Label();           // If true, this is where it should return after statements
                whileTop.here();
                check(LPAREN);
                Operand whileCondition = condition();
                check(RPAREN);

                CodeBuffer.falseJump(whileCondition);   // If false, jump forward somewhere

                symbolTable.setNextScopeIsLoop();       // TODO: This wont work for non block statement ?
                statement();

                CodeBuffer.jump(whileTop);              // If it was true, go to top of while
                whileCondition.falseLabel.here();       // If false, this is where to jump

                break;

            case READ:
                scan();
                check(LPAREN);

                Symbol readDesignator = designator().symbol;
                TypeKind readType = readDesignator.symbolType.typeKind;
                if (readType != TypeKind.INT && readType != TypeKind.CHAR) {
                    error("Can only read characters and numbers, type " + readDesignator.symbolType.typeKind + " can't");
                } else {
                    CodeBuffer.putByte(readType == TypeKind.INT ? Instruction.READ : Instruction.BREAD);
                }

                check(RPAREN);
                check(SEMICOLON);
                break;

            case PRINT:
                scan();
                check(LPAREN);

                Operand printExpression = expression();
                TypeKind printType = printExpression.type.typeKind;
                boolean isString = symbolTable.find("char[]").symbolType.equals(printExpression.type);

                if (printType != TypeKind.INT && printType != TypeKind.CHAR && !isString) {
                    error("Can only print characters and numbers, type " + printExpression.type.typeKind + " can't");
                } else {
                    if (isString) {
                        CodeBuffer.putByte(Instruction.PRINTS);
                    } else {
                        CodeBuffer.putByte(printType == TypeKind.INT ? Instruction.PRINT : Instruction.BPRINT);
                    }
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

    private static Operand designator() {
        Symbol designator = null;
        Operand operand = null;

        check(IDENT);
        if (prevToken.kind == IDENT) {
            designator = symbolTable.find(prevToken.text);
            if (designator == null) {
                error(prevToken.text + " not in scope");
                designator = new Symbol(token);
            }
            operand = new Operand(designator);
        }

        while (kind == PERIOD || kind == LBRACK) {
            if (kind == PERIOD) {
                scan();
                check(IDENT);

                if (designator != null) {
                    if (!designator.symbolType.isStruct()) {
                        error(designator + " has no accessible fields");
                    } else if (prevToken.kind == IDENT) {
                        // Designator is now going to be a field
                        designator = symbolTable.findField(prevToken.text, designator.symbolType);

                        if (designator == null) {
                            error(String.format("Error obtaining the field '" + prevToken.text + "'"));
                        } else {
                            CodeBuffer.load(operand);               // Address of struct
                            operand = new Operand(designator);      // Operand is now a field not a struct pointer
                            operand.kind = OperandKind.STRUCT_FIELD;
                        }
                    }
                }
            } else {
                if (operand != null) {
                    CodeBuffer.load(operand); // Load the pointer to array before expression loads index
                }

                scan();
                Operand expression = expression();

                if (designator != null) {
                    if (!designator.symbolType.isArray()) {
                        error("Array type expected, found " + expression.type);
                    } else if (expression.type.typeKind != TypeKind.INT) {
                        error("Expression of type " + TypeKind.INT + " expected");
                    } else {
                        Type arrayType = designator.symbolType.arrayType;
                        designator = designator.copy();
                        designator.symbolType = arrayType;

                        CodeBuffer.load(expression); // Load index

                        operand = new Operand(designator);
                        operand.kind = OperandKind.ARRAY_ELEMENT;
                    }
                }

                check(RBRACK);
            }
        }

        return operand;
    }

    private static Operand factor() {
        Symbol symbol = new Symbol(token);
        symbol.symbolType = new Type(TypeKind.NOTYPE);
        Operand operand = null;

        switch (kind) {
            case IDENT:
                operand = designator();
                symbol = operand.symbol;

                if (symbol.symbolKind == SymbolKind.FUNCTION) {
                    check(LPAREN);
                    actualParameters(symbolTable.find(symbol.name));
                    check(RPAREN);

                    if (symbol.name.equals("len")) {
                        CodeBuffer.putByte(Instruction.LENGTH);
                    } else if (symbol.name.equals("rand")) {
                        CodeBuffer.putByte(Instruction.RAND);
                    } else if (!symbol.name.equals("ctoi") && !symbol.name.equals("itoc")) {
                        CodeBuffer.putByte(Instruction.CALL);
                        CodeBuffer.putShort(symbol.address);
                    }
                } else if (symbol.symbolKind == SymbolKind.CONST) {
                    operand.kind = OperandKind.CONSTANT;
                } else if (operand.kind != OperandKind.STRUCT_FIELD && operand.kind != OperandKind.ARRAY_ELEMENT) {
                    operand.kind = operand.symbol.isGlobal ? OperandKind.GLOBAL : OperandKind.LOCAL;
                }

                break;

            case CHARACTER:
                symbol.symbolType = new Type(TypeKind.CHAR);
                symbol.value = token.value;
                symbol.symbolKind = SymbolKind.CONST;
                operand = new Operand(symbol);
                scan();
                break;

            case NUMBER:
                symbol.symbolType = new Type(TypeKind.INT);
//                symbol.value = token.value * (prevToken.kind == MINUS ? -1 : 1);
                symbol.value = token.value;
                symbol.symbolKind = SymbolKind.CONST;
                operand = new Operand(symbol);
                scan();
                break;

            case STRING:
                CodeBuffer.putByte(Instruction.LOAD_STRING);
                CodeBuffer.putWord(token.value);
                for (char c : token.text.toCharArray())
                    CodeBuffer.putByte(c);
                CodeBuffer.putByte('\0');

                operand = new Operand(symbolTable.find("char[]"));

                scan();
                break;

            case NEW:
                scan();

                if (kind == IDENT) {
                    symbol = symbolTable.find(token.text);
                    if (symbol == null) {
                        error(token.text + " not in scope");
                        symbol = new Symbol();
                    } else if (symbol.symbolKind != SymbolKind.TYPE) {
                        error(token.text + " is not a type");
                        symbol = new Symbol();
                    }
                }

                scan();

                if (kind == LBRACK) {
                    scan();

                    Operand arraySize = expression(); // Will load the size to CodeBuffer
                    if (arraySize.type.typeKind != TypeKind.INT) {
                        error("Expression of type " + TypeKind.INT + " expected");
                    }

                    CodeBuffer.putByte(Instruction.NEW_ARRAY);
                    CodeBuffer.putByte(symbol.symbolType.sizeInBytes);

                    symbol = symbolTable.find(symbol.symbolType.name + "[]");

                    if (symbol == null) {
                        symbol = new Symbol();
                    }

                    check(RBRACK);
                } else {
                    CodeBuffer.putByte(Instruction.NEW);
                    CodeBuffer.putByte(symbol.symbolType.fields.size());
                }

                operand = new Operand(symbol);

                break;

            case LPAREN:
                scan();
                operand = expression();
                check(RPAREN);
                break;

            default:
                error(kind + " not expected in an expression here");
        }

        if (operand != null) CodeBuffer.load(operand);
        else return new Operand(new Symbol(token));

        return operand;
    }

    private static Operand term() {
        Operand factor1 = factor();
        Operand factorN;

        if (kind == ASTERISK || kind == MOD || kind == SLASH) {
            if (!factor1.type.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + factor1.symbol + "'");
            }
        }

        while (kind == ASTERISK || kind == MOD || kind == SLASH) {
            TokenKind op = kind;

            scan();
            factorN = factor();

            if      (op == ASTERISK) CodeBuffer.putByte(Instruction.MUL);
            else if (op == SLASH)    CodeBuffer.putByte(Instruction.DIV);
            else                     CodeBuffer.putByte(Instruction.REM);

            if (!factorN.type.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + factorN.symbol + "'");
            }
        }

        return factor1;
    }

    private static Operand expression() {
        boolean negate = false;
        if (kind == MINUS) {
            negate = true;
            scan();
        }

        Operand term1 = term();
        Operand termN;

        if (kind == PLUS || kind == MINUS) {
            if (!term1.type.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + term1.symbol + "'");
            }
        }

        while (kind == PLUS || kind == MINUS) {
            TokenKind op = kind;

            scan();
            termN = term();

            if (op == PLUS) CodeBuffer.putByte(Instruction.ADD);
            else            CodeBuffer.putByte(Instruction.SUB);

            if (!termN.type.usedInArithmetics()) {
                error("Cannot do arithmetic operations on '" + termN.symbol + "'");
            }
        }

        if (negate) CodeBuffer.putByte(Instruction.NEG);

        return term1;
    }


    private static Operand conditionFactor() {
        expression();

        switch (kind) {
            case EQ: case GE: case GT: case LT: case LE: case NE:
                Operand operand = new Operand(kind);
                scan();
                expression();
                return operand;

            default:
                CodeBuffer.load(new Operand(0)); // if (a) -> if (a != 0) -> load a; const_0; jeq 0
                return new Operand(NE);
        }
    }

    private static Operand conditionTerm() {
        Operand operand = conditionFactor();


        while (kind == AND) {
            CodeBuffer.falseJump(operand); // if one factor is false, lazily skip the whole term

            scan();
            operand.condition = conditionFactor().condition;
        }

        return operand;
    }

    private static Operand condition() {
        Operand prev = conditionTerm();

        while (kind == OR) {
            CodeBuffer.trueJump(prev); // If one is term is true, while condition is true
            prev.falseLabel.here(); // a && b || c - if a was false, c is the place to jump (here)

            scan();
            Operand curr = conditionTerm();

            prev.condition = curr.condition;
            prev.falseLabel = curr.falseLabel;
        }

        return prev;
    }
}
