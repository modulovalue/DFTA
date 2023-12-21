package dfta;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class DeterminiserTextBook {
    final LinkedHashSet<LinkedHashSet<String>> qd = new LinkedHashSet<>();
    final LinkedHashSet<DTransition> deltad = new LinkedHashSet<>();

    public DeterminiserTextBook(IndicesB idx, IndicesA indices_a) {
        for (;;) {
            boolean new_transition = false;
            final ArrayList<LinkedHashSet<String>> qd_prev = new ArrayList<>(qd);
            final int qd_size = qd_prev.size();
            for (FuncSymb f : indices_a.constructors) {
                if (f.arity == 0) {
                    final LinkedHashSet<String> q0 = rhs_set(idx, idx.f_index.get(f));
                    if (!q0.isEmpty()) {
                        qd.add(q0);
                        new_transition |= deltad.add(new DTransition(f, q0, new ArrayList<>()));
                    }
                } else {
                    final double targetK = Math.pow(qd_size, f.arity);
                    for (int k = 0; k < targetK; k++) { // enumerate the delta-tuples
                        int temp = k;
                        final ArrayList<LinkedHashSet<String>> qtuple = new ArrayList<>();
                        final ArrayList<BitSet> deltatuple = new ArrayList<>();
                        for (int m = 0; m < f.arity; m++) {
                            qtuple.add(m, qd_prev.get(temp % qd_size));
                            BitSet result = or_all(idx, new BitSet(), qtuple.get(m), f, m);
                            deltatuple.add(m, result);
                            temp = temp / qd_size;
                        }
                        final LinkedHashSet<String> q0 = rhs_set(idx, and_all(deltatuple));
                        if (!q0.isEmpty()) {
                            qd.add(q0);
                            new_transition |= deltad.add(new DTransition(f, q0, qtuple));
                        }
                    }
                }
            }
            if (!new_transition) {
                break;
            }
        }
    }

    static BitSet or_all(final IndicesB idx, final BitSet init, Iterable<String> qs, FuncSymb f, int j) {
        final BitSet result = init;
        final LinkedHashMap<String, BitSet> lhsmap = idx.lhs_f.get(f).get(j);
        for (final String q : qs) {
            if (lhsmap.containsKey(q)) {
                result.or(lhsmap.get(q));
            }
        }
        return result;
    }

    static BitSet and_all(ArrayList<BitSet> values) {
        final BitSet result = (BitSet) values.get(0).clone();
        for (int i = 1; i < values.size(); i++) {
            result.and(values.get(i));
        };
        return result;
    }

    static LinkedHashSet<String> rhs_set(final IndicesB idx, final BitSet tSet) {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int i = tSet.nextSetBit(0); i >= 0; i = tSet.nextSetBit(i + 1)) {
            result.add(idx.transition_by_id.get(i + 1).q0);
        }
        return result;
    }
}

class DTransition {
    final FuncSymb f;
    final LinkedHashSet<String> q0;
    final ArrayList<LinkedHashSet<String>> lhs;

    public DTransition(FuncSymb f, LinkedHashSet<String> q0, ArrayList<LinkedHashSet<String>> lhs) {
        this.f = f;
        this.q0 = q0;
        this.lhs = lhs;
    }

    @Override
    public int hashCode() {
        return f.hashCode() * 31 + lhs.hashCode() * 17 + q0.hashCode();
    }

    @Override
    public boolean equals(Object g) {
        if (g == null) {
            return false;
        }
        if (getClass() != g.getClass()) {
            return false;
        }
        DTransition g1 = (DTransition) g;
        return (f.equals(g1.f) && lhs.equals(g1.lhs) && q0.equals(g1.q0));
    }
}
