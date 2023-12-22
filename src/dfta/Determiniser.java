package dfta;

import dfta.parser.TAModel;
import dfta.parser.TAModelTransition;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class Determiniser {
    // https://arxiv.org/abs/1511.03595
    // - don cares could reduce the amount of product transitions, see git history.
    // - this could support inclusion testing, see git history.
    // - this approach might be able to be used for more efficient minimization, see git history.
    public static DeterminiserResultP gallagher_product(final Index index) {
        final LinkedHashSet<LinkedHashSet<String>> q_d = new LinkedHashSet<>();
        final ArrayList<PTransition> delta_p = new ArrayList<>();
        // region states
        // region init
        final LinkedHashMap<Symbol, ArrayList<LinkedHashSet<BitSet>>> psi = new LinkedHashMap<>();
        final LinkedHashMap<Symbol, ArrayList<LinkedHashSet<BitSet>>> phi = new LinkedHashMap<>();
        final LinkedHashMap<Symbol, ArrayList<LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>>>> t_inverse_table = new LinkedHashMap<>();
        for (final Symbol f : index.a.constructors) {
            if (f.arity == 0) {
                final LinkedHashSet<String> q0 = rhs_set(index.b, index.b.f_index.get(f));
                if (!q0.isEmpty()) {
                    q_d.add(q0);
                }
            } else {
                final ArrayList<LinkedHashSet<BitSet>> psi_f = new ArrayList<>(f.arity);
                final ArrayList<LinkedHashSet<BitSet>> phi_f = new ArrayList<>(f.arity);
                final ArrayList<LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>>> t_inverse_table_f = new ArrayList<>();
                for (int j = 0; j < f.arity; j++) {
                    psi_f.add(j, new LinkedHashSet<>());
                    phi_f.add(j, new LinkedHashSet<>());
                    t_inverse_table_f.add(j, new LinkedHashMap<>());
                }
                psi.put(f, psi_f);
                phi.put(f, phi_f);
                t_inverse_table.put(f, t_inverse_table_f);
            }
        }
        // endregion
        // region main
        final ArrayList<LinkedHashSet<String>> qdnew = new ArrayList<>(q_d);
        final LinkedHashSet<LinkedHashSet<String>> qdnew_1 = new LinkedHashSet<>();
        while (!qdnew.isEmpty()) {
            qdnew_1.clear();
            for (final Symbol f : index.a.constructors) {
                if (f.arity > 0) {
                    // Initialise the Phi and Psi tuples.
                    final ArrayList<LinkedHashSet<BitSet>> psi_f = psi.get(f);
                    final ArrayList<LinkedHashSet<BitSet>> phi_f = phi.get(f);
                    for (int j = 0; j < f.arity; j++) {
                        final LinkedHashSet<BitSet> phi_f_j = new LinkedHashSet<>();
                        for(final LinkedHashSet<String> qs : qdnew) {
                            // region lhs_set
                            final BitSet h = or_all(index.b, new BitSet(index.a.transitions.size()), qs, f, j);
                            // Tabulate result for the t_inverse function.
                            if (!h.isEmpty()) {
                                final LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>> hm = t_inverse_table.get(f).get(j);
                                if (!hm.containsKey(h)) {
                                    hm.put(h, new LinkedHashSet<>());
                                }
                                hm.get(h).add(qs);
                            }
                            // endregion
                            if (!h.isEmpty()) {
                                phi_f_j.add(h);
                            }
                        }
                        // Remove sets already computed for jth argument.
                        phi_f_j.removeAll(psi_f.get(j));
                        phi_f.set(j, phi_f_j);
                    }
                    for (int j = 0; j < f.arity; j++) {
                        // if size of phi_f[j] = 0 then prod will be 0
                        if (phi_f.get(j).size() > 0) {
                            final ArrayList<ArrayList<BitSet>> psi_phi_tuple = new ArrayList<>();
                            for (int k = 0; k < f.arity; k++) {
                                if (k < j) {
                                    psi_phi_tuple.add(k, new ArrayList<>(psi_f.get(k)));
                                } else if (k == j) {
                                    psi_phi_tuple.add(k, new ArrayList<>(phi_f.get(j)));
                                } else {
                                    psi_phi_tuple.add(k, new ArrayList<>(phi_f.get(k)));
                                    psi_phi_tuple.get(k).addAll(psi_f.get(k));
                                }
                            }
                            int prod = 1;
                            for (int k = 0; k < f.arity; k++) {
                                prod = prod * psi_phi_tuple.get(k).size();
                            }
                            // enumerate the delta-tuples (cartesian product)
                            for (int k = 0; k < prod; k++) {
                                int temp = k;
                                // Re-initialise delta-tuple
                                final ArrayList<BitSet> deltatuple = new ArrayList<>();
                                for (int m = 0; m < f.arity; m++) {
                                    final int z = psi_phi_tuple.get(m).size();
                                    deltatuple.add(m, psi_phi_tuple.get(m).get(temp % z));
                                    temp = temp / z;
                                }
                                final LinkedHashSet<String> q0 = rhs_set(index.b, and_all(deltatuple));
                                if (!q0.isEmpty()) {
                                    if (q_d.add(q0)) {
                                        qdnew_1.add(q0);
                                    }
                                }
                            }
                        }
                    }
                    for (int j = 0; j < f.arity; j++) {
                        psi_f.get(j).addAll(phi_f.get(j));
                    }
                }
            }
            qdnew.clear();
            qdnew.addAll(qdnew_1);
        }
        // endregion
        // endregion
        // region transitions
        for (final Symbol f : index.a.constructors) {
            if (f.arity == 0) {
                final LinkedHashSet<String> q0 = rhs_set(index.b, index.b.f_index.get(f));
                if (!q0.isEmpty()) {
                    delta_p.add(new PTransition(f, q0, new ArrayList<>()));
                }
            } else {
                final ArrayList<ArrayList<BitSet>> psi_tuple = new ArrayList<>();
                final ArrayList<BitSet> delta_tuple = new ArrayList<>();
                for (int j = 0; j < f.arity; j++) {
                    psi_tuple.add(j, new ArrayList<>(psi.get(f).get(j)));
                    delta_tuple.add(j, new BitSet(index.a.transitions.size()));
                }
                int prod = 1;
                for (int j = 0; j < f.arity; j++) {
                    prod = prod * psi_tuple.get(j).size();
                }
                for (int j = 0; j < prod; j++) {
                    int temp = j;
                    for (int k = 0; k < f.arity; k++) {
                        final int z = psi_tuple.get(k).size();
                        delta_tuple.set(k, psi_tuple.get(k).get(temp % z));
                        temp = temp / z;
                    }
                    final LinkedHashSet<String> q0 = rhs_set(index.b, and_all(delta_tuple));
                    if (!q0.isEmpty()) {
                        final ArrayList<LinkedHashSet<LinkedHashSet<String>>> lhs = new ArrayList<>();
                        for (int m = 0; m < f.arity; m++) {
                            lhs.add(m, t_inverse_table.get(f).get(m).get(delta_tuple.get(m)));
                        }
                        delta_p.add(new PTransition(f, q0, lhs));
                    }
                }
            }
        }
        // endregion
        return new DeterminiserResultP(q_d, delta_p);
    }

    // https://jacquema.gitlabpages.inria.fr/files/tata.pdf
    public static DeterminiserResultD tata(final Index index) {
        final LinkedHashSet<LinkedHashSet<String>> q_d = new LinkedHashSet<>();
        final LinkedHashSet<DTransition> delta_d = new LinkedHashSet<>();
        for (;;) {
            boolean new_transition = false;
            final ArrayList<LinkedHashSet<String>> qd_prev = new ArrayList<>(q_d);
            final int qd_size = qd_prev.size();
            for (Symbol f : index.a.constructors) {
                if (f.arity == 0) {
                    final LinkedHashSet<String> q0 = rhs_set(index.b, index.b.f_index.get(f));
                    if (!q0.isEmpty()) {
                        q_d.add(q0);
                        new_transition |= delta_d.add(new DTransition(f, q0, new ArrayList<>()));
                    }
                } else {
                    final double targetK = Math.pow(qd_size, f.arity);
                    for (int k = 0; k < targetK; k++) { // enumerate the delta-tuples
                        int temp = k;
                        final ArrayList<LinkedHashSet<String>> qtuple = new ArrayList<>();
                        final ArrayList<BitSet> deltatuple = new ArrayList<>();
                        for (int m = 0; m < f.arity; m++) {
                            qtuple.add(m, qd_prev.get(temp % qd_size));
                            BitSet result = or_all(index.b, new BitSet(), qtuple.get(m), f, m);
                            deltatuple.add(m, result);
                            temp = temp / qd_size;
                        }
                        final LinkedHashSet<String> q0 = rhs_set(index.b, and_all(deltatuple));
                        if (!q0.isEmpty()) {
                            q_d.add(q0);
                            new_transition |= delta_d.add(new DTransition(f, q0, qtuple));
                        }
                    }
                }
            }
            if (!new_transition) {
                break;
            }
        }
        return new DeterminiserResultD(q_d, delta_d);
    }

    private static BitSet or_all(final IndicesB idx, final BitSet init, final Iterable<String> qs, final Symbol f, final int j) {
        final LinkedHashMap<String, BitSet> lhsmap = idx.lhs_f.get(f).get(j);
        for (final String q : qs) {
            if (lhsmap.containsKey(q)) {
                init.or(lhsmap.get(q));
            }
        }
        return init;
    }

    private static BitSet and_all(final ArrayList<BitSet> values) {
        final BitSet result = (BitSet) values.get(0).clone();
        for (int i = 1; i < values.size(); i++) {
            result.and(values.get(i));
        }
        return result;
    }

    private static LinkedHashSet<String> rhs_set(final IndicesB idx, final BitSet set) {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
            result.add(idx.transition_by_id.get(i).q0);
        }
        return result;
    }
}

