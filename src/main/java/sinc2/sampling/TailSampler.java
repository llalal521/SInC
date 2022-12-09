package sinc2.sampling;

import sinc2.common.Record;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TailSampler extends Sampler {
    @Override
    public SamplingInfo sample(SimpleKb originalKb, int budget, String sampledKbName) {
        SimpleRelation[] relations = originalKb.getRelations();
        String[] rel_names = new String[relations.length];
        List<Set<Record>> sampled_relations = new ArrayList<>(relations.length);
        for (int i = 0; i < relations.length; i++) {
            SimpleRelation relation = relations[i];
            rel_names[i] = relation.name;
            Set<Record> sampled_relation = new HashSet<>();
            sampled_relations.add(sampled_relation);
            int sample_capacity = (int) (budget * 1.0 / originalKb.totalRecords() * relation.totalRows());
            for (int j = 1; j <= sample_capacity; j++) {
                sampled_relation.add(new Record(relation.getRowAt(relation.totalRows() - j)));
            }
        }

        /* Format the sampled KB */
        return formatSampledKb(sampledKbName, sampled_relations, rel_names);
    }
}
