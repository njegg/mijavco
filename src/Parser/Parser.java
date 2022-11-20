package Parser;

import Scanner.Scanner;
import Scanner.Token;
import Scanner.TokenKind;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Scanner.TokenKind.*;

public class Parser {
    private static TokenKind kind;
    private static Token current;
    private static Token lookahead;

    private static int errors = 0;

    private static HashSet<TokenKind> firstStatement;

    public static void parse() {
        firstStatement = Stream.of(IDENT, IF, WHILE, BREAK, RETURN, READ, PRINT, LBRACE, SEMICOLON)
                .collect(Collectors.toCollection(HashSet::new));

        scan();
        mijava();
        System.out.println("Number of errors: " + errors);
    }

    private static void scan() {
        current = lookahead;
        lookahead = Scanner.nextToken();
        kind = lookahead.kind;
        System.out.println(lookahead);
    }

    private static void check(TokenKind expected) {
        if (expected == kind) {
            scan();
        } else {
            error(expected.name() + " expected, got " + kind.name());
        }
    }

    private static void error(String message) {
        System.err.printf("\nline: %-4d col: %-4d: ", lookahead.line, lookahead.column);
        System.err.println(message);

        throw new RuntimeException();
        // System.exit(1);
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

        check(SEMICOLON);
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

        check(NUMBER);
        check(SEMICOLON);
    }

    private static void block() {
        check(LBRACE);

        while (firstStatement.contains(kind))
            statement();

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
            case SEMICOLON: break;

            default:
                error(kind.name() + " not expected here");
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
                error(kind.name() + " unexpected in an factor of expression");
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
        else              error("Relational operator expected, got " + kind.name() + " instead");

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
