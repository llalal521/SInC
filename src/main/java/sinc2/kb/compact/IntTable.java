package sinc2.kb.compact;

import sinc2.kb.Record;
import sinc2.util.ArrayOperation;

import java.util.*;

/**
 * This class is for indexing a large 2D table of integers. The table is sorted according to each column. That is,
 * orderedRowsByCols[i] stores the references of the rows sorted, in ascending order, by the ith argument of each row.
 * valuesByCols[i] will be a 1D array of the values occur in the ith arguments of the rows, no duplication, sorted in
 * ascending order. The first n element (n is the number of rows in the table) in the 1D array startIdxByCols[i] stores
 * the first offset of the row in orderedRowsByCols[i] that the corresponding argument value occurs. That is, if
 * startIdxByCols[i][j]=d, startIdxByCols[i][j+1]=e, and valuesByCols[i][j]=v, that means for these rows:
 *   orderedRowsByCols[i][d-1]
 *   orderedRowsByCols[i][d]
 *   orderedRowsByCols[i][d+1]
 *   ...
 *   orderedRowsByCols[i][e-1]
 *   orderedRowsByCols[i][e]
 * the following holds:
 *   orderedRowsByCols[i][d-1][i]!=v
 *   orderedRowsByCols[i][d][i]=v
 *   orderedRowsByCols[i][d+1][i]=v
 *   ...
 *   orderedRowsByCols[i][e-1][i]=v
 *   orderedRowsByCols[i][e][i]!=v
 * We also append one more element, n, to startIdxByCols[i] indicating the end of the rows.
 *
 * Suppose the memory cost of all rows is M, the total space of this type of index will be no more than 3M. The weakness
 * of this data structure is the query time. The existence query time is about O(log n) if the values in the rows are
 * randomly distributed in at least one column. Therefore, we require that there are NO duplicated rows in the table.
 *
 * @since 2.1
 */
public class IntTable implements Iterable<int[]> {

    /** This value indicate that a certain row is not found in the table */
    public static final int NOT_FOUND = -1;

    /** Row references sorted by each column in ascending order */
    protected final int[][][] sortedRowsByCols;
    /** The index values of each column */
    protected final int[][] valuesByCols;
    /** The starting offset of each index value */
    protected final int[][] startOffsetsByCols;
    /** The column that should be used for existential queries */
    protected final int queryCol;
    /** Total rows in the table */
    protected final int totalRows;
    /** Total cols in the table */
    protected final int totalCols;

    /**
     * Creating a IntTable by an array of rows. There should NOT be any duplicated rows in the array, and all the rows
     * should be in the same length. The array should NOT be empty.
     */
    public IntTable(int[][] rows) {
        totalCols = rows[0].length;
        totalRows = rows.length;
        sortedRowsByCols = new int[totalCols][][];
        valuesByCols = new int[totalCols][];
        startOffsetsByCols = new int[totalCols][];
        int max_values = 0;
        int max_val_idx = 0;
        for (int col = totalCols - 1; col >= 0; col--) {
            /* Sort by values in the column */
            final int _col = col;
            Arrays.sort(rows, Comparator.comparingInt(e -> e[_col]));
            int[][] sorted_rows = rows.clone();
            List<Integer> values = new ArrayList<>(totalRows);
            List<Integer> start_offset = new ArrayList<>(totalRows);

            /* Find the position of each value */
            int current_val = sorted_rows[0][col];
            values.add(current_val);
            start_offset.add(0);
            for (int i = 1; i < totalRows; i++) {
                if (current_val != sorted_rows[i][col]) {
                    current_val = sorted_rows[i][col];
                    values.add(current_val);
                    start_offset.add(i);
                }
            }
            start_offset.add(totalRows);
            sortedRowsByCols[col] = sorted_rows;
            valuesByCols[col] = ArrayOperation.toArray(values);
            startOffsetsByCols[col] = ArrayOperation.toArray(start_offset);

            /* Find the column with the maximum values, and this column will be used for existence query */
            if (max_values < valuesByCols[col].length) {
                max_values = valuesByCols[col].length;
                max_val_idx = col;
            }
        }
        queryCol = max_val_idx;
    }

    /**
     * Check whether a row is in the table.
     */
    public boolean hasRow(int[] row) {
        return 0 <= whereIs(row);
    }

