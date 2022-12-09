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
    @Override
    public SamplingInfo sample(SimpleKb originalKb, int budget, String sampledKbName) {
        /* Build the adjacent list of each constant */
        Set<Edge>[] nodes = new Set[originalKb.totalConstants()+1];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new HashSet<>();
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
                nodes[row[0]].add(edge);
                nodes[row[1]].add(edge);
            }
        }

        /* Select major nodes, that is, ones with the largest degrees and the degree is no higher than the 90th percentile */
        Arrays.sort(nodes, Comparator.comparingInt((Set<Edge> e) -> e.size()).reversed());
        int sampled_edges = 0;
        for (int i = (int) (nodes.length * 0.1); i < nodes.length && sampled_edges < budget; i++) {
            Set<Edge> edges = nodes[i];
            sampled_edges += edges.size();
            for (Edge edge: edges) {
                sampled_relations.get(edge.pred).add(new Record(new int[]{edge.subj, edge.obj}));
            }
        }

        /* Format the sampled KB */
        return formatSampledKb(sampledKbName, sampled_relations, rel_names);
    }
}
