package codegen;

import parser.Parser;
import parser.Symbol;
import parser.Type;
import parser.TypeKind;

public class Operand {
    public OperandKind kind;
    public Type type;
    public Symbol symbol;

    public int value;
    public int address;


    public Operand(Symbol symbol) {
        type = symbol.symbolType;
        value = symbol.value;
        address = symbol.address;

        switch (symbol.symbolKind) {
            case CONST:
                kind = OperandKind.CONSTANT;
                break;

            case VAR:
                kind = symbol.isGlobal ? OperandKind.GLOBAL : OperandKind.LOCAL;
                break;

            case FUNCTION:
                kind = OperandKind.FUNCTION;
                break;

            default:
                Parser.error("Cannot read operand, symbol of kind " + symbol.symbolKind + " unexpected");
        }

        this.symbol = symbol;
    }
}
