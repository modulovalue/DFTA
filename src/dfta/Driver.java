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
//            switch ("includes") {
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
//                    final File file = new File("/Users/modulovalue/Desktop/DFTA/examples/AEXP")).FTA();
                    run(file);
                }
            }
        } catch (FileNotFoundException | ParseException e) {
            System.out.println(e.getMessage());
        }
    }

    static void run(final File file) throws FileNotFoundException, ParseException {
        final TAModel data = new FTAParser(new java.io.FileInputStream(file.getPath())).FTA();
        final long startTime = System.currentTimeMillis();
        final IndicesA indices_a = new IndicesA(data, "id", 0, true);
        final IndicesB indices_b = new IndicesB(indices_a);
        final long midTime = System.currentTimeMillis();
        System.out.println("=== Determinising: " + file.getAbsolutePath() + " ===");
        System.out.println("Number of input FTA states/transitions = " + indices_a.states.size() + "/" + indices_a.transitions.size());
        switch ("opt") {
//        switch ("tb") {
            case "opt" -> {
                final DeterminiserOpt det = new DeterminiserOpt(indices_b, indices_a);
                System.out.println("Number of DFTA states/normal transitions/product transitions = " + det.qd.size() + "/" + det.deltaDCount() + "/" + det.delta_p.size());
            }
            case "tb" -> {
                final DeterminiserTextBook det = new DeterminiserTextBook(indices_b, indices_a);
                System.out.println("Number of DFTA states/transitions = " + det.qd.size() + "/" + det.deltad.size());
            }
        }
        final long endTime = System.currentTimeMillis();
        System.out.println("File input time = " + ((midTime - startTime) / 1000.0) + ",");
        System.out.println("Determinisation time = " + ((endTime - midTime) / 1000.0) + ",");
    }
}
