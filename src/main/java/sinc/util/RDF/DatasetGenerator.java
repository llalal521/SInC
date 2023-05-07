package sinc.util.RDF;

import org.apache.jena.rdf.model.*;
import sinc.util.RDF.vocabulary.FamilyVocabulary;

import java.io.*;

public class DatasetGenerator {
    public static void main (String args[]) throws IOException {
        // create an empty model
        Model model = ModelFactory.createDefaultModel();
        // parse tsvFile
        // TODO prefix and literal ignored
        // TODO two arity convert to "type", represent type by string literal
        File tsvFile = new File("/Users/renhaotian/SInC/datasets/nell.tsv");
        File outFile = new File("/Users/renhaotian/SInC/rdf_datasets/nell.ttl");

        model.setNsPrefix("fa", "http://w3.org/family/1.0/");
        String prefix = "fa:";
        String namespace = "http://w3.org/family/1.0/";

        try(BufferedReader reader = new BufferedReader(new FileReader(tsvFile))){
            String line;
            while((line = reader.readLine()) != null){
                String[] strs = line.split("\t");
                if(strs.length > 3) continue; //skip triple or more relations
                Resource A, B;
                Property property;
                if(strs.length == 2){
                    property = ResourceFactory.createProperty(namespace, "type");
                    A = model.getResource(prefix + strs[1]);
                    if(A == null){
                        A = model.createResource(prefix + strs[1]);
                    }
                    A.addProperty(property, strs[0]);
                } else {
                    property = ResourceFactory.createProperty(namespace, strs[0]);
                    A = model.getResource(prefix + strs[1]);
                    B = model.getResource(prefix + strs[2]);
                    if(A == null){
                        A = model.createResource(prefix + strs[1]);
                    }
                    if(B == null){
                        B = model.createResource(prefix + strs[2]);
                    }
                    A.addProperty(property, B);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // print model
        model.write(new FileWriter(outFile), "Turtle");
    }
}
