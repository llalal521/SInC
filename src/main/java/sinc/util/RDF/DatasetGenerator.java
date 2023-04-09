package sinc.util.RDF;

import org.apache.jena.rdf.model.*;
import sinc.util.RDF.vocabulary.FamilyVocabulary;

import java.io.*;

public class DatasetGenerator {
    public static void main (String args[]) throws IOException {
        // create an empty model
        Model model = ModelFactory.createDefaultModel();
        // parse tsvFile
        // tsv相比rdf会丢失很多语义信息，比如会失去与原本的词汇表的link导致语义丢失
        // 这就导致从tsv到rdf的过程基本是不能自动化实现的（很难通过text去找到对应的词汇表）
        // 所以我就只在简单的模型上验证了——family
        File tsvFile = new File("/Users/renhaotian/SInC/datasets/family_simple.tsv");
        File outFile = new File("/Users/renhaotian/SInC/rdf_datasets/family_simple.ttl");

        // update ns map
        model.setNsPrefix("fa", "http://w3.org/family/1.0/");

        try(BufferedReader reader = new BufferedReader(new FileReader(tsvFile))){
            String line;
            while((line = reader.readLine()) != null){
                String[] strs = line.split("\t");
                if(strs.length > 3) continue; //skip triple or more relations
                strs[0] = "fa:" + strs[0];
                strs[1] = "fa:" + strs[1];
                strs[2] = "fa:" + strs[2];
                Resource A, B;
                // get Resource
                A = model.getResource(strs[1]);
                B = model.getResource(strs[2]);
                if(A == null){
                    A = model.createResource(strs[1]);
                }
                if(B == null && !strs[0].equals("gender")){
                    B = model.createResource(strs[2]);
                }
                // add Property
                if(strs[0].equals("fa:father")){
                    A.addProperty(FamilyVocabulary.father, B);
                }
                if(strs[0].equals("fa:mother")){
                    A.addProperty(FamilyVocabulary.mother, B);
                }
                if(strs[0].equals("fa:parent")){
                    A.addProperty(FamilyVocabulary.parent, B);
                }
                if(strs[0].equals("fa:gender")) {
                    A.addProperty(FamilyVocabulary.gender, strs[2]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // print model
        model.write(new FileWriter(outFile), "Turtle");
    }
}
