package sinc2.exp.hint.predefined;

import sinc2.kb.Record;

import java.util.*;

/**
 * Template miner for:
 * 6. Shared Source/Sink
 *   h(X, Y) :- p(Z, X), q(Z, Y); [(h, p, q)]
 *   h(X, Y) :- p(X, Z), q(Y, Z); [(h, p, q)]
 */
public class SharedSourceSinkMiner extends TemplateMiner {
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
            Map<Integer, Set<Record>> indices_p0 = new HashMap<>();
            Map<Integer, Set<Record>> indices_p1 = new HashMap<>();
            for (Record record: relation_p) {
                indices_p0.computeIfAbsent(record.args[0], k -> new HashSet<>()).add(record);
                indices_p1.computeIfAbsent(record.args[1], k -> new HashSet<>()).add(record);
            }

            /* Match another relation */
            for (int q = p; q < relations.size(); q++) {
                Set<Record> relation_q = relations.get(q);
                if (relation_q.isEmpty() || 2 != relation_q.iterator().next().args.length) {
                    continue;
                }

                /* Find matched arguments & construct entailments */
                Set<Record> ent_shared_source = new HashSet<>();
                Set<Record> ent_shared_source_dual = new HashSet<>();
                Set<Record> ent_shared_sink = new HashSet<>();
                Set<Record> ent_shared_sink_dual = new HashSet<>();
                for (Record record: relation_q) {
                    Set<Record> matched_records0 = indices_p0.get(record.args[0]);
                    Set<Record> matched_records1 = indices_p1.get(record.args[1]);
                    if (null != matched_records0) {
                        for (Record matched_record: matched_records0) {
                            ent_shared_source.add(new Record(new int[]{matched_record.args[1], record.args[1]}));
                            ent_shared_source_dual.add(new Record(new int[]{record.args[1], matched_record.args[1]}));
                        }
                    }
                    if (null != matched_records1) {
                        for (Record matched_record: matched_records1) {
                            ent_shared_sink.add(new Record(new int[]{matched_record.args[0], record.args[0]}));
                            ent_shared_sink_dual.add(new Record(new int[]{record.args[0], matched_record.args[0]}));
                        }
                    }
                }

                /* Match head & check validness */
                for (int h = 0; h < relations.size(); h++) {
                    if (h == p && p == q) {
                        continue;
                    }
                    Set<Record> head = relations.get(h);
                    if (head.isEmpty() || 2 != head.iterator().next().args.length) {
                        continue;
                    }
                    Set<Record> entailed_head = positiveEntailments.get(h);

                    checkThenAdd(
                            head, entailed_head, ent_shared_source, matched_rules,
                            sharedSourceRuleString(h, p, q, functorNames)
                    );
                    checkThenAdd(
                            head, entailed_head, ent_shared_source_dual, matched_rules,
                            sharedSourceRuleString(h, q, p, functorNames)
                    );
                    checkThenAdd(
                            head, entailed_head, ent_shared_sink, matched_rules,
                            sharedSinkRuleString(h, p, q, functorNames)
                    );
                    checkThenAdd(
                            head, entailed_head, ent_shared_sink_dual, matched_rules,
                            sharedSinkRuleString(h, q, p, functorNames)
                    );
                }
            }
        }

        return matched_rules;
    }

    protected String sharedSourceRuleString(int h, int p, int q, List<String> functorNames) {
        return String.format("%s(X,Y):-%s(Z,X),%s(Z,Y)", functorNames.get(h), functorNames.get(p), functorNames.get(q));
    }

    protected String sharedSinkRuleString(int h, int p, int q, List<String> functorNames) {
        return String.format("%s(X,Y):-%s(X,Z),%s(Y,Z)", functorNames.get(h), functorNames.get(p), functorNames.get(q));
    }

    @Override
    public int templateLength() {
        return 3;
    }

    @Override
    public String templateName() {
        return "SharedSourceSink";
    }
}
