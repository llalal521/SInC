package sinc2.util.kb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.common.Record;
import sinc2.kb.KbException;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class NumeratedKbTest {

    static TestKbManager testKbManager;

    @BeforeAll
    static void setupKb() throws IOException {
        testKbManager = new TestKbManager();
    }

    @AfterAll
    static void removeKb() {
        testKbManager.cleanUpKb();
    }


    @Test
    void testCreateEmpty() throws KbException {
        NumeratedKb kb = new NumeratedKb("test");
        assertEquals("test", kb.getName());
        assertEquals(0, kb.totalMappings());
        assertEquals(0, kb.totalRelations());
        assertEquals(0, kb.totalRecords());

        kb.addRecord("family", new String[]{"alice", "bob", "catherine"});
        assertEquals(0, kb.getRelation("family").getId());
        kb.addRecord(0, new String[]{"diana", "erick", "frederick"});
        kb.addRecord(0, new String[]{"gabby", "gabby", "gabby"});
        kb.addRecord(0, new String[]{"harry", "harry", "harry"});
        kb.addRecord(0, new String[]{"isaac", "isaac", "isaac"});
        kb.addRecord(0, new String[]{"jena", "jena", "jena"});
        kb.addRecord(0, new String[]{"kyle", "kyle", "kyle"});
        kb.addRecord(0, new String[]{"lily", "lily", "lily"});
        kb.addRecord("family", new Record(new int[]{7, 8, 9}));
        kb.addRecord(0, new Record(new int[]{10, 11, 12}));
        kb.removeRecord(0, new String[]{"gabby", "gabby", "gabby"});
        kb.removeRecord(0, new String[]{"harry", "harry", "harry"});
        kb.removeRecord(0, new String[]{"isaac", "isaac", "isaac"});
        kb.removeRecord(0, new String[]{"jena", "jena", "jena"});
        kb.removeRecord(0, new String[]{"kyle", "kyle", "kyle"});
        kb.removeRecord(0, new String[]{"lily", "lily", "lily"});

        assertEquals(12, kb.totalMappings());
        assertEquals(1, kb.totalRelations());
        assertEquals(4, kb.totalRecords());
        assertTrue(kb.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord("family", new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord(0, new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord(0, new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord(0, new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord(0, new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord("family", new Record(new int[]{1, 2, 3})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{10, 11, 12})));
        assertTrue(kb.hasRecord(0, new Record(new int[]{1, 2, 3})));
        assertTrue(kb.hasRecord(0, new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord(0, new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord(0, new Record(new int[]{10, 11, 12})));
    }

    @Test
    void testRead() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);

        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(14, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(12, kb.totalRecords());

        final int family_num = kb.getRelation("family").getId();
        assertTrue(kb.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord("family", new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord("family", new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{10, 11, 12})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{13, 14, 15})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{10, 11, 12})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{13, 14, 15})));

        final int mother_num = kb.getRelation("mother").getId();
        assertTrue(kb.hasRecord("mother", new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord("mother", new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord("mother", new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord("mother", new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{13, 15})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{13, 15})));

        final int father_num = kb.getRelation("father").getId();
        assertTrue(kb.hasRecord("father", new String[]{"bob", "catherine"}));
        assertTrue(kb.hasRecord("father", new String[]{"erick", "frederick"}));
        assertTrue(kb.hasRecord("father", new String[]{"harry", "isaac"}));
        assertTrue(kb.hasRecord("father", new String[]{"marvin", "nataly"}));
        assertTrue(kb.hasRecord(father_num, new String[]{"bob", "catherine"}));
        assertTrue(kb.hasRecord(father_num, new String[]{"erick", "frederick"}));
        assertTrue(kb.hasRecord(father_num, new String[]{"harry", "isaac"}));
        assertTrue(kb.hasRecord(father_num, new String[]{"marvin", "nataly"}));
        assertTrue(kb.hasRecord("father", new Record(new int[]{5, 6})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{8, 9})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{11, 12})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{16, 17})));
        assertTrue(kb.hasRecord(father_num, new Record(new int[]{5, 6})));
        assertTrue(kb.hasRecord(father_num, new Record(new int[]{8, 9})));
        assertTrue(kb.hasRecord(father_num, new Record(new int[]{11, 12})));
        assertTrue(kb.hasRecord(father_num, new Record(new int[]{16, 17})));
    }

    @Test
    void testWrite() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, false);
        String tmp_dir_path = testKbManager.createTmpDir();
        kb.dump(tmp_dir_path);
        NumeratedKb kb2 = new NumeratedKb(kb.getName(), tmp_dir_path, false);

        assertEquals(testKbManager.getKbName(), kb2.getName());
        assertEquals(14, kb2.totalMappings());
        assertEquals(3, kb2.totalRelations());
        assertEquals(12, kb2.totalRecords());

        final int family_num = kb2.getRelation("family").getId();
        assertTrue(kb2.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb2.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb2.hasRecord("family", new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb2.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb2.hasRecord(family_num, new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb2.hasRecord(family_num, new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb2.hasRecord(family_num, new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb2.hasRecord(family_num, new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{4, 5, 6})));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{7, 8, 9})));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{10, 11, 12})));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{13, 14, 3})));
        assertTrue(kb2.hasRecord(family_num, new Record(new int[]{4, 5, 6})));
        assertTrue(kb2.hasRecord(family_num, new Record(new int[]{7, 8, 9})));
        assertTrue(kb2.hasRecord(family_num, new Record(new int[]{10, 11, 12})));
        assertTrue(kb2.hasRecord(family_num, new Record(new int[]{13, 14, 3})));

        final int mother_num = kb2.getRelation("mother").getId();
        assertTrue(kb2.hasRecord("mother", new String[]{"alice", "catherine"}));
        assertTrue(kb2.hasRecord("mother", new String[]{"diana", "frederick"}));
        assertTrue(kb2.hasRecord("mother", new String[]{"gabby", "isaac"}));
        assertTrue(kb2.hasRecord("mother", new String[]{"jena", "lily"}));
        assertTrue(kb2.hasRecord(mother_num, new String[]{"alice", "catherine"}));
        assertTrue(kb2.hasRecord(mother_num, new String[]{"diana", "frederick"}));
        assertTrue(kb2.hasRecord(mother_num, new String[]{"gabby", "isaac"}));
        assertTrue(kb2.hasRecord(mother_num, new String[]{"jena", "lily"}));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{4, 6})));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{7, 9})));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{10, 12})));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{13, 3})));
        assertTrue(kb2.hasRecord(mother_num, new Record(new int[]{4, 6})));
        assertTrue(kb2.hasRecord(mother_num, new Record(new int[]{7, 9})));
        assertTrue(kb2.hasRecord(mother_num, new Record(new int[]{10, 12})));
        assertTrue(kb2.hasRecord(mother_num, new Record(new int[]{13, 3})));

        final int father_num = kb2.getRelation("father").getId();
        assertTrue(kb2.hasRecord("father", new String[]{"bob", "catherine"}));
        assertTrue(kb2.hasRecord("father", new String[]{"erick", "frederick"}));
        assertTrue(kb2.hasRecord("father", new String[]{"harry", "isaac"}));
        assertTrue(kb2.hasRecord("father", new String[]{"marvin", "nataly"}));
        assertTrue(kb2.hasRecord(father_num, new String[]{"bob", "catherine"}));
        assertTrue(kb2.hasRecord(father_num, new String[]{"erick", "frederick"}));
        assertTrue(kb2.hasRecord(father_num, new String[]{"harry", "isaac"}));
        assertTrue(kb2.hasRecord(father_num, new String[]{"marvin", "nataly"}));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{5, 6})));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{8, 9})));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{11, 12})));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{2, 1})));
        assertTrue(kb2.hasRecord(father_num, new Record(new int[]{5, 6})));
        assertTrue(kb2.hasRecord(father_num, new Record(new int[]{8, 9})));
        assertTrue(kb2.hasRecord(father_num, new Record(new int[]{11, 12})));
        assertTrue(kb2.hasRecord(father_num, new Record(new int[]{2, 1})));
    }

    @Test
    void testCreateRelation() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, false);
        KbRelation relation = kb.createRelation("rel", 2);
        assertEquals(14, kb.totalMappings());
        assertEquals(4, kb.totalRelations());
        assertEquals(12, kb.totalRecords());
        assertEquals("rel", relation.getName());
        assertEquals(2, relation.getArity());
        assertEquals(3, relation.getId());
        assertEquals(0, relation.totalRecords());
        assertSame(relation, kb.getRelation("rel"));
        assertSame(relation, kb.getRelation(3));

        kb.addRecord("rel2", new String[]{"a", "b", "c"});
        kb.addRecord("rel2", new Record(new int[]{4, 5, 6}));
        assertEquals(17, kb.totalMappings());
        assertEquals(5, kb.totalRelations());
        assertEquals(14, kb.totalRecords());
        assertNotNull(kb.getRelation("rel2"));
        assertNotNull(kb.getRelation(4));
        assertTrue(kb.hasRecord(4, new int[]{1, 2, 3}));
    }

    @Test
    void testLoadRelation() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR);
        KbRelation relation = new KbRelation("reflex", 3, 2);
        relation.addRecord(new Record(new int[]{4, 4}));
        relation.addRecord(new Record(new int[]{5, 5}));
        relation.addRecord(new Record(new int[]{6, 6}));
        relation.dump(TestKbManager.MEM_DIR, "reflex.rel");
        testKbManager.appendTmpFile(Paths.get(TestKbManager.MEM_DIR, "reflex.rel").toAbsolutePath().toString());

        relation = kb.loadRelation(TestKbManager.MEM_DIR, "reflex.rel", "reflex", 2, true);
        assertEquals(14, kb.totalMappings());
        assertEquals(4, kb.totalRelations());
        assertEquals(15, kb.totalRecords());
        assertEquals("reflex", relation.getName());
        assertEquals(2, relation.getArity());
        assertEquals(3, relation.getId());
        assertEquals(3, relation.totalRecords());
        assertNotNull(kb.getRelation("reflex"));
        assertNotNull(kb.getRelation(3));
        assertTrue(kb.hasRecord("reflex", new Record(new int[]{4, 4})));
        assertTrue(kb.hasRecord("reflex", new Record(new int[]{5, 5})));
        assertTrue(kb.hasRecord("reflex", new Record(new int[]{6, 6})));

        relation = new KbRelation("reflex2", 4, 2);
        relation.addRecord(new Record(new int[]{7, 7}));
        relation.addRecord(new Record(new int[]{99, 99}));
        relation.addRecord(new Record(new int[]{8, 8}));
        relation.dump(TestKbManager.MEM_DIR, "reflex2.rel");
        testKbManager.appendTmpFile(Paths.get(TestKbManager.MEM_DIR, "reflex2.rel").toAbsolutePath().toString());
        assertThrows(KbException.class, () -> kb.loadRelation(TestKbManager.MEM_DIR, "reflex2.rel", "reflex2", 2, true));
    }

    @Test
    void testDeleteRelation() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        final int father_num = kb.deleteRelation("father").getId();
        assertEquals(2, father_num);

        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(12, kb.totalMappings());
        assertEquals(2, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        final int family_num = kb.getRelation("family").getId();
        assertEquals(0, family_num);
        assertTrue(kb.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord("family", new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord(family_num, new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord("family", new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{10, 11, 12})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{13, 14, 15})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{10, 11, 12})));
        assertTrue(kb.hasRecord(family_num, new Record(new int[]{13, 14, 15})));

        final int mother_num = kb.getRelation("mother").getId();
        assertEquals(1, mother_num);
        assertTrue(kb.hasRecord("mother", new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord("mother", new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord("mother", new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord("mother", new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord(mother_num, new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{13, 15})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord(mother_num, new Record(new int[]{13, 15})));

        assertFalse(kb.hasRecord("father", new String[]{"bob", "catherine"}));
        assertFalse(kb.hasRecord("father", new String[]{"erick", "frederick"}));
        assertFalse(kb.hasRecord("father", new String[]{"harry", "isaac"}));
        assertFalse(kb.hasRecord("father", new String[]{"marvin", "nataly"}));
        assertFalse(kb.hasRecord(father_num, new String[]{"bob", "catherine"}));
        assertFalse(kb.hasRecord(father_num, new String[]{"erick", "frederick"}));
        assertFalse(kb.hasRecord(father_num, new String[]{"harry", "isaac"}));
        assertFalse(kb.hasRecord(father_num, new String[]{"marvin", "nataly"}));
        assertFalse(kb.hasRecord("father", new Record(new int[]{5, 6})));
        assertFalse(kb.hasRecord("father", new Record(new int[]{8, 9})));
        assertFalse(kb.hasRecord("father", new Record(new int[]{11, 12})));
        assertFalse(kb.hasRecord("father", new Record(new int[]{16, 17})));
        assertFalse(kb.hasRecord(father_num, new Record(new int[]{5, 6})));
        assertFalse(kb.hasRecord(father_num, new Record(new int[]{8, 9})));
        assertFalse(kb.hasRecord(father_num, new Record(new int[]{11, 12})));
        assertFalse(kb.hasRecord(father_num, new Record(new int[]{16, 17})));

        assertNull(kb.deleteRelation("father"));
    }

    @Test
    void testAddRecord() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        final int family_num = kb.getRelation("family").getId();
        kb.addRecord("family", new String[]{"o", "p", "q"});
        kb.addRecord(family_num, new String[]{"o", "o", "o"});
        assertEquals(2, kb.name2Num("p"));
        kb.addRecord("family", new int[]{2, 2, 2});
        assertEquals(3, kb.name2Num("q"));
        kb.addRecord(family_num, new int[]{3, 3, 3});

        assertEquals(17, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(16, kb.totalRecords());
        assertTrue(kb.hasRecord("family", new String[]{"o", "p", "q"}));
        assertTrue(kb.hasRecord("family", new String[]{"o", "o", "o"}));
        assertTrue(kb.hasRecord("family", new String[]{"p", "p", "p"}));
        assertTrue(kb.hasRecord("family", new String[]{"q", "q", "q"}));

        kb.addRecord("family", new String[]{"o", "o", "o"});
        assertThrows(KbException.class, () -> kb.addRecord("family", new String[]{"o"}));
        assertThrows(KbException.class, () -> kb.addRecord(family_num, new String[]{"o"}));
        assertThrows(KbException.class, () -> kb.addRecord("family", new int[]{1}));
        assertThrows(KbException.class, () -> kb.addRecord(family_num, new int[]{1}));
        assertThrows(KbException.class, () -> kb.addRecord(family_num, new int[]{200, 2, 2}));

        assertEquals(17, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(16, kb.totalRecords());
    }

    @Test
    void testAddRecords() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        final int family_num = kb.getRelation("family").getId();
        kb.addRecords("family", new String[][]{
                new String[]{"o", "o", "o"}, new String[]{"oo", "oo", "oo"}
        });
        kb.addRecords(family_num, new String[][]{
                new String[]{"p", "p", "p"}, new String[]{"pp", "pp", "pp"}
        });
        kb.addRecords(family_num, new String[][]{new String[]{"q", "q", "qq"}});
        kb.addRecords("family", new int[][]{new int[]{19, 19, 19}, new int[]{20, 20, 20}});
        kb.addRecords(family_num, new String[][]{new String[]{"r", "r", "rr"}});
        kb.addRecords(family_num, new int[][]{new int[]{21, 21, 21}, new int[]{22, 22, 22}});
        kb.removeRecord(family_num, new String[]{"q", "q", "qq"});
        kb.removeRecord(family_num, new String[]{"r", "r", "rr"});

        assertEquals(22, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(20, kb.totalRecords());
        assertTrue(kb.hasRecord("family", new String[]{"o", "o", "o"}));
        assertTrue(kb.hasRecord("family", new String[]{"oo", "oo", "oo"}));
        assertTrue(kb.hasRecord("family", new String[]{"p", "p", "p"}));
        assertTrue(kb.hasRecord("family", new String[]{"pp", "pp", "pp"}));
        assertTrue(kb.hasRecord("family", new String[]{"q", "q", "q"}));
        assertTrue(kb.hasRecord("family", new String[]{"qq", "qq", "qq"}));
        assertTrue(kb.hasRecord("family", new String[]{"r", "r", "r"}));
        assertTrue(kb.hasRecord("family", new String[]{"rr", "rr", "rr"}));

        kb.addRecords("family", new String[][]{new String[]{"o", "o", "o"}});
        assertThrows(KbException.class, () -> kb.addRecords("family", new String[][]{new String[]{"o"}}));
        assertThrows(KbException.class, () -> kb.addRecords(family_num, new String[][]{new String[]{"o"}}));
        assertThrows(KbException.class, () -> kb.addRecords("family", new int[][]{new int[]{1}}));
        assertThrows(KbException.class, () -> kb.addRecords(family_num, new int[][]{new int[]{1}}));
        assertThrows(KbException.class, () -> kb.addRecords(family_num, new int[][]{new int[]{200, 2, 2}}));

        assertEquals(22, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(20, kb.totalRecords());

        kb.addRecords("rel", new String[][]{new String[]{"o", "o"}, new String[]{"p", "p"}});
        kb.addRecords("rel2", new int[][]{new int[]{1}, new int[]{2}});

        assertEquals(22, kb.totalMappings());
        assertEquals(5, kb.totalRelations());
        assertEquals(24, kb.totalRecords());
    }

    @Test
    void testRemoveRecord() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        final int mother_num = kb.getRelation("mother").getId();
        final int father_num = kb.getRelation("father").getId();
        kb.removeRecord("mother", new String[]{"alice", "catherine"});
        kb.removeRecord(mother_num, new String[]{"diana", "frederick"});
        kb.removeRecord("father", new int[]{0xb, 0xc});
        kb.removeRecord(father_num, new int[]{0x10, 0x11});

        assertEquals(12, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        kb.removeRecord("mother", new String[]{"alice", "catherine"});
        kb.removeRecord(mother_num, new String[]{"diana", "frederick"});
        kb.removeRecord("father", new int[]{0xb, 0xc});
        kb.removeRecord(father_num, new int[]{0x10, 0x11});

        assertEquals(12, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        kb.removeRecord("rel", new String[]{"alice", "catherine"});
        kb.removeRecord(5, new String[]{"diana", "frederick"});
        kb.removeRecord("father", new int[]{0xafa, 0xc});
        kb.removeRecord(28, new int[]{0x11, 0x123});

        assertEquals(12, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(8, kb.totalRecords());
    }

    @Test
    void testTidyUp1() throws KbException, IOException {
        testWrite();
    }

    @Test
    void testTidyUp2() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        assertEquals(0, kb.getRelation("family").getId());
        assertEquals(1, kb.getRelation("mother").getId());
        assertEquals(2, kb.deleteRelation("father").getId());
        kb.tidyUp();

        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(12, kb.totalMappings());
        assertEquals(2, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        assertEquals(0, kb.getRelation("family").getId());
        assertEquals(1, kb.getRelation("mother").getId());
        assertNull(kb.getRelation("father"));
    }

    @Test
    void testTidyUp3() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        assertEquals(0, kb.getRelation("family").getId());
        assertEquals(1, kb.deleteRelation("mother").getId());
        assertEquals(2, kb.getRelation("father").getId());
        kb.tidyUp();

        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(14, kb.totalMappings());
        assertEquals(2, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        assertEquals(0, kb.getRelation("family").getId());
        assertNull(kb.getRelation("mother"));
        assertEquals(1, kb.getRelation("father").getId());
    }

    @Test
    void testTidyUp4() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        assertEquals(0, kb.deleteRelation("family").getId());
        assertEquals(1, kb.deleteRelation("mother").getId());
        assertEquals(2, kb.getRelation("father").getId());
        kb.tidyUp();

        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(8, kb.totalMappings());
        assertEquals(1, kb.totalRelations());
        assertEquals(4, kb.totalRecords());

        assertNull(kb.getRelation("family"));
        assertNull(kb.getRelation("mother"));
        assertEquals(0, kb.getRelation("father").getId());
    }
}