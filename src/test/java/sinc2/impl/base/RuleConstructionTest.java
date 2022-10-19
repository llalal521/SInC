package sinc2.impl.base;

import org.junit.jupiter.api.Test;
import sinc2.kb.KbException;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RuleConstructionTest {
    @Test
    void test1() throws KbException, IOException {
        SimpleKb kb = new SimpleKb("wn2021", "datasets/numerated");
        SimpleRelation rel_hypernyms = kb.getRelation("hypernyms");
        SimpleRelation rel_hyponyms = kb.getRelation("hyponyms");
        CachedRule rule = new CachedRule(rel_hypernyms.id, rel_hypernyms.totalCols(), new HashSet<>(), new HashMap<>(), kb);
        assertEquals("hypernyms(?,?):-", rule.toDumpString(kb));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(rel_hyponyms.id, rel_hyponyms.totalCols(), 0, 0, 0);
        assertEquals("hypernyms(X0,?):-hyponyms(X0,?)", rule.toDumpString(kb));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(rel_hyponyms.id, rel_hyponyms.totalCols(), 1, 0, 1);
        assertEquals("hypernyms(X0,X1):-hyponyms(X0,?),hyponyms(?,X1)", rule.toDumpString(kb));
    }

    @Test
    void test2() throws KbException, IOException {
        SimpleKb kb = new SimpleKb("wn2021", "datasets/numerated");
        SimpleRelation rel_hypernyms = kb.getRelation("hypernyms");
        SimpleRelation rel_hyponyms = kb.getRelation("hyponyms");
        CachedRule rule = new CachedRule(rel_hypernyms.id, rel_hypernyms.totalCols(), new HashSet<>(), new HashMap<>(), kb);
        assertEquals("hypernyms(?,?):-", rule.toDumpString(kb));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(rel_hyponyms.id, rel_hyponyms.totalCols(), 0, 0, 0);
        assertEquals("hypernyms(X0,?):-hyponyms(X0,?)", rule.toDumpString(kb));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(rel_hyponyms.id, rel_hyponyms.totalCols(), 0, 1, 1);
        assertEquals("hypernyms(X0,?):-hyponyms(X0,X1),hyponyms(X1,?)", rule.toDumpString(kb));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(0, 1, 2, 1);
        assertEquals("hypernyms(X0,X2):-hyponyms(X0,X1),hyponyms(X1,X2)", rule.toDumpString(kb));
    }
}
