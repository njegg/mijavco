package codegen;

public enum Error {
    NULL_POINTER("Null pointer", 1),
    NO_RETURN("No return value", 2),
    HEAP_OVERFLOW("Heap overflow ", 3),
    STACK_OVERFLOW("Stack overflow", 4),
    INDEX_OUT_OF_BOUNDS("Index out of bounds", 5),
    RUNTIME("Runtime ", 6);

    public final String message;
    public final int status;

    Error(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public static void exit(Error e, int pc, String instruction) {
        System.err.printf("\n%s error at instruction %d: %s%n", e.message, pc, instruction);
        System.err.println("Use -i option when compiling to have a better view at where it happened");
        System.exit(e.status);
    }
}
