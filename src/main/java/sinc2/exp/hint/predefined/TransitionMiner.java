package sinc2.exp.hint.predefined;

import sinc2.kb.IntTable;
import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.List;

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
    public List<MatchedRule> matchTemplate(SimpleRelation[] relations) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            if (2 != relation_p.totalCols()) {
                continue;
            }

            /* Match another relation */
            for (int q = 0; q < relations.length; q++) {
                SimpleRelation relation_q = relations[q];
                if (2 != relation_q.totalCols()) {
                    continue;
                }

                /* Find matched arguments & construct entailments */
                int[][] ent_transition = IntTable.join(relation_p, 1, 0, relation_q, 0, 1);
                int[][] ent_dual_trans = new int[ent_transition.length][];
                for (int i = 0; i < ent_transition.length; i++) {
                    int[] record = ent_transition[i];
                    ent_dual_trans[i] = new int[] {record[1], record[0]};
                }

                /* Match head & check validness */
                for (int h = 0; h < relations.length; h++) {
                    if (p == q && q == h) {
                        continue;
                    }
                    SimpleRelation head = relations[h];
                    if (2 != head.totalCols()) {
                        continue;
                    }
                    checkThenAdd(
                            head, ent_transition, matched_rules,
                            transitionRuleString(head.name, relation_p.name, relation_q.name)
                    );
                    checkThenAdd(
                            head, ent_dual_trans, matched_rules,
                            dualTransitionRuleString(head.name, relation_p.name, relation_q.name)
                    );
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
