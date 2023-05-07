package sinc.util.TDB;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

public class TDBAdapter {
    public static Model constructDB(String path){
        Dataset dataset = TDBFactory.createDataset("yago2s");
        return dataset.getDefaultModel();
    }
}
