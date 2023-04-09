package sinc.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import sinc.SincConfig;
import sinc.common.*;
import sinc.util.RDF.RDFModelHandler;
import sinc.util.graph.BaseGraphNode;

import java.io.*;
import java.util.*;

public class SInCWithSQL {

    protected static final int CONST_ID = -1;
    protected static final BaseGraphNode<Predicate> AXIOM_NODE = new BaseGraphNode<>(new Predicate("⊥", 0));

    protected final SincConfig config;
    protected final String kbPath;
    protected final String dumpPath;
    protected final PrintWriter logger;

    protected Model model;
    protected int stms;
    protected int consts;

    protected final Map<String, List<String>[]> func_2_promising_const_map = getFunctor2PromisingConstantMap();

    protected final List<Rule> hypothesis = new ArrayList<>();
    protected final Map<Predicate, BaseGraphNode<Predicate>> predicate2NodeMap = new HashMap<>();
    protected final Map<BaseGraphNode<Predicate>, Set<BaseGraphNode<Predicate>>> dependencyGraph = new HashMap<>();
    protected final Set<Predicate> startSet = new HashSet<>();
    protected final Set<Predicate> counterExamples = new HashSet<>();
    protected final Set<String> supplementaryConstants = new HashSet<>();
    protected final PerformanceMonitor performanceMonitor = new PerformanceMonitor();

    /* 终止执行的flag */
    protected boolean interrupted = false;

    protected static class GraphAnalyseResult {
        public int startSetSize = 0;
        public int startSetSizeWithoutFvs = 0;
        public int sccNumber = 0;
        public int sccVertices = 0;
        public int fvsVertices = 0;
    }

    public SInCWithSQL(SincConfig config, String kbPath, String dumpPath, String logPath) {
        this.config = config;
        this.kbPath = kbPath;
        this.dumpPath = dumpPath;
        PrintWriter writer;
        try {
            writer = (null == logPath) ? new PrintWriter(System.out, true) : new PrintWriter(logPath);
        } catch (IOException e) {
            writer = new PrintWriter(System.out);
        }
        this.logger = writer;
        Rule.MIN_FACT_COVERAGE = config.minFactCoverage;
        Rule.monitor = new RuleMonitor();
    }

    /**
     * load rdf dataset
     * @return
     */
    protected Model loadKb(){
        Model model = ModelFactory.createDefaultModel();
        model.read(this.kbPath);
        this.model = model;
//        this.stms = RDFModelHandler.getStmCnts(model);
        return model;
    }

    protected List<String> getTargetFunctors(){
        return RDFModelHandler.getProperties(model);
    }

    protected Rule getStartRule(String headFunctor, int arity, Set<RuleFingerPrint> cache){
        return new RuleWithSQL(headFunctor, arity, cache, model, stms, consts, func_2_promising_const_map);
    }

    protected Rule findRule(String headFunctor) throws InterruptedSignal {
        final Set<RuleFingerPrint> cache = new HashSet<>();
        final Rule start_rule = getStartRule(headFunctor, 2, cache); //triple's arity is 2

        /* 初始化beams */
        final Eval.EvalMetric eval_metric = config.evalMetric;
        final int beam_width = config.beamWidth;
        Set<Rule> beams = new HashSet<>();
        beams.add(start_rule);
        PriorityQueue<Rule> optimals = new PriorityQueue<>(
                Comparator.comparingDouble((Rule r) -> r.getEval().value(eval_metric)).reversed()
        );

        /* 寻找局部最优（只要进入这个循环，一定有局部最优） */
        while (true) {
            /* 根据当前beam遍历下一轮的所有candidates */
            PriorityQueue<Rule> candidates = new PriorityQueue<>(
                    Comparator.comparingDouble((Rule r) -> r.getEval().value(eval_metric)).reversed()
            );
            for (Rule r: beams) {
                logger.printf("Extend: %s\n", r);
                logger.flush();

                /* 遍历r的邻居 */
                int existing_candidates = candidates.size();
                findExtension(r, candidates);
                int extensions_cnt = candidates.size() - existing_candidates;
                int origins_cnt = 0;
                if (config.searchOrigins) {
                    findOrigin(r, candidates);
                    origins_cnt = candidates.size() - existing_candidates - extensions_cnt;
                }

                if (0 == (extensions_cnt + origins_cnt)) {
                    optimals.add(r);
                }

                /* 监测：分支数量信息 */
                final PerformanceMonitor.BranchInfo branch_info = new PerformanceMonitor.BranchInfo(
                        r.size(), extensions_cnt, origins_cnt
                );
                performanceMonitor.branchProgress.add(branch_info);
            }

            /* 如果有多个optimal，选择最优的返回 */
            final Rule loc_opt = optimals.peek();
            if (null != loc_opt) {
                final Rule peek_rule = candidates.peek();
                if (
                        null == peek_rule ||
                                /* 如果local optimal在当前的candidates里面不是最优的，则排除 */
                                loc_opt.getEval().value(eval_metric) > peek_rule.getEval().value(eval_metric)
                ) {
                    return loc_opt;
                }
            }

            /* 找出下一轮的beams */
            Set<Rule> new_beams = new HashSet<>();
            Rule beam_rule;
            while (new_beams.size() < beam_width && (null != (beam_rule = candidates.poll()))) {
                new_beams.add(beam_rule);
            }
            beams = new_beams;
        }
    }

