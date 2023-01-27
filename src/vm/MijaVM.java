package vm;

import codegen.Error;
import codegen.Instruction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Scanner;

public class MijaVM {
    private static int pc;
    private static int fbp, fsp, esp;
    private static int freep = 1; // 0 is reserved for null

    private static final int HEAP_SIZE_WORDS = 100_000;
    private static final int FSTACK_SIZE_WORDS = 400;
    private static final int ESTACK_SIZE_WORDS = 100;
    private static final int GLOBAL_DATA_SIZE_WORDS = 200;

    private static final int WORD_BYTES = 4;
    private static final int SHORT_BYTES = 2;

    private static byte[] codeData;
    private static final int[]  globalData  = new int[200];
    private static final int[]  heap        = new int[HEAP_SIZE_WORDS];
    private static final int[]  estack      = new int[ESTACK_SIZE_WORDS];
    private static final int[]  fstack      = new int[FSTACK_SIZE_WORDS];

    private static Scanner in;
    private static Random rand;

    private static Instruction instruction; // Current instruction

    public static void runFromFile(String filePath) throws IOException {
        File inputFile = new File(filePath);
        InputStream fileInputStream = new FileInputStream(inputFile);
        long inputFileSize = inputFile.length();

        if (inputFileSize > 3000) {
            throw new IllegalArgumentException("File to large");
        }

        codeData = new byte[(int) inputFileSize];
        fileInputStream.read(codeData);
        fileInputStream.close();

        char M = (char) getByte(0);
        char J = (char) getByte(1);

        if (M != 'M' || J != 'J') {
            throw new IllegalArgumentException("Illegal file format");
        }

        pc = getWord(2);
        getWord(4);

        in = new Scanner(System.in);
        rand = new Random();

        execute();

        in.close();
    }

    private static void execute() {
        var instructions = Instruction.values();

        while (true) {
            instruction = instructions[getByte()];

            switch (instruction) {
                /* Loading and storing */

                case CONST:
                    epush(getWord());
                    break;

                case CONST_M1: case CONST_0: case CONST_1: case CONST_2: case CONST_3: case CONST_4: case CONST_5:
                    epush(instruction.ordinal() - Instruction.CONST_0.ordinal());
                    break;


                case LOAD:
                    epush(fstack[fbp + getByte()]);
                    break;

                case NOP:
                    break;
                case LOAD_0: case LOAD_1: case LOAD_2: case LOAD_3: case LOAD_4: case LOAD_5:
                    int localLoadAddress = instruction.ordinal() - Instruction.LOAD_0.ordinal();
                    epush(fstack[fbp + localLoadAddress]);
                    break;


                case STORE:
                    fstack[fbp + getShort()] = epop();
                    break;

                case STORE_0: case STORE_1: case STORE_2: case STORE_3: case STORE_4: case STORE_5:
                    int localStoreAddress = instruction.ordinal() - Instruction.STORE_0.ordinal();
                    fstack[fbp + localStoreAddress] = epop();
                    break;

                case LOAD_GLOBAL:
                    int address = getShort();
                    epush(globalData[address]);
                    break;

                case STORE_GLOBAL:
                    address = getShort();
                    globalData[address] = epop();
                    break;

                /* Arrays */

                case NEW_ARRAY:
                    int length = epop();
                    int elementSize = getByte();

                    if (length < 0) {
                        System.err.println("Cannot initialize array with <0 elements");
                        Error.exit(Error.RUNTIME, pc, instruction.niceName);
                    }

                    heap[freep] = length;
                    heap[freep + 1] = elementSize;
                    malloc(WORD_BYTES, 2);

                    epush(malloc(elementSize , length));
                    break;

                case ARRAY_LOAD:
                    int index = epop();
                    address = epop();

                    if (address == 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }

                    length = heap[address - 2];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                        Error.exit(Error.INDEX_OUT_OF_BOUNDS, pc, instruction.niceName);
                    }

                    elementSize = heap[address - 1];

                    epush(heap[address + index * elementSize]);
                    break;

                case ARRAY_STORE:
                    int value = epop();
                    index = epop();
                    address = epop();

                    if (address == 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }

                    length = heap[address - 2];
                    elementSize = heap[address - 1];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                        Error.exit(Error.INDEX_OUT_OF_BOUNDS, pc, instruction.niceName);
                    }

                    heap[address + index * elementSize] = value;
                    break;

                case BARRAY_LOAD:
                    index = epop();
                    address = epop();

                    if (address == 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }

                    length = heap[address - 2];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                        Error.exit(Error.INDEX_OUT_OF_BOUNDS, pc, instruction.niceName);
                    }

                    int word = heap[address + index / 4];
                    int shiftAmount = 8 * (3 - index % 4);
                    word >>= shiftAmount;

                    epush((byte) word);
                    break;

                case BARRAY_STORE:
                    byte b = (byte) epop();
                    index = epop();
                    address = epop();

                    if (address == 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }

