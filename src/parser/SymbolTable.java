package parser;

public class SymbolTable {
    public Scope scope;

    public SymbolTable() {
        scope = new Scope();

        Type intType = new Type();
        intType.typeKind = TypeKind.INT;
        Type charType = new Type();
        charType.typeKind = TypeKind.CHAR;

        insert("int", SymbolKind.TYPE, intType);
        insert("char", SymbolKind.TYPE, charType);
    }

    public void openScope() {
        Scope innerScope = new Scope();
        innerScope.outer = scope;
        scope = innerScope;
    }

    public void closeScope() {
        scope = scope.outer;
    }

    public Symbol insert(String name, SymbolKind kind, Type type) {
        if (find(name) != null) {
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

        while (found == null && scope.outer != null) {
            current = current.outer;
            found = current.locals.getOrDefault(name, null);
        }

        return found;
    }

    public Symbol findField(String name, Type type) {
        return null;
    }
}
