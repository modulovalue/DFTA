package dfta;

import dfta.parser.TAModel;
import dfta.parser.TAModelTransition;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

class IndicesA {
    final LinkedHashSet<FTATransition> transitions = new LinkedHashSet<>();
    final LinkedHashSet<String> states = new LinkedHashSet<>();
    final LinkedHashSet<String> final_states = new LinkedHashSet<>();
    final LinkedHashSet<FuncSymb> constructors = new LinkedHashSet<>();

    public IndicesA(final TAModel data, String ftaId, int init_trans_count, boolean complete) {
        // region init delta
        for (final TAModelTransition transition : data.transitions) {
            init_trans_count++;
            final ArrayList<String> args1 = new ArrayList<>();
            for (final String s : transition.args) {
                args1.add(ftaId + s);
            }
            final FuncSymb fn = transition.fn;
            constructors.add(fn);
            final String q = ftaId + transition.q;
            states.add(q);
            transitions.add(new FTATransition(fn, q, args1, init_trans_count));
        }
        // endregion
        // region final states
        for (final String finalState : data.final_states) {
            final_states.add(ftaId + finalState);
        }
        // endregion
        // region complete delta
        if (complete) {
            for (FuncSymb funcSymb : constructors) {
                init_trans_count++;
                final ArrayList<String> args = new ArrayList<>();
                for (int j = 0; j < funcSymb.arity; j++) {
                    args.add(j, "'$any'");
                }
                transitions.add(new FTATransition(funcSymb, "'$any'", args, init_trans_count));
            }
        }
        // endregion
    }
}

class IndicesB {
    final LinkedHashMap<FuncSymb, ArrayList<LinkedHashMap<String, BitSet>>> lhs_f = new LinkedHashMap<>();
    final LinkedHashMap<Integer, FTATransition> transition_by_id = new LinkedHashMap<>();
    final LinkedHashMap<FuncSymb, BitSet> f_index = new LinkedHashMap<>();
    final LinkedHashMap<String, LinkedHashMap<FuncSymb, LinkedHashSet<Integer>>> rhs_idx = new LinkedHashMap<>();
    final LinkedHashMap<FuncSymb, LinkedHashSet<Integer>> rhs_f_idx = new LinkedHashMap<>();

    public IndicesB(final IndicesA indices_a) {
        for (final FTATransition t : indices_a.transitions) {
            transition_by_id.put(t.m, t);
            int arity = t.f.arity;
            if (!f_index.containsKey(t.f)) {
                f_index.put(t.f, new BitSet(indices_a.transitions.size()));
            }
            f_index.get(t.f).set(t.m - 1);  // set the bit for the mth transition
            if (!lhs_f.containsKey(t.f)) {
                lhs_f.put(t.f, new ArrayList<>());
                for (int i = 0; i < arity; i++) {
                    lhs_f.get(t.f).add(i, new LinkedHashMap<>());
                }
            }
            if (!rhs_idx.containsKey(t.q0)) {  // keep index for rhs states
                rhs_idx.put(t.q0, new LinkedHashMap<>());
            }
            if (!rhs_idx.get(t.q0).containsKey(t.f)) {
                rhs_idx.get(t.q0).put(t.f, new LinkedHashSet<>());
            }
            rhs_idx.get(t.q0).get(t.f).add(t.m - 1);
            if (!rhs_f_idx.containsKey(t.f)) {
                rhs_f_idx.put(t.f, new LinkedHashSet<>());
            }
            rhs_f_idx.get(t.f).add(t.m - 1);
            final ArrayList<LinkedHashMap<String, BitSet>> qmap = lhs_f.get(t.f);
            final ArrayList<String> args = t.lhs;
            for (int i = 0; i < arity; i++) {
                final String q = args.get(i);
                if (!qmap.get(i).containsKey(q)) {
                    qmap.get(i).put(q, new BitSet(indices_a.transitions.size()));
                }
                qmap.get(i).get(q).set(t.m - 1);
            }
        }
    }
}

class FTATransition implements Comparable<FTATransition> {
    FuncSymb f;
    String q0;
    ArrayList<String> lhs;
    final int m;

    public FTATransition(FuncSymb f, String q0, ArrayList<String> lhs, int m) {
        this.f = f;
        this.q0 = q0;
        this.lhs = lhs;
        this.m = m;
    }

    @Override
    public int hashCode() {
        return m;
    }

    @Override
    public boolean equals(Object g) {
        return (m == ((FTATransition) g).m);
    }

    @Override
    public int compareTo(FTATransition g) {
        int c = 0;
        if (m == g.m) c = 0;
        if (m > g.m) c = 1;
        if (m < g.m) c = -1;
        return c;
    }
}
