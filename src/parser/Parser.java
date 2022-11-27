package parser;

import compiler.Main;
import scanner.Scanner;
import scanner.Token;
import scanner.TokenKind;

import java.util.EnumSet;

import static scanner.TokenKind.*;

public class Parser {
    private static SymbolTable symbolTable;

    private static TokenKind kind;
    private static Token token;

    private static int lastError = 0;
    private static final int errorIgnoreDistance = 3;

    private static EnumSet<TokenKind> firstStatement;

    public static void parse() {
        symbolTable = new SymbolTable();
        firstStatement = EnumSet.of(IDENT, IF, WHILE, BREAK, RETURN, READ, PRINT, LBRACE, SEMICOLON);

        scan();
        mijava();
    }

    private static void scan() {
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

    private static void error(String message) {
        if (lastError >= errorIgnoreDistance) {
            System.err.printf("%s:%d:%d : %s\n", Scanner.getFilePath(), token.line, token.column, message);
            Main.error();
        }

        lastError = 0;
    }

    /**
     *  <pre>
     *  Program = "program" ident
     *            {ConstDecl | VarDecl | ClassDecl}
     *            "{" {MethodDecl} "}".
     *  </pre>
     */
    private static void mijava() {
        check(PROGRAM);
        check(IDENT);

        while (kind == IDENT || kind == CONST || kind == STRUCT) {
            if      (kind == IDENT) varDeclaration();
            else if (kind == CONST) constDeclaration();
            else                    structDeclaration();
        }

        /* If there was an error, skip to start of program block*/
        while (kind != LBRACE && kind != EOF)
            scan();

        check(LBRACE);

        while (kind == IDENT || kind == VOID)
            method();

        check(RBRACE);

        check(EOF);
    }

    /**
     *  <pre>
     *  VarDecl = Type ident {"," ident} ";".
     *  </pre>
     */
    private static void varDeclaration() {
        Type varType = type();

        do {
            if (kind == COMMA) scan();

            if (kind != IDENT) {
                error("Variable name expected, got: " + kind);
                break;
            } else if (varType.typeKind != TypeKind.NOTYPE) {
                Symbol var = symbolTable.insert(token.text, SymbolKind.VAR, varType);
                if (var == null) {
                    error(String.format("Name '%s' already in use", token.text));
                    break;
                }
            }

            scan();
        } while (kind == COMMA);

        if (kind == ASSIGN) {
            error("You can't assign values here");
        } else {
            check(SEMICOLON);
        }
    }

    private static Type type() {
        Type type = new Type();
        type.typeKind = TypeKind.NOTYPE;

        if (kind != IDENT) {
            error("Type name expected");
        } else {
            Symbol typeSymbol = symbolTable.find(token.text);
            if (typeSymbol == null || typeSymbol.symbolKind != SymbolKind.TYPE) {
                error('\'' + token.text + '\'' + " is not a type");
            } else {
                type.typeKind = typeSymbol.symbolType.typeKind;
            }
        }

        scan();

        if (kind == LBRACK) {
            scan();
            type.arrayTypeKind = type.typeKind;
            type.typeKind = TypeKind.ARRAY;
            check(RBRACK);
        }

        return type;
    }

    /**
     *  ConstDecl = "final" Type ident "=" (number | charConst) ";".
     */
    private static void constDeclaration() {
        check(CONST);
        type();
        check(IDENT);

        if (kind != ASSIGN)
            error("You forgot to initialize a constant");

        scan();

        if (kind != NUMBER && kind != CHARACTER) {
            error("Value expected");
        }

        scan();

        check(SEMICOLON);
    }

    private static void block() {
        /* Print new errors on block enter */
        lastError = errorIgnoreDistance;

        check(LBRACE);

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

        /* After exiting the block, print new errors */
        lastError = errorIgnoreDistance;
    }

    private static void statement() {
        switch (kind) {
            case IDENT:
                designator();

                switch (kind) {
                    case ASSIGN:
                        scan();
                        expression();
                        check(SEMICOLON);
                        break;

                    case LPAREN:
                        scan();
                        if (kind != RPAREN) actualParameters();
                        check(RPAREN);
                        check(SEMICOLON);
                        break;

                    case INC:
                    case DEC:
                        scan();
                        check(SEMICOLON);
                        break;

                    default:
                        error(kind + " not expected right after identifier");
                }
                break;

            case RETURN:
                scan();
                if (kind == SEMICOLON) break; // No return value
                expression();
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

            case BREAK: check(SEMICOLON); break;
            case SEMICOLON: scan(); break;

            default:
                error(kind + " not expected here");
        }
    }

    private static void actualParameters() {
        expression();
        while(kind == COMMA)
            expression();
    }

    private static void formalParameters() {
        type();
        check(IDENT);
        while (kind == COMMA) {
            scan();
            type();
            check(IDENT);
        }
    }

    private static void method() {
        if      (kind == VOID)  scan();
        else if (kind == IDENT) type();
        else                    error("Method return type expected");

        check(IDENT);

        check(LPAREN);
        if (kind == IDENT) formalParameters();
        check(RPAREN);

        while (kind == IDENT)
            varDeclaration();

        /* If there was an error, skip to start of the block */
        while (kind != LBRACE && kind != EOF)
            scan();

        block();
    }

    private static void structDeclaration() {
        check(STRUCT);
        check(IDENT);
        check(LBRACE);

        while (kind == IDENT)
            varDeclaration();

        check(RBRACE);
    }

    /**
     * <pre>
     * Designator = ident { "." ident | "[" Expr "]" }.
     * </pre>
     */
    private static void designator() {
        check(IDENT);

        while (kind == PERIOD || kind == LBRACK) {
            if (kind == PERIOD) {
                scan();
                check(IDENT);
            } else {
                scan();
                expression();
                check(RBRACK);
            }
        }
    }

    private static void factor() {
        switch (kind) {
            case IDENT:
                designator();
                if (kind == LPAREN) {
                    scan();
                    actualParameters();
                    check(RPAREN);
                }
                break;

            case CHARACTER:
            case NUMBER: scan(); break;

            case NEW:
                scan();
                check(IDENT);
                if (kind == LBRACK) {
                    scan();
                    expression();
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
    }

    private static void term() {
        factor();

        while (kind == ASTERISK || kind == MOD || kind == SLASH) {
            scan();
            factor();
        }
    }

    private static void expression() {
        if (kind == MINUS) scan();

        term();
        while (kind == PLUS || kind == MINUS) {
            scan();
            term();
        }
    }

    /**
     * cond: t {|| t}
     * term: f {&& f}
     * fact: expr [relop expr]
     */
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
