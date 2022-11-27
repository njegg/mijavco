package parser;

import java.util.HashMap;

public class Symbol {
    SymbolKind symbolKind;
    String name;
    Type symbolType;
    int address;

    int value;                      /* Const */
    boolean global;                 /* Var */

    int parameterCount;             /* Method */
    HashMap<String, Symbol> locals; /* Method */
}
