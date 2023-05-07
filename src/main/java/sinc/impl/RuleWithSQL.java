package sinc.impl;

import com.github.andrewoma.dexx.collection.Pair;
import org.apache.jena.ext.com.google.common.collect.HashMultimap;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.rdf.model.Model;
import sinc.common.*;
import sinc.util.RDF.RDFModelHandler;
import sinc.util.RDF.RDFQuery;


import java.util.*;

/**
 * SQL version of Rule implement
 * provide a function to translate a rule into SPARQL query
 */
public class RuleWithSQL extends Rule {
    public sinc.common.Eval returningEval;
    /**
     * pointer to rdf model
     */
    private final Model model;

    private final int stms;
    private final int consts;
    private final Map<String, List<String>[]> func_2_promising_const_map;

    private int evidence;

    public RuleWithSQL(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints, Model model, int stms,
                       int consts, Map<String, List<String>[]> func_2_promising_const_map) {
        super(headFunctor, arity, searchedFingerprints);
        this.func_2_promising_const_map = func_2_promising_const_map;
        this.model = model;
        this.stms = stms;
        this.consts = consts;
        this.eval = calculateEval();
    }

    public RuleWithSQL(Rule another, Model model, int stms, int consts, Map<String, List<String>[]> func_2_promising_const_map) {
        super(another);
        this.model = model;
        this.stms = stms;
        this.consts = consts;
        this.func_2_promising_const_map = func_2_promising_const_map;
//        this.eval = calculateEval();
    }

    @Override
    public Rule clone() {
        return new RuleWithSQL(this, model, stms, consts, func_2_promising_const_map);
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
        return ((double) evidence) / stms;
    }

    @Override
    protected Eval calculateEval() {
        RDFQuery.evals ++;
        if(structure.get(0).args[0] != null && structure.get(0).args[1] != null
                && structure.get(0).args[0].id == structure.get(0).args[1].id)
            return new Eval(eval, 0, 1, 1, getLength()); //不合要求的rule(head 上是两个bounded var就没有压缩的意义了)
        // 简单起见，只计算Minimization Ratio(evidance / in + not in + counter Example)
        return new Eval(eval, getEvidenceCnt(), getCounterExampleCnt() + evidence, stms, getLength());
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
        if(RDFQuery.map.containsKey("counter")){
            RDFQuery.map.put("counter", RDFQuery.map.get("counter") + 1);
        } else {
            RDFQuery.map.put("counter", 1);
        }
        int left_idx = 1; // free var holder in SPARQL
        int max_idx = boundedVars.size() - 1;
        if(structure.size() == 1)   return consts * consts - evidence;
        List<String> vars = new LinkedList<>();
        Multimap<String, List<Pair<Integer, String>>> map = HashMultimap.create();
        int cur_id = 0;
        int mul = 1;
        Predicate head = getHead();
        Map<Integer, Integer> head_bound_pos = new HashMap<>();
        for(int idx = 0; idx < 2; ++idx){
            Argument arg = head.args[idx];
            if(arg != null && arg.isVar){
                head_bound_pos.put(arg.id, cur_id);
                cur_id ++;
            }
            else {
                // TODO Add Literal Handler
                if(arg == null){
                    if(func_2_promising_const_map.get(head.functor) != null
                            && func_2_promising_const_map.get(head.functor)[idx].size() != 0){
                        mul *= func_2_promising_const_map.get(head.functor)[idx].size();
                    } else {
                        mul *= consts;
                    }
                }
            }
        }
        // Construct SELECT
        if(head_bound_pos.size() == 2){
            vars.add("a");
            vars.add("b");
        } else {
            vars.add("a");
        }

        // Construct WHERE
        for(int i = 1; i < structure.size(); ++i){
            Predicate body = structure.get(i);
            List<Pair<Integer, String>> p_vars = new LinkedList<>();
            for(int idx = 0; idx < 2; ++idx){
                Argument arg = body.args[idx];
                if(arg != null && arg.isVar){
                    if(head_bound_pos.containsKey(arg.id)){
                        int pos = head_bound_pos.get(arg.id);
                        char var = (char) ('a' + pos);
                        p_vars.add(new Pair<>(0, String.valueOf(var)));
                    } else {
                        char var = (char) ('c' + arg.id);
                        p_vars.add(new Pair<>(0, String.valueOf(var)));
                    }
                } else {
                    if(arg == null){
                        char var = (char) ('c' + max_idx + left_idx);
                        left_idx = left_idx + 1;
                        p_vars.add(new Pair<>(0, String.valueOf(var)));
                    } else {
                        String cons = RDFModelHandler.LiteralHandler(arg.name, 0); // TODO add type arg
                        p_vars.add(new Pair<>(1, cons));// constant pos
                    }
                }
            }
            map.put(body.functor, p_vars);
        }

        String queryString = RDFQuery.generateQueryString(model, vars, true, map);
        return Objects.requireNonNull(RDFQuery.queryWithSQLForOneVar(model, queryString, "a")).size() * mul - evidence;
    }

