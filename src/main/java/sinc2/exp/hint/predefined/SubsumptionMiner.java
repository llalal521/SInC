package sinc2.exp.hint.predefined;

import sinc2.exp.hint.HinterKb;
import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public List<MatchedRule> matchTemplate(HinterKb kb) {
        SimpleRelation[] relations = kb.getRelations();
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            arity = relation_p.totalCols();

            /* Match head & check validness */
            Set<Integer> rel_idxs = new HashSet<>();
            for (HinterKb.ColInfo col_info: kb.inverseSimilarCols(p, 0)) {
                if (0 == col_info.colIdx && relations[col_info.relIdx].totalCols() == arity) {
                    rel_idxs.add(col_info.relIdx);
                }
            }
            rel_idxs.remove(p); // p should not be the same as q
            for (int col_idx = 1; col_idx < arity; col_idx++) {
                Set<Integer> tmp = new HashSet<>();
                for (HinterKb.ColInfo col_info: kb.inverseSimilarCols(p, col_idx)) {
                    if (col_idx == col_info.colIdx) {
                        tmp.add(col_info.relIdx);
                    }
                }
                rel_idxs.removeIf(e -> !tmp.contains(e));
            }
            if (!rel_idxs.isEmpty()) {
                int[][] entailments = relation_p.getAllRows().clone();
                for (int h : rel_idxs) {
                    SimpleRelation head = relations[h];
                    checkThenAdd(
                            head, entailments, matched_rules,
                            subsumptionRuleString(head.name, relation_p.name)
                    );
                }
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
