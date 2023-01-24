package compiler;

import codegen.CodeBuffer;
import parser.Parser;
import scanner.Scanner;
import vm.MijaVM;

import java.io.File;
import java.io.IOException;
import java.rmi.ServerError;
import java.util.Arrays;

public class Mijavco {
    private static int errors = 0;

    private static final char ARGUMENT_PRINT_INSTRUCTIONS = 'i';
    private static final char ARGUMENT_PRINT_BYTECODE = 'b';
    private static final char ARGUMENT_RUN = 'r';

    private static boolean printInstructionsFlag = false;
    private static boolean printBytecodeFlag = false;
    private static boolean runFlag = false;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("mijavco requires parameters, usage:\n$ mijavco program.mj");
            return;
        }

        String inputFileName = args[0];

        if (inputFileName.matches(".*[.]obj")) {
            MijaVM.runFromFile(inputFileName);
            return;
        }

        Scanner.init(inputFileName);
        Parser.parse();

        if (errors > 0) {
            System.out.println("Number of errors: " + errors);
            return;
        }

        Arrays.stream(args).filter(s -> s.startsWith("-")).map(s -> s.substring(1).chars()).forEach(s -> s.forEach(i -> {
            switch (i) {
                case ARGUMENT_RUN:                  runFlag = true;                 break;
                case ARGUMENT_PRINT_BYTECODE:       printBytecodeFlag = true;       break;
                case ARGUMENT_PRINT_INSTRUCTIONS:   printInstructionsFlag = true;   break;

                default: System.err.println("Invalid option -" + (char) i); System.exit(1);
            }
        }));

        if (printInstructionsFlag) {
            CodeBuffer.printCode();
            System.out.println();
        }

        if (printBytecodeFlag) {
            System.out.print("Dump: ");
            CodeBuffer.dump();
        }

        File outputFile = CodeBuffer.createObjectFile();

        if (runFlag) {
            MijaVM.runFromFile(outputFile.getName());
        }
    }

    /**
     * Called by other parts of compiler
     */
    public static void error() {
        errors++;
    }
}