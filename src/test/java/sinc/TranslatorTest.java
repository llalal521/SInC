package sinc;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import sinc.impl.RuleWithSQL;
import sinc.util.RDF.RDFModelHandler;
import sinc.util.RDF.RDFQuery;

import java.util.HashSet;
import java.util.List;

public class TranslatorTest {
    @Test
    void testTranslate(){
        Model model = ModelFactory.createDefaultModel();
        model.read("/Users/renhaotian/SInC/rdf_datasets/family_simple.ttl");
        int num = RDFModelHandler.getConsts(model);
        String queryString = "PREFIX\tfa: <http://w3.org/family/1.0/>\n" +
                "SELECT\t DISTINCT ?a\n" +
                "WHERE{\n" +
                "\t?a\tfa:father\t?b .\n" +
                "}";
        List<String> list = RDFQuery.queryWithSQLForOneVar(model, queryString, "a");
        System.out.println(list.size());

//        RuleWithSQL rule = new RuleWithSQL("fa:parent", 2, new HashSet<>(), model);
//        System.out.println(rule.toDumpString());
//        System.out.println(rule.translate());
//        rule.boundFreeVars2NewVar("fa:father", 2, 0, 0, 0);
//        System.out.println(rule.toDumpString());
//        System.out.println(rule.translate());
//        String s = rule.translate();
//        List<String> res = RDFModelHandler.queryWithSQLForOneVar(model, s, "a");
//        s = "PREFIX	fa:	<http://w3.org/family/1.0/>\nSELECT	DISTINCT ?a\nWHERE { \n?a fa:father ?c\n}";
//        List<String> fas = RDFModelHandler.queryWithSQLForOneVar(model, s, "a");
//        s = "PREFIX	fa:	<http://w3.org/family/1.0/>\nSELECT	DISTINCT ?a\nWHERE { \n?a fa:parent ?c\n}";
//        List<String> pas = RDFModelHandler.queryWithSQLForOneVar(model, s, "a");
//        System.out.println(res);
//        System.out.println(fas);
//        System.out.println(pas);
    }
}
