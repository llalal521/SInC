package sinc2.exp.sampling;

import sinc2.common.SincException;
import sinc2.exp.hint.predefined.PredefinedHinter;
import sinc2.kb.KbException;
import sinc2.kb.SimpleKb;
import sinc2.sampling.*;
import sinc2.util.datagen.FamilyRelationGenerator;

import java.io.IOException;

public class TestHinterOnSampledFm {

    static final int DUP = 1;

    static void runHinter(int families, double errorRate, int dimension, int budget) throws KbException, IOException {
        System.out.println("Generating KB ...");
        SimpleKb original_kb = FamilyRelationGenerator.generateMedium("original", families, errorRate);

        System.out.println("Hinter on original ...");
        double coverage_threshold = 0.01;
        double tau_threshold = 0.5;
        String[] template_names = new String[]{"TypeInference", "Reflexive", "Subsumption", "Dual", "Transition", "SharedSourceSink"};
        String output_dir = "/dev/shm/TestHinterOnSampledKb";
        PredefinedHinter.run(original_kb, coverage_threshold, tau_threshold, template_names, output_dir);

        EdgeSampler edge_sampler = new EdgeSampler();
        MDRWSampler mdrw_sampler = new MDRWSampler(dimension);
        HeadSampler head_sampler = new HeadSampler();
        TailSampler tail_sampler = new TailSampler();
        MajorNodeSampler major_node_sampler = new MajorNodeSampler();
        SnowBallSampler snow_ball_sampler = new SnowBallSampler(4);
        for (int i = 0; i < DUP; i++) {
            System.out.printf("Compressing sampled #%d ...\n", i + 1);
//            System.out.print("Try Edge Sampler ... ");
//            long time_start = System.currentTimeMillis();
//            SamplingInfo sampling_info = edge_sampler.sample(original_kb, budget, "edge_sampled_" + (i + 1));
//            long time_done = System.currentTimeMillis();
//            System.out.printf("(sampling time: %d ms) ...\n", time_done - time_start);
//            PredefinedHinter.run(sampling_info.sampledKb, coverage_threshold, tau_threshold, template_names, output_dir);
//
//            System.out.print("Try MDRW Sampler ... ");
//            time_start = System.currentTimeMillis();
//            sampling_info = mdrw_sampler.sample(original_kb, budget, "mdrw_sampled" + (i + 1));
//            time_done = System.currentTimeMillis();
//            System.out.printf("(sampling time: %d ms) ...\n", time_done - time_start);
//            PredefinedHinter.run(sampling_info.sampledKb, coverage_threshold, tau_threshold, template_names, output_dir);

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

    public static void main(String[] args) throws SincException, KbException, IOException {
        if (4 != args.length) {
            System.out.println("Usage: <families> <error rate> <MDRW sampler dimension> <budget>");
        }
        int families = Integer.parseInt(args[0]);
        double error_rate = Double.parseDouble(args[1]);
        int dimension = Integer.parseInt(args[2]);
        int budget = Integer.parseInt(args[3]);
        runHinter(families, error_rate, dimension, budget);
    }
}