    public int getEvidenceCnt(){
        if(RDFQuery.map.containsKey("evi")){
            RDFQuery.map.put("evi", RDFQuery.map.get("evi") + 1);
        } else {
            RDFQuery.map.put("evi", 1);
        }
        int left_idx = 1; // free var holder in SPARQL
        int max_idx = boundedVars.size() - 1;
        boolean literal = false;
        String lit = "";
        List<String> vars = new LinkedList<>();
        Multimap<String, List<Pair<Integer, String>>> map = HashMultimap.create();
        Map<Integer, Integer> head_bound_pos = new HashMap<>();
        Predicate head = getHead();
        for(int idx = 0; idx < 2; ++idx){
            Argument arg = head.args[idx];
            if(arg != null && arg.isVar) head_bound_pos.put(arg.id, idx);
            else {
                if(arg != null){
                    literal = true;
                    lit = RDFModelHandler.LiteralHandler(arg.name, 0);
                }
            }
        }
        // construct SELECT vars: SELECT ?a ?b / SELECT ?a ?a(corner case)
        List<Pair<Integer, String>> h_vars = new LinkedList<>();
        if(structure.size() == 1 && structure.get(0).args[0] != null && structure.get(0).args[1] != null){
            vars.add("a");
            h_vars.add(new Pair<>(0, "a"));
            vars.add("a");
            h_vars.add(new Pair<>(0, "a"));
        } else {
            vars.add("a");
            h_vars.add(new Pair<>(0, "a"));
            if(!literal){
                vars.add("b");
                h_vars.add(new Pair<>(0, "b"));
            } else {
                h_vars.add(new Pair<>(1, lit));
            }
        }
        map.put(head.functor, h_vars);

        // construct WHERE Clause
        for(int i = 1; i < structure.size(); ++i){
            Predicate body = structure.get(i);
            List<Pair<Integer, String>> p_vars = new LinkedList<>();
            for(int idx = 0; idx < 2; ++idx){
                Argument arg = body.args[idx];
                if(arg != null && arg.isVar){
                    if(head_bound_pos.containsKey(arg.id)){
                        int pos = head_bound_pos.get(arg.id);
                        char var = (char) ('a' + pos);
                        p_vars.add(new Pair<>(0, String.valueOf(var)));
                    } else {
                        char var = (char) ('c' + arg.id);
                        p_vars.add(new Pair<>(0, String.valueOf(var)));
                    }
                } else {
                    if(arg == null){
                        char var = (char) ('c' + max_idx + left_idx);
                        left_idx = left_idx + 1;
                        p_vars.add(new Pair<>(0, String.valueOf(var)));
                    } else {
                        String cons = RDFModelHandler.LiteralHandler(arg.name, 0);
                        p_vars.add(new Pair<>(1, cons)); // constant pos
                    }
                }
            }
            map.put(body.functor, p_vars);
        }
        // query for newly proved
        String queryString = RDFQuery.generateQueryString(model, vars, true, map);
        List<String> res = RDFQuery.queryWithSQLForOneVar(model, queryString, "a");
        if(res == null) {
            System.out.println("query result is null");
            return 0;
        }
        evidence = res.size();
        return res.size();
    }
}
