package sinc2.exp.hint;

import sinc2.kb.IntTable;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HinterKb extends SimpleKb {
    protected final double similarityThreshold;
    protected ColInfo[][] colInfos;

    public static class ColInfo {
        public final int relIdx;
        public final int colIdx;
        public final int minVal;
        public final int maxVal;
        public final List<ColInfo> similarCols = new ArrayList<>();
        public final List<ColInfo> inverseSimilarCols = new ArrayList<>();

        public ColInfo(int relIdx, int colIdx, int minVal, int maxVal) {
            this.relIdx = relIdx;
            this.colIdx = colIdx;
            this.minVal = minVal;
            this.maxVal = maxVal;
        }
    }

    public HinterKb(String name, String basePath, double similarityThreshold) throws IOException {
        super(name, basePath);
        this.similarityThreshold = similarityThreshold;
        buildColumnSimilarities();
    }

    protected void buildColumnSimilarities() {
        /* Calculate & sort value intervals of columns */
        long time_start = System.currentTimeMillis();
        List<ColInfo> col_infos = new ArrayList<>();
        colInfos = new ColInfo[relations.length][];
        for (int rel_idx = 0; rel_idx < relations.length; rel_idx++) {
            SimpleRelation relation = relations[rel_idx];
            ColInfo[] _col_infos = new ColInfo[relation.totalCols()];
            colInfos[rel_idx] = _col_infos;
            for (int col_idx = 0; col_idx < relation.totalCols(); col_idx++) {
                ColInfo col_info = new ColInfo(rel_idx, col_idx, relation.minValue(col_idx), relation.maxValue(col_idx));
                col_info.similarCols.add(col_info);
                col_info.inverseSimilarCols.add(col_info);
                col_infos.add(col_info);
                _col_infos[col_idx] = col_info;
            }
        }
        col_infos.sort((colInfo1, colInfo2) -> {
            int diff = colInfo1.minVal - colInfo2.minVal;
            return (diff != 0) ? diff : (colInfo1.maxVal - colInfo2.maxVal);
        });

        /* Find the column pairs that the similarities are larger than the threshold */
        for (int i = 0; i < col_infos.size(); i++) {
            ColInfo col_info_i = col_infos.get(i);
            SimpleRelation relation_i = relations[col_info_i.relIdx];
            for (int j = i + 1; j < col_infos.size(); j++) {
                ColInfo col_info_j = col_infos.get(j);
                SimpleRelation relation_j = relations[col_info_j.relIdx];
                if (col_info_i.maxVal < col_info_j.minVal) {
                    break;
                }
                IntTable.SimInfo sim_info = IntTable.columnSimilarity(relation_i, col_info_i.colIdx, relation_j, col_info_j.colIdx);
                if (sim_info.simIJ >= similarityThreshold) {
                    col_info_i.similarCols.add(col_info_j);
                    col_info_j.inverseSimilarCols.add(col_info_i);
                }
                if (sim_info.simJI > similarityThreshold) {
                    col_info_j.similarCols.add(col_info_i);
                    col_info_i.inverseSimilarCols.add(col_info_j);
                }
            }
        }
        int total_similar_pairs = 0;
        for (ColInfo col_info: col_infos) {
            total_similar_pairs += col_info.similarCols.size();
        }
        long time_done = System.currentTimeMillis();
        System.out.printf(
                "Similarity indices built (total columns: %d, total pairs: %d). Time cost: %d ms\n",
                col_infos.size(), total_similar_pairs, time_done - time_start
        );
    }

    /**
     * Return a list of columns each of which covers a number of elements in the given column and the number is larger 
     * than the coverage threshold set in the KB.
     *
     * @param relIdx The index of the relation
     * @param colIdx The index of the column in the relation
     */
    public List<ColInfo> similarCols(int relIdx, int colIdx) {
        return colInfos[relIdx][colIdx].similarCols;
    }

    /**
     * Return a list of columns, and in each of the columns, the given column covers a number of elements and the number
     * is larger than the coverage threshold set in the KB.
     *
     * @param relIdx The index of the relation
     * @param colIdx The index of the column in the relation
     */
    public List<ColInfo> inverseSimilarCols(int relIdx, int colIdx) {
        return colInfos[relIdx][colIdx].inverseSimilarCols;
    }
}
