package dfta.parser;

import java.util.ArrayList;

public class TAModelTransition {
    public final String q;
    public final ArrayList<String> args;
    public final String fname;

    public TAModelTransition(String q, ArrayList<String> args, String fname) {
        this.q = q;
        this.args = args;
        this.fname = fname;
    }
}
