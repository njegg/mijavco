package codegen;

public enum Instruction {
    NOP("nop"),                 // 0
    LOAD_0("load_0"),           // 1
    LOAD_1("load_1"),           // 2
    LOAD_2("load_2"),           // 3
    LOAD_3("load_3"),           // 4
    LOAD_4("load_4"),           // 5
    LOAD_5("load_5"),           // 6
    LOAD("load"),               // 7
    STORE_0("store_0"),         // 8
    STORE_1("store_1"),         // 9
    STORE_2("store_2"),         // 10
    STORE_3("store_3"),         // 11
    STORE_4("store_4"),         // 12
    STORE_5("store_5"),         // 13
    STORE("store"),             // 14
    LOAD_GLOBAL("lglobal"),     // 15
    STORE_GLOBAL("sglobal"),    // 16
    LOAD_FIELD("lfield"),       // 17
    STORE_FIELD("sfield"),      // 18
    CONST("const"),             // 19
    CONST_M1("const_m1"),       // 20
    CONST_0("const_0"),         // 21
    CONST_1("const_1"),         // 22
    CONST_2("const_2"),         // 23
    CONST_3("const_3"),         // 24
    CONST_4("const_4"),         // 25
    CONST_5("const_5"),         // 26
    ADD("add"),                 // 27
    SUB("sub"),                 // 28
    MUL("mul"),                 // 29
    DIV("div"),                 // 30
    REM("rem"),                 // 31
    NEG("neg"),                 // 32
    SHL("shl"),                 // 33
    SHR("shr"),                 // 34
    INC("inc"),                 // 35
    NEW("new"),                 // 36
    NEW_ARRAY("newarr"),        // 37
    ARRAY_LOAD("aload"),        // 38
    ARRAY_STORE("astore"),      // 39
    BARRAY_LOAD("baload"),      // 40
    BARRAY_STORE("bastore"),    // 41
    LENGTH("length"),           // 42
    POP("pop"),                 // 43
    DUP("dup"),                 // 44
    DUP2("dup2"),               // 45
    JMP("jmp"),                 // 46
    JCC("jcc"),                 // 47
    CALL("call"),               // 48
    RETURN("return"),           // 49
    ENTER("enter"),             // 50
    EXIT("exit"),               // 51
    READ("read"),               // 52
    PRINT("print"),             // 53
    BREAD("bread"),             // 54
    BPRINT("bprint"),           // 55
    TRAP("trap");               // 56

    Instruction(String niceName) {
        this.niceName = niceName;
    }

    public final String niceName;
}
