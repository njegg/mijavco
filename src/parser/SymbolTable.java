package parser;

import scanner.Token;

import java.util.LinkedList;

public class SymbolTable {
    private final Scope rootScope;
    private final LinkedList<Scope> scopes; // For dumping
    private Scope scope;
    private boolean nextScopeIsLoop;

    public SymbolTable() {
        scope = new Scope(false);
        rootScope = scope;
        scopes = new LinkedList<>();

        insert("int", SymbolKind.TYPE, new Type(TypeKind.INT), null);
        insert("char", SymbolKind.TYPE, new Type(TypeKind.CHAR), null);

        Symbol nullSymbol = insert("null", SymbolKind.CONST, new Type(TypeKind.REFERENCE), null);
        nullSymbol.symbolType.fields = null;

        Symbol ctoi = insert("ctoi", SymbolKind.FUNCTION, new Type(TypeKind.INT), null);
        openScope(ctoi);
        ctoi.parameters = new LinkedList<>();
        ctoi.parameters.addLast(insert("c", SymbolKind.VAR, new Type(TypeKind.CHAR), null));
        closeScope();

        Symbol itoc = insert("itoc", SymbolKind.FUNCTION, new Type(TypeKind.CHAR), null);
        openScope(itoc);
        itoc.parameters = new LinkedList<>();
        itoc.parameters.addLast(insert("i", SymbolKind.VAR, new Type(TypeKind.INT), null));
        closeScope();

        // TODO garbage solution
        Symbol len = insert("len", SymbolKind.FUNCTION, new Type(TypeKind.INT), null);
        openScope(len);
        len.parameters = new LinkedList<>();
        len.parameters.addLast(insert("arr", SymbolKind.VAR, new Type(TypeKind.NOTYPE), null));
        closeScope();
    }

    public void openScope(Symbol function) {
        Scope innerScope = new Scope(nextScopeIsLoop);
        nextScopeIsLoop = false;
        innerScope.outer = scope;
        scope.inners.add(innerScope);
        scope = innerScope;

        scope.function = function;

        scopes.addLast(scope);
    }

    public void closeScope() {
        scope = scope.outer;
    }

    public Symbol insert(String name, SymbolKind kind, Type type, Token token) {
        Symbol exists = find(name);
        if (exists != null) {
            Parser.error(name + " is already in use as a " + exists.symbolKind);
            return null;
        }

        Symbol symbol = token == null ? new Symbol() : new Symbol(token);
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

    public Symbol getScopeFunction() {
        return scope.getFunction();
    }
    public void setNextScopeIsLoop() { nextScopeIsLoop = true; }
    public boolean isScopeLoop() { return scope.isLoop(); }

    public void dump() { rootScope.print(); }
}
