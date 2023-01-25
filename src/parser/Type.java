package parser;

import java.util.HashMap;

public class Type {
    public TypeKind typeKind;
    public Type arrayType;
    public HashMap<String, Symbol> fields; /* Struct */
    public String name;
    public int sizeInBytes;

    public Type(TypeKind kind) {
        typeKind = kind;
        sizeInBytes = 4; // Default, may change later

        if (typeKind == TypeKind.REFERENCE) {
            fields = new HashMap<>();
        } else if (typeKind == TypeKind.CHAR) {
            sizeInBytes = 1;
        }

        name = typeKind.toString();
    }

    public boolean usedInArithmetics() {
        return typeKind != TypeKind.NOTYPE && typeKind != TypeKind.REFERENCE;
    }

    public boolean isStruct() {
        return fields != null;
    }

    public boolean isArray() {
        return arrayType != null;
    }

    @Override
    public boolean equals(Object obj) {
        Type that = (Type) obj;
        if (this.typeKind != that.typeKind) return false;

        if (this.typeKind == TypeKind.REFERENCE) {
            if (that.arrayType != null && that.fields != null) return true; // special case for null

            if (this.arrayType != null) {
                return that.arrayType != null &&
                        (this.arrayType.equals(that.arrayType) || that.arrayType.typeKind == TypeKind.NOTYPE);
            } else {
                // Compares if they have same symbols (fields) by comparing their types
                // If fields are null, it's a special case for 'null constant'
                if (this.fields == null || that.fields == null) return true;
                return this.fields.equals(that.fields);
            }
        }

        return true;
    }

    public boolean assignableTo(Type that) {
        return that.typeKind != TypeKind.NOTYPE && this.typeKind != TypeKind.NOTYPE && that.equals(this);
    }


    @Override
    public String toString() {
        return name;
    }
}
