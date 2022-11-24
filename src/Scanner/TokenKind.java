package Scanner;

public enum TokenKind {
    ERROR,
    IDENT,
    NUMBER,
    CHARACTER,

    PLUS,
    MINUS,
    ASTERISK,
    SLASH,
    MOD,

    EQ,
    NEQ,
    LES,
    LEQ,
    GRE,
    GEQ,
    AND,
    OR,

    ASSIGN,
    INC,
    DEC,

    SEMICOLON,
    COMMA,
    PERIOD,

    LPAREN,
    RPAREN,
    LBRACK,
    RBRACK,
    LBRACE,
    RBRACE,

    BREAK,
    CLASS,
    ELSE,
    CONST,
    IF,
    NEW,
    PRINT,
    PROGRAM,
    READ,
    RETURN,
    VOID,
    WHILE,
    EOF,
}
