package parser;

public enum TypeKind {
    NOTYPE("notype"),
    INT("int"),
    CHAR("char"),
    REFERENCE("ref");

    private final String niceName;

    TypeKind(String name) {
        niceName = name;
    }

    @Override
    public String toString() {
        return niceName;
    }
}
