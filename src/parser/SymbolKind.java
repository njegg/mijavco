package parser;

public enum SymbolKind {
    CONST("constant"),
    VAR("variable"),
    FUNCTION("function"),
    TYPE("type");

    private String niceName;

    SymbolKind(String niceName) {
        this.niceName = niceName;
    }

    @Override
    public String toString() {
        return niceName;
    }
}
