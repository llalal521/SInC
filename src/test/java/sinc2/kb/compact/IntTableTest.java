package sinc2.kb.compact;

import org.junit.jupiter.api.Test;
import sinc2.kb.Record;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IntTableTest {
    @Test
    void testCreation() {
        int[][] rows = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        IntTable table = new IntTable(rows);

        int[][] expected_values_by_cols = new int[][] {
                new int[] {1, 2, 5},
                new int[] {2, 3, 4, 5},
                new int[] {2, 3, 9}
        };
        int[][] expected_start_offsets_by_cols = new int[][] {
                new int[] {0, 3, 4, 5},
                new int[] {0, 1, 2, 3, 5},
                new int[] {0, 1, 4, 5}
        };
        assertArrayEquals(expected_values_by_cols, table.valuesByCols);
        assertArrayEquals(expected_start_offsets_by_cols, table.startOffsetsByCols);
    }

    @Test
    void testExistenceQuery() {
        int[][] rows = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        IntTable table = new IntTable(rows);
        for (int[] row: rows) {
            assertTrue(table.hasRow(row));
        }
    }

    @Test
    void testGetSlice() {
        int[][] rows = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        IntTable table = new IntTable(rows);

        assertArrayEquals(new int[][] {new int[]{1, 2, 9}, new int[]{1, 5, 2}, new int[]{1, 5, 3}}, table.getSlice(0, 1));
        assertArrayEquals(new int[][] {new int[]{2, 4, 3}}, table.getSlice(0, 2));
        assertArrayEquals(new int[][] {new int[]{5, 3, 3}}, table.getSlice(0, 5));

        assertArrayEquals(new int[][] {new int[]{1, 2, 9}}, table.getSlice(1, 2));
        assertArrayEquals(new int[][] {new int[]{5, 3, 3}}, table.getSlice(1, 3));
        assertArrayEquals(new int[][] {new int[]{2, 4, 3}}, table.getSlice(1, 4));
//        assertArrayEquals(new int[][] {new int[]{1, 5, 2}, new int[]{1, 5, 3}}, table.getSlice(1, 5));
        int[][] expected_slice11 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {1, 5, 2}
        };
        int[][] expected_slice12 = new int[][] {
                new int[] {1, 5, 2},
                new int[] {1, 5, 3}
        };
        assertEqualToAtLeastOne(table.getSlice(1, 5), expected_slice11, expected_slice12);

        assertArrayEquals(new int[][] {new int[]{1, 5, 2}}, table.getSlice(2, 2));
        assertArrayEquals(new int[][] {new int[]{1, 2, 9}}, table.getSlice(2, 9));

        assertEquals(0, table.getSlice(0, 8).length);
        assertEquals(0, table.getSlice(0, 3).length);
        assertEquals(0, table.getSlice(0, 4).length);
        assertEquals(0, table.getSlice(1, 1).length);
        assertEquals(0, table.getSlice(2, 5).length);
        assertEquals(0, table.getSlice(2, 6).length);
    }

    @Test
    void testSelect() {
        int[][] rows = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        IntTable table = new IntTable(rows);
        rowsInTable(new int[][]{new int[]{1, 5, 3}, new int[]{1, 5, 2}, new int[]{1, 2, 9},}, table.select(0, 1));
        rowsInTable(new int[][]{new int[]{2, 4, 3}}, table.select(0, 2));
        rowsInTable(new int[][]{new int[]{5, 3, 3}}, table.select(0, 5));
        rowsInTable(new int[][]{new int[]{1, 2, 9}}, table.select(1, 2));
        rowsInTable(new int[][]{new int[]{5, 3, 3}}, table.select(1, 3));
        rowsInTable(new int[][]{new int[]{2, 4, 3}}, table.select(1, 4));
        rowsInTable(new int[][]{new int[]{1, 5, 3}, new int[]{1, 5, 2}}, table.select(1, 5));
        rowsInTable(new int[][]{new int[]{1, 5, 2}}, table.select(2, 2));
        rowsInTable(new int[][]{new int[]{2, 4, 3}, new int[]{1, 5, 3}, new int[]{2, 4, 3}}, table.select(2, 3));
        rowsInTable(new int[][]{new int[]{1, 2, 9}}, table.select(2, 9));
        assertNull(table.select(0, 8));
        assertNull(table.select(0, 3));
        assertNull(table.select(0, 4));
        assertNull(table.select(1, 1));
        assertNull(table.select(2, 5));
        assertNull(table.select(2, 6));
    }

    @Test
    void testMatch() {
        int[][] rows1 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        int[][] rows2 = new int[][] {
                new int[]{1, 1},
                new int[]{1, 2},
                new int[]{1, 3},
                new int[]{2, 2},
                new int[]{2, 3},
                new int[]{3, 3}
        };
        IntTable table1 = new IntTable(rows1);
        IntTable table2 = new IntTable(rows2);

        /* t1c0 & t2c0 */
        IntTable.MatchedSubTables matched_result = IntTable.matchAsSubTables(table1, 0, table2, 0);
        IntTable expected_sub_tab11 = new IntTable(new int[][]{
                new int[] {1, 5, 3},
                new int[] {1, 2, 9},
                new int[] {1, 5, 2},
        });
        IntTable expected_sub_tab21 = new IntTable(new int[][]{
                new int[]{1, 1},
                new int[]{1, 2},
                new int[]{1, 3},
        });
        IntTable expected_sub_tab12 = new IntTable(new int[][]{
                new int[] {2, 4, 3},
        });
        IntTable expected_sub_tab22 = new IntTable(new int[][]{
                new int[]{2, 2},
                new int[]{2, 3},
        });
        assertEquals(2, matched_result.subTables1.size());
        assertEquals(2, matched_result.subTables2.size());
        for (int i = 0; i < matched_result.subTables1.size(); i++) {
            IntTable actual_sub_tab1 = matched_result.subTables1.get(i);
            IntTable actual_sub_tab2 = matched_result.subTables2.get(i);
            int matched_value = actual_sub_tab1.iterator().next()[0];
            switch (matched_value) {
                case 1:
                    tableEqual(expected_sub_tab11, actual_sub_tab1);
                    tableEqual(expected_sub_tab21, actual_sub_tab2);
                    break;
                case 2:
                    tableEqual(expected_sub_tab12, actual_sub_tab1);
                    tableEqual(expected_sub_tab22, actual_sub_tab2);
                    break;
                default:
                    fail();
            }
        }

        /* t1c2 & t2c1 */
        matched_result = IntTable.matchAsSubTables(table1, 2, table2, 1);
        expected_sub_tab12 = new IntTable(new int[][]{
                new int[] {1, 5, 2},
        });
        expected_sub_tab22 = new IntTable(new int[][]{
                new int[]{2, 2},
                new int[]{1, 2},
        });
        IntTable expected_sub_tab13 = new IntTable(new int[][]{
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {5, 3, 3},
        });
        IntTable expected_sub_tab23 = new IntTable(new int[][]{
                new int[]{1, 3},
                new int[]{2, 3},
                new int[]{3, 3},
        });
        assertEquals(2, matched_result.subTables1.size());
        assertEquals(2, matched_result.subTables2.size());
        for (int i = 0; i < matched_result.subTables1.size(); i++) {
            IntTable actual_sub_tab1 = matched_result.subTables1.get(i);
            IntTable actual_sub_tab2 = matched_result.subTables2.get(i);
            int matched_value = actual_sub_tab1.iterator().next()[2];
            switch (matched_value) {
                case 2:
                    tableEqual(expected_sub_tab12, actual_sub_tab1);
                    tableEqual(expected_sub_tab22, actual_sub_tab2);
                    break;
                case 3:
                    tableEqual(expected_sub_tab13, actual_sub_tab1);
                    tableEqual(expected_sub_tab23, actual_sub_tab2);
                    break;
                default:
                    fail();
            }
        }
    }

    @Test
    void testIntersectionWithRows() {
        int[][] rows1 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        int[][] rows2 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
                new int[] {1, 1, 1},
                new int[] {2, 2, 2},
                new int[] {3, 3, 3},
        };
        int[][] expected_intersection = new int[][] {
                new int[] {1, 5, 3},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };

        IntTable table1 = new IntTable(rows1);
        int[][] actual_intersection1 = table1.intersection(rows2);
        intArrayElementsEqual(expected_intersection, actual_intersection1);

        IntTable table2 = new IntTable(rows2);
        int[][] actual_intersection2 = table2.intersection(rows1);
        intArrayElementsEqual(expected_intersection, actual_intersection2);
    }

    @Test
    void testIntersectionWithTable() {
        int[][] rows1 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        int[][] rows2 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
                new int[] {1, 1, 1},
                new int[] {2, 2, 2},
                new int[] {3, 3, 3},
        };
        IntTable table1 = new IntTable(rows1);
        IntTable table2 = new IntTable(rows2);
        int[][] expected_intersection = new int[][] {
                new int[] {1, 5, 3},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };

        int[][] actual_intersection1 = table1.intersection(table2);
        intArrayElementsEqual(expected_intersection, actual_intersection1);

        int[][] actual_intersection2 = table2.intersection(table1);
        intArrayElementsEqual(expected_intersection, actual_intersection2);
    }

    @Test
    void testJoin1() {
        int[][] rows1 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        int[][] rows2 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
                new int[] {1, 1, 1},
                new int[] {2, 2, 2},
                new int[] {3, 3, 3},
        };
        int[][] expected_join = new int[][] {
                new int[] {1, 9, 2, 2},
                new int[] {5, 3, 5, 3},
                new int[] {5, 3, 3, 3},
                new int[] {1, 3, 1, 3},
                new int[] {1, 2, 1, 3},
                new int[] {1, 3, 1, 2},
                new int[] {1, 2, 1, 2},
        };
        IntTable table1 = new IntTable(rows1);
        IntTable table2 = new IntTable(rows2);
        intArrayElementsEqual(expected_join, IntTable.join(table1, 1, new int[]{0, 2}, table2, 1, new int[]{0, 2}));
    }

    @Test
    void testJoin2() {
        int[][] rows1 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {2, 4, 3},
                new int[] {1, 2, 9},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
        };
        int[][] rows2 = new int[][] {
                new int[] {1, 5, 3},
                new int[] {5, 3, 3},
                new int[] {1, 5, 2},
                new int[] {1, 1, 1},
                new int[] {2, 2, 2},
                new int[] {3, 3, 3},
        };
        int[][] expected_join = new int[][] {
                new int[] {1, 2},
                new int[] {5, 5},
                new int[] {5, 3},
                new int[] {1, 1}
        };
        IntTable table1 = new IntTable(rows1);
        IntTable table2 = new IntTable(rows2);
        intArrayElementsEqual(expected_join, IntTable.join(table1, 1, 0, table2, 1, 0));
    }

    protected void assertEqualToAtLeastOne(Object[] actual, Object[]... expected) {
        boolean equal_found = false;
        for (Object[] expected_arr: expected) {
            if (Arrays.deepEquals(expected_arr, actual)) {
                equal_found = true;
                break;
            }
        }
        assertTrue(equal_found);
    }

    protected void rowsInTable(int[][] rows, IntTable table) {
        for (int[] row: rows) {
            assertTrue(table.hasRow(row));
        }
    }

    protected void tableEqual(IntTable table1, IntTable table2) {
        assertEquals(table1.totalRows, table2.totalRows);
        assertEquals(table1.totalCols, table2.totalCols);
        for (int[] row: table1) {
            assertTrue(table2.hasRow(row));
        }
        for (int[] row: table2) {
            assertTrue(table1.hasRow(row));
        }
    }

    protected void intArrayElementsEqual(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        Set<Record> expected_set = new HashSet<>();
        Set<Record> actual_set = new HashSet<>();
        for (int[] arr: expected) {
            expected_set.add(new Record(arr));
        }
        for (int[] arr: actual) {
            actual_set.add(new Record(arr));
        }
        assertEquals(expected_set, actual_set);
    }
}