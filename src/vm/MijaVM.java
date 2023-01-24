package vm;

import codegen.CodeBuffer;
import codegen.Instruction;

import javax.swing.plaf.synth.SynthUI;
import java.io.*;
import java.util.Arrays;

public class MijaVM {
    private static int startAddress;
    private static int pc;
    private static int fbp, fsp, esp;
    private static int freep;

    private static final int HEAP_SIZE_WORDS = 100_000;
    private static final int FSTACK_SIZE_WORDS = 400;
    private static final int ESTACK_SIZE_WORDS = 30;

    private static byte[] codeData;
    private static int[]  globalData;
    private static int[]  heap   = new int[HEAP_SIZE_WORDS];
    private static final int[]  estack = new int[ESTACK_SIZE_WORDS];
    private static final int[]  fstack = new int[FSTACK_SIZE_WORDS];


    public static void runFromFile(String filePath) throws IOException {
        File inputFile = new File(filePath);
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
                /* Loading and storing */

                case CONST:
                    epush(getWord());
                    break;

                case CONST_0: case CONST_1: case CONST_2: case CONST_3: case CONST_4: case CONST_5:
                    epush(instruction.ordinal() - Instruction.CONST_0.ordinal());
                    break;


                case LOAD:
                    epush(fstack[fbp + getByte()]);
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


                /* Arrays */

                case NEW_ARRAY:
                    int elements = epop();
                    int elementSize = getByte();

                    heap[freep] = elements;
                    heap[freep + 1] = elementSize;

                    epush(malloc(elementSize, elements) + 2);
                    break;

                case ARRAY_LOAD:

                    break;

                case LOAD_STRING:
                    int stringLength = getWord();
                    malloc(4, 2);
                    int stringHeapAddress = malloc(1, stringLength);

                    heap[stringHeapAddress++] = stringLength;
                    heap[stringHeapAddress++] = 1;

                    epush(stringHeapAddress);

                    int wordIndex = stringHeapAddress;
                    while (stringLength >= 4) {
                        heap[wordIndex] = (getByte() << 24) | (getByte() << 16) | (getByte() << 8) | getByte();
                        stringLength -= 4;
                        wordIndex++;
                    }

                    while (stringLength --> 0) {
                        heap[wordIndex] |= (getByte() << (8 * (stringLength + 1)));
                    }

                    break;

                /* Operations */

                case ADD:
                    epush(epop() + epop());
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

                    for (int i = 0; i < paramsCount; i++) fpush(0);   // space for parameters
                    for (int i = 0; i < localsCount; i++) fpush(0);        // init locals to 0

                    for (int i = paramsCount - 1; i >= 0; i--) fstack[fbp + i] = epop();

                    break;

                case EXIT:
                    fsp = fbp;      // base is old stack top
                    fbp = fpop();   // retrieve previously saved base pointer
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

        return address;
    }
}
