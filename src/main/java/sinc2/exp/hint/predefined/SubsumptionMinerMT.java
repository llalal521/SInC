package sinc2.exp.hint.predefined;

import sinc2.exp.hint.HinterKb;
import sinc2.kb.SimpleRelation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubsumptionMinerMT extends TemplateMinerMT {
    public SubsumptionMinerMT() {
    }

    public SubsumptionMinerMT(int threads) {
        super(threads);
    }

    @Override
    protected void matchTemplateHandler(HinterKb kb, List<MatchedRule> matchedRules) {
        SimpleRelation[] relations = kb.getRelations();
        for (int p = 0; p < relations.length; p++) {
            SimpleRelation relation_p = relations[p];
            if (2 != relation_p.totalCols()) {
                continue;
            }

            /* Match head & check validness */
            List<HinterKb.ColInfo> similar_cols = kb.inverseSimilarCols(p, 0);
            Set<Integer> rel_idxs = new HashSet<>(similar_cols.size());
            for (int i = 1; i < similar_cols.size(); i++) { // Skip the column itself
                HinterKb.ColInfo col_info = similar_cols.get(i);
                if (0 == col_info.colIdx && 2 == relations[col_info.relIdx].totalCols()) {
                    rel_idxs.add(col_info.relIdx);
                }
            }
            rel_idxs.remove(p); // p should not be the same as q
            similar_cols = kb.inverseSimilarCols(p, 1);
            Set<Integer> _rel_idxs = new HashSet<>(similar_cols.size());
            for (int i = 1; i < similar_cols.size(); i++) { // Skip the column itself
                HinterKb.ColInfo col_info = similar_cols.get(i);
                if (1 == col_info.colIdx) {
                    _rel_idxs.add(col_info.relIdx);
                }
            }
            rel_idxs.removeIf(e -> !_rel_idxs.contains(e));
            if (!rel_idxs.isEmpty()) {
                int[][] entailments = relation_p.getAllRows();
                for (int h : rel_idxs) {
                    SimpleRelation head = relations[h];
                    checkThenAdd(
                            head, entailments, matchedRules,
                            subsumptionRuleString(head.name, relation_p.name)
                    );
                }
            }
        }
    }

    protected String subsumptionRuleString(String h, String p) {
        return String.format("%s(X0,X1):-%s(X0,X1)", h, p);
    }

    @Override
    public int templateLength() {
        return 2;
    }

    @Override
    public String templateName() {
        return "Subsumption";
    }
}
