package sinc2.exp.hint.predefined;

import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * Template miner for:
 * 4. Dual:
 *   h(X, Y) :- p(Y, X)
 *
 * @since 2.0
 */
public class DualMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(SimpleRelation[] relations) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (SimpleRelation relation_p : relations) {
            if (2 != relation_p.totalCols()) {
                continue;
            }

            /* Find entailments */
            int[][] records = relation_p.getAllRows();
            int[][] entailments = new int[records.length][];
            for (int i = 0; i < records.length; i++) {
                int[] record = records[i];
                entailments[i] = new int[]{record[1], record[0]};
            }

            /* Match head & check validness */
            for (SimpleRelation head : relations) {
                if (2 != head.totalCols()) {
                    continue;
                }
                checkThenAdd(head, entailments, matched_rules, dualRuleString(head.name, relation_p.name));
            }
        }
        return matched_rules;
    }

    protected String dualRuleString(String h, String p) {
        return String.format("%s(X,Y):-%s(Y,X)", h, p);
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
