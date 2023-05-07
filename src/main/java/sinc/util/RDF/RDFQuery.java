package sinc.util.RDF;

import com.github.andrewoma.dexx.collection.Pair;
import org.apache.jena.base.Sys;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.util.*;

public class RDFQuery {
    public static int times = 0;

    public static Map<String, Integer> map = new HashMap<>();

    public static int evals = 0;
    public static int extens = 0;

    /**
     * function to generate simple query statements
     * @param model RDF model
     * @param vars vars to be selected
     * @param map property to with its vars
     * @return
     */
    public static String generateQueryString(Model model, List<String> vars, boolean distinct,
                                             Multimap<String, List<Pair<Integer, String>>> map){
        if(vars == null || vars.size() == 0)    return "";
        StringBuilder s = new StringBuilder();
        for(Map.Entry<String,String> entry : model.getNsPrefixMap().entrySet()){
            s.append("PREFIX\t").append(entry.getKey()).append(":\t<").append(entry.getValue()).append(">\n");
        }

        // SELECT Portion
        s.append(distinct ? "SELECT DISTINCT \t" : "SELECT \t");
        for(String str : vars){
            s.append("?").append(str).append('\t');
        }

        s.append("\n");
        // WHERE Portion
        s.append("WHERE {\n");
        for (Map.Entry<String, List<Pair<Integer, String>>> entry : map.entries()) {
            s.append(entry.getValue().get(0).component1() == 0 ? " ?" + entry.getValue().get(0).component2() : " " + entry.getValue().get(0).component2())
                    .append(" ").append(entry.getKey())
                    .append(entry.getValue().get(1).component1() == 0 ? " ?" + entry.getValue().get(1).component2() : " " + entry.getValue().get(1).component2())
                    .append(" .\n");
        }
        s.append("}\n");
//        System.out.println(s.toString());
        return s.toString();
    }

    /**
     * query RDF model with SPARQL For one result var(For now is enough)
     * @param model
     * @param queryString
     * @param var
     * @return
     */
    public static List<String> queryWithSQLForOneVar(Model model, String queryString, String var){
        if(Objects.equals(queryString, ""))   return null;
        List<String> res = new LinkedList<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
        ResultSet resultSet = queryExecution.execSelect();
        while(resultSet.hasNext()){
            QuerySolution querySolution = resultSet.nextSolution();
            res.add(querySolution.get(var).toString());
        }
        return res;
    }
}
