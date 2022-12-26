package sinc2.exp.hint.predefined;

import sinc2.exp.hint.ExperimentException;
import sinc2.exp.hint.HinterKb;
import sinc2.kb.SimpleRelation;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PredefinedHinterMT {
    /**
     * Args: <Path where the KB dir locates> <KB name> <Path to the hint file>
     */
    public static void main(String[] args) throws IOException, ExperimentException {
        if (4 != args.length) {
            System.err.println("Usage: java -jar hinter-predefined.jar <Path where the KB dir locates> <KB name> <Path to the hint file> <Threads>");
            return;
        }
        String kb_path = args[0];
        String kb_name = args[1];
        String hint_file_path = args[2];
        int threads = Integer.parseInt(args[3]);
        String output_dir_path = getOutputDirPath(hint_file_path, kb_name);
        run(kb_path, kb_name, hint_file_path, output_dir_path, threads);
    }

    public static void run(String kbPath, String kbName, String hintFilePath, String outputDirPath, int threads) throws IOException, ExperimentException {
        PrintStream log_stream = new PrintStream(Paths.get(outputDirPath, String.format("hinterMT_%s.log", kbName)).toFile());
        PrintStream original_out = System.out;
        PrintStream original_err = System.err;
        System.setOut(log_stream);
        System.setErr(log_stream);

        /* Load template file */
        long time_start = System.currentTimeMillis();
        BufferedReader reader = new BufferedReader(new FileReader(hintFilePath));
        try {
            TemplateMiner.COVERAGE_THRESHOLD = Double.parseDouble(reader.readLine());
        } catch (Exception e) {
            throw new ExperimentException("Missing fact coverage setting", e);
        }
        try {
            TemplateMiner.TAU_THRESHOLD = Double.parseDouble(reader.readLine());
        } catch (Exception e) {
            throw new ExperimentException("Missing compression ratio setting", e);
        }
        String line;
        List<String> template_names = new ArrayList<>();
        while (null != (line = reader.readLine())) {
            template_names.add(line);
        }
        long time_templates_loaded = System.currentTimeMillis();
        System.out.printf("Templates Loaded: %d ms\n", time_templates_loaded - time_start);
        System.out.flush();

        /* Load KB */
        HinterKb kb = new HinterKb(kbName, kbPath, TemplateMiner.COVERAGE_THRESHOLD);
        long time_kb_loaded = System.currentTimeMillis();
        System.out.printf("KB Loaded: %d s\n", (time_kb_loaded - time_start) / 1000);
        System.out.flush();

        /* Match the templates */
        for (int i = 0; i < template_names.size(); i++) {
            long time_miner_start = System.currentTimeMillis();
            String template_name = template_names.get(i);
            System.out.printf("Matching template (%d/%d): %s\n", i + 1, template_names.size(), template_name);
            TemplateMinerMT miner;
            switch (template_name) {
                case "Dual":
                    miner = new DualMinerMT(threads);
                    break;
                case "Subsumption":
                    miner = new SubsumptionMinerMT(threads);
                    break;
                case "SharedSourceSink":
                    miner = new SharedSourceSinkMinerMT(threads);
                    break;
                case "Transition":
                    miner = new TransitionMinerMT(threads);
                    break;
                default:
                    System.err.println("Unknown template: " + template_name);
                    continue;
            }
            List<MatchedRule> matched_rules = miner.matchTemplate(kb);
            miner.dumpResult(matched_rules, outputDirPath);
            long time_miner_done = System.currentTimeMillis();
            System.out.printf("Template matching done (Time Cost: %d s)\n", (time_miner_done - time_miner_start) / 1000);
        }
        long time_done = System.currentTimeMillis();

        /* Calculate matching statistics */
        int total_records = 0;
        int total_covered_records = 0;
        for (SimpleRelation relation: kb.getRelations()) {
            int relation_records = relation.totalRows();
            int covered_records = relation.totalEntailedRecords();
            System.out.printf(
                    "Relation Coverage: %s = %.2f%% (%d/%d)\n", relation.name,
                    covered_records * 100.0 / relation_records, covered_records, relation_records
            );
            total_records += relation_records;
            total_covered_records += covered_records;
        }
        System.out.printf(
                "Total Coverage: %.2f%% (%d/%d)\n",
                total_covered_records * 100.0 / total_records, total_covered_records, total_records
        );
        System.out.printf("Total Time: %d s\n", (time_done - time_start) / 1000);

        System.setOut(original_out);
        System.setErr(original_err);
    }

    protected static String getOutputDirPath(String hintFilePath, String kbName) throws ExperimentException {
        Path dir_path = Paths.get(
                new File(hintFilePath).toPath().toAbsolutePath().getParent().toString(),
                "Template_"+ kbName
        );
        File dir_file = dir_path.toFile();
        if (!dir_file.exists() && !dir_file.mkdirs()) {
            throw new ExperimentException("Template directory creation failed: " + dir_path.toString());
        }
        return dir_path.toString();
    }

}
