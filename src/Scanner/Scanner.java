package Scanner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

public class Scanner {
    private static final char EOL = '\n';
    private static final char EOF = (char) -1;

    private static HashMap<String, TokenCode> reservedNamesCodes;
    private static Reader in;
    private static char   ch;
    public  static int    col;
    public  static int    line;
    public  static int    error_count = 0;

    public static void init() throws IOException {
        col = 0;
        line = 1;
        in = new InputStreamReader(new FileInputStream("program.mj"));
        nextChar();

        reservedNamesCodes = new HashMap<>();

        String[] reservedNames = {
            "break", "class", "else", "final", "if", "new",
            "print", "program", "read", "return", "void", "while"
        };

        for (String name : reservedNames) {
            reservedNamesCodes.put(name, TokenCode.valueOf(name.toUpperCase()));
        }
    }

    private static void error(String msg) {
        System.err.printf("\nline: %-4d col: %-4d: ", line, col);
        System.err.println(msg);
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

        token.code = reservedNamesCodes.getOrDefault(token.text, TokenCode.IDENT);
    }

    private static void readNumber(Token token) {
        while (Character.isDigit(ch)) {
            token.value *= 10;
            token.value += ch - '0';
            nextChar();
        }

        token.code = TokenCode.NUMBER;
    }

    private static void readRest(Token token) {
        switch (ch) {
            case ';': token.code = TokenCode.SEMICOLON; nextChar(); break;
            case '.': token.code = TokenCode.PERIOD;    nextChar(); break;
            case ',': token.code = TokenCode.COMMA;     nextChar(); break;
            case ')': token.code = TokenCode.RPAREN;    nextChar(); break;
            case '(': token.code = TokenCode.LPAREN;    nextChar(); break;
            case '[': token.code = TokenCode.LBRACK;    nextChar(); break;
            case ']': token.code = TokenCode.RBRACK;    nextChar(); break;
            case '{': token.code = TokenCode.LBRACE;    nextChar(); break;
            case '}': token.code = TokenCode.RBRACE;    nextChar(); break;
            case '*': token.code = TokenCode.ASTERISK;  nextChar(); break;
            case '%': token.code = TokenCode.MOD;       nextChar(); break;

            case '/':
                nextChar();
                if (ch == '/') {
                    while (ch != EOL) nextChar();
                    token.code = TokenCode.NONE;
                    nextChar();
                } else {
                    token.code = TokenCode.SLASH;
                }
                break;

            case '=':
                nextChar();
                if (ch == '=') {
                    token.code = TokenCode.EQ;
                    nextChar();
                } else {
                    token.code = TokenCode.ASSIGN;
                }
                break;

            case '+':
                nextChar();
                if (ch == '+') {
                    token.code = TokenCode.INC;
                    nextChar();
                } else {
                    token.code = TokenCode.PLUS;
                }
                break;

            case '-':
                nextChar();
                if (ch == '-') {
                    token.code = TokenCode.DEC;
                    nextChar();
                } else {
                    token.code = TokenCode.MINUS;
                }
                break;

            case '>':
                nextChar();
                if (ch == '=') {
                    token.code = TokenCode.GEQ;
                    nextChar();
                } else {
                    token.code = TokenCode.GRE;
                }
                break;

            case '<':
                nextChar();
                if (ch == '=') {
                    token.code = TokenCode.LEQ;
                    nextChar();
                } else {
                    token.code = TokenCode.LES;
                }
                break;

            case '!':
                nextChar();
                if (ch == '=') {
                    token.code = TokenCode.NEQ;
                    nextChar();
                } else {
                    error("'=' expected after '!'");
                    token.code = TokenCode.NONE;
                }
                break;

            case '|':
                nextChar();
                if (ch == '|') {
                    token.code = TokenCode.OR;
                    nextChar();
                } else {
                    error("Another '|' expected");
                    token.code = TokenCode.NONE;
                }
                break;

            case '&':
                nextChar();
                if (ch == '&') {
                    token.code = TokenCode.AND;
                    nextChar();
                } else {
                    error("Another '&' expected");
                    token.code = TokenCode.NONE;
                }
                break;

            case EOF:
                token.code = TokenCode.EOF;
                break;

            default:
                error("Unexpected character '" + ch + "'");
                token.code = TokenCode.NONE;
                nextChar();
        }
    }
}
