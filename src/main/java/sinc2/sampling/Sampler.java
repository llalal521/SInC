package sinc2.sampling;

import sinc2.common.Record;
import sinc2.kb.SimpleKb;
import sinc2.util.ArrayOperation;

import java.util.*;

/**
 * The base class of samplers.
 *
 * @since 2.1
 */
public abstract class Sampler {
    /**
     * Sample the original KB given a budget.
     *
     * @return The sampled KB and the mapping of constant numerations
     */
    abstract public SamplingInfo sample(SimpleKb originalKb, int budget, String sampledKbName);

    /**
     * Format a list of sampled relations into a SimpleKb object and find the mappings of constant numerations.
     *
     * @param kbName           The name of the sampled KB
     * @param sampledRelations Sampled records organized in sets, each corresponding to the original relation
     * @param relNames         The list of relation names, every corresponding to the original relation
     * @return The sampled KB and the mapping of constant numerations
     */
    protected static SamplingInfo formatSampledKb(String kbName, List<Set<Record>> sampledRelations, String[] relNames) {
        /* Keep only non-empty relations */
        List<int[][]> non_empty_relations = new ArrayList<>();
        List<String> non_empty_rel_names = new ArrayList<>();
        List<Integer> const_map = new ArrayList<>();            // map from new to old
        Map<Integer, Integer> inverse_map = new HashMap<>();    // map from old to new
        const_map.add(0);    // 0 is not mapped to any constant number
        for (int rel_idx = 0; rel_idx < sampledRelations.size(); rel_idx++) {
            Set<Record> sampled_records = sampledRelations.get(rel_idx);
            if (!sampled_records.isEmpty()) {
                int[][] sampled_relation = new int[sampled_records.size()][];
                int record_idx = 0;
                for (Record _record: sampled_records) {
                    int[] record = new int[_record.args.length];
                    for (int arg_idx = 0; arg_idx < record.length; arg_idx++) {
                        record[arg_idx] = inverse_map.computeIfAbsent(_record.args[arg_idx], k -> {
                            const_map.add(k);
                            return const_map.size() - 1;
                        });
                    }
                    sampled_relation[record_idx] = record;
                    record_idx++;
                }
                non_empty_relations.add(sampled_relation);
                non_empty_rel_names.add(relNames[rel_idx]);
            }
        }

        return new SamplingInfo(
                new SimpleKb(
                        kbName, non_empty_relations.toArray(new int[0][][]), non_empty_rel_names.toArray(new String[0])
                ), ArrayOperation.toArray(const_map)
        );
    }
}
