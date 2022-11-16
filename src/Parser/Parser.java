package Parser;

import Scanner.Scanner;
import Scanner.Token;
import Scanner.TokenKind;
import static Scanner.TokenKind.*;

public class Parser {
    private static TokenKind kind;
    private static Token current;
    private static Token lookahead;

    private static int errors = 0;

    public static void parse() {
        scan();
        mijava();
        System.out.println("Number of errors: " + errors);
    }

    private static void scan() {
        current = lookahead;
        lookahead = Scanner.nextToken();
        kind = lookahead.kind;
    }

    private static void check(TokenKind expected) {
        if (expected == kind) {
            scan();
        } else {
            error(expected.name() + " expected");
        }
    }

    private static void error(String message) {
        System.err.printf("\nline: %-4d col: %-4d: ", lookahead.line, lookahead.column);
        System.err.println(message);

        System.exit(1);
    }

    /**
     *  <pre>
     *  Program = "program" ident
     *            {ConstDecl | VarDecl | ClassDecl}
     *            "{" {MethodDecl} "}".
     *  </pre>
     */
    private static void mijava() {
        check(PROGRAM); check(IDENT);

        while (kind == IDENT || kind == CONST) {
            if (kind == IDENT) varDeclaration();
            if (kind == CONST) constDeclaration();
        }

        check(LBRACE);


        check(RBRACE); check(EOF);
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
}
