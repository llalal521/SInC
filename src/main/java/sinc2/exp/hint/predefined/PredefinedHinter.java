package sinc2.exp.hint.predefined;

import sinc2.exp.hint.ExperimentException;
import sinc2.exp.hint.HinterKb;
import sinc2.kb.SimpleRelation;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class reads a hint template file and a KB to find evaluation of instantiated rules corresponding to several
 * pre-defined templates.
 *
 * The structure of the template file contains n+2 lines:
 *   - The first two lines are the settings of the thresholds of "Fact Coverage" and "τ". The output rules must satisfy
 *     the restrictions (evaluations no smaller than the thresholds);
 *   - Each of the following lines is a name of the predefined templates. Followings are available names:
 *       - TypeInference
 *       - Reflexive
 *       - Subsumption
 *       - Dual
 *       - Transition
 *       - SharedSourceSink
 *
 * The output is a directory containing several files. Each "rules_<Template Name>.tsv" file contains rules discovered
 * by the corresponding template miner and contains n+1 rows and 7 columns:
 *   - The first is the title row. Other rows are in the descent order of τ(r). Rules are sorted in descent order of
 *     compression ratio.
 *   - The columns are:
 *     1. Rule: An instance of the template, written as r.
 *     2. |r|
 *     3. E^+_r
 *     4. E^-_r
 *     5. Fact Coverage of r
 *     6. τ(r)
 *     7. δ(r)
 * The output directory also contains a log file, reporting the runtime information. The structure of the output directory
 * is as follows:
 * ━━Template_<KB name>
 *   ┣━rules_TypeInference.tsv
 *   ┣━rules_Reflexive.tsv
 *   ┣━...
 *   ┗━rules_<KB name>.log
 *
 * The following is an example "template.hint" file:
 *
 *   0.2
 *   0.8
 *   Dual
 *   Transition
 *
 * The following is an example output ".tsv" file:
 *
 *   rule	|r|	E+	E-	FC	τ	δ
 *   grandparent(X,Y):-parent(X,Z),parent(Z,Y)	3	150	0	93.75	0.98	147
 *   grandparent(X,Y):-father(X,Z),father(Z,Y)	3	100	0	62.50	0.97	97
 *
 * @since 2.0
 */
public class PredefinedHinter {

    /**
     * Args: <Path where the KB dir locates> <KB name> <Path to the hint file>
     */
    public static void main(String[] args) throws IOException, ExperimentException {
        if (3 != args.length) {
            System.err.println("Usage: java -jar hinter-predefined.jar <Path where the KB dir locates> <KB name> <Path to the hint file>");
            return;
        }
        String kb_path = args[0];
        String kb_name = args[1];
        String hint_file_path = args[2];
        String output_dir_path = getOutputDirPath(hint_file_path, kb_name);
        PrintStream log_stream = new PrintStream(Paths.get(output_dir_path, String.format("rules_%s.log", kb_name)).toFile());
        System.setOut(log_stream);
        System.setErr(log_stream);

        /* Load template file */
        long time_start = System.currentTimeMillis();
        BufferedReader reader = new BufferedReader(new FileReader(hint_file_path));
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
        HinterKb kb = new HinterKb(kb_name, kb_path, TemplateMiner.COVERAGE_THRESHOLD);
        long time_kb_loaded = System.currentTimeMillis();
        System.out.printf("KB Loaded: %d s\n", (time_kb_loaded - time_start) / 1000);
        System.out.flush();

        /* Match the templates */
        for (int i = 0; i < template_names.size(); i++) {
            long time_miner_start = System.currentTimeMillis();
            String template_name = template_names.get(i);
            System.out.printf("Matching template (%d/%d): %s\n", i + 1, template_names.size(), template_name);
            TemplateMiner miner = TemplateMiner.TemplateType.getMinerByName(template_name);
            List<MatchedRule> matched_rules = miner.matchTemplate(kb);
            miner.dumpResult(matched_rules, output_dir_path);
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
