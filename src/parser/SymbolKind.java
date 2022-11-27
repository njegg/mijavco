package parser;

public enum SymbolKind {
    CONST("constant"),
    VAR("variable"),
    METHOD("method"),
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