    /**
     * Find the offset, w.r.t. the sorted column used for queries, of the row in the table.
     * @return The offset of the row, or NOT_FOUND if the row is not in the table.
     */
    protected int whereIs(int[] row) {
        final int[] values = valuesByCols[queryCol];
        int idx = Arrays.binarySearch(values, row[queryCol]);
        if (0 > idx) {
            return NOT_FOUND;
        }
        final int[] start_offsets = startOffsetsByCols[queryCol];
        final int[][] rows = sortedRowsByCols[queryCol];
        for (int i = start_offsets[idx]; i < start_offsets[idx + 1]; i++) {
            if (Arrays.equals(row, rows[i])) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    @Override
    public Iterator<int[]> iterator() {
        return Arrays.stream(sortedRowsByCols[0]).iterator();
    }

    /**
     * Get a slice of the table where for every row r in the slice, r[col]=val.
     */
    public int[][] getSlice(int col, int val) {
        int idx = Arrays.binarySearch(valuesByCols[col], val);
        if (0 > idx) {
            return new int[0][];
        }
        final int[] start_offsets = startOffsetsByCols[col];
        int start_offset = start_offsets[idx];
        int length = start_offsets[idx + 1] - start_offset;
        int[][] slice = new int[length][];
        System.arraycopy(sortedRowsByCols[col], start_offset, slice, 0, length);
        return slice;
    }

    /**
     * Select and create a new IntTable. For every row r in the new table, r[col]=val.
     * @return The new table, or NULL if no such row in the original table.
     */
    public IntTable select(int col, int val) {
        int[][] slice = getSlice(col, val);
        if (0 == slice.length) {
            return null;
        }
        return new IntTable(slice);
    }

    static public class MatchedSubTables {
        public final List<IntTable> subTables1;
        public final List<IntTable> subTables2;

        public MatchedSubTables(List<IntTable> subTables1, List<IntTable> subTables2) {
            this.subTables1 = subTables1;
            this.subTables2 = subTables2;
        }
    }

    /**
     * Match the values of two columns in two IntTables. For each of the matched value v, derive a pair of new sub-tables
     * sub_tab1 and sub_tab2, such that: 1) for each row r1 in sub_tab1, r1 is in tab1, r1[col1]=v, and there is no row
     * r1' in tab1 but not in sub_tab1 that r1'[col]=v; 2) for each row r2 in sub_tab2, r2 is in tab2, t2[col2]=v, and
     * there is no row r2' in tab2 but not in sub_tab2 that r2'[col]=v.
     *
     * @return Two arrays of matched sub-tables. Each pair of sub-tables, subTables1[i] and subTables2[i], satisfies the
     * above restrictions.
     */
    static MatchedSubTables matchAsSubTables(IntTable tab1, int col1, IntTable tab2, int col2) {
        final int[] values1 = tab1.valuesByCols[col1];
        final int[] values2 = tab2.valuesByCols[col2];
        final int[] start_offsets1 = tab1.startOffsetsByCols[col1];
        final int[] start_offsets2 = tab2.startOffsetsByCols[col2];
        final int[][] sorted_rows1 = tab1.sortedRowsByCols[col1];
        final int[][] sorted_rows2 = tab2.sortedRowsByCols[col2];
        List<IntTable> sub_tables1 = new ArrayList<>();
        List<IntTable> sub_tables2 = new ArrayList<>();
        int idx1 = 0;
        int idx2 = 0;
        while (idx1 < values1.length && idx2 < values2.length) {
            int val1 = values1[idx1];
            int val2 = values2[idx2];
            if (val1 < val2) {
                idx1++;
            } else if (val1 > val2) {
                idx2++;
            } else {    // val1 == val2
                int start_idx1 = start_offsets1[idx1];
                int length1 = start_offsets1[idx1+1] - start_idx1;
                int[][] slice1 = new int[length1][];
                System.arraycopy(sorted_rows1, start_idx1, slice1, 0, length1);
                sub_tables1.add(new IntTable(slice1));

                int start_idx2 = start_offsets2[idx2];
                int length2 = start_offsets2[idx2+1] - start_idx2;
                int[][] slice2 = new int[length2][];
                System.arraycopy(sorted_rows2, start_idx2, slice2, 0, length2);
                sub_tables2.add(new IntTable(slice2));

                idx1++;
                idx2++;
            }
        }
        return new MatchedSubTables(sub_tables1, sub_tables2);
    }

    /**
     * Find the intersection of this table and another list of rows. The two should have the same number of columns, and
     * the list of rows should NOT contain repeated elements.
     * Note that this method WILL sort the 1D arrays in "rows" to some order.
     */
    public int[][] intersection(int[][] rows) {
        if (0 == rows.length || totalCols != rows[0].length) {
            return new int[0][];
        }
        Arrays.sort(rows, Comparator.comparingInt(r -> r[queryCol]));
        int[][] sorted_rows = sortedRowsByCols[queryCol];
        int[] values = valuesByCols[queryCol];
        int[] start_offsets = startOffsetsByCols[queryCol];
        List<int[]> results = new ArrayList<>();
        int idx = 0;
        int idx2 = 0;
        while (idx < values.length && idx2 < rows.length) {
            int val = values[idx];
            int val2 = rows[idx2][queryCol];
            if (val < val2) {
                idx++;
            } else if (val > val2) {
                idx2++;
            } else {    // val == val2
                final int[] row = rows[idx2];
                final int offset_end = start_offsets[idx+1];
                for (int offset = start_offsets[idx]; offset < offset_end; offset++) {
                    if (Arrays.equals(sorted_rows[offset], row)) {
                        results.add(row);
                        break;
                    }
                }
                idx2++;
            }
        }
        return results.toArray(new int[0][]);
    }

    /**
     * Find the intersection of this table and another. The two tables should have the same number of columns.
     */
    public int[][] intersection(IntTable another) {
        if (totalCols != another.totalCols) {
            return new int[0][];
        }
        final int target_col = (valuesByCols[queryCol].length >= another.valuesByCols[another.queryCol].length) ?
                queryCol : another.queryCol;
        int[][] sorted_rows = sortedRowsByCols[target_col];
        int[][] sorted_rows2 = another.sortedRowsByCols[target_col];
        int[] values = valuesByCols[target_col];
        int[] values2 = another.valuesByCols[target_col];
        int[] start_offsets = startOffsetsByCols[target_col];
        int[] start_offsets2 = another.startOffsetsByCols[target_col];
        List<int[]> results = new ArrayList<>();
        int idx = 0;
        int idx2 = 0;
        while (idx < values.length && idx2 < values2.length) {
            int val = values[idx];
            int val2 = values2[idx2];
            if (val < val2) {
                idx++;
            } else if (val > val2) {
                idx2++;
            } else {    // val1 == val2
                final int offset_end = start_offsets[idx+1];
                final int offset_end2 = start_offsets2[idx2+1];
                for (int offset = start_offsets[idx]; offset < offset_end; offset++) {
                    for (int offset2 = start_offsets2[idx2]; offset2 < offset_end2; offset2++) {
                        if (Arrays.equals(sorted_rows[offset], sorted_rows2[offset2])) {
                            results.add(sorted_rows[offset]);
                            break;
                        }
                    }
                }
                idx++;
                idx2++;
            }
        }
        return results.toArray(new int[0][]);
    }

    /**
     * Get the list of different values in a certain column. The returned array SHALL NOT be modified.
     */
    public int[] valuesInColumn(int col) {
        return valuesByCols[col];
    }

    /**
     * Get all the rows in the table. The returned array SHALL NOT be modified.
     */
    public int[][] getAllRows() {
        return sortedRowsByCols[0];
    }

    /**
     * Join two tables by "col1" in "tab1" and "col2" in "tab2". The semantics is the same as the following SQL query:
     * SELECT DISTINCT tab1.selectedCols1, tab2.selectedCols2
     * FROM tab1, tab2
     * WHERE tab1.col1=tab2.col2
     */
    public static int[][] join(
            IntTable tab1, int col1, int[] selectedCols1, IntTable tab2, int col2, int[] selectedCols2
    ) {
        final int result_arity = selectedCols1.length + selectedCols2.length;
        final int[] values1 = tab1.valuesByCols[col1];
        final int[] values2 = tab2.valuesByCols[col2];
        final int[] start_offsets1 = tab1.startOffsetsByCols[col1];
        final int[] start_offsets2 = tab2.startOffsetsByCols[col2];
        final int[][] sorted_rows1 = tab1.sortedRowsByCols[col1];
        final int[][] sorted_rows2 = tab2.sortedRowsByCols[col2];
        Set<Record> result_set = new HashSet<>();
        int idx1 = 0;
        int idx2 = 0;
        while (idx1 < values1.length && idx2 < values2.length) {
            int val1 = values1[idx1];
            int val2 = values2[idx2];
            if (val1 < val2) {
                idx1++;
            } else if (val1 > val2) {
                idx2++;
            } else {    // val1 == val2, note that repeated elements should be removed
                final int offset_end1 = start_offsets1[idx1+1];
                final int offset_end2 = start_offsets2[idx2+1];
                Set<Record> tab1_selected_cols = new HashSet<>();
                Set<Record> tab2_selected_cols = new HashSet<>();
                for (int offset1 = start_offsets1[idx1]; offset1 < offset_end1; offset1++) {
                    int[] row = sorted_rows1[offset1];
                    int[] selected_cols = new int[selectedCols1.length];
                    for (int i = 0; i < selected_cols.length; i++) {
                        selected_cols[i] = row[selectedCols1[i]];
                    }
                    tab1_selected_cols.add(new Record(selected_cols));
                }
                for (int offset2 = start_offsets2[idx2]; offset2 < offset_end2; offset2++) {
                    int[] row = sorted_rows2[offset2];
                    int[] selected_cols = new int[selectedCols2.length];
                    for (int i = 0; i < selected_cols.length; i++) {
                        selected_cols[i] = row[selectedCols2[i]];
                    }
                    tab2_selected_cols.add(new Record(selected_cols));
                }
                for (Record record1: tab1_selected_cols) {
                    for (Record record2: tab2_selected_cols) {
                        int[] combined = new int[result_arity];
                        System.arraycopy(record1.args, 0, combined, 0, selectedCols1.length);
                        System.arraycopy(record2.args, 0, combined, selectedCols1.length, selectedCols2.length);
                        result_set.add(new Record(combined));
                    }
                }

                idx1++;
                idx2++;
            }
        }
        int[][] results = new int[result_set.size()][];
        int idx = 0;
        for (Record record: result_set) {
            results[idx] = record.args;
            idx++;
        }
        return results;
    }


    /**
     * Join two tables by "col1" in "tab1" and "col2" in "tab2". The semantics is the same as the following SQL query:
     * SELECT DISTINCT tab1.selectedCol1, tab2.selectedCol2
     * FROM tab1, tab2
     * WHERE tab1.col1=tab2.col2
     */
    public static int[][] join(
            IntTable tab1, int col1, int selectedCol1, IntTable tab2, int col2, int selectedCol2
    ) {
        final int[] values1 = tab1.valuesByCols[col1];
        final int[] values2 = tab2.valuesByCols[col2];
        final int[] start_offsets1 = tab1.startOffsetsByCols[col1];
        final int[] start_offsets2 = tab2.startOffsetsByCols[col2];
        final int[][] sorted_rows1 = tab1.sortedRowsByCols[col1];
        final int[][] sorted_rows2 = tab2.sortedRowsByCols[col2];
        HashSet<Record> result_set = new HashSet<>();
        int idx1 = 0;
        int idx2 = 0;
        while (idx1 < values1.length && idx2 < values2.length) {
            int val1 = values1[idx1];
            int val2 = values2[idx2];
            if (val1 < val2) {
                idx1++;
            } else if (val1 > val2) {
                idx2++;
            } else {    // val1 == val2, note that repeated elements should be removed
                final int offset_end1 = start_offsets1[idx1+1];
                final int offset_end2 = start_offsets2[idx2+1];
                Set<Integer> tab1_selected_values = new HashSet<>();
                Set<Integer> tab2_selected_values = new HashSet<>();
                for (int offset1 = start_offsets1[idx1]; offset1 < offset_end1; offset1++) {
                    tab1_selected_values.add(sorted_rows1[offset1][selectedCol1]);
                }
                for (int offset2 = start_offsets2[idx2]; offset2 < offset_end2; offset2++) {
                    tab2_selected_values.add(sorted_rows2[offset2][selectedCol2]);
                }
                for (int selected_val1: tab1_selected_values) {
                    for (int selected_val2: tab2_selected_values) {
                        result_set.add(new Record(new int[]{selected_val1, selected_val2}));
                    }
                }

                idx1++;
                idx2++;
            }
        }
        int[][] results = new int[result_set.size()][];
        int idx = 0;
        for (Record record: result_set) {
            results[idx] = record.args;
            idx++;
        }
        return results;
    }

    public int totalRows() {
        return totalRows;
    }

    public int totalCols() {
        return totalCols;
    }
}
