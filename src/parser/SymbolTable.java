package parser;

import codegen.CodeBuffer;
import codegen.Operand;
import scanner.Token;

import java.util.HashMap;
import java.util.LinkedList;

public class SymbolTable {
    private final Scope rootScope;
    private final LinkedList<Scope> scopes; // For dumping
    private Scope scope;
    private boolean nextScopeIsLoop;

    private int nextGlobalAddress = 0;
    private int nextLocalAddress = 0;

    public SymbolTable() {
        scope = new Scope(false);
        rootScope = scope;
        scopes = new LinkedList<>();

        Type intType = new Type(TypeKind.INT);
        Type charType = new Type(TypeKind.CHAR);
        insert("int", SymbolKind.TYPE, intType , null);
        insert("char", SymbolKind.TYPE, charType, null);

        Type intArrType = new Type(TypeKind.REFERENCE);
        intArrType.arrayType = intType;
        intArrType.name = "int[]";

        Type charArrType = new Type(TypeKind.REFERENCE);
        charArrType.arrayType = charType;
        charArrType.name = "char[]";

        insert("int[]", SymbolKind.TYPE, intArrType, null);
        insert("char[]", SymbolKind.TYPE, charArrType, null);

        Symbol nullSymbol = insert("null", SymbolKind.CONST, new Type(TypeKind.REFERENCE), null);
        nullSymbol.symbolType.fields = new HashMap<>();
        nullSymbol.symbolType.arrayType = new Type(TypeKind.NOTYPE);

        Symbol ctoi = insert("ctoi", SymbolKind.FUNCTION, new Type(TypeKind.INT), null);
        openScope(ctoi);
        ctoi.parameters = new LinkedList<>();
        ctoi.parameters.addLast(insert("c", SymbolKind.VAR, new Type(TypeKind.CHAR), null));
        closeScope();
        ctoi.address = -1;

        Symbol rand = insert("rand", SymbolKind.FUNCTION, new Type(TypeKind.INT), null);
        openScope(rand);
        rand.parameters = new LinkedList<>();
        rand.parameters.addLast(insert("max", SymbolKind.VAR, new Type(TypeKind.INT), null));
        closeScope();
        rand.address = -1;

        Symbol itoc = insert("itoc", SymbolKind.FUNCTION, new Type(TypeKind.CHAR), null);
        openScope(itoc);
        itoc.parameters = new LinkedList<>();
        itoc.parameters.addLast(insert("i", SymbolKind.VAR, new Type(TypeKind.INT), null));
        closeScope();
        itoc.address = -1;

        Symbol len = insert("len", SymbolKind.FUNCTION, new Type(TypeKind.INT), null);
        openScope(len);
        len.parameters = new LinkedList<>();
        Type paramType = new Type(TypeKind.REFERENCE);
        paramType.arrayType = new Type(TypeKind.NOTYPE);
        paramType.name = "ref[]";
        len.parameters.addLast(insert("__arr", SymbolKind.VAR, paramType, null));
        len.address = -1;

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
        if (scope.function != null) {
            nextLocalAddress = 0;
        }

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
        symbol.isGlobal = scope.function == null;

        if (symbol.symbolKind == SymbolKind.CONST || symbol.symbolKind == SymbolKind.VAR) {
            symbol.address = symbol.isGlobal ? nextGlobalAddress++ : nextLocalAddress++;
        }

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
        return type.fields.getOrDefault(name, null);
    }

    public Symbol getScopeFunction() {
        return scope.getFunction();
    }

    public void setNextScopeIsLoop() { nextScopeIsLoop = true; }

    public boolean isScopeLoop() { return scope.isLoop(); }

    public int numberOfLocals() { return scope.locals.size(); }

    public void dump() { rootScope.print(); }

    public String getFunctionNameByAddress(int address) {
        return rootScope.locals.values()
                .stream()
                .filter(s -> s.symbolKind == SymbolKind.FUNCTION && s.address == address)
                .findFirst()
                .map(s -> s.name)
                .orElseThrow();
    }
}
