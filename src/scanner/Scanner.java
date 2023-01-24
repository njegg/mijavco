package scanner;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static scanner.TokenKind.*;

public class Scanner {
    private static String filePath;
    private static File inputFile;

    private static final char EOLCHAR = '\n';
    private static final char EOFCHAR = (char) -1;

    private static HashMap<String, TokenKind> reservedNamesCodes;
    private static HashSet<Character> escapedCharacters;

    private static Reader in;
    private static char   ch;
    private static int    col;
    private static int    line;

    public static void init(String filePath) throws IOException {
        col = 0;
        line = 1;

        inputFile = new File(filePath);
        Scanner.filePath = inputFile.getAbsolutePath();

        in = new InputStreamReader(new FileInputStream(inputFile));

        nextChar();

        escapedCharacters = Stream
                .of('n', '\\', '\'', '\"', 't')
                .collect(Collectors.toCollection(HashSet::new));

        reservedNamesCodes = new HashMap<>();
        /* "while" -> TokenKind.WHILE */
        Arrays.stream(TokenKind.values())
                        .forEach(k -> reservedNamesCodes.put(
                                k.name().toLowerCase(), k
                        ));
        reservedNamesCodes.remove("error");
    }

    /**
     * Used in parser errors
     */
    public static String getFilePath() {
        return filePath;
    }

    public static File getInputFile() { return inputFile; }

    private static void error(String message, Token token) {
        token.kind = ERROR;
        token.text = message;
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
        token.kind = NUMBER;

        int base = 10;
        if (ch == '0') {
            nextChar();

            if      (ch == 'h') base = 16;
            else if (ch == 'o') base = 8;
            else if (ch == 'b') base = 2;
            else if (!Character.isAlphabetic(ch)) return;
            else {
                error("Illegal number format character, use 'o' - octal, 'b' - binary or 'h' - hex", token);
                nextChar();
                return;
            }

            nextChar();
        }

        if (base == 10) {
            while (Character.isDigit(ch)) {
                token.value = token.value * 10 + (ch - '0');
                nextChar();
            }
        } else if (base == 8) {
            while (ch >= '0' && ch <= '7') {
                token.value = token.value * 8 + (ch - '0');
                nextChar();
            }
        } else if (base == 2) {
            while (ch == '0' || ch == '1') {
                token.value = token.value * 2 + (ch - '0');
                nextChar();
            }

        } else {
            while (isHex(ch)) {
                token.value = token.value * 16 + toHex(ch);
                nextChar();
            }
        }

        if (Character.isDigit(ch)) {
            error(String.format("'%c' is a illegal digit for base %d%n", ch, base), token);
            return;
        }
    }

    private static boolean isHex(char ch) {
        return Character.isDigit(ch) || "abcdef".indexOf(ch) != -1 || "ABCDEF".indexOf(ch) != -1 ;
    }

    private static int toHex(char ch) {
        if (Character.isDigit(ch)) {
            return ch - '0';
        } else if (Character.isLowerCase(ch)) {
            return ch - 'a' + 10;
        } else {
            return ch - 'A' + 10;
        }
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
                error("Illegal escape character '\\" + ch + '\'', token);
            }
        } else {
            if (ch == '\'' || ch == '\"') {
                error("Characters ' and \" should be escaped", token);
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
                error("Character literal too long", token);
                nextChar();
            } else if (ch == EOFCHAR) {
                error("Unexpected end of file, character literal not closed", token);
            } else {
                error("Character literal not closed", token);
            }

            return;
        }

        /* Closed properly */
        nextChar();
    }

    private static void readString(Token token) {
        nextChar();

        StringBuilder sb = new StringBuilder();

        while (ch != '\"') {
            sb.append(ch);
            nextChar();
        }

        token.kind = STRING;
        token.text = sb.toString();
        token.value = sb.length();

        nextChar();
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
                    token.kind = GE;
                    nextChar();
                } else {
                    token.kind = GT;
                }
                break;

            case '<':
                nextChar();
                if (ch == '=') {
                    token.kind = LE;
                    nextChar();
                } else {
                    token.kind = LT;
                }
                break;

            case '!':
                nextChar();
                if (ch == '=') {
                    token.kind = NE;
                    nextChar();
                } else {
                    error("'=' expected after '!'", token);
                }
                break;

            case '|':
                nextChar();
                if (ch == '|') {
                    token.kind = OR;
                    nextChar();
                } else {
                    error("Another '|' expected", token);
                }
                break;

            case '&':
                nextChar();
                if (ch == '&') {
                    token.kind = AND;
                    nextChar();
                } else {
                    error("Another '&' expected", token);
                }
                break;

            case EOFCHAR:
                token.kind = EOF;
                break;

            default:
                error("Unexpected character '" + ch + "'", token);
                nextChar();
        }
    }
}
