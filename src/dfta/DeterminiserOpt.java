package dfta;

import dfta.parser.TAModel;

import java.util.*;

public class DeterminiserOpt {
    // region preamble
    final LinkedHashSet<LinkedHashSet<String>> qd;
    final ArrayList<PTransition> delta_p;
    final IndicesA indices_a;
    final IndicesB indices_b;

    public DeterminiserOpt(IndicesB indices_b, IndicesA indices_a) {
        this.indices_b = indices_b;
        this.indices_a = indices_a;
        delta_p = new ArrayList<>();
        qd = new LinkedHashSet<>();
        // region states
        // region init
        final LinkedHashMap<FuncSymb, ArrayList<LinkedHashSet<BitSet>>> psi = new LinkedHashMap<>();
        final LinkedHashMap<FuncSymb, ArrayList<LinkedHashSet<BitSet>>> phi = new LinkedHashMap<>();
        final LinkedHashMap<FuncSymb, ArrayList<LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>>>> t_inverse_table = new LinkedHashMap<>();
        for (final FuncSymb f : this.indices_a.constructors) {
            if (f.arity == 0) {
                final LinkedHashSet<String> q0 = DeterminiserTextBook.rhs_set(indices_b, indices_b.f_index.get(f));
                if (!q0.isEmpty()) {
                    qd.add(q0);
                }
            } else {
                // Initialise:
                //  • Psi_1 ... Psi_n
                //  • Phi_1 ... Phi_n for each f/n.
                //  • the t_inverse_table.
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
        final ArrayList<LinkedHashSet<String>> qdnew = new ArrayList<>(qd);
        final LinkedHashSet<LinkedHashSet<String>> qdnew_1 = new LinkedHashSet<>();
        while (!qdnew.isEmpty()) {
            qdnew_1.clear();
            for (final FuncSymb f : this.indices_a.constructors) {
                if (f.arity > 0) {
                    // Initialise the Phi and Psi tuples.
                    final ArrayList<LinkedHashSet<BitSet>> psi_f = psi.get(f);
                    final ArrayList<LinkedHashSet<BitSet>> phi_f = phi.get(f);
                    for (int j = 0; j < f.arity; j++) {
                        final LinkedHashSet<BitSet> phi_f_j = new LinkedHashSet<>();
                        for(final LinkedHashSet<String> qs : qdnew) {
                            // region lhs_set
                            final BitSet h = DeterminiserTextBook.or_all(indices_b, new BitSet(this.indices_a.transitions.size()), qs, f, j);
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
                                final LinkedHashSet<String> q0 = DeterminiserTextBook.rhs_set(indices_b, DeterminiserTextBook.and_all(deltatuple));
                                if (!q0.isEmpty()) {
                                    if (qd.add(q0)) {
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
        for (final FuncSymb f : this.indices_a.constructors) {
            if (f.arity == 0) {
                final LinkedHashSet<String> q0 = DeterminiserTextBook.rhs_set(indices_b, indices_b.f_index.get(f));
                if (!q0.isEmpty()) {
                    delta_p.add(new PTransition(f, q0, new ArrayList<>()));
                }
            } else {
                final ArrayList<ArrayList<BitSet>> psi_tuple = new ArrayList<>();
                final ArrayList<BitSet> deltatuple = new ArrayList<>();
                // Initialise delta-tuple and psi-tuple.
                for (int j = 0; j < f.arity; j++) {
                    psi_tuple.add(j, new ArrayList<>(psi.get(f).get(j)));
                    deltatuple.add(j, new BitSet(this.indices_a.transitions.size()));
                }
                int prod = 1;
                for (int j = 0; j < f.arity; j++) {
                    prod = prod * psi_tuple.get(j).size();
                }
                for (int j = 0; j < prod; j++) {
                    int temp = j;
                    for (int k = 0; k < f.arity; k++) {
                        final int z = psi_tuple.get(k).size();
                        deltatuple.set(k, psi_tuple.get(k).get(temp % z));
                        temp = temp / z;
                    }
                    final LinkedHashSet<String> q0 = DeterminiserTextBook.rhs_set(indices_b, DeterminiserTextBook.and_all(deltatuple));
                    if (!q0.isEmpty()) {
                        final ArrayList<LinkedHashSet<LinkedHashSet<String>>> lhs = new ArrayList<>();
                        for (int m = 0; m < f.arity; m++) {
                            lhs.add(m, t_inverse_table.get(f).get(m).get(deltatuple.get(m)));
                        }
                        delta_p.add(new PTransition(f, q0, lhs));
                    }
                }
            }
        }
        // endregion
    }
    // endregion

    // region solver
    private int equivClass(ArrayList<LinkedHashSet<LinkedHashSet<String>>> p, LinkedHashSet<String> q) {
        int i = 0;
        int n = p.size();
        while (i < n) {
            if (p.get(i).contains(q)) return i;
            i++;
        }
        return -1;
    }

    public long deltaDCount() {
        double count = 0;
        double qdsize = qd.size();
        for (final PTransition deltad1 : delta_p) {
            double tcount = 1.0;
            for (final LinkedHashSet<LinkedHashSet<String>> lh : deltad1.lhs) {
                double argsize = lh.size();
                if (argsize == 0) argsize = qdsize;  // don't care argument
                tcount = tcount * argsize;
            }
            count = count + tcount;
        }
        return Math.round(count);
    }
    // endregion

    // region minimize
    static ArrayList<LinkedHashSet<LinkedHashSet<String>>> minimize_carrasco(DeterminiserOpt data, IndicesB idx, TAModel model) {
        // region collect final states
        final LinkedHashSet<LinkedHashSet<String>> qf = new LinkedHashSet<>();
        for (LinkedHashSet<String> q : data.qd) {
            for (String s : q) {
                if (model.final_states.contains(s)) {
                    qf.add(q);
                }
            }
        }
        // endregion
        // region rest
        final LinkedHashMap<LinkedHashSet<String>, LinkedHashSet<Signature>> sigs = new LinkedHashMap<>();
        final Signature dummy_sig = new Signature(new FuncSymb("#", 1), 1);
        for (final LinkedHashSet<String> q : data.qd) {
            sigs.put(q, new LinkedHashSet<>());
            // For all q ∈ F add (#, 1, 1) to sig(q).
            if (qf.contains(q)) {
                sigs.get(q).add(dummy_sig);
            }
        }
        // For all (σ, i1, . . . , im) ∈ Δ add (σ,m, k) to sig(ik) for k = 1, . . . , m.
        for (final PTransition t : data.delta_p) {
            for (int i = 0; i < t.f.arity; i++) {
                LinkedHashSet<LinkedHashSet<String>> qis = t.lhs.get(i);
                for (LinkedHashSet<String> qi : qis) {
                    sigs.get(qi).add(new Signature(t.f, i));
                }
            }
        }
        // Create a map from Sigma to Delta_d
        final LinkedHashMap<FuncSymb, LinkedHashSet<PTransition>> deltadMap = new LinkedHashMap<>();
        for (PTransition t : data.delta_p) {
            if (!deltadMap.containsKey(t.f)) {
                deltadMap.put(t.f, new LinkedHashSet<>());
            }
            deltadMap.get(t.f).add(t);
        }
        // Create an empty set Bsig for every different signature sig and for all q ∈ Q add q to set Bsig(q).
        final LinkedHashMap<LinkedHashSet<Signature>, LinkedHashSet<LinkedHashSet<String>>> siginv = new LinkedHashMap<>();
        for (LinkedHashSet<String> q : data.qd) {
            final LinkedHashSet<Signature> sigset = sigs.get(q);
            if (siginv.containsKey(sigset)) {
                siginv.get(sigset).add(q);
            } else {
                siginv.put(sigset, new LinkedHashSet<>());
                siginv.get(sigset).add(q);
            }
        }
        // Set P0 ← (Q) and P1 ← {Bs : Bs ≠ ∅}.
        ArrayList<LinkedHashSet<LinkedHashSet<String>>> p = new ArrayList<>();
        for (final LinkedHashSet<Signature> s : siginv.keySet()) {
            p.add(new LinkedHashSet<>(siginv.get(s)));
        }
        // Enqueue in K the first element from every class in P1
        final ArrayList<LinkedHashSet<String>> k = new ArrayList<>();
        for (final LinkedHashSet<LinkedHashSet<String>> pi : p) {
            k.add(new ArrayList<>(pi).get(0));
        }
        while (!k.isEmpty()) {
            // (a) Remove the first state q in K.
            final LinkedHashSet<String> q = k.remove(0);
            // (b) For all (σ, i1, . . . , im, j) ∈ Δ such that j ∼ q and for all k ≤ m
            for (final FuncSymb f : data.indices_a.constructors) {
                for (final PTransition t : deltadMap.get(f)) {
                    if (congruent(data, p, q, t.q0)) {
                        for (int i = 0; i < f.arity; i++) {
                            final LinkedHashSet<LinkedHashSet<String>> qi = t.lhs.get(i);
                            for (final LinkedHashSet<String> qij : qi) {
                                final int r = data.equivClass(p, qij);
                                if (p.get(r).size() > 1) { // can be split
                                    final ArrayList<LinkedHashSet<String>> prList = new ArrayList<>(p.get(r));
                                    final LinkedHashSet<String> next_qij;
                                    if (prList.indexOf(qij) < prList.size() - 1) {
                                        next_qij = prList.get(prList.indexOf(qij) + 1);
                                    } else {
                                        next_qij = prList.get(0);
                                    }
                                    final ArrayList<LinkedHashSet<LinkedHashSet<String>>> phi_q_new = new ArrayList<>();
                                    for (int l = 0; l < p.size(); l++) {
                                        phi_q_new.add(new LinkedHashSet<>());
                                    }
                                    // Find transitions f(i1, . . . , ik', . . . , im) -> j where /∼ j and ik ~ ik'
                                    for (PTransition t1 : deltadMap.get(f)) {
                                        if (t1.lhs.get(i).contains(next_qij)) {
                                            if (!congruent(data, p, q, t1.q0)) {
                                                final LinkedHashSet<LinkedHashSet<String>> qi1 = t1.lhs.get(i);
                                                final LinkedHashSet<LinkedHashSet<String>> phi_q = new LinkedHashSet<>(p.get(r));
                                                phi_q.retainAll(qi1);
                                                if (!phi_q.isEmpty()) {
                                                    int j = 0;
                                                    boolean split = true;
                                                    // check whether t and t1 args overlap
                                                    while (j < f.arity && split) {
                                                        if (j != i) {
                                                            split = split && data.nonEmptyIntersect(t.lhs.get(j), t1.lhs.get(j));
                                                        }
                                                        j++;
                                                    }
                                                    if (split) {
                                                        // add each element of phi_q to the new equivalence class
                                                        final LinkedHashSet<LinkedHashSet<String>> e = phi_q_new.get(data.equivClass(p, t1.q0));
                                                        for (LinkedHashSet<String> qk : phi_q) {
                                                            if (!e.contains(qk)) {
                                                                e.add(qk);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // replace phi[q] with the partitions in pi_new
                                    final LinkedHashSet<LinkedHashSet<String>> oldEquivClass = p.remove(r);
                                    for (final LinkedHashSet<LinkedHashSet<String>> newEquivClass : phi_q_new) {
                                        if (!newEquivClass.isEmpty()) {
                                            p.add(newEquivClass);
                                            oldEquivClass.removeAll(newEquivClass);
                                            final ArrayList<LinkedHashSet<String>> newList = new ArrayList<>(newEquivClass);
                                            // Add to K the first element from every subset created
                                            if (!k.contains(newList.get(0))) {
                                                k.add(newList.get(0));
                                            }
                                        }
                                    }
                                    // put back what is left of the original equiv class
                                    if (!oldEquivClass.isEmpty()) {
                                        p.add(oldEquivClass);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return p;
        // endregion
    }

    static private boolean congruent(DeterminiserOpt d, ArrayList<LinkedHashSet<LinkedHashSet<String>>> p, LinkedHashSet<String> q, LinkedHashSet<String> q0) {
        return d.equivClass(p, q) == d.equivClass(p, q0);
    }

    static private boolean nonEmptyIntersect(LinkedHashSet<LinkedHashSet<String>> qi, LinkedHashSet<LinkedHashSet<String>> qj) {
        for (final LinkedHashSet<String> s : qi) {
            if (qj.contains(s)) {
                return true;
            }
        }
        return false;
    }
    // endregion
}


class PTransition {
    final FuncSymb f;
    final LinkedHashSet<String> q0;
    final ArrayList<LinkedHashSet<LinkedHashSet<String>>> lhs;

    public PTransition(FuncSymb f, LinkedHashSet<String> q0, ArrayList<LinkedHashSet<LinkedHashSet<String>>> lhs) {
        this.f = f;
        this.q0 = q0;
        this.lhs = lhs;
    }
}

class Signature {
    final FuncSymb f;
    final int i;

    Signature(FuncSymb f, int i) {
        this.f = f;
        this.i = i;
    }

    @Override
    public boolean equals(Object g) {
        Signature g1 = (Signature) g;
        return i == g1.i && f.equals(g1.f);
    }

    @Override
    public int hashCode() {
        return i * 127 + f.hashCode();
    }

    public String toString() {
        return "(" + f + "," + i + ")";
    }
}
