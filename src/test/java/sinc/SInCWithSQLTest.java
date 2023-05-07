package sinc;

import sinc.impl.SInCWithGraph;
import sinc.util.RDF.RDFQuery;
import org.junit.jupiter.api.Test;
import sinc.common.Eval;
import sinc.impl.SInCWithSQL;

import java.util.Map;

public class SInCWithSQLTest {
    @Test
    void testSInCWithSQL(){
        Eval.EvalMetric metric = Eval.EvalMetric.CompressionRate;
        SincConfig config = new SincConfig(1, false, false, 10, false,
                metric, -0.1, 0, 0, 0.14, true,
                -1.0, false, false);
        SInCWithGraph sinc = new SInCWithGraph(config, "/Users/renhaotian/SInC/datasets/FB15K.tsv.simplified",
                "", "");
        sinc.run();
        for (Map.Entry<String, Integer> entry : RDFQuery.map.entrySet()) {
            System.out.println(entry.getKey() + ' ' + entry.getValue().toString());
        }
        System.out.println("evals: " + RDFQuery.evals);
        System.out.println("extens: " + RDFQuery.extens);
    }
}
