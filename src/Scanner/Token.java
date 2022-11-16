package Scanner;

public class Token {
    public TokenKind    kind;
    public int          line;
    public int          column;
    public int          value;		// for numbers and characters
    public String       text;	    // for names

    @Override
    public String toString() {
        String s = String.format("line: %-4d col: %-4d\t%10s\t",
                line,
                column,
                kind
        );

        switch (kind) {
            case NUMBER: s += value; break;
            case IDENT:  s += text;                 break;
            default:
        }

        return s;
    }
}
