package scanner;

public enum TokenKind {
    IDENT("identifier"),
    NUMBER("number literal"),
    CHARACTER("character literal"),

    PLUS("'+'"),
    MINUS("'-'"),
    ASTERISK("'*'"),
    SLASH("'/'"),
    MOD("'%'"),

    EQ("'=='"),
    NEQ("'!='"),
    LES("'<'"),
    LEQ("'<='"),
    GRE("'>'"),
    GEQ("'>='"),
    AND("&&"),
    OR("||"),

    ASSIGN("'=='"),
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
    EOF("End of file"),
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
