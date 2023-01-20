package parser;

import scanner.Token;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Symbol {
    public SymbolKind symbolKind;
    public String name;
    public Type symbolType;
    public int address;
    public boolean isGlobal;
    public int value;

    public LinkedList<Symbol> parameters;        /* Function */
    public LinkedHashMap<String, Symbol> locals; /* Function */

    public int column, line;

    Symbol() {
        symbolKind = SymbolKind.NOSYM;
        symbolType = new Type(TypeKind.NOTYPE);
        name = "symbol";
    }

    Symbol(Token token) {
        this();
        column = token.column;
        line = token.line;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s",
                    symbolKind,
                    symbolType,
                    name
                );
    }

    public Symbol copy() {
        Symbol copy = new Symbol();
        copy.symbolType = this.symbolType;
        copy.symbolKind = this.symbolKind;
        copy.name = this.name;
        copy.parameters = this.parameters;
        copy.address = this.address;
        copy.isGlobal = this.isGlobal;
        copy.value = this.value;
        copy.locals = this.locals;

        return copy;
    }
}
