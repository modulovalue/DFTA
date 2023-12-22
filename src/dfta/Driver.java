package dfta;

import dfta.parser.FTAParser;
import dfta.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class Driver {
    public static void main(final String[] args) {
        final String example_root = "/Users/modulovalue/Desktop/DFTA/examples/";
        try {
//            switch ("all") {
            switch ("one") {
                case "all" -> {
                    final File[] files = new File(example_root).listFiles();
                    for (final File file : Arrays.stream(files).sorted().toList()) {
                        run(file);
                    }
                }
                case "one" -> {
//                    final File file = new File(example_root + "A0053");
//                    final File file = new File(example_root + "A0063");
                    final File file = new File(example_root + "A493");
//                    final File file = new File(example_root + "A620");
                    run(file);
                }
            }
        } catch (FileNotFoundException | ParseException e) {
            System.out.println(e.getMessage());
        }
    }

    static void run(final File file) throws FileNotFoundException, ParseException {
        final long start_time = System.currentTimeMillis();
        final Index index = new Index(new FTAParser(new java.io.FileInputStream(file.getPath())).FTA(), true);
        final long midTime = System.currentTimeMillis();
        System.out.println("=== Determinising: " + file.getAbsolutePath() + " ===");
        System.out.println("Number of input FTA states/transitions = " + index.a.states.size() + "/" + index.a.transitions.size());
        switch ("gallagher_product") {
//        switch ("tata") {
//        switch ("powerset") {
            case "gallagher_product" -> {
                final var det = Determiniser.powerset_with_reduction_and_gallagherproducttransitions(index);
                System.out.println("Number of DFTA states/normal transitions/product transitions = " + det.Qd.size() + "/" + det.Δd_count() + "/" + det.Δp.size());
            }
            case "tata" -> {
                final var det = Determiniser.powerset_with_reduction(index);
                System.out.println("Number of DFTA states/transitions = " + det.Qd.size() + "/" + det.Δd.size());
            }
            case "powerset" -> {
                final var det = Determiniser.powerset();
                System.out.println("Number of DFTA states/transitions = " + det.Qd.size() + "/" + det.Δd.size());
            }
        }
        final long endTime = System.currentTimeMillis();
        System.out.println("File input time = " + ((midTime - start_time) / 1000.0) + ",");
        System.out.println("Determinisation time = " + ((endTime - midTime) / 1000.0) + ",");
    }
}
