package scanner;

/*
 * Do not change order
 */
public enum TokenKind {
    IDENT("identifier"),
    NUMBER("number literal"),
    CHARACTER("character literal"),
    STRING("string"),

    PLUS("'+'"),
    MINUS("'-'"),
    ASTERISK("'*'"),
    SLASH("'/'"),
    MOD("'%'"),

    EQ("'=='"),
    NE("'!='"),
    LT("'<'"),
    LE("'<='"),
    GT("'>'"),
    GE("'>='"),
    AND("&&"),
    OR("||"),

    ASSIGN("'='"),
    INC("'++'"),
    DEC("'--'"),

    SEMICOLON("';'"),
    COMMA("','"),
    PERIOD("'.'"),

    LPAREN("'('"),
    RPAREN("')'"),
    LBRACK("'['"),
    RBRACK("']'"),
    LBRACE("'{'"),
    RBRACE("'}'"),

    BREAK("'break' statement"),
    STRUCT("'struct'"),
    ELSE("'else' statement"),
    CONST("'const'"),
    IF("'if' statement"),
    NEW("operator new"),
    PRINT("print()"),
    PROGRAM("'program'"),
    READ("read()"),
    RETURN("'return'"),
    VOID("'void'"),
    WHILE("'while'"),
    EOF("end of file"),
    ERROR("ERROR");

    private final String niceName;

    TokenKind(String niceName) {
        this.niceName = niceName;
    }

    @Override
    public String toString() {
        return niceName;
    }
}
