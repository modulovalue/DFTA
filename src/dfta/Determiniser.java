package dfta;

import dfta.parser.TAModel;
import dfta.parser.TAModelTransition;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class Determiniser {
    // https://arxiv.org/abs/1511.03595
    // - "don't cares" could reduce the amount of product transitions, see git history.
    // - this could support inclusion testing, see git history.
    // - this approach might be able to be used for more efficient minimization, see git history.
    public static DeterminiserResultP powerset_with_reduction_and_gallagherproducttransitions(final Index index) {
        // region states
        // region init
        final LinkedHashSet<LinkedHashSet<String>> Qd = new LinkedHashSet<>();
        final LinkedHashMap<Symb, ArrayList<LinkedHashSet<BitSet>>> Ψ = new LinkedHashMap<>();
        final LinkedHashMap<Symb, ArrayList<LinkedHashSet<BitSet>>> Φ = new LinkedHashMap<>();
        final LinkedHashMap<Symb, ArrayList<LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>>>> Tinverse = new LinkedHashMap<>();
        for (final Symb f : index.a.constants) {
            final BitSet Δ_set = index.b.f_index.get(f);
            final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
            if (!Q0.isEmpty()) {
                Qd.add(Q0);
            }
        }
        for (final Symb f : index.a.inconstants) {
            final ArrayList<LinkedHashSet<BitSet>> Ψ_f = new ArrayList<>(f.arity);
            final ArrayList<LinkedHashSet<BitSet>> Φ_f = new ArrayList<>(f.arity);
            final ArrayList<LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>>> Tinverse_f = new ArrayList<>();
            for (int j = 0; j < f.arity; j++) {
                Ψ_f.add(j, new LinkedHashSet<>());
                Φ_f.add(j, new LinkedHashSet<>());
                Tinverse_f.add(j, new LinkedHashMap<>());
            }
            Ψ.put(f, Ψ_f);
            Φ.put(f, Φ_f);
            Tinverse.put(f, Tinverse_f);
        }
        // endregion
        // region populate
        final ArrayList<LinkedHashSet<String>> Qd_temp = new ArrayList<>(Qd);
        final LinkedHashSet<LinkedHashSet<String>> Qd_sentinel = new LinkedHashSet<>();
        while (!Qd_temp.isEmpty()) {
            Qd_sentinel.clear();
            for (final Symb f : index.a.inconstants) {
                final ArrayList<LinkedHashSet<BitSet>> Ψ_f = Ψ.get(f);
                final ArrayList<LinkedHashSet<BitSet>> Φ_f = Φ.get(f);
                for (int j = 0; j < f.arity; j++) {
                    final LinkedHashSet<BitSet> Φ_f_j = new LinkedHashSet<>();
                    for (final LinkedHashSet<String> qs : Qd_temp) {
                        // region lhs_set
                        final BitSet h = or_all(index.b, new BitSet(index.a.transitions.size()), qs, f, j);
                        // Tabulate result for the t_inverse function.
                        if (!h.isEmpty()) {
                            final LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>> hm = Tinverse.get(f).get(j);
                            if (!hm.containsKey(h)) {
                                hm.put(h, new LinkedHashSet<>());
                            }
                            hm.get(h).add(qs);
                        }
                        // endregion
                        if (!h.isEmpty()) {
                            Φ_f_j.add(h);
                        }
                    }
                    // Remove sets already computed for jth argument.
                    Φ_f_j.removeAll(Ψ_f.get(j));
                    Φ_f.set(j, Φ_f_j);
                }
                for (int j = 0; j < f.arity; j++) {
                    if (Φ_f.get(j).size() > 0) {
                        final ArrayList<ArrayList<BitSet>> Ψ_Φ_tuple = new ArrayList<>();
                        for (int k = 0; k < f.arity; k++) {
                            if (k < j) {
                                Ψ_Φ_tuple.add(k, new ArrayList<>(Ψ_f.get(k)));
                            } else if (k == j) {
                                Ψ_Φ_tuple.add(k, new ArrayList<>(Φ_f.get(k)));
                            } else {
                                Ψ_Φ_tuple.add(k, new ArrayList<>(Φ_f.get(k)));
                                Ψ_Φ_tuple.get(k).addAll(Ψ_f.get(k));
                            }
                        }
                        int prod = 1;
                        for (int k = 0; k < f.arity; k++) {
                            prod = prod * Ψ_Φ_tuple.get(k).size();
                        }
                        // Enumerate the delta-tuples (cartesian product).
                        for (int k = 0; k < prod; k++) {
                            int temp = k;
                            // Re-initialise delta-tuple.
                            final ArrayList<BitSet> Δ_tuple = new ArrayList<>();
                            for (int m = 0; m < f.arity; m++) {
                                final int z = Ψ_Φ_tuple.get(m).size();
                                Δ_tuple.add(m, Ψ_Φ_tuple.get(m).get(temp % z));
                                temp = temp / z;
                            }
                            final BitSet Δ_set = and_all(Δ_tuple);
                            final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
                            if (!Q0.isEmpty()) {
                                if (Qd.add(Q0)) {
                                    Qd_sentinel.add(Q0);
                                }
                            }
                        }
                    }
                }
                for (int j = 0; j < f.arity; j++) {
                    Ψ_f.get(j).addAll(Φ_f.get(j));
                }
            }
            Qd_temp.clear();
            Qd_temp.addAll(Qd_sentinel);
        }
        // endregion
        // endregion
        // region transitions
        final ArrayList<PTransition> Δp = new ArrayList<>();
        for (final Symb f : index.a.constants) {
            final BitSet Δ_set = index.b.f_index.get(f);
            final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
            if (!Q0.isEmpty()) {
                Δp.add(new PTransition(f, Q0, new ArrayList<>()));
            }
        }
        for (final Symb f : index.a.inconstants) {
            int prod = 1;
            for (int m = 0; m < f.arity; m++) {
                prod = prod * Ψ.get(f).get(m).size();
            }
            final ArrayList<ArrayList<BitSet>> Ψ_tuple = new ArrayList<>();
            final ArrayList<BitSet> Δ_tuple = new ArrayList<>();
            for (int m = 0; m < f.arity; m++) {
                final LinkedHashSet<BitSet> Ψ_f_m = Ψ.get(f).get(m);
                Ψ_tuple.add(m, new ArrayList<>(Ψ_f_m));
                Δ_tuple.add(m, new BitSet(index.a.transitions.size()));
            }
            for (int j = 0; j < prod; j++) {
                int temp = j;
                for (int m = 0; m < f.arity; m++) {
                    final ArrayList<BitSet> Ψ_tuple_m = Ψ_tuple.get(m);
                    final int z = Ψ_tuple_m.size();
                    Δ_tuple.set(m, Ψ_tuple_m.get(temp % z));
                    temp = temp / z;
                }
                final BitSet Δ_set = and_all(Δ_tuple);
                final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
                if (!Q0.isEmpty()) {
                    final ArrayList<LinkedHashSet<LinkedHashSet<String>>> lhs = new ArrayList<>();
                    for (int m = 0; m < f.arity; m++) {
                        lhs.add(m, Tinverse.get(f).get(m).get(Δ_tuple.get(m)));
                    }
                    Δp.add(new PTransition(f, Q0, lhs));
                }
            }
        }
        // endregion
        // region result
        return new DeterminiserResultP(Qd, Δp);
        // endregion
    }

    // "TextBook": https://arxiv.org/abs/1511.03595
    // TATA: https://jacquema.gitlabpages.inria.fr/files/tata.pdf
    // Page 26: https://www21.in.tum.de/~lammich/2015_SS_Automata2/slides/handout.pdf
    public static DeterminiserResultD powerset_with_reduction(final Index index) {
        // region prepare
        final LinkedHashSet<LinkedHashSet<String>> Qd = new LinkedHashSet<>();
        final LinkedHashSet<DTransition> Δd = new LinkedHashSet<>();
        // endregion
        // region main
        for (;;) {
            boolean new_transition = false;
            final ArrayList<LinkedHashSet<String>> Qd_prev = new ArrayList<>(Qd);
            final int Qd_size = Qd_prev.size();
            for (Symb f : index.a.constants) {
                final BitSet Δ_set = index.b.f_index.get(f);
                final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
                if (!Q0.isEmpty()) {
                    Qd.add(Q0);
                    new_transition |= Δd.add(new DTransition(f, Q0, new ArrayList<>()));
                }
            }
            for (Symb f : index.a.inconstants) {
                final double target_k = Math.pow(Qd_size, f.arity);
                for (int k = 0; k < target_k; k++) { // Enumerate the delta-tuples.
                    int temp = k;
                    final ArrayList<LinkedHashSet<String>> Q_tuple = new ArrayList<>();
                    for (int m = 0; m < f.arity; m++) {
                        final LinkedHashSet<String> Q_tuple_m = Qd_prev.get(temp % Qd_size);
                        temp = temp / Qd_size;
                        Q_tuple.add(m, Q_tuple_m);
                    }
                    final ArrayList<BitSet> Δ_tuple = new ArrayList<>();
                    for (int m = 0; m < f.arity; m++) {
                        BitSet result = or_all(index.b, new BitSet(), Q_tuple.get(m), f, m);
                        Δ_tuple.add(m, result);
                    }
                    final BitSet Δ_set = and_all(Δ_tuple);
                    final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
                    if (!Q0.isEmpty()) {
                        Qd.add(Q0);
                        new_transition |= Δd.add(new DTransition(f, Q0, Q_tuple));
                    }
                }
            }
            if (!new_transition) {
                break;
            }
        }
        // endregion
        // region result
        return new DeterminiserResultD(Qd, Δd);
        // endregion
    }

    // Page 25 https://www21.in.tum.de/~lammich/2015_SS_Automata2/slides/handout.pdf
    public static DeterminiserResultD powerset() {
        // region prepare
        final LinkedHashSet<LinkedHashSet<String>> Qd = new LinkedHashSet<>();
        final LinkedHashSet<DTransition> Δd = new LinkedHashSet<>();
        // endregion
        // region main
        System.out.println("Powerset determiniser not supported.");
        System.exit(-1);
        // endregion
        // region result
        return new DeterminiserResultD(Qd, Δd);
        // endregion
    }

    private static BitSet or_all(final IndicesB idx, final BitSet init, final Iterable<String> qs, final Symb f, final int j) {
        final LinkedHashMap<String, BitSet> lhs_map = idx.lhs_f.get(f).get(j);
        for (final String q : qs) {
            if (lhs_map.containsKey(q)) {
                init.or(lhs_map.get(q));
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
            result.add(idx.transition_by_id.get(i).Q0);
        }
        return result;
    }
}

class DeterminiserResultD {
    final LinkedHashSet<LinkedHashSet<String>> Qd;
    final LinkedHashSet<DTransition> Δd;

    DeterminiserResultD(final LinkedHashSet<LinkedHashSet<String>> Qd, final LinkedHashSet<DTransition> Δd) {
        this.Qd = Qd;
        this.Δd = Δd;
    }
}

class DeterminiserResultP {
    final LinkedHashSet<LinkedHashSet<String>> Qd;
    final ArrayList<PTransition> Δp;

    DeterminiserResultP(final LinkedHashSet<LinkedHashSet<String>> Qd, final ArrayList<PTransition> Δp) {
        this.Qd = Qd;
        this.Δp = Δp;
    }

    public long Δd_count() {
        double count = 0;
        double Qd_size = Qd.size();
        for (final PTransition Δp_1 : Δp) {
            double t_count = 1.0;
            for (final LinkedHashSet<LinkedHashSet<String>> lh : Δp_1.args) {
                double arg_size = lh.size();
                if (arg_size == 0) arg_size = Qd_size;  // Don't care argument.
                t_count = t_count * arg_size;
            }
            count = count + t_count;
        }
        return Math.round(count);
    }
}

class DTransition {
    final Symb f;
    final LinkedHashSet<String> Q0;
    final ArrayList<LinkedHashSet<String>> args;

    public DTransition(final Symb f, final LinkedHashSet<String> Q0, final ArrayList<LinkedHashSet<String>> args) {
        this.f = f;
        this.Q0 = Q0;
        this.args = args;
    }

    @Override
    public int hashCode() {
        return f.hashCode() * 31 + args.hashCode() * 17 + Q0.hashCode();
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
        return (f.equals(g1.f) && args.equals(g1.args) && Q0.equals(g1.Q0));
    }
}

class PTransition {
    final Symb f;
    final LinkedHashSet<String> Q0;
    final ArrayList<LinkedHashSet<LinkedHashSet<String>>> args;

    public PTransition(final Symb f, final LinkedHashSet<String> Q0, final ArrayList<LinkedHashSet<LinkedHashSet<String>>> args) {
        this.f = f;
        this.Q0 = Q0;
        this.args = args;
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
    final LinkedHashSet<Symb> symbs = new LinkedHashSet<>();
    final LinkedHashSet<Symb> constants = new LinkedHashSet<>();
    final LinkedHashSet<Symb> inconstants = new LinkedHashSet<>();

    public IndicesA(final TAModel data, final boolean complete) {
        int cur_transition_id = 0;
        // region init delta
        for (final TAModelTransition transition : data.transitions) {
            final ArrayList<String> args = new ArrayList<>(transition.args);
            final Symb fn = new Symb(transition.fname, transition.args.size());
            symbs.add(fn);
            if (fn.arity == 0) {
                constants.add(fn);
            } else {
                inconstants.add(fn);
            }
            states.add(transition.q);
            transitions.add(new Transition(fn, transition.q, args, cur_transition_id));
            cur_transition_id++;
        }
        // endregion
        // region complete delta
        if (complete) {
            for (final Symb f : symbs) {
                final ArrayList<String> args = new ArrayList<>();
                for (int j = 0; j < f.arity; j++) {
                    args.add(j, "'$any'");
                }
                transitions.add(new Transition(f, "'$any'", args, cur_transition_id));
                cur_transition_id++;
            }
        }
        // endregion
        // region final states
        final_states.addAll(data.final_states);
        // endregion
    }
}

class IndicesB {
    final LinkedHashMap<Integer, Transition> transition_by_id = new LinkedHashMap<>();
    final LinkedHashMap<Symb, BitSet> f_index = new LinkedHashMap<>();
    final LinkedHashMap<Symb, ArrayList<LinkedHashMap<String, BitSet>>> lhs_f = new LinkedHashMap<>();
    final LinkedHashMap<String, LinkedHashMap<Symb, LinkedHashSet<Integer>>> rhs_idx = new LinkedHashMap<>();
    final LinkedHashMap<Symb, LinkedHashSet<Integer>> rhs_f_idx = new LinkedHashMap<>();

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
            f_index.get(t.f).set(t.m);  // Set the bit for the mth transition.
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
            final ArrayList<String> args = t.args;
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
            if (!rhs_idx.containsKey(t.Q0)) {  // Keep index for rhs states.
                rhs_idx.put(t.Q0, new LinkedHashMap<>());
            }
            if (!rhs_idx.get(t.Q0).containsKey(t.f)) {
                rhs_idx.get(t.Q0).put(t.f, new LinkedHashSet<>());
            }
            rhs_idx.get(t.Q0).get(t.f).add(t.m);
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
    final Symb f;
    final String Q0;
    final ArrayList<String> args;
    final int m;

    public Transition(final Symb f, final String Q0, final ArrayList<String> args, final int m) {
        this.f = f;
        this.Q0 = Q0;
        this.args = args;
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

class Symb {
    final String fname;
    final int arity;

    public Symb(final String fname, final int arity) {
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
        Symb g1 = (Symb) g;
        return (fname.equals(g1.fname) && arity == g1.arity);
    }
}
