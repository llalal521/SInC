package sinc.impl;

import com.github.andrewoma.dexx.collection.Pair;
import org.apache.jena.base.Sys;
import org.apache.jena.ext.com.google.common.collect.HashMultimap;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import sinc.common.*;
import sinc.util.RDF.RDFModelHandler;
import sinc.util.RDF.RDFQuery;


import java.util.*;

/**
 * SQL version of Rule implement
 * provide a function to translate a rule into SPARQL query
 */
public class RuleWithGraph extends Rule {
    public sinc.common.Eval returningEval;
    /**
     * pointer to rdf model
     */
    private final Graph graph;

    private final int consts;
    private final Map<String, List<String>[]> func_2_promising_const_map;

    private int evidence;

    public RuleWithGraph(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints, Graph graph,
                       Map<String, List<String>[]> func_2_promising_const_map) {
        super(headFunctor, arity, searchedFingerprints);
        this.func_2_promising_const_map = func_2_promising_const_map;
        this.graph = graph;
        this.consts = graph.nodes.size();
        this.eval = calculateEval();
    }

    public RuleWithGraph(Rule another, Graph graph, int consts, Map<String, List<String>[]> func_2_promising_const_map) {
        super(another);
        this.graph = graph;
        this.consts = graph.nodes.size();
        this.func_2_promising_const_map = func_2_promising_const_map;
//        this.eval = calculateEval();
    }

    @Override
    public Rule clone() {
        return new RuleWithGraph(this, graph, consts, func_2_promising_const_map);
    }

    @Override
    public final UpdateStatus removeBoundedArg(int predIdx, int argIdx) {
        /* Cached Rule 不支持向前做cache */
        return UpdateStatus.INVALID;
    }

    @Override
    protected final UpdateStatus removeBoundedArgHandler(int predIdx, int argIdx) {
        /* 这里也是什么都不做 */
        return UpdateStatus.INVALID;
    }

    @Override
    protected double factCoverage() {
        return ((double) evidence) / graph.subjects.get(graph.ids.get(getHead().functor)).size();
    }

    @Override
    protected Eval calculateEval() {
//        System.out.println(this.toDumpString());
        RDFQuery.evals ++;
        if(structure.get(0).args[0] != null && structure.get(0).args[1] != null
                && structure.get(0).args[0].id == structure.get(0).args[1].id)
            return new Eval(eval, 0, 1, 1, getLength()); //不合要求的rule(head 上是两个bounded var就没有压缩的意义了)
        // 简单起见，只计算Minimization Ratio(evidance / in + not in + counter Example)
        return new Eval(eval, getEvidenceCnt(), getCounterExampleCnt() + evidence,
                graph.stms.get(graph.ids.get(getHead().functor)), getLength());
    }

    protected int getLength(){
        int max_idx = boundedVars.size() - 1;
        ArrayList<Integer> times = new ArrayList<>();
        for(int i = 0; i <= max_idx; ++i)   times.add(0);
        for(Predicate p : structure){
            for(Argument a : p.args){
                if(a != null && a.isVar){
                    times.set(a.id, times.get(a.id) + 1);
                }
            }
        }
        int res = 0;
        for(Integer i : times){
            if(i == 0)  continue;
            res += i - 1;
        }
        return res;
    }

    protected int getCounterExampleCnt(){
        if(structure.size() == 1)   return consts * consts - evidence;
        Map<Integer, Pair<Integer, Integer>> vars = new HashMap<>();
        List<String> arg_struct = new LinkedList<>();
        int free_num = boundedVars.size();
        List<Integer> args = new ArrayList<>();
        args.add(0);
        args.add(0);

        for(int i = 1; i < structure.size(); ++i){
            arg_struct.add(structure.get(i).functor);
            for(int j = 0; j < 2; ++j){
                Argument arg = structure.get(i).args[j];
                if(arg == null){
                    args.set(j, free_num);
                    free_num ++;
                } else {
                    args.set(j, arg.id);
                }
            }
            vars.put(i - 1, new Pair<>(args.get(0), args.get(1)));
        }

        Predicate head = getHead();
        int type = -1; // 0: A(X, ?), 1: B(?, X), 2: C(X,Y)
        Argument arg1 = head.args[0];
        Argument arg2 = head.args[1];
        if(arg1 == null && arg2 != null){
            int arg_id = arg2.id;
            for(int i = 1; i < structure.size(); ++i){
                Predicate body = structure.get(i);
                if(body.args[0] != null && body.args[0].id == arg_id){
                    return consts * graph.Rule2Num(vars, arg_struct, i - 1, true).size() - evidence;
                }
                if(body.args[1] != null && body.args[1].id == arg_id){
                    return consts * graph.Rule2Num(vars, arg_struct, i - 1, false).size() - evidence;
                }
            }
        }
        if(arg2 == null && arg1 != null) {
            int arg_id = arg1.id;
            for(int i = 1; i < structure.size(); ++i){
                Predicate body = structure.get(i);
                if(body.args[0] != null && body.args[0].id == arg_id){
                    return consts * graph.Rule2Num(vars, arg_struct, i - 1, true).size() - evidence;
                }
                if(body.args[1] != null && body.args[1].id == arg_id){
                    return consts * graph.Rule2Num(vars, arg_struct, i - 1, false).size() - evidence;
                }
            }
        }
        int sub1_pos = -1, obj1_pos = -1;
        boolean is_sub1 = false, is_sub2 = false;
        for(int i = 1; i < structure.size(); ++i){
            if(sub1_pos != -1 && obj1_pos != -1) break;
            Predicate body = structure.get(i);
            if(body.args[0] != null){
                if(body.args[0].id == arg1.id){
                    is_sub1 = true;
                    sub1_pos = i - 1;
                }
                if(body.args[0].id == arg2.id){
                    is_sub2 = true;
                    obj1_pos = i - 1;
                }
            }
            if(body.args[1] != null){
                if(body.args[1].id == arg1.id){
                    is_sub1 = false;
                    sub1_pos = i - 1;
                }
                if(body.args[1].id == arg2.id){
                    is_sub2 = false;
                    obj1_pos = i - 1;
                }
            }
        }
        return graph.Rule2Num(vars, arg_struct, sub1_pos, is_sub1, obj1_pos, is_sub2) - evidence;
    }

    public int getEvidenceCnt(){
        Map<Integer, Pair<Integer, Integer>> vars = new HashMap<>();
        int free_num = boundedVars.size();

        List<String> arg_struct = new LinkedList<>();
        List<Integer> args = new ArrayList<>();
        args.add(0);
        args.add(0);

        for(int i = 0; i < structure.size(); ++i){
            arg_struct.add(structure.get(i).functor);
            for(int j = 0; j < 2; ++j){
                Argument arg = structure.get(i).args[j];
                if(arg == null){
                    args.set(j, free_num);
                    free_num ++;
                } else {
                    args.set(j, arg.id);
                }
            }
            vars.put(i, new Pair<>(args.get(0), args.get(1)));
        }

//        System.out.println(toDumpString());
//        System.out.println(vars);

        evidence = graph.Rule2Num(vars, arg_struct);
        return evidence;
    }
}
