package parser;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Scope {
    Scope outer;
    LinkedList<Scope> inners;
    LinkedHashMap<String, Symbol> locals;
    Symbol function;

    int variableCount;

    private final boolean isLoop;

    public Scope(boolean isLoop) {
        this.isLoop = isLoop;

        locals = new LinkedHashMap<>();
        inners = new LinkedList<>();
        variableCount = 0;
        outer = null;
    }

    public void print() {
        locals.values().forEach(System.out::println);
        inners.forEach(Scope::print);
    }

    public boolean isLoop() {
        return isLoop || outer != null && outer.isLoop();
    }

    public Symbol getFunction() {
        return function != null ? function : (outer != null ? outer.getFunction() : null);
    }
}
