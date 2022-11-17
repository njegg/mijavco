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

        while (kind == IDENT || kind == CONST) {
            if (kind == IDENT) varDeclaration();
            if (kind == CONST) constDeclaration();
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
    }

    /**
     *  ConstDecl = "final" Type ident "=" (number | charConst) ";".
     */
    private static void constDeclaration() {
        check(CONST);
        type();
        check(IDENT);

        if (kind != ASSIGN) {
            error("You forgot to initialize a constant");
        }
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

                    case LBRACE:
                        scan();
                        /* TODO */
                        check(RBRACE);
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

            case LPAREN:
                block();
                break;

            /* TODO: if, while, read, print */

            case BREAK: check(SEMICOLON); break;
            case SEMICOLON: break;

            default:
                error("This makes no sense");
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


    /**
     * <pre>
     * Designator = ident { "." ident | "[" Expr "]" }.
     * </pre>
     */
    private static void designator() {
        check(IDENT);

        while (kind == PERIOD || kind == LBRACK) {
            scan();
            if (kind == PERIOD) check(IDENT);
            else                expression(); check(RBRACK);
        }
    }

    public static void factor() {
        switch (kind) {
            case IDENT:
                designator();
                if (kind == LBRACE) {
                    scan();
                    /* TODO */
                    check(RBRACE);
                }
                break;

            case CHARACTER:
            case NUMBER: scan(); break;

            /*
             * TODO:
             * "new" ident [ "[" Expr "]" ]
             * "(" Expr ")".
            */

            default:
                error("Expected something that returns some value, got " + kind.name());
        }

    }

    public static void term() {
        factor();

        while (kind == ASTERISK || kind == MOD || kind == SLASH) {
            scan();
            factor();
        }
    }

    public static void expression() {
        if (kind == MINUS) scan();

        term();
        while (kind == PLUS || kind == MINUS) {
            scan();
            term();
        }
    }
}
