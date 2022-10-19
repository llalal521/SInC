package sinc2.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;
import sinc2.util.LittleEndianIntIO;
import sinc2.util.kb.KbRelation;
import sinc2.util.kb.NumerationMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PredicateTest {

    static final String TEST_DIR = "/dev/shm";
    static final String KB_NAME = UUID.randomUUID().toString();
    static NumerationMap map = new NumerationMap();
    static SimpleKb kb;

    @BeforeAll
    static void setup() throws IOException {
        assertEquals(1, map.mapName("family"));
        assertEquals(2, map.mapName("alice"));
        assertEquals(3, map.mapName("bob"));
        assertEquals(4, map.mapName("catherine"));

        File kb_dir = Paths.get(TEST_DIR, KB_NAME).toFile();
        assertTrue(kb_dir.exists() || kb_dir.mkdirs());

        FileOutputStream fos = new FileOutputStream(KbRelation.getRelFilePath(kb_dir.getAbsolutePath(), "family", 3, 2).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(1));
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(3));
        fos.write(LittleEndianIntIO.leInt2ByteArray(1));
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.close();
        fos = new FileOutputStream(KbRelation.getRelFilePath(kb_dir.getAbsolutePath(), "father", 2, 2).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(3));
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.write(LittleEndianIntIO.leInt2ByteArray(6));
        fos.close();
        fos = new FileOutputStream(KbRelation.getRelFilePath(kb_dir.getAbsolutePath(), "mother", 2, 1).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(1));
        fos.write(LittleEndianIntIO.leInt2ByteArray(3));
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.write(LittleEndianIntIO.leInt2ByteArray(4));
        fos.close();
        fos = new FileOutputStream(KbRelation.getRelFilePath(kb_dir.getAbsolutePath(), "isMale", 1, 1).toFile());
        fos.write(LittleEndianIntIO.leInt2ByteArray(2));
        fos.close();

        /* KB:
         * family       father  mother  isMale
         * 1    2   3   2   3   1   3   2
         * 1    2   4   4   6   2   4
         */
        kb = new SimpleKb(KB_NAME, TEST_DIR);
    }

    @Test
    void testEquality() {
        Predicate p1 = new Predicate(1, 3);
        p1.args[0] = Argument.variable(1);
        p1.args[2] = Argument.constant(2);

        Predicate p2 = new Predicate(1, 3);
        p2.args[0] = Argument.variable(1);
        p2.args[2] = Argument.constant(2);

        assertEquals(p2, p1);

        Set<Predicate> set = new HashSet<>();
        set.add(p2);
        assertFalse(set.add(p1));
    }

    @Test
    void testStringifyWithMap() {
        Predicate p = new Predicate(1, 7);
        p.args[0] = Argument.constant(2);
        p.args[1] = Argument.constant(3);
        p.args[2] = Argument.constant(4);
        p.args[3] = Argument.variable(0);
        p.args[4] = Argument.variable(3);
        p.args[5] = Argument.EMPTY_VALUE;

        assertEquals("family(alice,bob,catherine,X0,X3,?,?)", p.toString(map));
    }

    @Test
    void testStringfyWithKb() {
        SimpleRelation rel_family = kb.getRelation("family");
        assertNotNull(rel_family);
        Predicate p = new Predicate(rel_family.id, 7);
        p.args[0] = Argument.constant(2);
        p.args[1] = Argument.constant(3);
        p.args[2] = Argument.constant(4);
        p.args[3] = Argument.variable(0);
        p.args[4] = Argument.variable(3);
        p.args[5] = Argument.EMPTY_VALUE;
        assertEquals("family(2,3,4,X0,X3,?,?)", p.toString(kb));
    }
}