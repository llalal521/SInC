package sinc.util.RDF;

import org.apache.jena.ext.com.google.common.collect.HashMultimap;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import java.io.File;
import java.util.*;

public class RDFModelHandler {
    /**
     * build a graph model from a RDF file
     * @param filepath absolute path
     * @return RDF Model
     */
    public static Model getModel(String filepath){
        Model model = RDFDataMgr.loadModel(filepath);
        File file = new File(filepath);
        if(!file.exists()){
            System.out.println("file not found");
            return null;
        }
        return model;
    }

    public static int getStmCnts(Model model){
        if(RDFQuery.map.containsKey("stm")){
            RDFQuery.map.put("stm", RDFQuery.map.get("stm") + 1);
        } else {
            RDFQuery.map.put("stm", 1);
        }
        String queryString = "PREFIX fa: <http://w3.org/family/1.0>\n" +
                "SELECT ?a\n" +
                "WHERE{\n" +
                "?a ?x ?b\n" +
                "}\n";
        return RDFQuery.queryWithSQLForOneVar(model, queryString, "a").size();
    }

    public static int getConsts(Model model){
        if(RDFQuery.map.containsKey("consts")){
            RDFQuery.map.put("consts", RDFQuery.map.get("consts") + 2);
        } else {
            RDFQuery.map.put("consts", 2);
        }
        String queryString = "PREFIX fa: <http://w3.org/family/1.0>\n" +
                "SELECT DISTINCT ?a\n" +
                "WHERE{\n" +
                "?a ?x ?b\n" +
                "FILTER(!isLITERAL(?a))\n" +
                "}\n";
        Set<String> judge = new HashSet<>(Objects.requireNonNull(RDFQuery.queryWithSQLForOneVar(model, queryString, "a")));
        queryString = "PREFIX fa: <http://w3.org/family/1.0>\n" +
                "SELECT DISTINCT ?b\n" +
                "WHERE{\n" +
                "?a ?x ?b\n" +
                "FILTER(!isLITERAL(?b))\n" +
                "}\n";
        judge.addAll(Objects.requireNonNull(RDFQuery.queryWithSQLForOneVar(model, queryString, "b")));
        return judge.size();
    }

    public static List<String> getProperties(Model model){
        if(RDFQuery.map.containsKey("pros")){
            RDFQuery.map.put("pros", RDFQuery.map.get("pros") + 1);
        } else {
            RDFQuery.map.put("pros", 1);
        }
        String queryString = "PREFIX fa: <http://w3.org/family/1.0>\n" +
                "SELECT DISTINCT ?x\n" +
                "WHERE{\n" +
                "?a ?x ?b\n" +
                "}\n";
        List<String> list = RDFQuery.queryWithSQLForOneVar(model, queryString, "x");
        assert list != null;
        for(int i = 0; i < list.size(); ++i){
            for(Map.Entry<String,String> entry : model.getNsPrefixMap().entrySet()) {
                if (list.get(i).contains(entry.getValue())){
                    list.set(i, list.get(i).replace(entry.getValue(), entry.getKey() + ":"));
                }
            }
        }
        return list;
    }

    public static Map<String, Integer> getPropertyToArity(Model model){
        Map<String, Integer> map = new HashMap<>();
        List<String> properties = getProperties(model);
        for(String s : properties){
            map.put(s, 2);
        }
        return map;
    }

    /***
     * three types of literal:
     * string literal: add enclosing ""
     * typed literal: no special process TODO maybe wrong
     * plain literal: no special process TODO maybe wrong
     * @param literal
     * @param type
     * @return
     */
    public static String LiteralHandler(String literal, int type){
        String res = "";
        if(type == 0){
            res = "\"" + literal + "\"";
        }
        return res;
    }

}
