import Parser.Parser;
import Scanner.Scanner;
import Scanner.Token;
import Scanner.TokenKind;

import java.io.IOException;

public class Main {
    static {
        try {
            Scanner.init();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Scanner initialization went wrong");
        }
    }
    public static void main(String[] args) throws IOException {
        testScanner();
    }

    private static void testScanner() {
        Token token = new Token();
        token.kind = TokenKind.ERROR;

        while (token.kind != TokenKind.EOF) {
            token = Scanner.nextToken();
            System.out.println(token);
        }
    }

    private static void testParser() throws IOException {
        Parser.parse();
    }
}