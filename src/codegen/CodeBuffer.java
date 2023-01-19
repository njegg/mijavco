package codegen;

import static codegen.Instruction.*;

public class CodeBuffer {
    private static final byte[] buffer = new byte[3000];

    public static int size = 0;
    public static int mainStart;

    public static void printCode() {
        for (int i = 0; i < size; i++)
            System.out.print(buffer[i] + " ");

        System.out.println();
    }

    public static void printInstructionSet() {
        for (Instruction i : Instruction.values()) {
            System.out.println(String.format("%-12s %d", i.niceName, i.ordinal()));
        }
    }

    public static void putByte(int x) {
        buffer[size++] = (byte)x;
    }

    public static void putShort(int x) {
        putByte(x >> 8);
        putByte(x);
    }

    public static void putWord(int x) {
        putShort(x >> 16);
        putShort(x);
    }

    public static void load(Operand operand) {
        if (operand.kind == null) return;

        switch (operand.kind) {
            case CONSTANT:
                int value = operand.value;
                if (value == -1)  {
                    putByte(CONST_M1.ordinal());
                } else if (value >= 0 && value <= 5) {
                    putByte(Instruction.valueOf("CONST_" + value).ordinal());
                } else {
                    putByte(CONST.ordinal());
                    putWord(value);
                }
                break;

            case LOCAL:
                break;
            case GLOBAL:
                break;
            case STACK:
                break;
            case CLASS_FIELD:
                break;
            case ARRAY_ELEMENT:
                break;
            case FUNCTION:
                break;
        }
    }
}
