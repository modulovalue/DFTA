package foo;

import dfta.DeterminiserOpt;
import dfta.DeterminiserTextBook;
import foo.parser.FTAParser;
import foo.parser.ParseException;
import foo.parser.TAModel;
import foo.parser.TAModelTransition;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

// TODO • get this to run DFTA upstream master and serialize determinized.
public class Driver {
    static final String root = "/Users/modulovalue/Desktop/DFTA/";
    static final String all_example_root = root + "examples/";
    static final String d_fixture_root = root + "examples_d/";
    static final String p_fixture_root = root + "examples_p/";

    public static void main(final String[] args) throws FileNotFoundException, dfta.parser.ParseException, ParseException {
        // We start the parser with some irrelevant file so that we can reinit it in a clean way.
        new dfta.parser.FTAParser(new java.io.FileInputStream(all_example_root + "A0053"));
        new File(d_fixture_root).mkdir();
        new File(p_fixture_root).mkdir();
        try {
            switch ("all") {
//            switch ("one") {
//            switch ("other") {
                case "all" -> {
                    final File[] files = new File(all_example_root).listFiles();
                    final var sorted_Files = Arrays.stream(files).sorted().toList();
                    int i = 0;
                    for (final File file : sorted_Files) {
                        i++;
                        System.out.println(i + "/" + sorted_Files.size() + " " + file.getName());
                        run(
                            file,
                            d_fixture_root + file.getName(),
                            p_fixture_root + file.getName(),
//                             "gallagher_product_old"
//                            "gallagher_product"
                             "tata_old"
                            // "tata"
                            // "powerset"
                        );
                    }
                }
                case "one" -> {
//                    final File file = new File(all_example_root + "A0053");
//                    final File file = new File(all_example_root + "A0063");
                    final File file = new File(all_example_root + "A0088");
//                    final File file = new File(all_example_root + "A1003");
//                    final File file = new File(all_example_root + "A447");
//                    final File file = new File(all_example_root + "A493");
//                    final File file = new File(all_example_root + "A620");
                    run(
                        file,
                        d_fixture_root + file.getName(),
                        p_fixture_root + file.getName(),
                        // "gallagher_product_old"
                         "gallagher_product"
                        // "tata_old"
                        // "tata"
                        // "powerset"
                    );
                }
                case "other" -> {
//                    final var b = Determiniser.powerset_with_reduction_and_gallagherproducttransitions_upstream(new Index(all_example_root + "/A0053", true));
                    final var b = Determiniser.powerset_with_reduction_upstream(new Index(all_example_root + "/A0053", true));
                    System.out.println(b.write());
                }
            }
        } catch (ParseException | IOException e) {
            System.out.println(e.getMessage());
        } catch (dfta.parser.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    static void run(final File file, final String result_d, final String result_p, final String flavor) throws IOException, ParseException, dfta.parser.ParseException {
        // region index
        final long index_start_time = System.currentTimeMillis();
        final Index index = new Index(file.getPath(), true);
        final long index_end_time = System.currentTimeMillis();
        System.out.println("File input time = " + ((index_end_time - index_start_time) / 1000.0) + ",");
        System.out.println("=== Determinising: " + file.getAbsolutePath() + " ===");
        final long run_start_time = System.currentTimeMillis();
        // endregion
        // region run
        boolean success = false;
        switch (flavor) {
            case "gallagher_product" -> {
                final var det = with_timeout(Determiniser::powerset_with_reduction_and_gallagherproducttransitions, index);
                if (det != null) {
                    success = true;
                    System.out.println("Number of DFTA states/normal transitions/product transitions = " + det.Qd.size() + "/" + det.Δd_count() + "/" + det.Δp.size());
                    if (result_p != null) {
                        new File(result_p).createNewFile();
                        final var writer = new FileWriter(result_p);
                        writer.write(det.write());
                        writer.close();
                    }
                }
            }
            case "gallagher_product_old" -> {
                final var det = with_timeout(Determiniser::powerset_with_reduction_and_gallagherproducttransitions_upstream, index);
                if (det != null) {
                    success = true;
                    System.out.println("Number of DFTA states/normal transitions/product transitions = " + det.Qd.size() + "/" + det.Δd_count() + "/" + det.Δp.size());
                    if (result_p != null) {
                        new File(result_p).createNewFile();
                        final var writer = new FileWriter(result_p);
                        writer.write(det.write());
                        writer.close();
                    }
                }
            }
            case "tata" -> {
                final var det = with_timeout(Determiniser::powerset_with_reduction, index);
                if (det != null) {
                    success = true;
                    System.out.println("Number of DFTA states/transitions = " + det.Qd.size() + "/" + det.Δd.size());
                    if (result_d != null) {
                        new File(result_d).createNewFile();
                        final var writer = new FileWriter(result_d);
                        writer.write(det.write());
                        writer.close();
                    }
                }
            }
            case "tata_old" -> {
                final var det = with_timeout(Determiniser::powerset_with_reduction_upstream, index);
                if (det != null) {
                    success = true;
                    System.out.println("Number of DFTA states/transitions = " + det.Qd.size() + "/" + det.Δd.size());
                    if (result_d != null) {
                        new File(result_d).createNewFile();
                        final var writer = new FileWriter(result_d);
                        writer.write(det.write());
                        writer.close();
                    }
                }
            }
            case "powerset" -> {
                final var det = with_timeout(Determiniser::powerset, index);
                if (det != null) {
                    success = true;
                    System.out.println("Number of DFTA states/transitions = " + det.Qd.size() + "/" + det.Δd.size());
                    if (result_d != null) {
                        new File(result_d).createNewFile();
                        final var writer = new FileWriter(result_d);
                        writer.write(det.write());
                        writer.close();
                    }
                }
            }
        }
        final long run_end_time = System.currentTimeMillis();
        if (success) {
            System.out.println("Number of input FTA states/transitions = " + index.a.Q.size() + "/" + index.a.Δ.size());
            System.out.println("Determinisation time = " + ((run_end_time - run_start_time) / 1000.0) + ",");
        }
        // endregion
    }

    // https://stackoverflow.com/questions/4978187/apply-timeout-control-around-java-operation
    static <R> R with_timeout(final Function<Index, R> fn, final Index index) {
//        return fn.apply(index);
        final ExecutorService threadPool = Executors.newCachedThreadPool();
        Callable<R> callable = () -> fn.apply(index);
        Future<R> future = threadPool.submit(callable);
        try {
            // throws a TimeoutException after 1000 ms.
            return future.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            System.out.println("        => ERROR: Timeout while running operation A " + e);
            return null;
        } catch (InterruptedException e) {
            System.out.println("        => ERROR: Timeout while running operation B");
            Thread.currentThread().interrupt();
            return null;
        } catch (TimeoutException e) {
            System.out.println("        => ERROR: Timeout while running operation C");
            return null;
        } finally {
            future.cancel(true);
            threadPool.close();
        }
    }
}

class Determiniser {
    // TODO take a string and return the index?
    public static DeterminiserResultP powerset_with_reduction_and_gallagherproducttransitions(final Index index) {
        // region states
        // region init
        final LinkedHashSet<LinkedHashSet<String>> Qd = new LinkedHashSet<>();
        for (final Symb f : index.b.constants) {
            final BitSet Δ_set = index.b.f_index.get(f);
            final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
            if (!Q0.isEmpty()) {
                Qd.add(Q0);
            }
        }
        final LinkedHashMap<Symb, ArrayList<LinkedHashSet<BitSet>>> Ψ = new LinkedHashMap<>();
        final LinkedHashMap<Symb, ArrayList<LinkedHashSet<BitSet>>> Φ = new LinkedHashMap<>();
        final LinkedHashMap<Symb, ArrayList<LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>>>> Tinverse = new LinkedHashMap<>();
        for (final Symb f : index.b.inconstants) {
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
            for (final Symb f : index.b.inconstants) {
                final ArrayList<LinkedHashMap<String, BitSet>> lhs_f_f = index.b.lhs_f.get(f);
                final ArrayList<LinkedHashSet<BitSet>> Ψ_f = Ψ.get(f);
                final ArrayList<LinkedHashSet<BitSet>> Φ_f = Φ.get(f);
                for (int j = 0; j < f.arity; j++) {
                    final LinkedHashSet<BitSet> Φ_f_j = new LinkedHashSet<>();
                    for (final LinkedHashSet<String> qs : Qd_temp) {
                        final BitSet h = or_all(new BitSet(index.a.Δ.size()), qs, lhs_f_f, j);
                        // Tabulate result for the t_inverse function.
                        if (!h.isEmpty()) {
                            final LinkedHashMap<BitSet, LinkedHashSet<LinkedHashSet<String>>> hm = Tinverse.get(f).get(j);
                            if (!hm.containsKey(h)) {
                                hm.put(h, new LinkedHashSet<>());
                            }
                            hm.get(h).add(qs);
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
        for (final Symb f : index.b.constants) {
            final BitSet Δ_set = index.b.f_index.get(f);
            final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
            if (!Q0.isEmpty()) {
                Δp.add(new PTransition(f, Q0, new ArrayList<>()));
            }
        }
        for (final Symb f : index.b.inconstants) {
            final ArrayList<LinkedHashSet<BitSet>> Ψ_f = Ψ.get(f);
            int prod = 1;
            final ArrayList<ArrayList<BitSet>> Ψ_tuple = new ArrayList<>();
            final ArrayList<BitSet> Δ_tuple = new ArrayList<>();
            for (int m = 0; m < f.arity; m++) {
                final LinkedHashSet<BitSet> Ψ_f_m = Ψ_f.get(m);
                prod = prod * Ψ_f_m.size();
                Ψ_tuple.add(m, new ArrayList<>(Ψ_f_m));
                Δ_tuple.add(m, new BitSet(index.a.Δ.size()));
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
        return new DeterminiserResultP(index, Qd, Δp);
        // endregion
    }

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
            for (final Symb f : index.b.constants) {
                final BitSet Δ_set = index.b.f_index.get(f);
                final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
                if (!Q0.isEmpty()) {
                    Qd.add(Q0);
                    new_transition |= Δd.add(new DTransition(f, Q0, new ArrayList<>()));
                }
            }
            for (final Symb f : index.b.inconstants) {
                final ArrayList<LinkedHashMap<String, BitSet>> lhs_f_f = index.b.lhs_f.get(f);
                final double target_k = Math.pow(Qd_size, f.arity);
                for (int k = 0; k < target_k; k++) { // Enumerate the delta-tuples.
                    int temp = k;
                    final ArrayList<LinkedHashSet<String>> Q_tuples = new ArrayList<>();
                    for (int m = 0; m < f.arity; m++) {
                        final LinkedHashSet<String> Q_tuple_m = Qd_prev.get(temp % Qd_size);
                        temp = temp / Qd_size;
                        Q_tuples.add(m, Q_tuple_m);
                    }
                    final ArrayList<BitSet> Δ_tuple = new ArrayList<>();
                    for (int m = 0; m < f.arity; m++) {
                        BitSet result = or_all(new BitSet(), Q_tuples.get(m), lhs_f_f, m);
                        Δ_tuple.add(m, result);
                    }
                    final BitSet Δ_set = and_all(Δ_tuple);
                    final LinkedHashSet<String> Q0 = rhs_set(index.b, Δ_set);
                    if (!Q0.isEmpty()) {
                        Qd.add(Q0);
                        new_transition |= Δd.add(new DTransition(f, Q0, Q_tuples));
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

    public static DeterminiserResultP powerset_with_reduction_and_gallagherproducttransitions_upstream(final Index index) {
        try {
            dfta.parser.FTAParser.ReInit(new FileInputStream(index.path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        dfta.parser.FTAParser.transitions = new LinkedHashSet();
        dfta.parser.FTAParser.finalStates = new LinkedHashSet();
        try {
            dfta.parser.FTAParser.FTA();
        } catch (dfta.parser.ParseException e) {
            throw new RuntimeException(e);
        }
        final var det = new DeterminiserOpt("", dfta.parser.FTAParser.transitions, dfta.parser.FTAParser.finalStates, true, false,false);
        det.makeDfta();
        det.showStats(true);

        final LinkedHashSet<LinkedHashSet<String>> second_Qd = det.qd;
        final ArrayList<dfta.PTransition> second_DELTAd = det.deltad;
        final ArrayList<foo.PTransition> result_DELTAd = new ArrayList<>();
        for (final dfta.PTransition x : second_DELTAd) {
            result_DELTAd.add(new PTransition(new Symb(x.f.fname, x.f.arity), x.q0, x.lhs));
        }
        return new DeterminiserResultP(index, second_Qd, result_DELTAd);
    }

    public static DeterminiserResultD powerset_with_reduction_upstream(final Index index) {
        try {
            dfta.parser.FTAParser.ReInit(new FileInputStream(index.path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        dfta.parser.FTAParser.transitions = new LinkedHashSet();
        dfta.parser.FTAParser.finalStates = new LinkedHashSet();
        try {
            dfta.parser.FTAParser.FTA();
        } catch (dfta.parser.ParseException e) {
            throw new RuntimeException(e);
        }
        final var det = new DeterminiserTextBook(dfta.parser.FTAParser.transitions, dfta.parser.FTAParser.finalStates, true, true);
        det.makeDfta();
        det.showStats(true);

        final LinkedHashSet<LinkedHashSet<String>> first_Qd = det.qd;
        final LinkedHashSet<dfta.DTransition> first_DELTAd = det.deltad;
        final LinkedHashSet<foo.DTransition> result_DELTAd = new LinkedHashSet<>();
        for (final dfta.DTransition x : first_DELTAd) {
            result_DELTAd.add(new DTransition(new Symb(x.f.fname, x.f.arity), x.q0, x.lhs));
        }
        return new DeterminiserResultD(first_Qd, result_DELTAd);
    }

    // Page 25 https://www21.in.tum.de/~lammich/2015_SS_Automata2/slides/handout.pdf
    public static DeterminiserResultD powerset(final Index index) {
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

    private static BitSet or_all(final BitSet init, final Iterable<String> qs, final ArrayList<LinkedHashMap<String, BitSet>> lhs_f_f, final int j) {
        final LinkedHashMap<String, BitSet> lhs_map = lhs_f_f.get(j);
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

    String write() {
        final StringBuffer b = new StringBuffer();
        b.append("{\n");
        b.append("  \"type\": \"D\",\n");
        b.append("  \"version\": 1,\n");
        b.append("  \"states\": [\n");
        b.append("    " + String.join(",\n    ", Qd.stream().map(JsonUtil::write_string_list).toList()) + "\n");
        b.append("  ],\n");
        b.append("  \"transitions\": [\n");
        int i = 0;
        for (final DTransition w : this.Δd) {
            b.append("    {\n");
            b.append("      \"arity\": " + w.f.arity + ",\n");
            b.append("      \"name\": \"" + w.f.fname + "\",\n");
            b.append("      \"state\": " + JsonUtil.write_string_list(w.Q0) + ",\n");
            b.append("      \"args\": [\n");
            int j = 0;
            for (final LinkedHashSet<String> x : w.args) {
                b.append("        " + JsonUtil.write_string_list(x));
                if (j < w.args.size() - 1) {
                    b.append(",\n");
                    j++;
                } else {
                    b.append("\n");
                }
            }
            b.append("      ]\n");
            b.append("    }");
            if (i < this.Δd.size() - 1) {
                b.append(",\n");
                i++;
            } else {
                b.append("\n");
            }
        }
        b.append("  ]\n");
        b.append("}");
        return b.toString();
    }
}

class DeterminiserResultP {
    final Index index;
    final LinkedHashSet<LinkedHashSet<String>> Qd;
    final ArrayList<PTransition> Δp;

    DeterminiserResultP(Index index, final LinkedHashSet<LinkedHashSet<String>> Qd, final ArrayList<PTransition> Δp) {
        this.index = index;
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
                // Don't care argument.
                if (arg_size == 0) arg_size = Qd_size;
                t_count = t_count * arg_size;
            }
            count = count + t_count;
        }
        return Math.round(count);
    }

    public String write() {
        final StringBuffer b = new StringBuffer();
        b.append("{\n");
        b.append("  \"type\": \"P\",\n");
        b.append("  \"version\": 1,\n");
        b.append("  \"states\": [\n");
        b.append("    " + String.join(",\n    ", Qd.stream().map(JsonUtil::write_string_list).toList()) + "\n");
        b.append("  ],\n");
        b.append("  \"transitions\": [\n");
        int i = 0;
        for (final PTransition w : this.Δp) {
            b.append("    {\n");
            b.append("      \"arity\": " + w.f.arity + ",\n");
            b.append("      \"name\": \"" + w.f.fname + "\",\n");
            b.append("      \"state\": " + JsonUtil.write_string_list(w.Q0) + ",\n");
            b.append("      \"args\": [\n");
            int j = 0;
            for (final LinkedHashSet<LinkedHashSet<String>> x : w.args) {
                b.append("        [\n");
                int k = 0;
                for (final LinkedHashSet<String> y : x) {
                    b.append("          " + JsonUtil.write_string_list(y));
                    if (k < x.size() - 1) {
                        b.append(",\n");
                        k++;
                    } else {
                        b.append("\n");
                    }
                }
                b.append("        ]");
                if (j < w.args.size() - 1) {
                    b.append(",\n");
                    j++;
                } else {
                    b.append("\n");
                }
            }
            b.append("      ]\n");
            b.append("    }");
            if (i < this.Δp.size() - 1) {
                b.append(",\n");
                i++;
            } else {
                b.append("\n");
            }
        }
        b.append("  ]\n");
        b.append("}");
        return b.toString();
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
    final String path;

    public Index(String path, boolean complete) throws FileNotFoundException, ParseException {
        final var model = new FTAParser(new FileInputStream(path)).FTA();
        a = new IndicesA(model, complete);
        b = new IndicesB(a);
        this.path = path;
    }
}

class IndicesA {
    final LinkedHashSet<String> Q = new LinkedHashSet<>();
    final LinkedHashSet<String> Qf = new LinkedHashSet<>();
    final LinkedHashSet<Symb> Σ = new LinkedHashSet<>();
    final LinkedHashSet<Transition> Δ = new LinkedHashSet<>();

    public IndicesA(final TAModel data, final boolean complete) {
        // region transitions
        int cur_transition_id = 0;
        // region init delta
        for (final TAModelTransition transition : data.transitions) {
            final ArrayList<String> args = new ArrayList<>(transition.args);
            final Symb fn = new Symb(transition.fname, transition.args.size());
            Σ.add(fn);
            Q.add(transition.q);
            Δ.add(new Transition(fn, transition.q, args, cur_transition_id));
            cur_transition_id++;
        }
        // endregion
        // region complete delta
        if (complete) {
            for (final Symb f : Σ) {
                final ArrayList<String> args = new ArrayList<>();
                for (int j = 0; j < f.arity; j++) {
                    args.add(j, "'$any'");
                }
                Δ.add(new Transition(f, "'$any'", args, cur_transition_id));
                cur_transition_id++;
            }
        }
        // endregion
        // endregion
        // region final states
        Qf.addAll(data.final_states);
        // endregion
    }
}

class IndicesB {
    final LinkedHashMap<Integer, Transition> transition_by_id = new LinkedHashMap<>();
    final LinkedHashMap<Symb, BitSet> f_index = new LinkedHashMap<>();
    final LinkedHashMap<Symb, ArrayList<LinkedHashMap<String, BitSet>>> lhs_f = new LinkedHashMap<>();
    final LinkedHashSet<Symb> constants = new LinkedHashSet<>();
    final LinkedHashSet<Symb> inconstants = new LinkedHashSet<>();

    public IndicesB(final IndicesA indices_a) {
        // region transition_by_id
        for (final Transition t : indices_a.Δ) {
            transition_by_id.put(t.m, t);
        }
        // endregion
        // region f_index
        for (final Transition t : indices_a.Δ) {
            if (!f_index.containsKey(t.f)) {
                f_index.put(t.f, new BitSet(indices_a.Δ.size()));
            }
            f_index.get(t.f).set(t.m);  // Set the bit for the mth transition.
        }
        // endregion
        // region lhs_f
        for (final Transition t : indices_a.Δ) {
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
                    qmap.get(i).put(q, new BitSet(indices_a.Δ.size()));
                }
                qmap.get(i).get(q).set(t.m);
            }
        }
        // endregion
        // region transition constants/inconstants
        for(final Symb s : indices_a.Σ) {
            if (s.arity == 0) {
                constants.add(s);
            } else {
                inconstants.add(s);
            }
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

class JsonUtil {
    static String write_string_list(Iterable<String> strs) {
        String str = "";
        str += "[";
        final var i = strs.iterator();
        while(i.hasNext()) {
            str += "\"" + i.next() + "\"";
            if (i.hasNext()) {
                str += ", ";
            }
        }
        str += "]";
        return str;
    }
}
