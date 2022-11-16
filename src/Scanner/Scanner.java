package Scanner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;

public class Scanner {
    private static final char EOL = '\n';
    private static final char EOF = (char) -1;

    private static HashMap<String, TokenKind> reservedNamesCodes;
    private static Reader in;
    private static char   ch;
    private static int    col;
    private static int    line;
    private static int    error_count = 0;

    public static void init() throws IOException {
        col = 0;
        line = 1;
        in = new InputStreamReader(new FileInputStream("program.mj"));
        nextChar();

        reservedNamesCodes = new HashMap<>();

//        String[] reservedNames = {
//            "break", "class", "else", "const", "if", "new",
//            "print", "program", "read", "return", "void", "while"
//        };

        String[] reservedNames = Arrays.stream(TokenKind.values())
                .map(k -> k.name().toLowerCase())
                .toArray(String[]::new);

        for (String name : reservedNames) {
            reservedNamesCodes.put(name, TokenKind.valueOf(name.toUpperCase()));
        }
    }

    private static void error(String message) {
        System.err.printf("\nline: %-4d col: %-4d: ", line, col);
        System.err.println(message);
        error_count++;
    }

    private static void nextChar() {
        try {
            ch = (char) in.read();

            if (ch == EOL) {
                col = 0;
                line++;
            } else {
                col++;
            }
        } catch (IOException e) {
            System.err.println("Rip reader");
        }
    }

    public static Token nextToken() {
        while (Character.isWhitespace(ch)) nextChar();

        Token token = new Token();
        token.column = col;
        token.line = line;

        if (Character.isLetter(ch) || ch == '_') {
            readIdent(token);
        } else if (Character.isDigit(ch)) {
            readNumber(token);
        } else {
            readRest(token);
        }

        return token;
    }

    private static void readIdent(Token token) {
        StringBuilder stringBuilder = new StringBuilder();

        while (Character.isDigit(ch) || Character.isLetter(ch) || ch == '_') {
            stringBuilder.append(ch);
            nextChar();
        }

        token.text = stringBuilder.toString();
        token.kind = reservedNamesCodes.getOrDefault(token.text, TokenKind.IDENT);
    }

    private static void readNumber(Token token) {
        while (Character.isDigit(ch)) {
            token.value *= 10;
            token.value += ch - '0';
            nextChar();
        }

        token.kind = TokenKind.NUMBER;
    }

    private static void readRest(Token token) {
        switch (ch) {
            case ';': token.kind = TokenKind.SEMICOLON; nextChar(); break;
            case '.': token.kind = TokenKind.PERIOD;    nextChar(); break;
            case ',': token.kind = TokenKind.COMMA;     nextChar(); break;
            case ')': token.kind = TokenKind.RPAREN;    nextChar(); break;
            case '(': token.kind = TokenKind.LPAREN;    nextChar(); break;
            case '[': token.kind = TokenKind.LBRACK;    nextChar(); break;
            case ']': token.kind = TokenKind.RBRACK;    nextChar(); break;
            case '{': token.kind = TokenKind.LBRACE;    nextChar(); break;
            case '}': token.kind = TokenKind.RBRACE;    nextChar(); break;
            case '*': token.kind = TokenKind.ASTERISK;  nextChar(); break;
            case '%': token.kind = TokenKind.MOD;       nextChar(); break;

            case '/':
                nextChar();
                if (ch == '/') {
                    while (ch != EOL) nextChar();
                    token.kind = TokenKind.ERROR;
                    nextChar();
                } else {
                    token.kind = TokenKind.SLASH;
                }
                break;

            case '=':
                nextChar();
                if (ch == '=') {
                    token.kind = TokenKind.EQ;
                    nextChar();
                } else {
                    token.kind = TokenKind.ASSIGN;
                }
                break;

            case '+':
                nextChar();
                if (ch == '+') {
                    token.kind = TokenKind.INC;
                    nextChar();
                } else {
                    token.kind = TokenKind.PLUS;
                }
                break;

            case '-':
                nextChar();
                if (ch == '-') {
                    token.kind = TokenKind.DEC;
                    nextChar();
                } else {
                    token.kind = TokenKind.MINUS;
                }
                break;

            case '>':
                nextChar();
                if (ch == '=') {
                    token.kind = TokenKind.GEQ;
                    nextChar();
                } else {
                    token.kind = TokenKind.GRE;
                }
                break;

            case '<':
                nextChar();
                if (ch == '=') {
                    token.kind = TokenKind.LEQ;
                    nextChar();
                } else {
                    token.kind = TokenKind.LES;
                }
                break;

            case '!':
                nextChar();
                if (ch == '=') {
                    token.kind = TokenKind.NEQ;
                    nextChar();
                } else {
                    error("'=' expected after '!'");
                    token.kind = TokenKind.ERROR;
                }
                break;

            case '|':
                nextChar();
                if (ch == '|') {
                    token.kind = TokenKind.OR;
                    nextChar();
                } else {
                    error("Another '|' expected");
                    token.kind = TokenKind.ERROR;
                }
                break;

            case '&':
                nextChar();
                if (ch == '&') {
                    token.kind = TokenKind.AND;
                    nextChar();
                } else {
                    error("Another '&' expected");
                    token.kind = TokenKind.ERROR;
                }
                break;

            case EOF:
                token.kind = TokenKind.EOF;
                break;

            default:
                error("Unexpected character '" + ch + "'");
                token.kind = TokenKind.ERROR;
                nextChar();
        }
    }
}
