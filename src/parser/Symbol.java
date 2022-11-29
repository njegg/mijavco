package parser;

import java.util.HashMap;

public class Symbol {
    SymbolKind symbolKind;
    String name;
    Type symbolType;
    int address;

    int value;                      /* Const */
    boolean global;                 /* Var */

    int parameterCount;             /* Function */
    HashMap<String, Symbol> locals; /* Function */

    @Override
    public String toString() {
        return String.format("%s %s %s",
                    symbolKind,
                    symbolType.typeKind,
                    name
                );
    }
}
