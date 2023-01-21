package codegen;

import parser.Parser;

import static codegen.Instruction.*;

public class CodeBuffer {
    private static final byte[] buffer = new byte[3000];

    public static int pc = 0;
    public static int mainStart;

    public static void printCode() {
        for (int i = 0; i < pc; i++)
            System.out.print(buffer[i] + " ");

        System.out.println();
    }

    public static void printInstructionSet() {
        for (Instruction i : Instruction.values()) {
            System.out.println(String.format("%-12s %d", i.niceName, i.ordinal()));
        }
    }

    public static void putByte(int x) {
        buffer[pc++] = (byte)x;
    }

    public static void putShort(int x) {
        putByte(x >> 8);
        putByte(x);
    }

    public static void putShort(int x, int address) {
        buffer[address] = (byte) (x >> 8);
        buffer[address + 1] = (byte) x;
    }

    public static void putWord(int x) {
        putShort(x >> 16);
        putShort(x);
    }

    public static int getShort(int address) {
        return buffer[address] << 8 + buffer[address + 1];
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
                int address = operand.address;

                if (address >= 0 && address <= 5) {
                    putByte(LOAD_0.ordinal() + address);
                } else {
                    putByte(LOAD.ordinal());
                    putWord(operand.address);
                }

                break;

            case GLOBAL:
                putByte(LOAD_GLOBAL.ordinal());
                putWord(operand.address);
                break;

            case ESTACK:
                break;

            case CLASS_FIELD:
                putByte(LOAD_FIELD.ordinal());
                putWord(operand.address);
                break;

            case ARRAY_ELEMENT:
                break;
            case FUNCTION:
                break;
        }

        operand.kind = OperandKind.ESTACK;
    }

    public static void store(Operand location, Operand operand) {
        int address;

        switch (location.kind) {
            case LOCAL:
                address = location.address;
                if (address >= 0 && address <= 5) {
                    putByte(STORE_0.ordinal() + address);
                } else {
                    putByte(STORE.ordinal());
                    putWord(address);
                }

                break;

            case GLOBAL:
                putByte(STORE_GLOBAL.ordinal());
                putWord(location.address);
                break;

            case CLASS_FIELD:
                putByte(STORE_FIELD.ordinal());
                putWord(location.address);
                break;

            case ARRAY_ELEMENT:
                putByte(ARRAY_STORE.ordinal());
                break;

            default:
                Parser.error("??" + location.kind);
        }
    }
}