class DeterminiserResultD {
    final LinkedHashSet<LinkedHashSet<String>> q_d;
    final LinkedHashSet<DTransition> delta_d;

    DeterminiserResultD(final LinkedHashSet<LinkedHashSet<String>> q_d, final LinkedHashSet<DTransition> delta_d) {
        this.q_d = q_d;
        this.delta_d = delta_d;
    }
}

class DeterminiserResultP {
    final LinkedHashSet<LinkedHashSet<String>> q_d;
    final ArrayList<PTransition> delta_p;

    DeterminiserResultP(final LinkedHashSet<LinkedHashSet<String>> q_d, final ArrayList<PTransition> delta_p) {
        this.q_d = q_d;
        this.delta_p = delta_p;
    }

    public long delta_d_count() {
        double count = 0;
        double q_d_size = q_d.size();
        for (final PTransition delta_d_1 : delta_p) {
            double t_count = 1.0;
            for (final LinkedHashSet<LinkedHashSet<String>> lh : delta_d_1.lhs) {
                double arg_size = lh.size();
                if (arg_size == 0) arg_size = q_d_size;  // don't care argument
                t_count = t_count * arg_size;
            }
            count = count + t_count;
        }
        return Math.round(count);
    }
}

class DTransition {
    final Symbol f;
    final LinkedHashSet<String> q0;
    final ArrayList<LinkedHashSet<String>> lhs;

