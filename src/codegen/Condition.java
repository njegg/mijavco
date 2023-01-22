package codegen;

import scanner.TokenKind;

import static codegen.Instruction.*;

public enum Condition {
    EQ(JEQ), NE(JNE), LT(JLT), GE(JGE), GT(JGT), LE(JLE);

    public final Instruction jumpInstruction;
    private final Instruction[] inverseJumpInstruction = new Instruction[]
            {JNE, JEQ, JGE, JLT, JLE, JGT};

    Condition(Instruction instruction) {
        jumpInstruction = instruction;
    }

    public Instruction inverseJumpInstruction() {
        return inverseJumpInstruction[ordinal()];
    }

    public static Condition getByToken(TokenKind kind) {
        switch (kind) {
            case EQ: return EQ;
            case NE: return NE;
            case LT: return LT;
            case LE: return LE;
            case GT: return GT;
            case GE: return GE;
            default: return null;
        }
    }
}
