package sinc2.exp.sampling;

import sinc2.common.SincException;
import sinc2.exp.hint.predefined.PredefinedHinter;
import sinc2.kb.KbException;
import sinc2.kb.SimpleKb;
import sinc2.sampling.*;
import sinc2.util.LittleEndianIntIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class TestHinterOnDataset {
    static final int DUP = 1;

    static void runHinter(String kbName, String kbPath, int budget) throws IOException {
        System.out.println("Loading KB ...");
        SimpleKb original_kb = new SimpleKb(kbName, kbPath);
        Set<Integer> type_values = loadTypeValues(kbName, kbPath);

        System.out.println("Hinter on original ...");
        double coverage_threshold = 0.01;
        double tau_threshold = 0.5;
        String[] template_names = new String[]{"TypeInference", "Reflexive", "Subsumption", "Dual", "Transition", "SharedSourceSink"};
        String output_dir = "/dev/shm/TestHinterOnSampledKb";
//        PredefinedHinter.run(original_kb, coverage_threshold, tau_threshold, template_names, output_dir);

        HeadSampler head_sampler = new HeadSampler();
        TailSampler tail_sampler = new TailSampler();
        MajorNodeSampler major_node_sampler = new MajorNodeSampler(type_values);
        SnowBallSampler snow_ball_sampler = new SnowBallSampler(4);
        for (int i = 0; i < DUP; i++) {
            System.out.printf("Compressing sampled #%d ...\n", i + 1);

            System.out.print("Try Head Sampler ... ");
            long time_start = System.currentTimeMillis();
            SamplingInfo sampling_info = head_sampler.sample(original_kb, budget, "head_sampled" + (i + 1));
            long time_done = System.currentTimeMillis();
            System.out.printf("(sampling time: %d ms) ...\n", time_done - time_start);
            PredefinedHinter.run(sampling_info.sampledKb, coverage_threshold, tau_threshold, template_names, output_dir);

            System.out.print("Try Tail Sampler ... ");
            time_start = System.currentTimeMillis();
            sampling_info = tail_sampler.sample(original_kb, budget, "tail_sampled" + (i + 1));
            time_done = System.currentTimeMillis();
            System.out.printf("(sampling time: %d ms) ...\n", time_done - time_start);
            PredefinedHinter.run(sampling_info.sampledKb, coverage_threshold, tau_threshold, template_names, output_dir);

            System.out.print("Try Snow Ball Sampler ... ");
            time_start = System.currentTimeMillis();
            sampling_info = snow_ball_sampler.sample(original_kb, budget, "sb_sampled" + (i + 1));
            time_done = System.currentTimeMillis();
            System.out.printf("(sampling time: %d ms) ...\n", time_done - time_start);
            PredefinedHinter.run(sampling_info.sampledKb, coverage_threshold, tau_threshold, template_names, output_dir);

            System.out.print("Try Major Node Sampler ... ");
            time_start = System.currentTimeMillis();
            sampling_info = major_node_sampler.sample(original_kb, budget, "mn_sampled" + (i + 1));
            time_done = System.currentTimeMillis();
            System.out.printf("(sampling time: %d ms) ...\n", time_done - time_start);
            PredefinedHinter.run(sampling_info.sampledKb, coverage_threshold, tau_threshold, template_names, output_dir);
        }
    }

    static protected Set<Integer> loadTypeValues(String kbName, String kbPath) {
        File type_value_file = Paths.get(kbPath, kbName, "TypeValues.dat").toFile();
        Set<Integer> type_values = new HashSet<>();
        try (FileInputStream fis = new FileInputStream(type_value_file)) {
            byte[] buffer = new byte[Integer.BYTES];
            while (Integer.BYTES == fis.read(buffer)) {
                type_values.add(LittleEndianIntIO.byteArray2LeInt(buffer));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return type_values;
    }

    public static void main(String[] args) throws SincException, KbException, IOException {
        if (3 != args.length) {
            System.out.println("Usage: <KB name> <KB path> <budget>");
        }
        String kb_name = args[0];
        String kb_path = args[1];
        int budget = Integer.parseInt(args[2]);
        runHinter(kb_name, kb_path, budget);
    }
}
