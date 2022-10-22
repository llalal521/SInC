package sinc2.exp.hint.predefined;

import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * Template miner for:
 * 1. Type Inference:
 *   h(X) :- p(..., Xi, ...), φ(p) > 1
 *
 * @since 2.0
 */
public class TypeInferenceMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(SimpleRelation[] relations) {
        List<MatchedRule> matched_rules = new ArrayList<>();

        /* Check if there are type relations */
        List<SimpleRelation> type_relations = new ArrayList<>();
        for (SimpleRelation relation : relations) {
            if (1 == relation.totalCols()) {
                type_relations.add(relation);
            }
        }
        if (type_relations.isEmpty()) {
            return matched_rules;
        }

        for (SimpleRelation relation_p : relations) {
            final int arity = relation_p.totalCols();
            if (1 >= arity) {
                /* If the arity of p is also 1, the patterns are collided with subsumption */
                continue;
            }

            /* Match for every argument */
            for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                /* Find entailments */
                int[] values = relation_p.valuesInColumn(arg_idx);
                int[][] entailments = new int[values.length][];
                for (int i = 0; i < values.length; i++) {
                    entailments[i] = new int[]{values[i]};
                }

                /* Match types */
                for (SimpleRelation head : type_relations) {
                    checkThenAdd(
                            head, entailments, matched_rules,
                            typeInferenceRuleString(head.name, relation_p.name, arg_idx, arity)
                    );
                }
            }
        }
        return matched_rules;
    }

    protected String typeInferenceRuleString(String h, String p, int bodyArgIdx, int bodyArity) {
        return h + "(X):-" + p + '(' + "?,".repeat(bodyArgIdx) + 'X' +
                ",?".repeat(bodyArity - bodyArgIdx - 1) + ')';
    }

    @Override
    public int templateLength() {
        return 1;
    }

    @Override
    public String templateName() {
        return "TypeInference";
    }
}
