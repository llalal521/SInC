package sinc.common;

import com.github.andrewoma.dexx.collection.Pair;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.reasoner.rulesys.builtins.Max;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Graph {
    public final Map<Integer, String> predicates = new HashMap<>(); //ok
    public final Map<String, Integer> ids = new HashMap<>(); //ok
    public int relation_num;
    public final List<Node> nodes = new LinkedList<>(); //ok
    public final Map<String, Node> name2node= new HashMap<>(); //ok
    public final Map<Integer, Set<Node>> subjects = new HashMap<>(); //ok
    public final Map<Integer, Integer> stms = new HashMap<>();

    public Graph(String tsv_path){
        File tsvFile = new File(tsv_path);
        Map<Integer, List<Pair<Node, Node>>> tuples = new HashMap<>();

        relation_num = 0;
        // parse file
        try(BufferedReader reader = new BufferedReader(new FileReader(tsvFile))){
            String line;
            while((line = reader.readLine()) != null){
                String[] strs = line.split("\t");
                if(strs.length != 3) continue; //skip triple or more relations
                // add a new predicate
                if(!ids.containsKey(strs[0])){
                    ids.put(strs[0], relation_num);
                    predicates.put(relation_num, strs[0]);
                    subjects.put(relation_num, new HashSet<>());
                    relation_num ++;
                }
                // handle subject
                int predicate_id = ids.get(strs[0]);
                if(!stms.containsKey(predicate_id)){
                    stms.put(predicate_id, 1);
                } else{
                    stms.put(predicate_id, stms.get(predicate_id) + 1);
                }
                Node sub, obj;
                if(!name2node.containsKey(strs[1])){
                    sub = new Node(strs[1]);
                    name2node.put(strs[1], sub);
                    subjects.get(predicate_id).add(sub);
                    nodes.add(sub);
                } else {
                    sub = name2node.get(strs[1]);
                    subjects.get(predicate_id).add(sub);
                }
                if(!name2node.containsKey(strs[2])){
                    obj = new Node(strs[2]);
                    name2node.put(strs[2], obj);
                    nodes.add(obj);
                } else{
                    obj = name2node.get(strs[2]);
                }
                if(tuples.containsKey(predicate_id)){
                    tuples.get(predicate_id).add(new Pair<>(sub, obj));
                } else {
                    tuples.put(predicate_id, new LinkedList<>());
                    tuples.get(predicate_id).add(new Pair<>(sub, obj));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(Node node : nodes){
            node.setNum(relation_num);
        }
        for(int i = 0; i < relation_num; ++i){
            List<Pair<Node, Node>> list = tuples.get(i);
            for(Pair<Node, Node> pair : list){
                pair.component1().setObject(i, pair.component2());
                pair.component2().setSubject(i, pair.component1());
            }
        }
    }

    public int Rule2Num(Map<Integer, Pair<Integer, Integer>> rule, List<String> structure){
        String head = structure.get(0);
        int id = ids.get(structure.get(0));
        Set<Node> subs = subjects.get(id);
        Map<Integer, Node> path = new HashMap<>();
        Map<Integer, Pair<Integer, Integer>> arg_vars = new HashMap<>(rule);
        arg_vars.remove(0);
        List<Integer> allSame = getAllSame(rule, 0);
        List<Integer> subSame = getSubSame(rule, 0);
        List<Integer> objSame = getObjSame(rule, 0);
        List<Integer> soos = getSoos(rule, 0);
        List<Integer> so = getSo(rule, 0);
        List<Integer> os = getOs(rule, 0);

        int res = 0;
        for(Node sub : subs) {
            if(sub.adjacent.get(ids.get(head)) == null) continue;
            for(Node obj : sub.adjacent.get(ids.get(head))) {
                path.put(rule.get(0).component1(), sub);
                path.put(rule.get(0).component2(), obj);

                boolean judge = true;
                if (!allSame.isEmpty()) {
                    for (Integer idx : allSame) {
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.adjacent.get(pid);
                        if (nodes == null || !nodes.contains(obj)){
                            judge = false;
                            break;
                        }
                    }
                }

                if(!judge){
                    path.clear();
                    continue;
                }

                if (!soos.isEmpty()) {
                    for (Integer idx : soos) {
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.reverse.get(pid);
                        if (nodes == null || !nodes.contains(obj)){
                            judge = false;
                            break;
                        }
                    }
                }

                if(!judge){
                    path.clear();
                    continue;
                }

                //接下来的情况需要递归搜索
                if(!subSame.isEmpty()){
                    for(Integer idx : subSame){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.adjacent.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component2(),
                                    idx, ids.get(structure.get(idx)), true, sub, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!os.isEmpty()){
                    for(Integer idx : os){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = obj.adjacent.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component2(),
                                    idx, ids.get(structure.get(idx)), true, obj, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!objSame.isEmpty()){
                    for(Integer idx : objSame){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = obj.reverse.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component1(),
                                    idx, ids.get(structure.get(idx)), false, obj, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!so.isEmpty()){
                    for(Integer idx : so){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.reverse.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component1(),
                                    idx, ids.get(structure.get(idx)), false, sub, structure);
                        }
                    }
                }
                if(judge){
                    res ++;
                }
                path.clear();
            }
        }
        return res;
    }

    /**
     *
     * @param rule construct {index: [arg_id1, arg_id2]}
     * @param structure: predicate functor array
     * @param pos: the index of chosen predicate(which has one var same with head and can cover all conditions)
     * @param is_sub: which arg is the same var of the chosen one
     * @return num of counters
     */
    public Set<Node> Rule2Num(Map<Integer, Pair<Integer, Integer>> rule, List<String> structure, int pos, boolean is_sub){
        int id = ids.get(structure.get(pos));
        Set<Node> subs = subjects.get(id);
        Set<Node> set = new HashSet<>();
        Map<Integer, Node> path = new HashMap<>();
        Map<Integer, Pair<Integer, Integer>> arg_vars = new HashMap<>(rule);
        arg_vars.remove(pos);
        List<Integer> allSame = getAllSame(rule, pos);
        List<Integer> subSame = getSubSame(rule, pos);
        List<Integer> objSame = getObjSame(rule, pos);
        List<Integer> soos = getSoos(rule, pos);
        List<Integer> so = getSo(rule, pos);
        List<Integer> os = getOs(rule, pos);

        int res = 0;
        for(Node sub : subs) {
            if(sub.adjacent.get(id) == null) continue;
            for(Node obj : sub.adjacent.get(id)) {
                path.put(rule.get(pos).component1(), sub);
                path.put(rule.get(pos).component2(), obj);

                boolean judge = true;
                if (!allSame.isEmpty()) {
                    for (Integer idx : allSame) {
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.adjacent.get(pid);
                        if (nodes == null || !nodes.contains(obj)){
                            judge = false;
                            break;
                        }
                    }
                }

                if(!judge){
                    path.clear();
                    continue;
                }

                if (!soos.isEmpty()) {
                    for (Integer idx : soos) {
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.reverse.get(pid);
                        if (nodes == null || !nodes.contains(obj)){
                            judge = false;
                            break;
                        }
                    }
                }

                if(!judge){
                    path.clear();
                    continue;
                }

                //接下来的情况需要递归搜索
                if(!subSame.isEmpty()){
                    for(Integer idx : subSame){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.adjacent.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component2(),
                                    idx, ids.get(structure.get(idx)), true, sub, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!os.isEmpty()){
                    for(Integer idx : os){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = obj.adjacent.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component2(),
                                    idx, ids.get(structure.get(idx)), true, obj, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!objSame.isEmpty()){
                    for(Integer idx : objSame){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = obj.reverse.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component1(),
                                    idx, ids.get(structure.get(idx)), false, obj, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!so.isEmpty()){
                    for(Integer idx : so){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.reverse.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component1(),
                                    idx, ids.get(structure.get(idx)), false, sub, structure);
                        }
                    }
                }
                if(judge){
                    set.add(is_sub ? sub : obj);
                }
                path.clear();
            }
        }
        return set;
    }

    public int Rule2Num(Map<Integer, Pair<Integer, Integer>> rule, List<String> structure, int sub1_pos, boolean is_sub1,
                 int obj1_pos, boolean is_sub2){
        Set<Node> subs = Rule2Num(rule, structure, sub1_pos, is_sub1);
        Set<Node> objs = Rule2Num(rule, structure, obj1_pos, is_sub2);
        Map<Integer, Node> path = new HashMap<>();
        int sub_pos = is_sub1? rule.get(sub1_pos).component1() : rule.get(sub1_pos).component2();
        int obj_pos = is_sub2? rule.get(obj1_pos).component1() : rule.get(obj1_pos).component2();
        Map<Integer, Pair<Integer, Integer>> arg_vars = new HashMap<>(rule);
        rule.put(10, new Pair<>(sub_pos, obj_pos));
        List<Integer> allSame = getAllSame(rule, 10);
        List<Integer> subSame = getSubSame(rule, 10);
        List<Integer> objSame = getObjSame(rule, 10);
        List<Integer> soos = getSoos(rule, 10);
        List<Integer> so = getSo(rule, 10);
        List<Integer> os = getOs(rule, 10);
        int res = 0;

        for(Node sub : subs) {
            for(Node obj : objs) {
                path.put(sub_pos, sub);
                path.put(obj_pos, obj);

                boolean judge = true;
                if (!allSame.isEmpty()) {
                    for (Integer idx : allSame) {
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.adjacent.get(pid);
                        if (nodes == null || !nodes.contains(obj)){
                            judge = false;
                            break;
                        }
                    }
                }

                if(!judge){
                    path.clear();
                    continue;
                }

                if (!soos.isEmpty()) {
                    for (Integer idx : soos) {
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.reverse.get(pid);
                        if (nodes == null || !nodes.contains(obj)){
                            judge = false;
                            break;
                        }
                    }
                }

                if(!judge){
                    path.clear();
                    continue;
                }

                //接下来的情况需要递归搜索
                if(!subSame.isEmpty()){
                    for(Integer idx : subSame){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.adjacent.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component2(),
                                    idx, ids.get(structure.get(idx)), true, sub, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!os.isEmpty()){
                    for(Integer idx : os){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = obj.adjacent.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component2(),
                                    idx, ids.get(structure.get(idx)), true, obj, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!objSame.isEmpty()){
                    for(Integer idx : objSame){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = obj.reverse.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component1(),
                                    idx, ids.get(structure.get(idx)), false, obj, structure);
                        }
                    }
                }
                if(!judge){
                    path.clear();
                    continue;
                }
                if(!so.isEmpty()){
                    for(Integer idx : so){
                        int pid = ids.get(structure.get(idx));
                        Set<Node> nodes = sub.reverse.get(pid);
                        if(nodes == null){
                            judge = false;
                            break;
                        } else {
                            judge = dfsJudge(path, new HashMap<>(arg_vars), rule.get(idx).component1(),
                                    idx, ids.get(structure.get(idx)), false, sub, structure);
                        }
                    }
                }
                if(judge){
                    res ++;
                }
                path.clear();
            }
        }
        return res;
    }
    boolean dfsJudge(Map<Integer, Node> path, Map<Integer, Pair<Integer, Integer>> vars, int arg_id
            , int pre_pos, int pred_id, boolean sub, Node node, List<String> structure){
        // 两个递归终止条件
        if(vars.size() == 0)    return true;
        int temp = sub? vars.get(pre_pos).component2() : vars.get(pre_pos).component1();
        if(path.containsKey(temp)){
            if(sub){
                return node.adjacent.get(pred_id).contains(path.get(temp));
            }
            return node.reverse.get(pred_id).contains(path.get(temp));
        }

        List<Integer> sub_same = new ArrayList<>();
        List<Integer> obj_same = new ArrayList<>();

        Map<Integer, Pair<Integer, Integer>> arg_vars = new HashMap<>(vars);
        for(Map.Entry<Integer, Pair<Integer, Integer>> entry : vars.entrySet()){
            if(entry.getKey() == pre_pos)   continue;
            if(entry.getValue().component1() == arg_id){
                sub_same.add(entry.getKey());
            } else {
                if(entry.getValue().component2() == arg_id){
                    obj_same.add(entry.getKey());
                }
            }
        }
        arg_vars.remove(pre_pos);

        Set<Node> nodes = sub? node.adjacent.get(pred_id)
                : node.reverse.get(pred_id);

        boolean res = true;
        for(Node next : nodes){
            path.put(arg_id, next);
            if(!sub_same.isEmpty()){
                for(Integer idx : sub_same){
                    int new_pred_id = ids.get(structure.get(idx));
                    if(next.adjacent.get(new_pred_id) == null){
                        res = false;
                        break;
                    } else {
                        res = dfsJudge(path, new HashMap<>(arg_vars), vars.get(idx).component2(), idx, new_pred_id
                                , true, next, structure);
                        path.remove(arg_id);
                        if(!res) break;
                    }
                }
            }
            if(!res)    continue;
            if(!obj_same.isEmpty()){
                for(Integer idx : obj_same){
                    int new_pred_id = ids.get(structure.get(idx));
                    if(next.reverse.get(new_pred_id) == null){
                        res = false;
                        break;
                    } else {
                        res = dfsJudge(path, new HashMap<>(arg_vars), vars.get(idx).component1(), idx, new_pred_id
                                , false, next, structure);
                        path.remove(arg_id);
                        if(!res) break;
                    }
                }
            }
            path.remove(arg_id);
            if(res) return true;
        }
        return false;
    }

    private List<Integer> getAllSame(Map<Integer, Pair<Integer, Integer>> map, int pos){
        Pair<Integer, Integer> pair = map.get(pos);
        List<Integer> res = new LinkedList<>();
        for(Map.Entry<Integer, Pair<Integer, Integer>> entry : map.entrySet()){
            if(entry.getKey() == pos)   continue;
            if(Objects.equals(entry.getValue().component1(), pair.component1()) &&
                    Objects.equals(entry.getValue().component2(), pair.component2())){
                res.add(entry.getKey());
            }
        }
        return res;
    }

    private List<Integer> getSubSame(Map<Integer, Pair<Integer, Integer>> map, int pos){
        Pair<Integer, Integer> pair = map.get(pos);
        List<Integer> res = new LinkedList<>();
        for(Map.Entry<Integer, Pair<Integer, Integer>> entry : map.entrySet()){
            if(entry.getKey() == pos)   continue;
            if(Objects.equals(entry.getValue().component1(), pair.component1()) &&
                    !Objects.equals(entry.getValue().component2(), pair.component2())){
                res.add(entry.getKey());
            }
        }
        return res;
    }

    private List<Integer> getObjSame(Map<Integer, Pair<Integer, Integer>> map, int pos){
        Pair<Integer, Integer> pair = map.get(pos);
        List<Integer> res = new LinkedList<>();
        for(Map.Entry<Integer, Pair<Integer, Integer>> entry : map.entrySet()){
            if(entry.getKey() == pos)   continue;
            if(!Objects.equals(entry.getValue().component1(), pair.component1()) &&
                    Objects.equals(entry.getValue().component2(), pair.component2())){
                res.add(entry.getKey());
            }
        }
        return res;
    }

    private List<Integer> getSoos(Map<Integer, Pair<Integer, Integer>> map, int pos){
        Pair<Integer, Integer> pair = map.get(pos);
        List<Integer> res = new LinkedList<>();
        for(Map.Entry<Integer, Pair<Integer, Integer>> entry : map.entrySet()){
            if(entry.getKey() == pos)   continue;
            if(Objects.equals(entry.getValue().component1(), pair.component2()) &&
                    Objects.equals(entry.getValue().component2(), pair.component1())){
                res.add(entry.getKey());
            }
        }
        return res;
    }

    private List<Integer> getOs(Map<Integer, Pair<Integer, Integer>> map, int pos){
        Pair<Integer, Integer> pair = map.get(pos);
        List<Integer> res = new LinkedList<>();
        for(Map.Entry<Integer, Pair<Integer, Integer>> entry : map.entrySet()){
            if(entry.getKey() == pos)   continue;
            if(Objects.equals(entry.getValue().component1(), pair.component2()) &&
                    !Objects.equals(entry.getValue().component2(), pair.component1())){
                res.add(entry.getKey());
            }
        }
        return res;
    }

    private List<Integer> getSo(Map<Integer, Pair<Integer, Integer>> map, int pos){
        Pair<Integer, Integer> pair = map.get(pos);
        List<Integer> res = new LinkedList<>();
        for(Map.Entry<Integer, Pair<Integer, Integer>> entry : map.entrySet()){
            if(entry.getKey() == pos)   continue;
            if(Objects.equals(entry.getValue().component2(), pair.component1()) &&
                    !Objects.equals(entry.getValue().component1(), pair.component2())){
                res.add(entry.getKey());
            }
        }
        return res;
    }

    private Set<Pair<Node, Node>> And(Set<Pair<Node, Node>> a, Set<Pair<Node, Node>> b){
        Set<Pair<Node, Node>> res = new HashSet<>();
        for(Pair<Node, Node> pair : a){
            if(b.contains(pair)){
                res.add(pair);
            }
        }
        return res;
    }
}
