package sinc2.util.kb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.common.Record;
import sinc2.kb.KbException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Note: The testing integers are very small. So the encoding procedures by "Argument.constant()" is omitted.
 */
class KbRelationTest {

    static TestKbManager testKbManager;

    @BeforeAll
    static void setupKb() throws IOException {
        testKbManager = new TestKbManager();
    }

    @AfterAll
    static void removeKb() {
        testKbManager.cleanUpKb();
    }

    void checkRecordSet(Set<Record> expected, KbRelation relation) {
        assertEquals(expected.size(), relation.totalRecords());
        assertEquals(expected, relation.getRecords());
        for (Record record: relation) {
            assertTrue(expected.contains(record));
        }
        for (Record record: expected) {
            assertTrue(relation.hasRecord(record));
        }
    }

    @Test
    void testCreateEmpty() throws KbException {
        KbRelation relation = new KbRelation("relation", 1, 4);

        assertEquals("relation", relation.getName());
        assertEquals(4, relation.getArity());
        assertEquals(0, relation.totalRecords());
        assertEquals(1, relation.getId());
        assertEquals(new HashSet<Record>(), relation.getRecords());

        relation.addRecord(new Record(new int[]{1, 2, 3, 4}));
        relation.addRecords(new Record[]{new Record(new int[]{5, 6, 7, 8}), new Record(new int[]{11, 22, 33, 44})});
        relation.addRecords(List.of(new Record(new int[]{55, 66, 77, 88}), new Record(new int[]{555, 666, 777, 888})));
        relation.addRecord(new Record(new int[]{5, 6, 7, 8}));

        assertEquals(4, relation.getArity());
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{1, 2, 3, 4}), new Record(new int[]{5, 6, 7, 8}), new Record(new int[]{11, 22, 33, 44}),
                new Record(new int[]{55, 66, 77, 88}), new Record(new int[]{555, 666, 777, 888})
        )), relation);

        relation.addRecord(new Record(new int[]{111, 222, 333, 444}));
        relation.removeRecord(new Record(new int[]{5, 6, 7, 8}));
        relation.removeRecord(new Record(new int[]{111, 222, 333, 444}));

        assertEquals(4, relation.getArity());
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{1, 2, 3, 4}), new Record(new int[]{11, 22, 33, 44}),
                new Record(new int[]{55, 66, 77, 88}), new Record(new int[]{555, 666, 777, 888})
        )), relation);
    }

    @Test
    void testRead1() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "family", 0, 3, "0.rel", testKbManager.getKbPath(), null
        );

        assertEquals("family", relation.getName());
        assertEquals(0, relation.getId());
        assertEquals(3, relation.getArity());
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}),
                new Record(new int[]{0xd, 0xe, 0xf})
        )), relation);
    }

    @Test
    void testRead2() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "mother", 1, 2, "1.rel", testKbManager.getKbPath(), null
        );

        assertEquals("mother", relation.getName());
        assertEquals(1, relation.getId());
        assertEquals(2, relation.getArity());
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 6}), new Record(new int[]{7, 9}), new Record(new int[]{0xa, 0xc}),
                new Record(new int[]{0xd, 0xf})
        )), relation);
    }

    @Test
    void testRead3() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "father", 2, 2, "2.rel", testKbManager.getKbPath(), null
        );

        assertEquals("father", relation.getName());
        assertEquals(2, relation.getId());
        assertEquals(2, relation.getArity());
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{5, 6}), new Record(new int[]{8, 9}), new Record(new int[]{0xb, 0xc}),
                new Record(new int[]{0x10, 0x11})
        )), relation);
    }

    @Test
    void testWrite1() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "family", 0, 3, "0.rel", testKbManager.getKbPath(), null
        );
        String tmp_dir_path = testKbManager.createTmpDir();
        relation.dump(tmp_dir_path, "0.rel");
        KbRelation relation2 = new KbRelation(
                "family", 0, 3, "0.rel", tmp_dir_path, null
        );

        assertEquals("family", relation2.getName());
        assertEquals(0, relation.getId());
        assertEquals(3, relation2.getArity());
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}),
                new Record(new int[]{0xd, 0xe, 0xf})
        )), relation2);
    }

    @Test
    void testWrite2() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "family", 0, 3, "0.rel", testKbManager.getKbPath(), null
        );
        relation.dump(testKbManager.getKbPath(), "family.ceg");
        KbRelation relation2 = new KbRelation(
                "family", 0, 3, "family.ceg", testKbManager.getKbPath(), null
        );

        assertEquals("family", relation2.getName());
        assertEquals(0, relation.getId());
        assertEquals(3, relation2.getArity());
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}),
                new Record(new int[]{0xd, 0xe, 0xf})
        )), relation2);
    }

    @Test
    void testAddRecord() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "family", 0, 3, "0.rel", testKbManager.getKbPath(), null
        );
        relation.addRecord(new Record(new int[]{4, 4, 4}));
        relation.addRecord(new Record(new int[]{5, 5, 5}));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}),
                new Record(new int[]{0xd, 0xe, 0xf}),
                new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5})
        )), relation);

        relation.addRecord(new Record(new int[]{4, 5, 6}));
        assertThrows(KbException.class, () -> relation.addRecord(new Record(new int[]{4, 4})));
        assertThrows(KbException.class, () -> relation.addRecord(new Record(new int[]{4, 4, 5, 5})));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}),
                new Record(new int[]{0xd, 0xe, 0xf}),
                new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5})
        )), relation);
    }

    @Test
    void testAddRecords() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "family", 0, 3, "0.rel", testKbManager.getKbPath(), null
        );
        relation.addRecords(List.of(new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5})));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}), new Record(new int[]{0xd, 0xe, 0xf}),
                new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5})
        )), relation);

        relation.addRecord(new Record(new int[]{4, 5, 6}));
        assertThrows(KbException.class, () -> relation.addRecords(new Record[]{new Record(new int[]{4, 4}), new Record(new int[]{6, 6, 6})}));
        assertThrows(KbException.class, () -> relation.addRecords(List.of(new Record(new int[]{4, 4, 5, 5}))));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}), new Record(new int[]{0xd, 0xe, 0xf}),
                new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5})
        )), relation);
    }

    @Test
    void testRemoveRecord() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "family", 0, 3, "0.rel", testKbManager.getKbPath(), null
        );
        relation.removeRecord(new Record(new int[]{4, 4, 4}));
        relation.removeRecord(new Record(new int[]{4, 7, 10}));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}), new Record(new int[]{0xd, 0xe, 0xf})
        )), relation);

        relation.removeRecord(new Record(new int[]{4, 5, 6}));
        relation.removeRecord(new Record(new int[]{0xa, 0xb, 0xc}));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{7, 8, 9}), new Record(new int[]{0xd, 0xe, 0xf})
        )), relation);

        relation.removeRecord(new Record(new int[]{4, 5, 6}));
        relation.removeRecord(new Record(new int[]{0xa, 0xb, 0xc}));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{7, 8, 9}), new Record(new int[]{0xd, 0xe, 0xf})
        )), relation);

        relation.removeRecord(new Record(new int[]{7, 8, 9}));
        relation.removeRecord(new Record(new int[]{0xd, 0xe, 0xf}));
        checkRecordSet(new HashSet<>(), relation);
    }

    @Test
    void testAddWithRemoveRecord() throws IOException, KbException {
        KbRelation relation = new KbRelation(
                "family", 0, 3, "0.rel", testKbManager.getKbPath(), null
        );
        relation.removeRecord(new Record(new int[]{4, 4, 4}));
        relation.removeRecord(new Record(new int[]{4, 7, 10}));
        relation.addRecord(new Record(new int[]{4, 4, 4}));
        relation.addRecord(new Record(new int[]{5, 5, 5}));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{4, 5, 6}), new Record(new int[]{7, 8, 9}), new Record(new int[]{0xa, 0xb, 0xc}), new Record(new int[]{0xd, 0xe, 0xf}),
                new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5})
        )), relation);

        relation.removeRecord(new Record(new int[]{4, 5, 6}));
        relation.removeRecord(new Record(new int[]{0xa, 0xb, 0xc}));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{7, 8, 9}), new Record(new int[]{0xd, 0xe, 0xf}), new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5})
        )), relation);

        relation.addRecord(new Record(new int[]{4, 5, 6}));
        assertThrows(KbException.class, () -> relation.addRecord(new Record(new int[]{4, 4})));
        assertThrows(KbException.class, () -> relation.addRecord(new Record(new int[]{4, 4, 5, 5})));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{7, 8, 9}), new Record(new int[]{0xd, 0xe, 0xf}), new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5}), new Record(new int[]{4, 5, 6})
        )), relation);

        relation.addRecords(new Record[]{
                new Record(new int[]{4, 5, 6}), new Record(new int[]{4, 5, 6}), new Record(new int[]{4, 5, 6}), new Record(new int[]{6, 6, 6})
        });
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{7, 8, 9}), new Record(new int[]{0xd, 0xe, 0xf}), new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5}),
                new Record(new int[]{4, 5, 6}), new Record(new int[]{6, 6, 6})
        )), relation);

        relation.removeRecord(new Record(new int[]{4, 5, 6}));
        relation.removeRecord(new Record(new int[]{0xa, 0xb, 0xc}));
        checkRecordSet(new HashSet<>(List.of(
                new Record(new int[]{7, 8, 9}), new Record(new int[]{0xd, 0xe, 0xf}), new Record(new int[]{4, 4, 4}), new Record(new int[]{5, 5, 5}), new Record(new int[]{6, 6, 6})
        )), relation);
    }
}