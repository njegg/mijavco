package codegen;

import parser.Parser;
import parser.TypeKind;
import scanner.Scanner;

import java.io.*;

import static codegen.Instruction.*;

public class CodeBuffer {
    private static final byte[] buffer = new byte[3000];
    private static final int codeStartAddress = 8; // Space for header

    public static int pc = codeStartAddress;
    public static int mainStart;

    public static void printCode() {
        writeHeader();

        System.out.println("main: " + getWord(0));
        System.out.println("size: " + getWord(4));

        var instructions = Instruction.values();

        int i = codeStartAddress;
        while (i < pc) {
            Instruction instruction = instructions[buffer[i]];

            if (instruction == ENTER) {
                System.out.printf("%n%s:%n", Parser.symbolTable.getFunctionNameByAddress(i));
            }

            System.out.printf("%03d  %-16s", i, instruction.niceName);

            int iSize = instruction.size;
            if (iSize > 1) {
                int paramValue = 0;

                int j = i + 1;
                while (j < i + iSize) {
                    int b = buffer[j];
                    System.out.printf("%03d ", b);

                    paramValue <<= 8;
                    paramValue |= b;

                    j++;
                }

                System.out.printf("%s (%d)", " ".repeat(4 * (4 - iSize + 1)), paramValue);
            }

            i += instruction.size;

            System.out.println();
        }
    }

    public static void dump() {
        for (int i = 0; i < pc; i++) {
            System.out.printf("%d ", buffer[i]);
        }
        System.out.println();
    }

    public static void printInstructionSet() {
        for (Instruction i : Instruction.values()) {
            System.out.printf("%-12s %d%n", i.niceName, i.ordinal());
        }
    }

    public static void writeHeader() {
        putWord(mainStart, 0);
        putWord(pc - 1, 4);
    }


    public static void createObjectFile() {
        String inputFileName = Scanner.getInputFile().getName();
        String outputFileName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));

        try (OutputStream os = new FileOutputStream(outputFileName + ".obj")) {
            os.write(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void putByte(int x) {
        buffer[pc++] = (byte)x;
    }

    public static void putByte(int x, int address) {
        buffer[address] = (byte)x;
    }

    public static void putByte(Instruction instruction) {
        putByte(instruction.ordinal());
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

    public static void putWord(int x, int address) {
        putShort(x >> 16, address);
        putShort(x, address + 2);
    }

    public static int getShort(int address) {
        return (buffer[address] << 8) + buffer[address + 1];
    }

    public static int getWord(int address) {
        return (getShort(address) << 16) + getShort(address + 2);
    }

    public static byte[] getBuffer() { return buffer; }


    public static void load(Operand operand) {
        if (operand.kind == null) return;

        switch (operand.kind) {
            case CONSTANT:
                int value = operand.value;
                if (value == -1)  {
                    putByte(CONST_M1);
                } else if (value >= 0 && value <= 5) {
                    putByte(Instruction.valueOf("CONST_" + value));
                } else {
                    putByte(CONST);
                    putWord(value);
                }
                break;

            case LOCAL:
                int address = operand.address;

                if (address >= 0 && address <= 5) {
                    putByte(LOAD_0.ordinal() + address);
                } else {
                    putByte(LOAD);
                    putWord(operand.address);
                }

                break;

            case GLOBAL:
                putByte(LOAD_GLOBAL);
                putWord(operand.address);
                break;

            case ESTACK:
                break;

            case CLASS_FIELD:
                putByte(LOAD_FIELD);
                putByte(operand.address);
                break;

            case CONDITION:
                putByte(operand.condition.jumpInstruction);
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
                    putByte(STORE);
                    putWord(address);
                }

                break;

            case GLOBAL:
                putByte(STORE_GLOBAL);
                putWord(location.address);
                break;

            case CLASS_FIELD:
                putByte(STORE_FIELD);
                putByte(location.address);
                break;

            case ARRAY_ELEMENT:
                putByte(operand.symbol.symbolType.typeKind == TypeKind.CHAR ?
                     BARRAY_STORE : ARRAY_STORE);
                break;

            default:
                throw new RuntimeException("Internal Compiler Error; Cannot store operand");
        }
    }

    public static void trueJump(Operand condOperand) {
        putByte(condOperand.condition.jumpInstruction);
        condOperand.trueLabel.put();
    }

    public static void falseJump(Operand condOperand) {
        putByte(condOperand.condition.inverseJumpInstruction());
        condOperand.falseLabel.put();
    }

    public static void jump(Label label) {
        putByte(JMP);
        label.put();
    }
}
