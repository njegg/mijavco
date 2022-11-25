package compiler;

import parser.Parser;
import scanner.Scanner;

import java.io.IOException;

public class Main {
    private static int errors;

    static {
        try {
            Scanner.init("program.mj");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Scanner initialization went wrong");
        }

        errors = 0;
    }

    public static void main(String[] args) {
        Parser.parse();

        System.out.println("Number of errors:" + errors);
    }

    public static void error() {
        errors++;
    }
}