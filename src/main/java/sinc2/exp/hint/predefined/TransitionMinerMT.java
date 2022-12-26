package sinc2.exp.hint.predefined;

import sinc2.exp.hint.HinterKb;
import sinc2.kb.IntTable;
import sinc2.kb.SimpleRelation;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TransitionMinerMT extends TemplateMinerMT {
    static final double TASK_GENERATOR_FACTOR = SharedSourceSinkMinerMT.TASK_GENERATOR_FACTOR;

    public TransitionMinerMT() {
    }

    public TransitionMinerMT(int threads) {
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

            /* Match another relation */
            for (int q = 0; q < relations.length; q++) {
                SimpleRelation relation_q = relations[q];
                if (2 != relation_q.totalCols()) {
                    continue;
                }

                /* Find matched arguments & construct entailments */
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
                int[][] ent_transition = null;
                if (!rel_idxs_h.isEmpty()) {
                    ent_transition = IntTable.join(relation_p, 1, 0, relation_q, 0, 1);
                    Arrays.sort(ent_transition, ENTAILED_RECORDS_COMPARATOR);
                    for (int h : rel_idxs_h) {
                        SimpleRelation head = relations[h];
                        checkThenAdd(
                                head, ent_transition, matchedRules,
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
                    Arrays.sort(ent_dual_trans, ENTAILED_RECORDS_COMPARATOR);
                    for (int h : rel_idxs_h_dual) {
                        SimpleRelation head = relations[h];
                        checkThenAdd(
                                head, ent_dual_trans, matchedRules,
                                dualTransitionRuleString(head.name, relation_p.name, relation_q.name)
                        );
                    }
                }
            }
        }
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
