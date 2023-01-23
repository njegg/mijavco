package compiler;

import codegen.CodeBuffer;
import parser.Parser;
import scanner.Scanner;

import java.io.IOException;

public class Mijavco {
    private static int errors = 0;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("mijavco requires parameters, usage:\n$ mijavco program.mj");
            return;
        }

        Scanner.init(args[0]);
        Parser.parse();

        if (errors > 0) {
            System.out.println("Number of errors: " + errors);
        } else {
            CodeBuffer.printCode();
            System.out.println();

            System.out.print("Dump: ");
            CodeBuffer.dump();

            CodeBuffer.createObjectFile();
        }
    }

    /**
     * Called by other parts of compiler
     */
    public static void error() {
        errors++;
    }
}