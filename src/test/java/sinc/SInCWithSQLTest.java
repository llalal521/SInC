package sinc;

import org.junit.jupiter.api.Test;
import sinc.common.Eval;
import sinc.impl.SInCWithSQL;

public class SInCWithSQLTest {
    @Test
    void testSInCWithSQL(){
        Eval.EvalMetric metric = Eval.EvalMetric.CompressionRate;
        SincConfig config = new SincConfig(1, false, false, 6, false,
                metric, 0.1, 0.1, 0, 0.14, true,
                -1.0, false, false);
        SInCWithSQL sinc = new SInCWithSQL(config, "/Users/renhaotian/SInC/rdf_datasets/family_medium.ttl",
                "", "");
        sinc.run();
    }
}
