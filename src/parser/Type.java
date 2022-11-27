package parser;

import java.util.HashMap;

public class Type {
    TypeKind typeKind;
    TypeKind arrayTypeKind;
    HashMap<String, Symbol> fields; /* Struct */
}
