package dfta;

import dfta.parser.FTAParser;
import dfta.parser.ParseException;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.function.Function;

// TODO • get this to run DFTA upstream master and serialize determinized.
public class Driver {
    static final String root = "/Users/modulovalue/Desktop/DFTA/";
    static final String all_example_root = root + "examples/";
    static final String d_fixture_root = root + "examples_d/";
    static final String p_fixture_root = root + "examples_p/";

    public static void main(final String[] args) {
        new File(d_fixture_root).mkdir();
        new File(p_fixture_root).mkdir();
        try {
            switch ("all") {
//            switch ("one") {
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
                            p_fixture_root + file.getName()
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
                        p_fixture_root + file.getName()
                    );
                }
            }
        } catch (ParseException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    static void run(final File file, final String result_d, final String result_p) throws IOException, ParseException {
        final long start_time = System.currentTimeMillis();
        final Index index = new Index(new FTAParser(new java.io.FileInputStream(file.getPath())).FTA(), true);
        final long midTime = System.currentTimeMillis();
        System.out.println("=== Determinising: " + file.getAbsolutePath() + " ===");
        boolean success = false;
        switch ("gallagher_product") {
//        switch ("tata") {
//        switch ("powerset") {
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
        if (success) {
            System.out.println("Number of input FTA states/transitions = " + index.a.Q.size() + "/" + index.a.Δ.size());
            final long endTime = System.currentTimeMillis();
            System.out.println("File input time = " + ((midTime - start_time) / 1000.0) + ",");
            System.out.println("Determinisation time = " + ((endTime - midTime) / 1000.0) + ",");
        }
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
            System.out.println("        => ERROR: Timeout while running operation A");
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
