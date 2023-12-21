package dfta.parser;

import dfta.FuncSymb;

import java.util.ArrayList;

public class TAModelTransition {
    public final String q;
    public final ArrayList<String> args;
    public final FuncSymb fn;

    public TAModelTransition(String q, ArrayList<String> args, FuncSymb fn) {
        this.q = q;
        this.args = args;
        this.fn = fn;
    }
}
