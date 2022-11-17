package sinc2.exp.hint.predefined;

import sinc2.exp.hint.HinterKb;
import sinc2.kb.IntTable;
import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template miner for:
 * 5. Transition
 *   h(X, Y) :- p(X, Z), q(Z, Y); [(h, p, q)]
 *   h(X, Y) :- p(Y, Z), q(Z, X); [(h, p, q)]
 *
 * @since 2.0
 */
public class TransitionMiner extends TemplateMiner {

    @Override
    public List<MatchedRule> matchTemplate(HinterKb kb) {
        SimpleRelation[] relations = kb.getRelations();
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            if (2 != relation_p.totalCols()) {
                continue;
            }

            /* Match another relation */
            Set<Integer> rel_idxs_q = new HashSet<>();
            for (HinterKb.ColInfo col_info : kb.similarCols(p, 1)) {
                if (0 == col_info.colIdx && 2 == relations[col_info.relIdx].totalCols()) {
                    rel_idxs_q.add(col_info.relIdx);
                }
            }
            Set<Integer> _rel_idxs_q = new HashSet<>();
            for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 1)) {
                if (0 == col_info.colIdx) {
                    _rel_idxs_q.add(col_info.relIdx);
                }
            }
            rel_idxs_q.removeIf(e -> !_rel_idxs_q.contains(e));
            for (int q : rel_idxs_q) {
                SimpleRelation relation_q = relations[q];

                /* Find matched arguments & construct entailments */
                int[][] ent_transition = null;
                Set<Integer> rel_idxs_h = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 0)) {
                    if (0 == col_info.colIdx && 2 == relations[col_info.relIdx].totalCols() && (p != q || q != col_info.relIdx)) {
                        rel_idxs_h.add(col_info.relIdx);
                    }
                }
                Set<Integer> _rel_idxs_h = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(q, 1)) {
                    if (1 == col_info.colIdx) {
                        _rel_idxs_h.add(col_info.relIdx);
                    }
                }
                rel_idxs_h.removeIf(e -> !_rel_idxs_h.contains(e));
                if (!rel_idxs_h.isEmpty()) {
                    ent_transition = IntTable.join(relation_p, 1, 0, relation_q, 0, 1);
                    for (int h : rel_idxs_h) {
                        SimpleRelation head = relations[h];
                        checkThenAdd(
                                head, ent_transition, matched_rules,
                                transitionRuleString(head.name, relation_p.name, relation_q.name)
                        );
                    }
                }

                /* Find matched arguments & construct entailments (dual) */
                Set<Integer> rel_idxs_h_dual = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 0)) {
                    if (1 == col_info.colIdx && 2 == relations[col_info.relIdx].totalCols() && (p != q || q != col_info.relIdx)) {
                        rel_idxs_h_dual.add(col_info.relIdx);
                    }
                }
                Set<Integer> _rel_idxs_h_dual = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(q, 1)) {
                    if (0 == col_info.colIdx) {
                        _rel_idxs_h_dual.add(col_info.relIdx);
                    }
                }
                rel_idxs_h_dual.removeIf(e -> !_rel_idxs_h_dual.contains(e));
                if (!rel_idxs_h_dual.isEmpty()) {
                    int[][] ent_dual_trans = null;
                    if (null != ent_transition) {
                        ent_dual_trans = new int[ent_transition.length][];
                        for (int i = 0; i < ent_transition.length; i++) {
                            int[] record = ent_transition[i];
                            ent_dual_trans[i] = new int[]{record[1], record[0]};
                        }
                    } else {
                        ent_dual_trans = IntTable.join(relation_q, 0, 1, relation_p, 1, 0);
                    }
                    for (int h : rel_idxs_h_dual) {
                        SimpleRelation head = relations[h];
                        checkThenAdd(
                                head, ent_dual_trans, matched_rules,
                                dualTransitionRuleString(head.name, relation_p.name, relation_q.name)
                        );
                    }
                }
            }
        }
        return matched_rules;
    }

    @Override
    public int templateLength() {
        return 3;
    }

    @Override
    public String templateName() {
        return "Transition";
    }

    protected String transitionRuleString(String h, String p, String q) {
        return String.format("%s(X,Y):-%s(X,Z),%s(Z,Y)", h, p, q);
    }

    protected String dualTransitionRuleString(String h, String p, String q) {
        return String.format("%s(X,Y):-%s(Y,Z),%s(Z,X)", h, p, q);
    }
}
