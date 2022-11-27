package compiler;

import parser.Parser;
import scanner.Scanner;

import java.io.IOException;

public class Main {
    private static int errors = 0;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("mijavco requires parameters, usage:\n$ mijavco program.mj");
            return;
        }

        Scanner.init(args[0]);
        Parser.parse();

        System.out.println("Number of errors:" + errors);
    }

    /**
     * Called by other parts of compiler
     */
    public static void error() {
        errors++;
    }
}