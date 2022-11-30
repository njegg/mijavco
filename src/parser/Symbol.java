package parser;

import java.util.HashMap;

public class Symbol {
    SymbolKind symbolKind;
    String name;
    String typeName;
    Type symbolType;
    int address;

    int value;                      /* Const */
    boolean global;                 /* Var */

    int parameterCount;             /* Function */
    HashMap<String, Symbol> locals; /* Function */

    Symbol() {
        symbolKind = SymbolKind.NOSYM;
        symbolType = new Type(TypeKind.NOTYPE);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s",
                    symbolKind,
                    symbolType,
                    name
                );
    }

    public boolean assignable(Symbol that) {
        return this.symbolType.equals(that.symbolType);
    }
}
