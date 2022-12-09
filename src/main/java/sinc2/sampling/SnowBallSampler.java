package sinc2.sampling;

import sinc2.common.Record;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.util.*;

/**
 * A Snow-Ball sampler. The original KB should be a knowledge graph.
 *
 * @since 2.1
 */
public class SnowBallSampler extends Sampler {

    final int rounds;

    public SnowBallSampler(int rounds) {
        this.rounds = rounds;
    }

    @Override
    public SamplingInfo sample(SimpleKb originalKb, int budget, String sampledKbName) {
        final int avg_deg = originalKb.totalRecords() / originalKb.totalConstants() +
                ((0 == originalKb.totalRecords() % originalKb.totalConstants()) ? 0 : 1);
        final int start_nodes = (int) Math.ceil(Math.pow(budget, 1.0/rounds) / avg_deg);

        /* Build the adjacent list of each constant */
        Node[] nodes = new Node[originalKb.totalConstants()+1];
        for (int i = 1; i < nodes.length; i++) {
            nodes[i] = new Node(i);
        }
        SimpleRelation[] relations = originalKb.getRelations();
        String[] rel_names = new String[relations.length];
        int[] rows_in_relations = new int[relations.length+1];
        int total_rows = 0;
        rows_in_relations[0] = 0;
        List<Set<Record>> sampled_relations = new ArrayList<>(relations.length);
        for (int i = 0; i < relations.length; i++) {
            SimpleRelation relation = relations[i];
            rel_names[i] = relation.name;
            total_rows += relation.totalRows();
            rows_in_relations[i+1] = total_rows;
            sampled_relations.add(new HashSet<>());
            for (int[] record: relation) {
                int subj = record[0];
                int obj = record[1];
                nodes[subj].outEdgeInNodes.add(new EdgeInNode(relation.id, obj));
                nodes[obj].inEdgeInNodes.add(new EdgeInNode(relation.id, subj));
            }
        }

        /* Sample m constants proportional to their degrees (in + out) */
        /* In the following procedure, m edges are sampled and m/2 subjects and m/2 objects are selected */
        Random random = new Random();
        List<Integer> selected_nodes = new ArrayList<>();
        for (int i = 0; i < start_nodes; i++) {
            int rand_idx = random.nextInt(total_rows);
            int rel_idx = Arrays.binarySearch(rows_in_relations, rand_idx);
            rel_idx = (rel_idx < 0) ? -rel_idx - 2 : rel_idx;
            int[] record = relations[rel_idx].getRowAt(rand_idx - rows_in_relations[rel_idx]);
            int node_num = record[random.nextInt(2)];
            selected_nodes.add(node_num);
        }

        /* run the snow-balls */
        for (int i = 0; i < rounds - 1; i++) {
            List<Integer> new_selected_nodes = new ArrayList<>();
            for (int node_num: selected_nodes) {
                Edge[] rand_edges = nodes[node_num].randEdges(avg_deg);
                for (Edge edge: rand_edges) {
                    sampled_relations.get(edge.pred).add(new Record(new int[]{edge.subj, edge.obj}));
                    new_selected_nodes.add((node_num == edge.subj) ? edge.obj : edge.subj);
                }
            }
            selected_nodes = new_selected_nodes;
        }
        for (int node_num: selected_nodes) {    // Last round, new nodes are not recorded
            Edge[] rand_edges = nodes[node_num].randEdges(avg_deg);
            for (Edge edge: rand_edges) {
                sampled_relations.get(edge.pred).add(new Record(new int[]{edge.subj, edge.obj}));
            }
        }

        /* Format the sampled KB */
        return formatSampledKb(sampledKbName, sampled_relations, rel_names);
    }
}
