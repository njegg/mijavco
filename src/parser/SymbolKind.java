package parser;

public enum SymbolKind {
    CONST("constant"),
    VAR("variable"),
    FUNCTION("function"),
    TYPE("type"),
    NOSYM("");

    private final String niceName;

    SymbolKind(String niceName) {
        this.niceName = niceName;
    }

    @Override
    public String toString() {
        return niceName;
    }
}
