package Parser;

import Scanner.Scanner;
import Scanner.Token;
import Scanner.TokenKind;

import java.util.EnumSet;

import static Scanner.TokenKind.*;

public class Parser {
    private static TokenKind kind;
    private static Token token;

    private static int errors = 0;
    private static int lastError = 0;

    private static EnumSet<TokenKind> firstStatement;

    public static void parse() {
        firstStatement = EnumSet.of(IDENT, IF, WHILE, BREAK, RETURN, READ, PRINT, LBRACE, SEMICOLON);

        scan();
        mijava();

        System.out.println("Number of errors:" + errors);
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
        } else {
            error(expected + " expected, got " + kind);
        }

        lastError++;
    }

    private static void error(String message) {
        if (lastError > 3) {
            System.err.printf("%s:%d:%d : %s\n", Scanner.getFilePath(), token.line, token.column, message);
            errors++;
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

        while (kind == IDENT || kind == CONST || kind == CLASS) {
            if      (kind == IDENT) varDeclaration();
            else if (kind == CONST) constDeclaration();
            else                    classDeclaration();
        }

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
        type();
        check(IDENT);

        while (kind == COMMA) {
            scan();
            check(IDENT);
        }

        if (kind == ASSIGN) {
            error("You can assign values here");
            scan();
        } else {
            check(SEMICOLON);
        }
    }

    private static void type() {
        check(IDENT);
        if (kind == LBRACK) {
            scan();
            check(RBRACK);
        }
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
                        actualParameters();
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
        while(kind == SEMICOLON)
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

        block();
    }

    private static void classDeclaration() {
        check(CLASS);
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

            case LBRACE:
                scan();
                expression();
                check(RBRACE);
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

    private static void conditionFactor() {
        expression();

        if (kind == EQ  ||
            kind == NEQ ||
            kind == GRE ||
            kind == GEQ ||
            kind == LES ||
            kind == LEQ ) scan();
        else              error("Relational operator expected, got " + kind + " instead");

        expression();
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
