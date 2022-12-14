package sinc2.sampling;

import org.junit.jupiter.api.Test;
import sinc2.common.Record;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SamplerTest {
    @Test
    void testFormatSampledKb() {
        List<Set<Record>> sampled_relations = new ArrayList<>(List.of(
           new HashSet<>(List.of(new Record(new int[]{1, 3}))),
           new HashSet<>(),
           new HashSet<>(List.of(new Record(new int[]{1, 3}), new Record(new int[]{3, 8})))
        ));
        String[] rel_names = new String[]{"ra", "rb", "rc"};
        SimpleKb expected_kb = new SimpleKb(
                "test",
                new int[][][]{
                        new int[][]{new int[]{1, 2}},
                        new int[][]{new int[]{1, 2}, new int[]{2, 3}}
                }, new String[]{"ra", "rc"}
        );
        int[] expected_map = new int[]{0, 1, 3, 8};
        SamplingInfo sampling_info = Sampler.formatSampledKb("test", sampled_relations, rel_names);
        assertArrayEquals(expected_map, sampling_info.constMap);
        assertKbEqual(expected_kb, sampling_info.sampledKb);
    }

    protected void assertKbEqual(SimpleKb expectedKb, SimpleKb actualKb) {
        assertEquals(expectedKb.getName(), actualKb.getName());
        assertEquals(expectedKb.totalRelations(), actualKb.totalRelations());
        for (int rel_idx = 0; rel_idx < expectedKb.totalRelations(); rel_idx++) {
            SimpleRelation expected_relation = expectedKb.getRelation(rel_idx);
            SimpleRelation actual_relation = actualKb.getRelation(rel_idx);
            assertEquals(expected_relation.name, actual_relation.name);
            assertEquals(expected_relation.totalRows(), actual_relation.totalRows());
            assertEquals(expected_relation.totalCols(), actual_relation.totalCols());
            for (int[] row: expected_relation) {
                assertTrue(actual_relation.hasRow(row));
            }
        }
    }
}