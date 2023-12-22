package dfta;

import dfta.parser.TAModel;

import java.util.*;

public class DeterminiserOpt {
    // region preamble
    final LinkedHashSet<LinkedHashSet<String>> qd;
    final ArrayList<PTransition> delta_p;

    public DeterminiserOpt(IndicesB indices_b, IndicesA indices_a) {
        delta_p = new ArrayList<>();
        qd = new LinkedHashSet<>();
        // region states
        // region init
        final LinkedHashMap<FuncSymb, ArrayList<LinkedHashSet<BitSet>>> psi = new LinkedHashMap<>();
        final LinkedHashMap<FuncSymb, ArrayList<LinkedHashSet<BitSet>>> phi = new LinkedHashMap<>();
        final LinkedHashMap<FuncSymb, ArrayList<LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>>>> t_inverse_table = new LinkedHashMap<>();
        for (final FuncSymb f : indices_a.constructors) {
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
            for (final FuncSymb f : indices_a.constructors) {
                if (f.arity > 0) {
                    // Initialise the Phi and Psi tuples.
                    final ArrayList<LinkedHashSet<BitSet>> psi_f = psi.get(f);
                    final ArrayList<LinkedHashSet<BitSet>> phi_f = phi.get(f);
                    for (int j = 0; j < f.arity; j++) {
                        final LinkedHashSet<BitSet> phi_f_j = new LinkedHashSet<>();
                        for(final LinkedHashSet<String> qs : qdnew) {
                            // region lhs_set
                            final BitSet h = DeterminiserTextBook.or_all(indices_b, new BitSet(indices_a.transitions.size()), qs, f, j);
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
        for (final FuncSymb f : indices_a.constructors) {
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
                    deltatuple.add(j, new BitSet(indices_a.transitions.size()));
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
