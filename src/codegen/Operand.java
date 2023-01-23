package codegen;

import parser.Parser;
import parser.Symbol;
import parser.Type;
import parser.TypeKind;
import scanner.TokenKind;

public class Operand {
    public OperandKind kind;
    public Type type;
    public Symbol symbol;
    public Condition condition;
    public Label trueLabel;
    public Label falseLabel;

    public int value;
    public int address;

    public Operand(Symbol symbol) {
        type = symbol.symbolType;
        value = symbol.value;
        address = symbol.address;
        this.symbol = symbol;

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

            case TYPE:
                break;

            default:
                Parser.error("Cannot read operand, symbol of kind " + symbol.symbolKind + " unexpected");
        }
    }

    public Operand(int constantValue) {
        this.kind = OperandKind.CONSTANT;
        this.type = new Type(TypeKind.INT);
        this.value = constantValue;
    }

    public Operand(TokenKind condOperator) {
        this.kind = OperandKind.CONDITION;
        condition = Condition.getByToken(condOperator);

        trueLabel = new Label();
        falseLabel = new Label();
    }

    @Override
    public String toString() {
        return symbol.toString();
    }
}
