package dfta;

import dfta.parser.FTAParser;
import dfta.parser.ParseException;
import dfta.parser.TAModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class Driver {
    public static void main(final String[] args) {
        try {
//            switch ("all") {
            switch ("one") {
                case "all" -> {
                    final File[] files = new File("/Users/modulovalue/Desktop/DFTA/examples").listFiles();
                    for (final File file : Arrays.stream(files).sorted().toList()) {
                        run(file);
                    }
                }
                case "one" -> {
//                    final File file = new File("/Users/modulovalue/Desktop/flutter_mindmap/pkgs/flutter_companion/layer0/humanspec/lib/parser/sine/hele/ta/examples/A0053");
//                    final File file = new File("/Users/modulovalue/Desktop/flutter_mindmap/pkgs/flutter_companion/layer0/humanspec/lib/parser/sine/hele/ta/examples/A0063");
                    final File file = new File("/Users/modulovalue/Desktop/flutter_mindmap/pkgs/flutter_companion/layer0/humanspec/lib/parser/sine/hele/ta/examples/A493");
//                    final File file = new File("/Users/modulovalue/Desktop/flutter_mindmap/pkgs/flutter_companion/layer0/humanspec/lib/parser/sine/hele/ta/examples/A620");
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
            case "gallagher_product" -> {
                final var det = Determiniser.gallagher_product(index);
                System.out.println("Number of DFTA states/normal transitions/product transitions = " + det.q_d.size() + "/" + det.delta_d_count() + "/" + det.delta_p.size());
            }
            case "tata" -> {
                final var det = Determiniser.tata(index);
                System.out.println("Number of DFTA states/transitions = " + det.q_d.size() + "/" + det.delta_d.size());
            }
        }
        final long endTime = System.currentTimeMillis();
        System.out.println("File input time = " + ((midTime - start_time) / 1000.0) + ",");
        System.out.println("Determinisation time = " + ((endTime - midTime) / 1000.0) + ",");
    }
}
