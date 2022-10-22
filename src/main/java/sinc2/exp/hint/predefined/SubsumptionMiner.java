package sinc2.exp.hint.predefined;

import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * Template miner for:
 * 3. Subsumption:
 *   h(X0, ..., Xk) :- p(X0, ..., Xk); [(h, p)]
 *
 * @since 2.0
 */
public class SubsumptionMiner extends TemplateMiner {

    protected int arity = 0;

    @Override
    public List<MatchedRule> matchTemplate(SimpleRelation[] relations) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            arity = relation_p.totalCols();

            /* Match head & check validness */
            for (int h = 0; h < relations.length; h++) {
                if (h == p) {
                    continue;
                }
                SimpleRelation head = relations[h];
                if (relation_p.totalCols() != head.totalCols()) {
                    continue;
                }
                checkThenAdd(
                        head, relation_p.getAllRows().clone(), matched_rules,
                        subsumptionRuleString(head.name, relation_p.name)
                );
            }
        }
        return matched_rules;
    }

    protected String subsumptionRuleString(String h, String p) {
        StringBuilder builder = new StringBuilder();
        builder.append('(').append("X0");   // Assumes that the arity here is no less than 1
        for (int i = 1; i < arity; i++) {
            builder.append(',').append('X').append(i);
        }
        builder.append(')');
        String arg_str = builder.toString();
        return String.format("%s%s:-%s%s", h, arg_str, p, arg_str);
    }

    @Override
    public int templateLength() {
        return arity * 2;
    }

    @Override
    public String templateName() {
        return "Subsumption";
    }
}
