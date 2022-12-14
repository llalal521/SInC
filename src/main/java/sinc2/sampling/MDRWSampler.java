package sinc2.sampling;

import sinc2.common.Record;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.util.*;

/**
 * Multi-dimensional Random Walk Sampler. Reference:
 *   Ribeiro, B., & Towsley, D. (2010, November). Estimating and sampling graphs with multidimensional random walks.
 *   In Proceedings of the 10th ACM SIGCOMM conference on Internet measurement (pp. 390-403).
 *
 * NOTE: This sampler should only be applied on KBs where all relations are binary (i.e., knowledge graphs).
 *
 * @since 2.1
 */
public class MDRWSampler extends Sampler {

    /** The dimension of the sampler */
    public final int dimension;

    /**
     * Initialize the sampler with a dimension.
     */
    public MDRWSampler(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public SamplingInfo sample(SimpleKb originalKb, int budget, String sampledKbName) {
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
        int[] selected_nodes = new int[dimension];
        int[] edges_in_nodes = new int[dimension+1];
        int total_edges = 0;
        for (int i = 0; i < dimension; i++) {
            int rand_idx = random.nextInt(total_rows);
            int rel_idx = Arrays.binarySearch(rows_in_relations, rand_idx);
            rel_idx = (rel_idx < 0) ? -rel_idx - 2 : rel_idx;
            int[] record = relations[rel_idx].getRowAt(rand_idx - rows_in_relations[rel_idx]);
            int node_num = record[random.nextInt(2)];
            selected_nodes[i] = node_num;
            total_edges += nodes[node_num].totalEdges();
            edges_in_nodes[i+1] = total_edges;
        }

        /* Sample by MDRW */
        for (int i = 0; i < budget; i++) {
            /* Select one edge */
            int rand_idx = random.nextInt(total_edges);
            int selected_idx = Arrays.binarySearch(edges_in_nodes, rand_idx);
            selected_idx = (selected_idx < 0) ? -selected_idx - 2 : selected_idx;
            int node_num = selected_nodes[selected_idx];
            Edge edge = nodes[node_num].getEdgeAt(rand_idx - edges_in_nodes[selected_idx]);
            sampled_relations.get(edge.pred).add(new Record(new int[]{edge.subj, edge.obj}));

            /* Change node */
            int new_node_num = (edge.subj == node_num) ? edge.obj : edge.subj;
            selected_nodes[selected_idx] = new_node_num;
            int delta_edge_cnt = edges_in_nodes[selected_idx+1] - edges_in_nodes[selected_idx] - nodes[new_node_num].totalEdges();
            for (int j = selected_idx+1; j <= dimension; j++) {
                edges_in_nodes[j] -= delta_edge_cnt;
            }
            total_edges -= delta_edge_cnt;
        }

        /* Format the sampled KB */
        return formatSampledKb(sampledKbName, sampled_relations, rel_names);
    }
}
