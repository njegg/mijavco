package parser;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Symbol {
    public SymbolKind symbolKind;
    public String name;
    public Type symbolType;
    public int address;

    public int value;                            /* Const */
    public boolean global;                       /* Var */

    public LinkedList<Symbol> parameters;        /* Function */
    public LinkedHashMap<String, Symbol> locals; /* Function */

    Symbol() {
        symbolKind = SymbolKind.NOSYM;
        symbolType = new Type(TypeKind.NOTYPE);
        name = "symbol";
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

    public Symbol copy() {
        Symbol copy = new Symbol();
        copy.symbolType = this.symbolType;
        copy.symbolKind = this.symbolKind;
        copy.name = this.name;
        copy.parameters = this.parameters;
        copy.address = this.address;
        copy.global = this.global;
        copy.value = this.value;
        copy.locals = this.locals;

        return copy;
    }
}
