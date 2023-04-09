package sinc.util.RDF.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class FamilyVocabulary {
        public static final String uri = "http://w3.org/family/1.0/";

        public static final Property father = cp("father");
        public static final Property mother = cp("mother");
        public static final Property parent = cp("parent");
        public static final Property gender = cp("gender");

        public FamilyVocabulary() {
        }

        public static String getURI() {
            return "http://w3.org/family/1.0/";
        }

        private static Property cp(String ln) {
            return ResourceFactory.createProperty("http://w3.org/family/1.0/", ln);
        }

}