    public DTransition(final Symbol f, final LinkedHashSet<String> q0, final ArrayList<LinkedHashSet<String>> lhs) {
        this.f = f;
        this.q0 = q0;
        this.lhs = lhs;
    }

    @Override
    public int hashCode() {
        return f.hashCode() * 31 + lhs.hashCode() * 17 + q0.hashCode();
    }

    @Override
    public boolean equals(final Object g) {
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

class PTransition {
    final Symbol f;
    final LinkedHashSet<String> q0;
    final ArrayList<LinkedHashSet<LinkedHashSet<String>>> lhs;

    public PTransition(final Symbol f, final LinkedHashSet<String> q0, final ArrayList<LinkedHashSet<LinkedHashSet<String>>> lhs) {
        this.f = f;
        this.q0 = q0;
        this.lhs = lhs;
    }
}

class Index {
    final IndicesA a;
    final IndicesB b;

    public Index(TAModel model, boolean complete) {
        a = new IndicesA(model, complete);
        b = new IndicesB(a);
    }
}

class IndicesA {
    final LinkedHashSet<Transition> transitions = new LinkedHashSet<>();
    final LinkedHashSet<String> states = new LinkedHashSet<>();
    final LinkedHashSet<String> final_states = new LinkedHashSet<>();
    final LinkedHashSet<Symbol> constructors = new LinkedHashSet<>();

    public IndicesA(final TAModel data, final boolean complete) {
        int cur_transition_id = 0;
        // region init delta
        for (final TAModelTransition transition : data.transitions) {
            final ArrayList<String> args1 = new ArrayList<>(transition.args);
            final Symbol fn = new Symbol(transition.fname, transition.args.size());
            constructors.add(fn);
            states.add(transition.q);
            transitions.add(new Transition(fn, transition.q, args1, cur_transition_id));
            cur_transition_id++;
        }
        // endregion
        // region final states
        final_states.addAll(data.final_states);
        // endregion
        // region complete delta
        if (complete) {
            for (final Symbol symb : constructors) {
                final ArrayList<String> args = new ArrayList<>();
                for (int j = 0; j < symb.arity; j++) {
                    args.add(j, "'$any'");
                }
                transitions.add(new Transition(symb, "'$any'", args, cur_transition_id));
                cur_transition_id++;
            }
        }
        // endregion
    }
}

class IndicesB {
    final LinkedHashMap<Symbol, BitSet> f_index = new LinkedHashMap<>();
    final LinkedHashMap<Integer, Transition> transition_by_id = new LinkedHashMap<>();
    final LinkedHashMap<Symbol, ArrayList<LinkedHashMap<String, BitSet>>> lhs_f = new LinkedHashMap<>();
    final LinkedHashMap<String, LinkedHashMap<Symbol, LinkedHashSet<Integer>>> rhs_idx = new LinkedHashMap<>();
    final LinkedHashMap<Symbol, LinkedHashSet<Integer>> rhs_f_idx = new LinkedHashMap<>();

    public IndicesB(final IndicesA indices_a) {
        // region transition_by_id
        for (final Transition t : indices_a.transitions) {
            transition_by_id.put(t.m, t);
        }
        // endregion
        // region f_index
        for (final Transition t : indices_a.transitions) {
            if (!f_index.containsKey(t.f)) {
                f_index.put(t.f, new BitSet(indices_a.transitions.size()));
            }
            f_index.get(t.f).set(t.m);  // set the bit for the mth transition
        }
        // endregion
        // region lhs_f
        for (final Transition t : indices_a.transitions) {
            int arity = t.f.arity;
            if (!lhs_f.containsKey(t.f)) {
                lhs_f.put(t.f, new ArrayList<>());
                for (int i = 0; i < arity; i++) {
                    lhs_f.get(t.f).add(i, new LinkedHashMap<>());
                }
            }
            final ArrayList<LinkedHashMap<String, BitSet>> qmap = lhs_f.get(t.f);
            final ArrayList<String> args = t.lhs;
            for (int i = 0; i < arity; i++) {
                final String q = args.get(i);
                if (!qmap.get(i).containsKey(q)) {
                    qmap.get(i).put(q, new BitSet(indices_a.transitions.size()));
                }
                qmap.get(i).get(q).set(t.m);
            }
        }
        // endregion
        // region rhs_idx
        for (final Transition t : indices_a.transitions) {
            if (!rhs_idx.containsKey(t.q0)) {  // keep index for rhs states
                rhs_idx.put(t.q0, new LinkedHashMap<>());
            }
            if (!rhs_idx.get(t.q0).containsKey(t.f)) {
                rhs_idx.get(t.q0).put(t.f, new LinkedHashSet<>());
            }
            rhs_idx.get(t.q0).get(t.f).add(t.m);
        }
        // endregion
        // region rhs_f_idx
        for (final Transition t : indices_a.transitions) {
            if (!rhs_f_idx.containsKey(t.f)) {
                rhs_f_idx.put(t.f, new LinkedHashSet<>());
            }
            rhs_f_idx.get(t.f).add(t.m);
        }
        // endregion
    }
}

class Transition {
    final Symbol f;
    final String q0;
    final ArrayList<String> lhs;
    final int m;

    public Transition(final Symbol f, final String q0, final ArrayList<String> lhs, final int m) {
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
    public boolean equals(final Object g) {
        return (m == ((Transition) g).m);
    }
}

class Symbol {
    final String fname;
    final int arity;

    public Symbol(final String fname, final int arity) {
        this.fname = fname;
        this.arity = arity;
    }

    @Override
    public int hashCode() {
        return arity * 31 + fname.hashCode();
    }

    @Override
    public boolean equals(final Object g) {
        if (g == null) {
            return false;
        }
        if (getClass() != g.getClass()) {
            return false;
        }
        Symbol g1 = (Symbol) g;
        return (fname.equals(g1.fname) && arity == g1.arity);
    }
}
