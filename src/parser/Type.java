package parser;

import javax.lang.model.type.NoType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Type {
    TypeKind typeKind;
    Type arrayType;
    HashMap<String, Symbol> fields; /* Struct */
    String name;

    public Type(TypeKind kind) {
        typeKind = kind;
        if (typeKind == TypeKind.REFERENCE) {
            fields = new HashMap<>();
        }
    }

    @Override
    public boolean equals(Object obj) {
        Type that = (Type) obj;
        if (this.typeKind != that.typeKind) return false;

        if (this.typeKind == TypeKind.REFERENCE) {
            if (this.arrayType != null) {
                return that.arrayType != null && this.arrayType.equals(that.arrayType);
            } else {
                // Compares if they have same symbols (fields) by comparing their types
                // If fields are null, it's a special case for 'null constant'
                if (this.fields == null || that.fields == null) return true;
                return this.fields.equals(that.fields);
            }
        }

        return true;
    }

    public boolean usedInArithmetics() {
        return typeKind != TypeKind.NOTYPE;
    }

    @Override
    public String toString() {
        return name;
    }
}
