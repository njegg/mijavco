package Scanner;

public class Token {
    public TokenCode    code;
    public int          line;
    public int          column;
    public int          pos;

    public int      value;		// for numbers and characters
    public String   text;	    // for names

    @Override
    public String toString() {
        String s = String.format("line: %-4d col: %-4d\t%10s\t",
                line,
                column,
                code
        );

        switch (code) {
            case NUMBER: s += value; break;
            case IDENT:  s += text;                 break;
            default:
        }

        return s;
    }
}
