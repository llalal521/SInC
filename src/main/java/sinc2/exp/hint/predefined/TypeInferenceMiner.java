package sinc2.exp.hint.predefined;

import sinc2.exp.hint.HinterKb;
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
    public List<MatchedRule> matchTemplate(HinterKb kb) {
        SimpleRelation[] relations = kb.getRelations();
        List<MatchedRule> matched_rules = new ArrayList<>();

        /* Check if there are type relations */
        boolean no_type_relation = true;
        for (SimpleRelation relation : relations) {
            if (1 == relation.totalCols()) {
                no_type_relation = false;
                break;
            }
        }
        if (no_type_relation) {
            return matched_rules;
        }

        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            final int arity = relation_p.totalCols();
            if (1 >= arity) {
                /* If the arity of p is also 1, the patterns are collided with subsumption */
                continue;
            }

            /* Match for every argument */
            for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                /* Find entailments */
                int[][] entailments = null;
                List<SimpleRelation> heads = new ArrayList<>();
                for (HinterKb.ColInfo similar_col: kb.inverseSimilarCols(p, arg_idx)) {
                    SimpleRelation head = relations[similar_col.relIdx];
                    if (1 == head.totalCols()) {
                        heads.add(head);
                    }
                }
                if (!heads.isEmpty()) {
                    int[] values = relation_p.valuesInColumn(arg_idx);
                    entailments = new int[values.length][];
                    for (int i = 0; i < values.length; i++) {
                        entailments[i] = new int[]{values[i]};
                    }
                    /* Match types */
                    for (SimpleRelation head: heads) {
                        checkThenAdd(
                                head, entailments, matched_rules,
                                typeInferenceRuleString(head.name, relation_p.name, arg_idx, arity)
                        );
                    }
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
