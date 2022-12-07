package sinc2.kb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.util.kb.TestKbManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleKbTest {
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
    void testLoad() throws IOException {
        SimpleKb kb = new SimpleKb(testKbManager.getKbName(), TestKbManager.MEM_DIR);
        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(3, kb.totalRelations());
        assertEquals(12, kb.totalRecords());
        assertEquals(17, kb.totalConstants());  // Here the numbers 1, 2, and 3 should be taken into consideration, even though they do not appear in the records
        kb.hasRecord("family", new int[]{4, 5, 6});
        kb.hasRecord("family", new int[]{7, 8, 9});
        kb.hasRecord("family", new int[]{10, 11, 12});
        kb.hasRecord("family", new int[]{13, 14, 15});
        kb.hasRecord("mother", new int[]{4, 6});
        kb.hasRecord("mother", new int[]{7, 9});
        kb.hasRecord("mother", new int[]{10, 12});
        kb.hasRecord("mother", new int[]{13, 15});
        kb.hasRecord("father", new int[]{5, 6});
        kb.hasRecord("father", new int[]{8, 9});
        kb.hasRecord("father", new int[]{11, 12});
        kb.hasRecord("father", new int[]{16, 17});
    }
}