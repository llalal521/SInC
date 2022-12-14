package sinc2.sampling;

import sinc2.common.Record;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.util.*;

/**
 * A simple edge sampler.
 *
 * @since 2.1
 */
public class EdgeSampler extends Sampler {
    @Override
    public SamplingInfo sample(SimpleKb originalKb, int budget, String sampledKbName) {
        /* List the relations and rows in relations in order */
        SimpleRelation[] relations = originalKb.getRelations();
        String[] rel_names = new String[relations.length];
        int[] rows_in_relations = new int[relations.length+1];
        int total_rows = 0;
        rows_in_relations[0] = 0;
        List<Set<Record>> sampled_relations = new ArrayList<>(relations.length);
        for (int i = 0; i < relations.length; i++) {
            total_rows += relations[i].totalRows();
            rows_in_relations[i+1] = total_rows;
            rel_names[i] = relations[i].name;
            sampled_relations.add(new HashSet<>());
        }

        /* Sample edges in a uniform distribution */
        Random random = new Random();
        for (int i = 0; i < budget; i++) {
            int rand_idx = random.nextInt(total_rows);
            int rel_idx = Arrays.binarySearch(rows_in_relations, rand_idx);
            rel_idx = (rel_idx < 0) ? -rel_idx - 2 : rel_idx;
            int[] record = relations[rel_idx].getRowAt(rand_idx - rows_in_relations[rel_idx]);
            sampled_relations.get(rel_idx).add(new Record(record));
        }

        /* Format the new KB */
        return formatSampledKb(sampledKbName, sampled_relations, rel_names);
    }
}
