package parser;

import java.util.LinkedList;

public class SymbolTable {
    private Scope scope;
    private LinkedList<Scope> scopes; // For dumping

    public SymbolTable() {
        scope = new Scope();
        scopes = new LinkedList<>();

        insert("int", SymbolKind.TYPE, new Type(TypeKind.INT));
        insert("char", SymbolKind.TYPE, new Type(TypeKind.CHAR));

        Symbol nullSymbol = insert("null", SymbolKind.CONST, new Type(TypeKind.REFERENCE));
        nullSymbol.symbolType.fields = null;

        insert("ctoi", SymbolKind.FUNCTION, new Type(TypeKind.INT));
        insert("itoc", SymbolKind.FUNCTION, new Type(TypeKind.INT));
    }

    public void openScope(Symbol function) {
        Scope innerScope = new Scope();
        innerScope.outer = scope;
        scope = innerScope;

        scope.function = function;

        scopes.addLast(scope);
    }

    public void closeScope() {
        scope = scope.outer;
    }

    public Symbol insert(String name, SymbolKind kind, Type type) {
        Symbol exists = find(name);
        if (exists != null) {
            Parser.error(name + " is already in use as a " + exists.symbolKind);
            return null;
        }

        Symbol symbol = new Symbol();
        symbol.symbolType = type;
        symbol.name = name;
        symbol.symbolKind = kind;

        scope.locals.put(name, symbol);

        return symbol;
    }

    public Symbol find(String name) {
        Symbol found = scope.locals.getOrDefault(name, null);
        Scope current = scope;

        while (found == null && current.outer != null) {
            current = current.outer;
            found = current.locals.getOrDefault(name, null);
        }

        return found;
    }

    public Symbol findField(String name, Type type) {
        return null;
    }

    public TypeKind getScopeFunctionType() {
        return scope.function == null ? null : scope.function.symbolType.typeKind;
    }

    public void dump() {
        scopes.forEach(s -> scope.locals.values().forEach(System.out::println));
    }
}
