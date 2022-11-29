package parser;

import java.util.HashMap;

public class Scope {
    Scope outer;
    HashMap<String, Symbol> locals;
    Symbol function;

    int variableCount;

    public Scope() {
        locals = new HashMap<>();
        variableCount = 0;
        outer = null;
    }
}