    /**
     * 遍历extensions，把得分更高的放入candidates列表
     */
    protected void findExtension(final Rule rule, PriorityQueue<Rule> candidates) throws InterruptedSignal {
        Eval eval = rule.getEval();
        if (config.stopCompressionRate <= eval.value(Eval.EvalMetric.CompressionRate) || 0 == eval.getNegCnt()) {
            /* 如果到达停止阈值，不再进行extension */
            return;
        }

        /* 先找到所有空白的参数 */
        class ArgPos {
            public final int predIdx;
            public final int argIdx;

            public ArgPos(int predIdx, int argIdx) {
                this.predIdx = predIdx;
                this.argIdx = argIdx;
            }
        }
        List<ArgPos> vacant_list = new ArrayList<>();    // 空白参数记录表：{pred_idx, arg_idx}
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.length(); pred_idx++) {
            final Predicate pred_info = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < pred_info.arity(); arg_idx++) {
                if (null == pred_info.args[arg_idx]) {
                    vacant_list.add(new ArgPos(pred_idx, arg_idx));
                }
            }
        }

        /* 尝试增加已知变量 */
        final Map<String, Integer> func_2_arity_map = getFunctor2ArityMap();
        for (int var_id = 0; var_id < rule.usedBoundedVars(); var_id++) {
            List<VarIndicator> var_locations = rule.getVarLocations(var_id);
            for (ArgPos vacant: vacant_list) {
                for (VarIndicator var_location: var_locations) {
//                    if (columnSimilar(rule.getPredicate(vacant.predIdx).functor, vacant.argIdx, var_location.functor, var_location.idx)) {
                        /* 尝试将已知变量填入空白参数 */
                        final Rule new_rule = rule.clone();
                        final Rule.UpdateStatus update_status = new_rule.boundFreeVar2ExistingVar(
                                vacant.predIdx, vacant.argIdx, var_id
                        );
                        checkThenAddRule(update_status, new_rule, rule, candidates);
                        break;
//                    }
                }
            }

            for (Map.Entry<String, Integer> entry: func_2_arity_map.entrySet()) {
                /* 拓展一个谓词，并尝试一个已知变量 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    for (VarIndicator var_location: var_locations) {
//                        if (columnSimilar(functor, arg_idx, var_location.functor, var_location.idx)) {
                            final Rule new_rule = rule.clone();
                            final Rule.UpdateStatus update_status = new_rule.boundFreeVar2ExistingVar(
                                    functor, arity, arg_idx, var_id
                            );
                            checkThenAddRule(update_status, new_rule, rule, candidates);
//                        }
                    }
                }
            }
        }

        for (int i = 0; i < vacant_list.size(); i++) {
            /* 找到新变量的第一个位置 */
            final ArgPos first_vacant = vacant_list.get(i);
            final String functor1 = rule.getPredicate(first_vacant.predIdx).functor;

            /* 拓展一个常量 */
            final Predicate predicate = rule.getPredicate(first_vacant.predIdx);
            if(func_2_promising_const_map.get(predicate.functor) != null) {
                final List<String> const_list = func_2_promising_const_map.get(predicate.functor)[first_vacant.argIdx];
                for (String const_symbol : const_list) {
                    final Rule new_rule = rule.clone();
                    final Rule.UpdateStatus update_status = new_rule.boundFreeVar2Constant(
                            first_vacant.predIdx, first_vacant.argIdx, const_symbol
                    );
                    checkThenAddRule(update_status, new_rule, rule, candidates);
                }
            }

            /* 找到两个位置尝试同一个新变量 */
            for (int j = i + 1; j < vacant_list.size(); j++) {
                /* 新变量的第二个位置可以是当前rule中的其他空位 */
                final ArgPos second_vacant = vacant_list.get(j);
//                if (columnSimilar(functor1, first_vacant.argIdx,rule.getPredicate(second_vacant.predIdx).functor, second_vacant.argIdx)) {
                    final Rule new_rule = rule.clone();
                    final Rule.UpdateStatus update_status = new_rule.boundFreeVars2NewVar(
                            first_vacant.predIdx, first_vacant.argIdx, second_vacant.predIdx, second_vacant.argIdx
                    );
                    checkThenAddRule(update_status, new_rule, rule, candidates);
//                }
            }
            for (Map.Entry<String, Integer> entry: func_2_arity_map.entrySet()) {
                /* 新变量的第二个位置也可以是拓展一个谓词以后的位置 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
//                    if (columnSimilar(functor1, first_vacant.argIdx, functor, arg_idx)) {
                        final Rule new_rule = rule.clone();
                        final Rule.UpdateStatus update_status = new_rule.boundFreeVars2NewVar(
                                functor, arity, arg_idx, first_vacant.predIdx, first_vacant.argIdx
                        );
                        checkThenAddRule(update_status, new_rule, rule, candidates);
//                    }
                }
            }
        }
    }

    protected Map<String, Integer> getFunctor2ArityMap(){
        return RDFModelHandler.getPropertyToArity(model);
    }
//
    protected Map<String, List<String>[]> getFunctor2PromisingConstantMap(){
        Map<String, List<String>[]> map = new HashMap<>();
        List<String> list = new LinkedList<>();
        list.add("fa:male"); list.add("fa:female");
        List<String>[] constants = new List[2];
        constants[0] = new LinkedList<>();
        constants[1] = list;
        map.put("fa:gender", constants);
        return map;
    }

//    @Override
//    protected boolean columnSimilar(String functor1, int idx1, String functor2, int idx2) {
//
//    }


    protected void findOrigin(Rule rule, PriorityQueue<Rule> candidates) throws InterruptedSignal {
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.length(); pred_idx++) {
            /* 从Head开始删除可能会出现Head中没有Bounded Var但是Body不为空的情况，按照定义来说，这种规则是不在
               搜索空间中的，但是会被isInvalid方法检查出来 */
            final Predicate predicate = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (null != predicate.args[arg_idx]) {
                    final Rule new_rule = rule.clone();
                    final Rule.UpdateStatus update_status = new_rule.removeBoundedArg(pred_idx, arg_idx);
                    checkThenAddRule(update_status, new_rule, rule, candidates);
                }
            }
        }
    }

    protected void showMonitor() {
        performanceMonitor.show(logger);
        Rule.monitor.show(logger);
        logger.flush();
    }

    public List<Rule> getHypothesis() {
        return hypothesis;
    }

    public Set<Predicate> getStartSet() {
        return startSet;
    }

    public Set<Predicate> getCounterExamples() {
        return counterExamples;
    }

    public Set<String> getSupplementaryConstants() {
        return supplementaryConstants;
    }

