package sinc2.exp.hint.predefined;

import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * Template miner for:
 * 2. Reflexive:
 *   h(X, X) :-
 *
 * @since 2.0
 */
public class ReflexiveMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(SimpleRelation[] relations) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (SimpleRelation head : relations) {
            if (2 != head.totalCols()) {
                continue;
            }

            /* Find entailments */
            List<int[]> entailments = new ArrayList<>();
            for (int[] record : head) {
                if (record[0] == record[1]) {
                    entailments.add(record);
                }
            }

            /* Match head & check validness */
            checkThenAdd(head, entailments.toArray(new int[0][]), matched_rules, head.name + "(X,X):-");
        }
        return matched_rules;
    }

    @Override
    public int templateLength() {
        return 1;
    }

    @Override
    public String templateName() {
        return "Reflexive";
    }
}
