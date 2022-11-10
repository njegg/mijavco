import Scanner.Scanner;
import Scanner.Token;
import Scanner.TokenCode;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner.init();

        Token token = new Token();
        token.code = TokenCode.NONE;

        while (token.code != TokenCode.EOF) {
            token = Scanner.nextToken();
            System.out.println(token);
        }
    }
}