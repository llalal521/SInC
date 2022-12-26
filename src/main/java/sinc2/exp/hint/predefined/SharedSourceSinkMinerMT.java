package sinc2.exp.hint.predefined;

import sinc2.exp.hint.HinterKb;
import sinc2.kb.IntTable;
import sinc2.kb.SimpleRelation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SharedSourceSinkMinerMT extends TemplateMinerMT {
    static final double TASK_GENERATOR_FACTOR = 0.2;

    public SharedSourceSinkMinerMT() {
    }

    public SharedSourceSinkMinerMT(int threads) {
        super(threads);
    }

    @Override
    protected void matchTemplateHandler(HinterKb kb, List<MatchedRule> matchedRules) {
        SimpleRelation[] relations = kb.getRelations();
        int task_generators = Math.max(1, (int) (threadPool.getCorePoolSize() * TASK_GENERATOR_FACTOR));
        int step_size = relations.length / task_generators + ((0 == relations.length % task_generators) ? 0 : 1);
        ThreadPoolExecutor task_generator_threads = (ThreadPoolExecutor) Executors.newFixedThreadPool(task_generators);
        for (int t = 0; t < task_generators; t++) {
            int start = t * step_size;
            task_generator_threads.submit(() -> matchWithRangeOfP(
                    kb, matchedRules, start, Math.min(relations.length, start + step_size)
            ));
        }
        task_generator_threads.shutdown();
        try {
            if (!task_generator_threads.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.printf("TemplateMiner '%s' time out.\n", templateName());
            }
        } catch (InterruptedException e) {
            System.err.println("TemplateMinerMT thread pool interrupted");
            e.printStackTrace();
        }
    }

    protected void matchWithRangeOfP(HinterKb kb, List<MatchedRule> matchedRules, int start, int end) {
        SimpleRelation[] relations = kb.getRelations();
        for (int p = start; p < end; p++) {
            SimpleRelation relation_p = relations[p];
            if (2 != relation_p.totalCols()) {
                continue;
            }

            for (int q = p; q < relations.length; q++) {
                SimpleRelation relation_q = relations[q];
                if (2 != relation_q.totalCols()) {
                    continue;
                }

                /* Match shared source */
                {
                    /* Find matched arguments & construct entailments */
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
                    int[][] ent_shared_source = null;
                    if (!rel_idxs_h.isEmpty()) {
                        ent_shared_source = IntTable.join(relation_p, 0, 1, relation_q, 0, 1);
                        Arrays.sort(ent_shared_source, ENTAILED_RECORDS_COMPARATOR);
                        for (int h : rel_idxs_h) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_source, matchedRules,
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
                        Arrays.sort(ent_shared_source_dual, ENTAILED_RECORDS_COMPARATOR);
                        for (int h : rel_idxs_h_dual) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_source_dual, matchedRules,
                                    sharedSourceRuleString(head.name, relation_q.name, relation_p.name)
                            );
                        }
                    }
                }

                /* Match shared sink */
                {
                    /* Find matched arguments & construct entailments */
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
                    int[][] ent_shared_sink = null;
                    if (!rel_idxs_h.isEmpty()) {
                        ent_shared_sink = IntTable.join(relation_p, 1, 0, relation_q, 1, 0);
                        Arrays.sort(ent_shared_sink, ENTAILED_RECORDS_COMPARATOR);
                        for (int h : rel_idxs_h) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_sink, matchedRules,
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
                        Arrays.sort(ent_shared_sink_dual, ENTAILED_RECORDS_COMPARATOR);
                        for (int h : rel_idxs_h_dual) {
                            SimpleRelation head = relations[h];
                            checkThenAdd(
                                    head, ent_shared_sink_dual, matchedRules,
                                    sharedSinkRuleString(head.name, relation_q.name, relation_p.name)
                            );
                        }
                    }
                }
            }
        }
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
