package sinc2;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.common.InterruptedSignal;
import sinc2.common.Predicate;
import sinc2.kb.KbException;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;
import sinc2.rule.*;
import sinc2.util.LittleEndianIntIO;
import sinc2.util.graph.GraphNode;
import sinc2.util.kb.NumeratedKb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RelationMinerTest {

    static class TestRelationMiner extends RelationMiner {

        static final BareRule BAD_RULE = new BareRule(0, 0, new HashSet<>(), new HashMap<>());
        static {
            BAD_RULE.returningEval = Eval.MIN;
        }

        public TestRelationMiner(
                SimpleKb kb, int targetRelation, EvalMetric evalMetric, int beamwidth, double stopCompressionRatio,
                Map<Predicate, GraphNode<Predicate>> predicate2NodeMap, Map<GraphNode<Predicate>,
                Set<GraphNode<Predicate>>> dependencyGraph, PrintWriter logger
        ) {
            super(kb, targetRelation, evalMetric, beamwidth, stopCompressionRatio, predicate2NodeMap, dependencyGraph, logger);
        }

        @Override
        protected Rule getStartRule() {
            return new BareRule(targetRelation, kb.getRelation(targetRelation).totalCols(), new HashSet<>(), new HashMap<>());
        }

        @Override
        protected int checkThenAddRule(UpdateStatus updateStatus, Rule updatedRule, Rule originalRule, Rule[] candidates) throws InterruptedSignal {
            return super.checkThenAddRule(updateStatus, updatedRule, BAD_RULE, candidates);
        }

        @Override
        protected void selectAsBeam(Rule r) {}
    }

    /*
     * KB: family/3, father/2, mother/2, isMale/1
     */
    static final String TEST_DIR = "/dev/shm";
    static final String KB_NAME = UUID.randomUUID().toString();
    static SimpleKb kb;

    @BeforeAll
    static void setupKb() throws KbException, IOException {
        File kb_dir = Paths.get(TEST_DIR, KB_NAME).toFile();
        assertTrue(kb_dir.exists() || kb_dir.mkdirs());

        /* Create relation name mapping */
        PrintWriter writer = new PrintWriter(NumeratedKb.getRelInfoFilePath(KB_NAME, TEST_DIR).toFile());
        writer.println("family\t3\t2");
        writer.println("father\t2\t2");
        writer.println("mother\t2\t2");
        writer.println("isMale\t1\t1");
        writer.close();

        /* Create relation data files */
        String kb_dir_path = kb_dir.getAbsolutePath();
        FileOutputStream fos = new FileOutputStream(Paths.get(kb_dir_path, "0.rel").toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(1));
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(3));
        fos.write(LittleEndianIntIO.leInt2ByteArray(1));
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.close();
        fos = new FileOutputStream(Paths.get(kb_dir_path, "1.rel").toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(3));
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.write(LittleEndianIntIO.leInt2ByteArray(6));
        fos.close();
        fos = new FileOutputStream(Paths.get(kb_dir_path, "2.rel").toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(1));
        fos.write(LittleEndianIntIO.leInt2ByteArray(3));
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.close();
        fos = new FileOutputStream(Paths.get(kb_dir_path, "3.rel").toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.close();

        /* KB:
         * family       father  mother  isMale
         * 1    2   3   2   3   1   3   2
         * 1    2   4   4   6   2   4
         */
        kb = new SimpleKb(KB_NAME, TEST_DIR);
        SimpleRelation.MIN_CONSTANT_COVERAGE = 0.6;
        kb.updatePromisingConstants();
    }

    @Test
    void testFindSpecializations1() throws InterruptedSignal {
        SimpleRelation rel_family = kb.getRelation("family");
        SimpleRelation rel_father = kb.getRelation("father");
        SimpleRelation rel_mother = kb.getRelation("mother");
        SimpleRelation rel_is_male = kb.getRelation("isMale");
        assertNotNull(rel_family);
        assertNotNull(rel_father);
        assertNotNull(rel_mother);
        assertNotNull(rel_is_male);

        Rule base_rule = new BareRule(rel_family.id, 3, new HashSet<>(), new HashMap<>());
        assertEquals("family(?,?,?):-", base_rule.toDumpString(kb));

        Set<String> expected_specs = new HashSet<>(List.of(
                "family(X0,X0,?):-",
                "family(X0,?,X0):-",//
                "family(?,X0,X0):-",
                "family(X0,?,?):-family(X0,?,?)",
                "family(X0,?,?):-family(?,X0,?)",
                "family(X0,?,?):-family(?,?,X0)",
                "family(?,X0,?):-family(X0,?,?)",//
                "family(?,X0,?):-family(?,X0,?)",
                "family(?,X0,?):-family(?,?,X0)",
                "family(?,?,X0):-family(X0,?,?)",
                "family(?,?,X0):-family(?,X0,?)",
                "family(?,?,X0):-family(?,?,X0)",
                "family(X0,?,?):-mother(X0,?)",
                "family(X0,?,?):-mother(?,X0)",
                "family(?,X0,?):-mother(X0,?)",
                "family(?,X0,?):-mother(?,X0)",
                "family(?,?,X0):-mother(X0,?)",
                "family(?,?,X0):-mother(?,X0)",
                "family(X0,?,?):-father(X0,?)",
                "family(X0,?,?):-father(?,X0)",
                "family(?,X0,?):-father(X0,?)",
                "family(?,X0,?):-father(?,X0)",
                "family(?,?,X0):-father(X0,?)",
                "family(?,?,X0):-father(?,X0)",
                "family(X0,?,?):-isMale(X0)",
                "family(?,X0,?):-isMale(X0)",//
                "family(?,?,X0):-isMale(X0)",
                "family(1,?,?):-",
                "family(?,2,?):-"
        ));

        RelationMiner miner = new TestRelationMiner(
                kb, rel_family.id, EvalMetric.CompressionCapacity, 1, 1.0,
                new HashMap<>(), new HashMap<>(), new PrintWriter(System.out)
        );
        Rule[] spec_rules = new Rule[expected_specs.size() * 2];
        int actual_spec_cnt = miner.findSpecializations(base_rule, spec_rules);
        Set<String> actual_specs =new HashSet<>();
        for (Rule rule: spec_rules) {
            if (null == rule) {
                break;
            }
            actual_specs.add(rule.toDumpString(kb));
        }
        assertEquals(expected_specs.size(), actual_spec_cnt);
        assertEquals(expected_specs, actual_specs);
    }

    @Test
    void testFindSpecializations2() throws InterruptedSignal {
        SimpleRelation rel_family = kb.getRelation("family");
        SimpleRelation rel_father = kb.getRelation("father");
        SimpleRelation rel_mother = kb.getRelation("mother");
        SimpleRelation rel_is_male = kb.getRelation("isMale");
        assertNotNull(rel_family);
        assertNotNull(rel_father);
        assertNotNull(rel_mother);
        assertNotNull(rel_is_male);

        Rule base_rule = new BareRule(rel_family.id, 3, new HashSet<>(), new HashMap<>());
        base_rule.cvt2Uvs2NewLv(rel_father.id, 2, 0, 0, 1);
        assertEquals("family(?,X0,?):-father(X0,?)", base_rule.toDumpString(kb));

        Set<String> expected_specs = new HashSet<>(List.of(
                "family(X0,X0,?):-father(X0,?)",
                "family(?,X0,X0):-father(X0,?)",
                "family(?,X0,?):-father(X0,X0)",
                "family(?,X0,?):-father(X0,?),family(X0,?,?)",
                "family(?,X0,?):-father(X0,?),family(?,X0,?)",
                "family(?,X0,?):-father(X0,?),family(?,?,X0)",
                "family(?,X0,?):-father(X0,?),mother(X0,?)",
                "family(?,X0,?):-father(X0,?),mother(?,X0)",
                "family(?,X0,?):-father(X0,?),father(X0,?)",
                "family(?,X0,?):-father(X0,?),father(?,X0)",
                "family(?,X0,?):-father(X0,?),isMale(X0)",
                "family(X1,X0,X1):-father(X0,?)",
                "family(X1,X0,?):-father(X0,X1)",
                "family(?,X0,X1):-father(X0,X1)",
                "family(X1,X0,?):-father(X0,?),family(X1,?,?)",
                "family(X1,X0,?):-father(X0,?),family(?,X1,?)",
                "family(X1,X0,?):-father(X0,?),family(?,?,X1)",
                "family(?,X0,X1):-father(X0,?),family(X1,?,?)",
                "family(?,X0,X1):-father(X0,?),family(?,X1,?)",
                "family(?,X0,X1):-father(X0,?),family(?,?,X1)",
                "family(?,X0,?):-father(X0,X1),family(X1,?,?)",
                "family(?,X0,?):-father(X0,X1),family(?,X1,?)",
                "family(?,X0,?):-father(X0,X1),family(?,?,X1)",
                "family(X1,X0,?):-father(X0,?),father(X1,?)",
                "family(X1,X0,?):-father(X0,?),father(?,X1)",
                "family(?,X0,X1):-father(X0,?),father(X1,?)",
                "family(?,X0,X1):-father(X0,?),father(?,X1)",
                "family(?,X0,?):-father(X0,X1),father(X1,?)",
                "family(?,X0,?):-father(X0,X1),father(?,X1)",
                "family(X1,X0,?):-father(X0,?),mother(X1,?)",
                "family(X1,X0,?):-father(X0,?),mother(?,X1)",
                "family(?,X0,X1):-father(X0,?),mother(X1,?)",
                "family(?,X0,X1):-father(X0,?),mother(?,X1)",
                "family(?,X0,?):-father(X0,X1),mother(X1,?)",
                "family(?,X0,?):-father(X0,X1),mother(?,X1)",
                "family(X1,X0,?):-father(X0,?),isMale(X1)",
                "family(?,X0,X1):-father(X0,?),isMale(X1)",
                "family(?,X0,?):-father(X0,X1),isMale(X1)",
                "family(1,X0,?):-father(X0,?)"
        ));

        RelationMiner miner = new TestRelationMiner(
                kb, rel_family.id, EvalMetric.CompressionCapacity, 1, 1.0,
                new HashMap<>(), new HashMap<>(), new PrintWriter(System.out)
        );
        Rule[] spec_rules = new Rule[expected_specs.size() * 2];
        assertEquals(expected_specs.size(), miner.findSpecializations(base_rule, spec_rules));
        Set<String> actual_specs =new HashSet<>();
        for (Rule rule: spec_rules) {
            if (null == rule) {
                break;
            }
            actual_specs.add(rule.toDumpString(kb));
        }
        assertEquals(expected_specs, actual_specs);
    }

    @Test
    void testFindGeneralizations() throws InterruptedSignal {
        SimpleRelation rel_family = kb.getRelation("family");
        SimpleRelation rel_father = kb.getRelation("father");
        SimpleRelation rel_mother = kb.getRelation("mother");
        SimpleRelation rel_is_male = kb.getRelation("isMale");
        assertNotNull(rel_family);
        assertNotNull(rel_father);
        assertNotNull(rel_mother);
        assertNotNull(rel_is_male);

        Rule base_rule = new BareRule(rel_family.id, 3, new HashSet<>(), new HashMap<>());
        base_rule.cvt2Uvs2NewLv(rel_father.id
                , 2, 0, 0, 1);
        base_rule.cvt2Uvs2NewLv(rel_mother.id, 2, 0, 0, 0);
        base_rule.cvt2Uvs2NewLv(0,2, 1, 1);
        base_rule.cvt1Uv2ExtLv(rel_is_male.id, 1, 0, 2);
        assertEquals("family(X1,X0,X2):-father(X0,X2),mother(X1,?),isMale(X2)", base_rule.toDumpString(kb));

        Set<String> expected_specs = new HashSet<>(List.of(
                "family(?,X0,X1):-father(X0,X1),isMale(X1)",
                "family(X1,?,X0):-father(?,X0),mother(X1,?),isMale(X0)",
                "family(X1,X0,?):-father(X0,X2),mother(X1,?),isMale(X2)",
//                "family(X1,X0,X2):-father(X0,X2),mother(X1,?)",  // This rule is in the cache
                "family(X1,X0,X2):-father(X0,?),mother(X1,?),isMale(X2)"
        ));

        RelationMiner miner = new TestRelationMiner(
                kb, rel_family.id, EvalMetric.CompressionCapacity, 1, 1.0,
                new HashMap<>(), new HashMap<>(), new PrintWriter(System.out)
        );
        Rule[] spec_rules = new Rule[expected_specs.size() * 2];
        int added_rules = miner.findGeneralizations(base_rule, spec_rules);
        assertEquals(expected_specs.size(), added_rules);
        Set<String> actual_specs =new HashSet<>();
        for (Rule rule: spec_rules) {
            if (null == rule) {
                break;
            }
            actual_specs.add(rule.toDumpString(kb));
        }
        assertEquals(expected_specs, actual_specs);
    }
}