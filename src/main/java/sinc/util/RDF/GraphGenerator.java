package sinc.util.RDF;

import org.apache.jena.rdf.model.*;
import sinc.common.Graph;

import java.io.*;

public class GraphGenerator {
    public static void main (String args[]) throws IOException {
        // parse tsvFile
        // TODO prefix and literal ignored
        // TODO two arity convert to "type", represent type by string literal
        Graph graph = new Graph("/Users/renhaotian/SInC/datasets/nell.tsv");
        System.out.println(graph);
    }
}
