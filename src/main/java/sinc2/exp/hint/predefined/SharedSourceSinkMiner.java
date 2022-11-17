package sinc2.exp.hint.predefined;

import sinc2.common.Record;
import sinc2.exp.hint.HinterKb;
import sinc2.kb.IntTable;
import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template miner for:
 * 6. Shared Source/Sink
 *   h(X, Y) :- p(Z, X), q(Z, Y); [(h, p, q)]
 *   h(X, Y) :- p(X, Z), q(Y, Z); [(h, p, q)]
 */
public class SharedSourceSinkMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(HinterKb kb) {
        SimpleRelation[] relations = kb.getRelations();
        List<MatchedRule> matched_rules = new ArrayList<>();
        Set<Record> checked_pq_4_shared_source = new HashSet<>();
        Set<Record> checked_pq_4_shared_sink = new HashSet<>();
        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            if (2 != relation_p.totalCols()) {
                continue;
            }

            /* Match shared source */
            {
                /* Match another relation */
                Set<Integer> rel_idxs_q = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.similarCols(p, 0)) {
                    if (0 == col_info.colIdx && 2 == relations[col_info.relIdx].totalCols()) {
                        rel_idxs_q.add(col_info.relIdx);
                    }
                }
                Set<Integer> _rel_idxs_q = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 0)) {
                    if (0 == col_info.colIdx) {
                        _rel_idxs_q.add(col_info.relIdx);
                    }
                }
                rel_idxs_q.removeIf(e -> !_rel_idxs_q.contains(e));
                for (int q : rel_idxs_q) {
                    if (!checked_pq_4_shared_source.add(new Record(new int[]{p, q}))) {
                        /* Skip duplicated p-q & q-p pair */
                        continue;
                    }
                    checked_pq_4_shared_source.add(new Record(new int[]{q, p}));
                    SimpleRelation relation_q = relations[q];

                    /* Find matched arguments & construct entailments */
                    int[][] ent_shared_source = null;
                    Set<Integer> rel_idxs_h = new HashSet<>();
                    for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 1)) {
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
                        ent_shared_source = IntTable.join(relation_p, 0, 1, relation_q, 0, 1);
                        for (int h : rel_idxs_h) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_source, matched_rules,
                                    sharedSourceRuleString(head.name, relation_p.name, relation_q.name)
                            );
                        }
                    }

                    /* Find matched arguments & construct entailments (dual) */
                    Set<Integer> rel_idxs_h_dual = new HashSet<>();
                    for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 1)) {
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
                        int[][] ent_shared_source_dual = null;
                        if (null != ent_shared_source) {
                            ent_shared_source_dual = new int[ent_shared_source.length][];
                            for (int i = 0; i < ent_shared_source.length; i++) {
                                int[] record = ent_shared_source[i];
                                ent_shared_source_dual[i] = new int[]{record[1], record[0]};
                            }
                        } else {
                            ent_shared_source_dual = IntTable.join(relation_q, 0, 1, relation_p, 0, 1);
                        }
                        for (int h : rel_idxs_h_dual) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_source_dual, matched_rules,
                                    sharedSourceRuleString(head.name, relation_q.name, relation_p.name)
                            );
                        }
                    }
                }
            }

            /* Match shared sink */
            {
                /* Match another relation */
                Set<Integer> rel_idxs_q = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.similarCols(p, 1)) {
                    if (1 == col_info.colIdx && 2 == relations[col_info.relIdx].totalCols()) {
                        rel_idxs_q.add(col_info.relIdx);
                    }
                }
                Set<Integer> _rel_idxs_q = new HashSet<>();
                for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 1)) {
                    if (1 == col_info.colIdx) {
                        _rel_idxs_q.add(col_info.relIdx);
                    }
                }
                rel_idxs_q.removeIf(e -> !_rel_idxs_q.contains(e));
                for (int q : rel_idxs_q) {
                    if (!checked_pq_4_shared_sink.add(new Record(new int[]{p, q}))) {
                        /* Skip duplicated p-q & q-p pair */
                        continue;
                    }
                    checked_pq_4_shared_sink.add(new Record(new int[]{q, p}));
                    SimpleRelation relation_q = relations[q];

                    /* Find matched arguments & construct entailments */
                    int[][] ent_shared_sink = null;
                    Set<Integer> rel_idxs_h = new HashSet<>();
                    for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(p, 0)) {
                        if (0 == col_info.colIdx && 2 == relations[col_info.relIdx].totalCols() && (p != q || q != col_info.relIdx)) {
                            rel_idxs_h.add(col_info.relIdx);
                        }
                    }
                    Set<Integer> _rel_idxs_h = new HashSet<>();
                    for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(q, 0)) {
                        if (1 == col_info.colIdx) {
                            _rel_idxs_h.add(col_info.relIdx);
                        }
                    }
                    rel_idxs_h.removeIf(e -> !_rel_idxs_h.contains(e));
                    if (!rel_idxs_h.isEmpty()) {
                        ent_shared_sink = IntTable.join(relation_p, 1, 0, relation_q, 1, 0);
                        for (int h : rel_idxs_h) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_sink, matched_rules,
                                    sharedSinkRuleString(head.name, relation_p.name, relation_q.name)
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
                    for (HinterKb.ColInfo col_info : kb.inverseSimilarCols(q, 0)) {
                        if (0 == col_info.colIdx) {
                            _rel_idxs_h_dual.add(col_info.relIdx);
                        }
                    }
                    rel_idxs_h_dual.removeIf(e -> !_rel_idxs_h_dual.contains(e));
                    if (!rel_idxs_h_dual.isEmpty()) {
                        int[][] ent_shared_sink_dual = null;
                        if (null != ent_shared_sink) {
                            ent_shared_sink_dual = new int[ent_shared_sink.length][];
                            for (int i = 0; i < ent_shared_sink.length; i++) {
                                int[] record = ent_shared_sink[i];
                                ent_shared_sink_dual[i] = new int[]{record[1], record[0]};
                            }
                        } else {
                            ent_shared_sink_dual = IntTable.join(relation_q, 1, 0, relation_p, 1, 0);
                        }
                        for (int h : rel_idxs_h_dual) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_sink_dual, matched_rules,
                                    sharedSinkRuleString(head.name, relation_q.name, relation_p.name)
                            );
                        }
                    }
                }
            }
        }

        return matched_rules;
    }

    protected String sharedSourceRuleString(String h, String p, String q) {
        return String.format("%s(X,Y):-%s(Z,X),%s(Z,Y)", h, p, q);
    }

    protected String sharedSinkRuleString(String h, String p, String q) {
        return String.format("%s(X,Y):-%s(X,Z),%s(Y,Z)", h, p, q);
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
