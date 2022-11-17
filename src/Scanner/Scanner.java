package Scanner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static Scanner.TokenKind.*;

public class Scanner {
    private static final char EOLCHAR = '\n';
    private static final char EOFCHAR = (char) -1;

    private static HashMap<String, TokenKind> reservedNamesCodes;
    private static HashSet<Character> escapedCharacters;
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

        escapedCharacters = Stream
                .of('n', '\\', '\'', '\"', 't')
                .collect(Collectors.toCollection(HashSet::new));

        reservedNamesCodes = new HashMap<>();

        /* Convert all TokenKind's to array of lowercase strings */
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

            if (ch == EOLCHAR) {
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
        } else if (ch == '\'') {
            readCharacter(token);
        } else if (ch == '\"') {
            readString(token);
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
        token.kind = reservedNamesCodes.getOrDefault(token.text, IDENT);
    }

    private static void readNumber(Token token) {
        while (Character.isDigit(ch)) {
            token.value *= 10;
            token.value += ch - '0';
            nextChar();
        }

        token.kind = NUMBER;
    }

    private static void readCharacter(Token token) {
        token.kind = CHARACTER;
        nextChar();

        boolean escaped = false;

        /* Escaped character */
        if (ch == '\\') {
            nextChar();
            escaped = true;

            // TODO:
            if (!escapedCharacters.contains(ch)) {
                error("Illegal escape character '\\" + ch + '\'');
                token.kind = ERROR;
            }
        } else {
            if (ch == '\'' || ch == '\"') {
                token.kind = ERROR;
                error("Characters ' and \" should be escaped");
            }
        }

        token.value = ch;
        /* Change the value for special cases */
        if (escaped) {
            if      (ch == 'n') token.value = '\n';
            else if (ch == 't') token.value = '\t';
        }

        nextChar();

        /* Char literal not closed correctly */
        if (ch != '\'') {
            /* Find ending \', \n or EOF */
            while (ch != '\'' && ch != EOLCHAR && ch != EOFCHAR) nextChar();

            if (ch == '\'') {
                token.kind = ERROR;
                error("Character literal too long");
                nextChar();
            } else if (ch == EOFCHAR) {
                token.kind = EOF;
                error("Unexpected end of file, character literal not closed");
            } else {
                token.kind = ERROR;
                error("Character literal not closed");
            }

            return;
        }

        /* Closed properly */
        nextChar();
    }

    private static void readString(Token token) {

    }

    private static void readRest(Token token) {

        switch (ch) {
            case ';': token.kind = SEMICOLON; nextChar(); break;
            case '.': token.kind = PERIOD;    nextChar(); break;
            case ',': token.kind = COMMA;     nextChar(); break;
            case ')': token.kind = RPAREN;    nextChar(); break;
            case '(': token.kind = LPAREN;    nextChar(); break;
            case '[': token.kind = LBRACK;    nextChar(); break;
            case ']': token.kind = RBRACK;    nextChar(); break;
            case '{': token.kind = LBRACE;    nextChar(); break;
            case '}': token.kind = RBRACE;    nextChar(); break;
            case '*': token.kind = ASTERISK;  nextChar(); break;
            case '%': token.kind = MOD;       nextChar(); break;

            case '/':
                nextChar();
                if (ch == '/') {
                    while (ch != EOLCHAR && ch != EOFCHAR) nextChar();

                    Token tokenAfterComment = nextToken();

                    token.kind = tokenAfterComment.kind;
                    token.line = tokenAfterComment.line;
                    token.value = tokenAfterComment.value;
                    token.text = tokenAfterComment.text;
                    token.column = tokenAfterComment.column;
                } else {
                    token.kind = SLASH;
                }
                break;

            case '=':
                nextChar();
                if (ch == '=') {
                    token.kind = EQ;
                    nextChar();
                } else {
                    token.kind = ASSIGN;
                }
                break;

            case '+':
                nextChar();
                if (ch == '+') {
                    token.kind = INC;
                    nextChar();
                } else {
                    token.kind = PLUS;
                }
                break;

            case '-':
                nextChar();
                if (ch == '-') {
                    token.kind = DEC;
                    nextChar();
                } else {
                    token.kind = MINUS;
                }
                break;

            case '>':
                nextChar();
                if (ch == '=') {
                    token.kind = GEQ;
                    nextChar();
                } else {
                    token.kind = GRE;
                }
                break;

            case '<':
                nextChar();
                if (ch == '=') {
                    token.kind = LEQ;
                    nextChar();
                } else {
                    token.kind = LES;
                }
                break;

            case '!':
                nextChar();
                if (ch == '=') {
                    token.kind = NEQ;
                    nextChar();
                } else {
                    error("'=' expected after '!'");
                    token.kind = ERROR;
                }
                break;

            case '|':
                nextChar();
                if (ch == '|') {
                    token.kind = OR;
                    nextChar();
                } else {
                    error("Another '|' expected");
                    token.kind = ERROR;
                }
                break;

            case '&':
                nextChar();
                if (ch == '&') {
                    token.kind = AND;
                    nextChar();
                } else {
                    error("Another '&' expected");
                    token.kind = ERROR;
                }
                break;

            case EOFCHAR:
                token.kind = EOF;
                break;

            default:
                error("Unexpected character '" + ch + "'");
                token.kind = ERROR;
                nextChar();
        }
    }
}
