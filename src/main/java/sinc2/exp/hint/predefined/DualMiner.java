package sinc2.exp.hint.predefined;

import sinc2.kb.Record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template miner for:
 * 4. Dual:
 *   h(X, Y) :- p(Y, X)
 *
 * @since 2.0
 */
public class DualMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(
            List<Set<Record>> relations, List<Set<Record>> positiveEntailments, List<String> functorNames
    ) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.size(); p++) {
            Set<Record> relation_p = relations.get(p);
            if (relation_p.isEmpty() || 2 != relation_p.iterator().next().args.length) {
                continue;
            }

            /* Find entailments */
            Set<Record> entailments = new HashSet<>();
            for (Record record: relation_p) {
                entailments.add(new Record(new int[]{record.args[1], record.args[0]}));
            }

            /* Match head & check validness */
            for (int h = 0; h < relations.size(); h++) {
                Set<Record> head = relations.get(h);
                if (head.isEmpty() || 2 != head.iterator().next().args.length) {
                    continue;
                }
                checkThenAdd(
                        head, positiveEntailments.get(h), entailments, matched_rules,
                        dualRuleString(h, p, functorNames)
                );
            }
        }
        return matched_rules;
    }

    protected String dualRuleString(int h, int p, List<String> functorNames) {
        return String.format("%s(X,Y):-%s(Y,X)", functorNames.get(h), functorNames.get(p));
    }

    @Override
    public int templateLength() {
        return 2;
    }

    @Override
    public String templateName() {
        return "Dual";
    }
}
