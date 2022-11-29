package parser;

import java.util.HashMap;

public class Type {
    TypeKind typeKind;
    Type arrayType;
    HashMap<String, Symbol> fields; /* Struct */

    public Type() {
        this(TypeKind.NOTYPE);
    }

    public Type(TypeKind kind) {
        typeKind = kind;
    }
}
