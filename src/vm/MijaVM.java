package vm;

import codegen.Instruction;

import java.io.*;

public class MijaVM {
    private static int startAddress;
    private static int pc;
    private static int fp, fsp, esp;
    private static int freep;

    private static final int HEAP_SIZE_WORDS = 100_000;
    private static final int FSTACK_SIZE_WORDS = 400;
    private static final int ESTACK_SIZE_WORDS = 30;

    private static byte codeData[];
    private static int  globalData[];
    private static int  heap[] = new int[HEAP_SIZE_WORDS];
    private static int  estack[] = new int[ESTACK_SIZE_WORDS];
    private static int  fstack[] = new int[FSTACK_SIZE_WORDS];

    private static void epush(int x) {
        if (esp == ESTACK_SIZE_WORDS) throw new RuntimeException("Expression stack overflow");
        estack[esp++] = x;
    }

    private static int epop() {
        if (esp == 0) throw new RuntimeException("Tried to pop empty expression stack");
        return estack[--esp];
    }

    private static void fpush(int x) {
        if (fp == FSTACK_SIZE_WORDS) throw new RuntimeException("Frame stack overflow");
        estack[fp++] = x;
    }

    private static int fpop() {
        if (fp == 0) throw new RuntimeException("Tried to pop empty frame stack");
        return fstack[--fp];
    }


    private static byte getByte() {
        return codeData[pc++];
    }

    private static byte getByte(int address) {
        return codeData[address];
    }

    private static short getShort() {
        return (short) (((short) getByte() << 8) | (getByte() & 0b11111111));
    }

    private static short getShort(int address) {
        return (short) (((short) getByte(address) << 8) | (getByte(address + 1) & 0b11111111));
    }

    private static int getWord() {
        return (getShort() << 16) | (getShort() & 0b11111111_11111111);
    }

    private static int getWord(int address) {
        return (getShort(address) << 16) | (getShort(address + 2) & 0b11111111_11111111);
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Provide a .obj as an argument, usage:\n$mijavm program.obj");
            return;
        }

        File inputFile = new File(args[0]);
        InputStream in = new FileInputStream(inputFile);
        long inputFileSize = inputFile.length();

        if (inputFileSize > 3000) {
            throw new IllegalArgumentException("File to large");
        }

        codeData = new byte[(int) inputFileSize];
        in.read(codeData);
        in.close();

        char M = (char) getByte(0);
        char J = (char) getByte(1);

        if (M != 'M' || J != 'J') {
            throw new IllegalArgumentException("Illegal file format");
        }

        pc = getWord(2);
        getWord(4);

        execute();
    }

    private static void execute() {
        var instructions = Instruction.values();

        while (true) {
            Instruction instruction = instructions[getByte()];

            switch (instruction) {
                case CONST:
                    epush(getWord());
                    break;

                case BPRINT:
                    System.out.print((char) epop());
                    break;

                case RETURN:
                    if (fsp == 0) return;
                    break;

                case ENTER:
                    getShort();
                    break;
            }
        }
    }
}
