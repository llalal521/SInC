package sinc2.exp.hint.predefined;

import sinc2.kb.compact.IntTable;
import sinc2.kb.compact.SimpleRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * Template miner for:
 * 6. Shared Source/Sink
 *   h(X, Y) :- p(Z, X), q(Z, Y); [(h, p, q)]
 *   h(X, Y) :- p(X, Z), q(Y, Z); [(h, p, q)]
 */
public class SharedSourceSinkMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(SimpleRelation[] relations) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            if (2 != relation_p.totalCols()) {
                continue;
            }

            /* Match another relation */
            for (int q = p; q < relations.length; q++) {
                SimpleRelation relation_q = relations[q];
                if (2 != relation_q.totalCols()) {
                    continue;
                }

                /* Find matched arguments & construct entailments */
                int[][] ent_shared_source = IntTable.join(relation_p, 0, 1, relation_q, 0, 1);
                int[][] ent_shared_source_dual = new int[ent_shared_source.length][];
                for (int i = 0; i < ent_shared_source.length; i++) {
                    int[] record = ent_shared_source[i];
                    ent_shared_source_dual[i] = new int[]{record[1], record[0]};
                }
                int[][] ent_shared_sink = IntTable.join(relation_p, 1, 0, relation_q, 1, 0);
                int[][] ent_shared_sink_dual = new int[ent_shared_sink.length][];
                for (int i = 0; i < ent_shared_sink.length; i++) {
                    int[] record = ent_shared_sink[i];
                    ent_shared_sink_dual[i] = new int[]{record[1], record[0]};
                }

                /* Match head & check validness */
                for (int h = 0; h < relations.length; h++) {
                    if (h == p && p == q) {
                        continue;
                    }
                    SimpleRelation head = relations[h];
                    if (2 != head.totalCols()) {
                        continue;
                    }

                    checkThenAdd(
                            head, ent_shared_source, matched_rules,
                            sharedSourceRuleString(head.name, relation_p.name, relation_q.name)
                    );
                    checkThenAdd(
                            head, ent_shared_source_dual, matched_rules,
                            sharedSourceRuleString(head.name, relation_q.name, relation_p.name)
                    );
                    checkThenAdd(
                            head, ent_shared_sink, matched_rules,
                            sharedSinkRuleString(head.name, relation_p.name, relation_q.name)
                    );
                    checkThenAdd(
                            head, ent_shared_sink_dual, matched_rules,
                            sharedSinkRuleString(head.name, relation_q.name, relation_p.name)
                    );
                }
            }
        }

        return matched_rules;
    }

    protected String sharedSourceRuleString(String h, String p, String q) {
        return String.format("%s(X,Y):-%s(Z,X),%s(Z,Y)", h, p, q);
    }

    protected String sharedSinkRuleString(String h, String p, String q) {
        return String.format("%s(X,Y):-%s(X,Z),%s(Y,Z)", h, p, q);
    }

    @Override
    public int templateLength() {
        return 3;
    }

    @Override
    public String templateName() {
        return "SharedSourceSink";
    }
}
