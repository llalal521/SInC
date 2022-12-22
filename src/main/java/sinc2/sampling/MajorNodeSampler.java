package sinc2.sampling;

import sinc2.common.Record;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.util.*;

/**
 * Sampling edges of major nodes in KGs.
 *
 * @since 2.1
 */
public class MajorNodeSampler extends Sampler {

    static class SimpleNode {
        public final int num;
        public final List<Edge> edges = new ArrayList<>();

        public SimpleNode(int num) {
            this.num = num;
        }
    }

    protected final Set<Integer> typeValues;

    public MajorNodeSampler(Set<Integer> typeValues) {
        this.typeValues = typeValues;
    }

    @Override
    public SamplingInfo sample(SimpleKb originalKb, int budget, String sampledKbName) {
        /* Build the adjacent list of each constant */
        System.out.println("Building adjacent list ...");
        long time_start = System.currentTimeMillis();
        SimpleNode[] nodes = new SimpleNode[originalKb.totalConstants()+1];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new SimpleNode(i);
        }
        SimpleRelation[] relations = originalKb.getRelations();
        String[] rel_names = new String[relations.length];
        List<Set<Record>> sampled_relations = new ArrayList<>(relations.length);
        for (int i = 0; i < relations.length; i++) {
            SimpleRelation relation = relations[i];
            rel_names[i] = relation.name;
            sampled_relations.add(new HashSet<>());
            for (int[] row: relation) {
                Edge edge = new Edge(row[0], relation.id, row[1]);
                nodes[row[0]].edges.add(edge);
                nodes[row[1]].edges.add(edge);
            }
        }
        long time_adjacent_complete = System.currentTimeMillis();
        System.out.printf("Done (%d s)\n", (time_adjacent_complete - time_start) / 1000);

        /* Select major nodes, that is, ones with the largest degrees and the node is not a type entity */
        System.out.println("Selecting major nodes ...");
        Arrays.sort(nodes, Comparator.comparingInt((SimpleNode n) -> n.edges.size()).reversed());
        int sampled_edges = 0;
        for (int i = 0; i < nodes.length && sampled_edges < budget; i++) {
            SimpleNode node = nodes[i];
            if (typeValues.contains(node.num)) {
                continue;
            }
            sampled_edges += node.edges.size();
            for (Edge edge: node.edges) {
                sampled_relations.get(edge.pred).add(new Record(new int[]{edge.subj, edge.obj}));
            }
        }
        long time_selected = System.currentTimeMillis();
        System.out.printf("Done (%d s)\n", (time_selected - time_adjacent_complete) / 1000);

        /* Format the sampled KB */
        System.out.println("Reformatting sampled KB ...");
        SamplingInfo ret = formatSampledKb(sampledKbName, sampled_relations, rel_names);
        long time_formatted = System.currentTimeMillis();
        System.out.printf("Done (%d s)\n", (time_formatted - time_selected) / 1000);
        System.out.printf("Total Time: %d s\n", (time_formatted - time_start) / 1000);
        return ret;
    }
}