                    length = heap[address - 2];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                        Error.exit(Error.INDEX_OUT_OF_BOUNDS, pc, instruction.niceName);
                    }

                    word = heap[address + index / 4];
                    shiftAmount = 8 * (3 - index % 4);

                    int insertValue = b << shiftAmount;
                    int clearByteMask = ~(0xff << shiftAmount);

                    heap[address + index / 4] = word & clearByteMask | insertValue;
                    break;

                case LENGTH:
                    address = epop() - 2;
                    if (address < 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }
                    epush(heap[address]);
                    break;

                case LOAD_STRING:
                    length = getWord();
                    malloc(WORD_BYTES, 2); // 2 words for length and element size
                    address = malloc(1, length);

                    heap[address++] = length;
                    heap[address++] = 1;

                    epush(address);

                    index = address;
                    while (length >= 4) {
                        heap[index] = (getByte() << 24) | (getByte() << 16) | (getByte() << 8) | getByte();
                        length -= 4;
                        index++;
                    }

                    while (length --> 0) {
                        heap[index] |= (getByte() << (8 * (length + 1)));
                    }

                    break;


                /* Structs */

                case NEW:
                    int fieldCount = getByte() & 0xff;
                    epush(malloc(WORD_BYTES, fieldCount));
                    break;

                case STORE_FIELD:
                    value = epop();
                    address = epop();

                    if (address == 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }

                    index = getByte() & 0xff;
                    heap[address + index] = value;
                    break;

                case LOAD_FIELD:
                    address = epop();
                    if (address == 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }

                    index = getByte();
                    epush(heap[address + index]);
                    break;

                /* Operations */

                case ADD: epush(epop() + epop()); break;
                case SUB: epush(epop() - epop()); break;
                case MUL: epush(epop() * epop()); break;
                case DIV: epush(epop() / epop()); break;
                case REM: epush(epop() % epop()); break;


                /* Jumps */

                case JMP:
                    int jumpAmount = getShort();
                    pc += jumpAmount - 3; // -3 so its relative to jmp instruction
                    break;

                case JEQ:
                    jumpAmount = getShort();
                    if (epop() == epop()) pc += jumpAmount - 3;
                    break;

                case JNE:
                    jumpAmount = getShort();
                    if (epop() != epop()) pc += jumpAmount - 3;
                    break;

                case JGT:
                    jumpAmount = getShort();
                    if (epop() < epop()) pc += jumpAmount - 3;
                    break;

                case JLE:
                    jumpAmount = getShort();
                    if (epop() >= epop()) pc += jumpAmount - 3;
                    break;

                case JLT:
                    jumpAmount = getShort();
                    if (epop() > epop()) pc += jumpAmount - 3;
                    break;

                case JGE:
                    jumpAmount = getShort();
                    if (epop() <= epop()) pc += jumpAmount - 3;
                    break;


                /* IO */

                case BPRINT:
                    System.out.print((char) epop());
                    break;

                case PRINT:
                    System.out.print(epop());
                    break;

                case PRINTS:
                    int stringAddress = epop();

                    if (stringAddress == 0) {
                        Error.exit(Error.NULL_POINTER, pc, instruction.niceName);
                    }

                    int len = heap[stringAddress - 2];
                    int currentWordAddress = stringAddress;

                    while (len >= 4) {
                        int wordToPrint = heap[currentWordAddress];

                        for (int i = 3; i >= 0; i--)
                            System.out.printf("%c", wordToPrint >> (8 * i) & 0xff);

                        len -= 4;
                        currentWordAddress++;
                    }


                    while (len > 0) {
                        char c = (char) ((heap[currentWordAddress] >> (8 * len)) & 0xff);
                        System.out.print(c);
                        len--;
                    }

                    break;

                case CALL:
                    int callAddress = getShort();
                    fpush(pc);
                    pc = callAddress;
                    break;

                case RETURN:
                    if (fsp == 0) return; // no caller = main, exit
                    pc = fpop(); // get pc that was saved before calling
                    break;

                case ENTER:
                    int paramsCount = getByte();
                    int localsCount = getByte();

                    fpush(fbp);  // save base pointer
                    fbp = fsp;   // base pointer is at top of old stack frame

                    // Space for locals and parameters
                    for (int i = 0; i < paramsCount; i++) fpush(0);
                    for (int i = 0; i < localsCount; i++) fpush(0);

                    // Loading parameters from estack in reverse
                    for (int i = paramsCount - 1; i >= 0; i--) fstack[fbp + i] = epop();

                    break;

                case EXIT:
                    fsp = fbp;      // base is old stack top
                    fbp = fpop();   // retrieve previously saved base pointer
                    break;

                case POP: epop(); break;

                case READ:
                    epush(in.nextInt());
                    break;

                case BREAD:
                    epush(in.nextByte());
                    break;

                case RAND:
                    epush(rand.nextInt(epop()));
                    break;

                case NEG:
                    epush(-epop());
                    break;

                case TRAP:
                    Error.exit(Error.values()[getByte()], pc, instruction.niceName);
                    break;
            }
        }
    }

    private static void error(String msg) {
        System.err.println("Error: " + msg);
        System.exit(1);
    }

    private static void epush(int x) {
        if (esp == ESTACK_SIZE_WORDS) error("Expression stack overflow");
        estack[esp++] = x;
    }

    private static int epop() {
        if (esp == 0) error("Tried to pop empty expression stack");
        return estack[--esp];
    }

    private static void fpush(int x) {
        if (fsp == FSTACK_SIZE_WORDS) error("Frame stack overflow");
        fstack[fsp++] = x;
    }

    private static int fpop() {
        if (fsp == 0) error("Tried to pop empty frame stack");
        return fstack[--fsp];
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

    /**
     * Allocate n blocks of memory, size bytes each
     */
    private static int malloc(int size, int n) {
        int address = freep;
        int bytes = size * n;
        int words = (bytes + 3) / 4;

        freep += words;

        if (freep >= HEAP_SIZE_WORDS) {
            Error.exit(Error.HEAP_OVERFLOW, pc, instruction.niceName);
        }

        return address;
    }
}
