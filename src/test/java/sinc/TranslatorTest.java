package sinc;

import com.github.andrewoma.dexx.collection.Pair;
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import sinc.common.Graph;
import sinc.common.InterruptedSignal;
import sinc.common.Rule;
import sinc.impl.RuleWithGraph;
import sinc.impl.RuleWithSQL;
import sinc.util.RDF.RDFModelHandler;
import sinc.util.RDF.RDFQuery;

import java.util.*;

public class TranslatorTest {
    @Test
    void testTranslate(){
        Graph graph = new Graph("/Users/renhaotian/SInC/datasets/FB15k.tsv.simplified");
        // customer(X1, X0) <- business_location(X0, X1)
        Map<Integer, Pair<Integer, Integer>> rule1 = new HashMap<>();
        rule1.put(0, new Pair<>(0, 1));
        rule1.put(1, new Pair<>(1, 0));
        List<String> structure1 = new LinkedList<>();
        structure1.add("customer");
        structure1.add("business_location");
        int evid = graph.Rule2Num(rule1, structure1);
        System.out.println("Evidence:");
        System.out.println(evid);
        // <------------------------- 分割线 ------------------------>
        Map<Integer, Pair<Integer, Integer>> rule = new HashMap<>();
        rule.put(0, new Pair<>(1, 0));
        List<String> structure = new LinkedList<>();
        structure.add("business_location");
        int res = graph.Rule2Num(rule, structure, 0, false, 0, true);
        System.out.println("counters: ");
        System.out.println(res);
        // <------------------------- 分割线 ------------------------>
//        Model model = ModelFactory.createDefaultModel();
//        model.read("/Users/renhaotian/SInC/rdf_datasets/elti.ttl");
//        int num = RDFModelHandler.getConsts(model);
//        String queryString = "PREFIX\tfa: <http://w3.org/family/1.0/>\n" +
//                "SELECT\t DISTINCT ?a ?b\n" +
//                "WHERE{\n" +
//                "\t?a\tfa:father\t?b .\n" +
//                "\t?c\tfa:wife\t?a .\n" +
//                "\t?c\tfa:mother\t?b \n" +
//                "}";
//        List<String> list = RDFQuery.queryWithSQLForOneVar(model, queryString, "a");
//        System.out.println("evidence:");
//        System.out.println(list.size());
//        // <------------------------- 分割线 ------------------------>
//        queryString = "PREFIX\tfa: <http://w3.org/family/1.0/>\n" +
//                "SELECT\t DISTINCT ?a ?b\n" +
//                "WHERE{\n" +
//                "\t?c\tfa:wife\t?a .\n" +
//                "\t?c\tfa:mother\t?b \n" +
//                "}";
//        list = RDFQuery.queryWithSQLForOneVar(model, queryString, "a");
//        System.out.println("counters:");
//        System.out.println(list.size());
    }
}
