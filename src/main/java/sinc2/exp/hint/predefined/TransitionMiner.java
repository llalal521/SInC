package sinc2.exp.hint.predefined;

import sinc2.kb.Record;

import java.util.*;

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
    public List<MatchedRule> matchTemplate(
            List<Set<Record>> relations, List<Set<Record>> positiveEntailments, List<String> functorNames
    ) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.size(); p++) {
            Set<Record> relation_p = relations.get(p);
            if (relation_p.isEmpty() || 2 != relation_p.iterator().next().args.length) {
                continue;
            }

            /* Build temporary indices */
            Map<Integer, Set<Record>> indices_p1 = new HashMap<>();
            for (Record record: relation_p) {
                indices_p1.computeIfAbsent(record.args[1], k -> new HashSet<>()).add(record);
            }

            /* Match another relation */
            for (int q = 0; q < relations.size(); q++) {
                Set<Record> relation_q = relations.get(q);
                if (relation_q.isEmpty() || 2 != relation_q.iterator().next().args.length) {
                    continue;
                }

                /* Find matched arguments & construct entailments */
                Set<Record> ent_transition = new HashSet<>();
                Set<Record> ent_dual_trans = new HashSet<>();
                for (Record record: relation_q) {
                    Set<Record> matched_records = indices_p1.get(record.args[0]);
                    if (null != matched_records) {
                        for (Record matched_record : matched_records) {
                            ent_transition.add(new Record(new int[]{matched_record.args[0], record.args[1]}));
                            ent_dual_trans.add(new Record(new int[]{record.args[1], matched_record.args[0]}));
                        }
                    }
                }

                /* Match head & check validness */
                for (int h = 0; h < relations.size(); h++) {
                    if (p == q && q == h) {
                        continue;
                    }
                    Set<Record> head = relations.get(h);
                    if (head.isEmpty() || 2 != head.iterator().next().args.length) {
                        continue;
                    }
                    Set<Record> entailed_head = positiveEntailments.get(h);
                    checkThenAdd(
                            head, entailed_head, ent_transition, matched_rules,
                            transitionRuleString(h, p, q, functorNames)
                    );
                    checkThenAdd(
                            head, entailed_head, ent_dual_trans, matched_rules,
                            dualTransitionRuleString(h, p, q, functorNames)
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

    protected String transitionRuleString(int h, int p, int q, List<String> functorNames) {
        return String.format("%s(X,Y):-%s(X,Z),%s(Z,Y)", functorNames.get(h), functorNames.get(p), functorNames.get(q));
    }

    protected String dualTransitionRuleString(int h, int p, int q, List<String> functorNames) {
        return String.format("%s(X,Y):-%s(Y,Z),%s(Z,X)", functorNames.get(h), functorNames.get(p), functorNames.get(q));
    }
}
