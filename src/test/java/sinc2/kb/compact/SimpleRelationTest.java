package sinc2.kb.compact;

import org.junit.jupiter.api.Test;
import sinc2.kb.KbRelation;
import sinc2.util.LittleEndianIntIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRelationTest {

    protected static final String TEST_DIR = "/dev/shm";

    @Test
    void testLoad() throws IOException {
        File relation_file = KbRelation.getRelFilePath(TEST_DIR, "family", 3, 4).toFile();
        FileOutputStream fos = new FileOutputStream(relation_file);
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.write(LittleEndianIntIO.leInt2ByteArray(5));
        fos.write(LittleEndianIntIO.leInt2ByteArray(6));
        fos.write(LittleEndianIntIO.leInt2ByteArray(7));
        fos.write(LittleEndianIntIO.leInt2ByteArray(8));
        fos.write(LittleEndianIntIO.leInt2ByteArray(9));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xa));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xb));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xc));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xd));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xe));
        fos.write(LittleEndianIntIO.leInt2ByteArray(0xf));
        fos.close();

        SimpleRelation relation = new SimpleRelation("family", 0, 3, 4, TEST_DIR);
        assertEquals(4, relation.totalRows());
        assertEquals(3, relation.totalCols());
        assertTrue(relation.hasRow(new int[]{4, 5, 6}));
        assertTrue(relation.hasRow(new int[]{7, 8, 9}));
        assertTrue(relation.hasRow(new int[]{10, 11, 12}));
        assertTrue(relation.hasRow(new int[]{13, 14, 15}));
    }

    @Test
    void testSetEntailment() {
        int[][] records = new int[55][];
        for (int i = 0; i < 55; i++) {
            records[i] = new int[]{i, i, i};
        }
        SimpleRelation relation = new SimpleRelation("test", 0, records);

        relation.setAsEntailed(new int[]{0, 0, 0});
        relation.setAsEntailed(new int[]{1, 1, 1});
        relation.setAsEntailed(new int[]{31, 31, 31});
        relation.setAsEntailed(new int[]{47, 47, 47});
        relation.setAsEntailed(new int[]{1, 2, 3});
        assertTrue(relation.isEntailed(new int[]{0, 0, 0}));
        assertTrue(relation.isEntailed(new int[]{1, 1, 1}));
        assertTrue(relation.isEntailed(new int[]{31, 31, 31}));
        assertTrue(relation.isEntailed(new int[]{47, 47, 47}));
        assertFalse(relation.isEntailed(new int[]{1, 2, 3}));
        assertFalse(relation.isEntailed(new int[]{3, 3, 3}));
        assertEquals(4, relation.totalEntailedRecords());

        relation.setAsNotEntailed(new int[]{31, 31, 31});
        relation.setAsNotEntailed(new int[]{47, 47, 47});
        relation.setAsNotEntailed(new int[]{4, 5, 6});
        assertTrue(relation.isEntailed(new int[]{0, 0, 0}));
        assertTrue(relation.isEntailed(new int[]{1, 1, 1}));
        assertFalse(relation.isEntailed(new int[]{31, 31, 31}));
        assertFalse(relation.isEntailed(new int[]{47, 47, 47}));
        assertFalse(relation.isEntailed(new int[]{1, 2, 3}));
        assertFalse(relation.isEntailed(new int[]{3, 3, 3}));
        assertEquals(2, relation.totalEntailedRecords());
    }

    @Test
    void testSetAllEntailment() {
        int[][] records = new int[55][];
        for (int i = 0; i < 55; i++) {
            records[i] = new int[]{i, i, i};
        }
        SimpleRelation relation = new SimpleRelation("test", 0, records);

        relation.setAllAsEntailed(new int[][] {
                new int[]{0, 0, 0},
                new int[]{1, 1, 1},
                new int[]{31, 31, 31},
                new int[]{47, 47, 47},
                new int[]{1, 2, 3}
        });
        assertTrue(relation.isEntailed(new int[]{0, 0, 0}));
        assertTrue(relation.isEntailed(new int[]{1, 1, 1}));
        assertTrue(relation.isEntailed(new int[]{31, 31, 31}));
        assertTrue(relation.isEntailed(new int[]{47, 47, 47}));
        assertFalse(relation.isEntailed(new int[]{1, 2, 3}));
        assertFalse(relation.isEntailed(new int[]{3, 3, 3}));
        assertEquals(4, relation.totalEntailedRecords());
    }
}