//    public abstract Set<String> getAllConstants();

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    protected void checkThenAddRule(Rule.UpdateStatus updateStatus, Rule extendedRule, Rule originalRule, Queue<Rule> candidates)
            throws InterruptedSignal {
        switch (updateStatus) {
            case NORMAL:
                if (extendedRule.getEval().value(config.evalMetric) >= originalRule.getEval().value(config.evalMetric)
                    && extendedRule.length() < 4) { //TODO limit search space
                    candidates.add(extendedRule);
                }
                break;
            case INVALID:
                performanceMonitor.invalidSearches++;
                break;
            case DUPLICATED:
                performanceMonitor.duplications++;
                break;
            case INSUFFICIENT_COVERAGE:
                performanceMonitor.fcFilteredRules++;
                break;
            case TABU_PRUNED:
                performanceMonitor.tabuPruned++;
                break;
            default:
                throw new Error("Unknown Update Status of Rule: " + updateStatus.name());
        }
        if (interrupted) {
            throw new InterruptedSignal("Interrupted");
        }
    }

    protected void targetDone(String functor) {
        /* 这里什么也不做，给后续处理留空间 */
    }

//    public abstract String getModelName();
//
//    protected abstract boolean columnSimilar(String functor1, int idx1, String functor2, int idx2);

    private void runHandler() {
        final long time_start = System.currentTimeMillis();
        try {
            /* 加载KB */
            model = RDFModelHandler.getModel(kbPath);
            stms = RDFModelHandler.getStmCnts(model);
            consts = RDFModelHandler.getConsts(model);
            final long time_kb_loaded = System.currentTimeMillis();
            performanceMonitor.kbLoadTime = time_kb_loaded - time_start;

            /* 逐个functor找rule */
            final List<String> target_head_functors = getTargetFunctors();
//            final List<String> target_head_functors = new LinkedList<>();
            final int total_targets = target_head_functors.size();

            do {
                final long time_rule_finding_start = System.currentTimeMillis();
                final int last_idx = target_head_functors.size() - 1;
                final String functor = target_head_functors.get(last_idx);
                final Rule rule = findRule(functor);
                final long time_rule_found = System.currentTimeMillis();
                performanceMonitor.hypothesisMiningTime += time_rule_found - time_rule_finding_start;

                if (null != rule && rule.getEval().useful(config.evalMetric)) {
                    logger.printf("Found: %s\n", rule);
                    hypothesis.add(rule);
                    performanceMonitor.hypothesisSize += rule.size();

                    /* 更新grpah和counter example */
//                    UpdateResult update_result = updateKb(rule);
//                    counterExamples.addAll(update_result.counterExamples);
//                    updateGraph(update_result.groundings);
                    final long time_kb_updated = System.currentTimeMillis();
                    performanceMonitor.dependencyAnalysisTime += time_kb_updated - time_rule_found;
                    target_head_functors.remove(last_idx);
                } else {
                    target_head_functors.remove(last_idx);
                    logger.printf("Target Done: %d/%d\n", total_targets - target_head_functors.size(), total_targets);
                    targetDone(functor);
                }
            } while (!target_head_functors.isEmpty());
            performanceMonitor.hypothesisRuleNumber = hypothesis.size();
            performanceMonitor.counterExampleSize = counterExamples.size();

            /* 打印所有rules */
            logger.println("\n### Hypothesis Found ###");
            for (Rule rule : hypothesis) {
                logger.println(rule);
            }
            logger.println();
            /* 打印当前已经得到的rules */
            logger.println("\n### Hypothesis Found (Before Error) ###");
            for (Rule rule : hypothesis) {
                logger.println(rule);
            }
            logger.println();
            showMonitor();
            logger.println("!!! The Result is Reserved Before EXCEPTION !!!");
        } catch (InterruptedSignal e){
            e.printStackTrace();
            /* 打印所有rules */
            logger.println("\n### Hypothesis Found ###");
            for (Rule rule : hypothesis) {
                logger.println(rule);
            }
            logger.println();
            /* 打印当前已经得到的rules */
            logger.println("\n### Hypothesis Found (Before Error) ###");
            for (Rule rule : hypothesis) {
                logger.println(rule);
            }
            logger.println();
            showMonitor();
            logger.println("!!! The Result is Reserved Before EXCEPTION !!!");
        }
    }

    public final void run() {
        Thread task = new Thread(this::runHandler);
        task.start();

        try {
            while (task.isAlive() && (System.in.available() <= 0)) {
                Thread.sleep(1000);
            }
            logger.println("Exit normally");
            logger.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            interrupted = true;
            try {
                task.join();
                logger.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
