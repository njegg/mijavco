package codegen;

import parser.Parser;

public class Label {
    private int address;
    private boolean defined;

    public Label() {
        this.defined = false;
        this.address = 0;
    }

    public void put() {
        int pc = CodeBuffer.pc;

        if (defined) {
            CodeBuffer.putShort(address - (pc - 1));
        } else {
            CodeBuffer.putShort(address); // Address of previous unresolved jump value
            address = pc;                 // Current jump value is unresolved, save address to its
        }
    }

    public void resolve() {
        if (defined) {
            System.err.println("Internal Compiler Error");
            System.exit(1);
        }

        // pc is now on the place where all unresolved jumps of this label should jump to
        // address is at the last unresolved jump by this label
        // Visit all unresolved jumps and set their value to point to current address
        // Unresolved jumps have value addresses of previous unresolved jumps that should jump to same label
        // Last unresolved jump (top) has value 0
        while (address != 0) {
            int lastUnresolved = address;
            address = CodeBuffer.getShort(lastUnresolved); // Before last

            CodeBuffer.putShort(CodeBuffer.pc - (lastUnresolved - 1), lastUnresolved);
        }

        defined = true;
        address = CodeBuffer.pc;
    }
}
