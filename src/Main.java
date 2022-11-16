import Parser.Parser;
import Scanner.Scanner;
import Scanner.Token;
import Scanner.TokenKind;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner.init();
        Parser.parse();
    }
}