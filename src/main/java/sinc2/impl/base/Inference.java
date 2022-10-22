package sinc2.impl.base;

/**
 * This class is used for inferring new facts from current KB.
 *
 * Todo: Re-implement this
 *
 * @since 2.0
 */
public class Inference {

//    /**
//     * This class of rules is only used for inferring facts from current KB. Thus, the constructor with the rule structure
//     * and the "infer" methods should be used.
//     */
//    private static class InferenceRuleHandler extends CachedRule {
//
//        private final static Set<Fingerprint> fingerprints = new HashSet<>();
//        private final static Map<MultiSet<Integer>, Set<Fingerprint>> tabuMap = new HashMap<>();
//
//        public InferenceRuleHandler(List<Predicate> structure, NumeratedKb kb) {
//            /* Fingerprints and tabus are not used in this class */
//            super(structure, fingerprints, tabuMap, kb);
//        }
//
//        /**
//         * This class only updates the "allCache".
//         */
//        @Override
//        protected void constructCache() {
//            /* Find the variable locations for "posCache" and "allCache" */
//            class ConstRestriction {    // This class is used for representing constant restrictions in the rule
//                public final int argIdx;    // The argument index of the constant in a predicate
//                public final int constantArg;   // The argument of the constant
//
//                public ConstRestriction(int argIdx, int constantArg) {
//                    this.argIdx = argIdx;
//                    this.constantArg = constantArg;
//                }
//            }
//            List<ArgLocation>[] lv_id_locs_with_head = new List[usedLimitedVars()];     // LV id as the index of the array
//            List<ArgLocation>[] lv_id_locs_without_head = new List[usedLimitedVars()];  // LV id as the index
//            List<ConstRestriction>[] const_restriction_lists = new List[structure.size()];   // Predicate index as the index
//            Predicate head_pred = getHead();
//            for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
//                int argument = head_pred.args[arg_idx];
//                if (Argument.isVariable(argument)) {
//                    int id = Argument.decode(argument);
//                    if (null == lv_id_locs_with_head[id]) {
//                        lv_id_locs_with_head[id] = new ArrayList<>();
//                    }
//                    lv_id_locs_with_head[id].add(new ArgLocation(HEAD_PRED_IDX, arg_idx));
//                } else if (Argument.isConstant(argument)) {
//                    if (null == const_restriction_lists[HEAD_PRED_IDX]) {
//                        const_restriction_lists[HEAD_PRED_IDX] = new ArrayList<>();
//                    }
//                    const_restriction_lists[HEAD_PRED_IDX].add(new ConstRestriction(arg_idx, argument));
//                }
//            }
//            for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
//                Predicate body_pred = structure.get(pred_idx);
//                for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
//                    int argument = body_pred.args[arg_idx];
//                    if (Argument.isVariable(argument)) {
//                        int id = Argument.decode(argument);
//                        ArgLocation arg_loc = new ArgLocation(pred_idx, arg_idx);
//                        if (null == lv_id_locs_without_head[id]) {
//                            lv_id_locs_without_head[id] = new ArrayList<>();
//                        }
//                        lv_id_locs_without_head[id].add(arg_loc);
//                    } else if (Argument.isConstant(argument)) {
//                        if (null == const_restriction_lists[pred_idx]) {
//                            const_restriction_lists[pred_idx] = new ArrayList<>();
//                        }
//                        const_restriction_lists[pred_idx].add(new ConstRestriction(arg_idx, argument));
//                    }
//                }
//            }
//
//            /* Find PLVs */
//            for (int vid = 0; vid < lv_id_locs_without_head.length; vid++) {
//                List<ArgLocation> lv_locs = lv_id_locs_without_head[vid];
//                if (null != lv_locs && 1 == lv_locs.size()) {
//                    ArgLocation plv_loc = lv_locs.get(0);
//                    ArgLocation plv_loc_in_head = lv_id_locs_with_head[vid].get(0);
//                    plvList.set(vid, new PlvLoc(plv_loc.predIdx, plv_loc.argIdx, plv_loc_in_head.argIdx));
//                    lv_id_locs_without_head[vid] = null;
//                }
//            }
//
//            /* Construct the initial cache entry (where only constant restrictions are applied) */
//            /* If any of the compliance sets is empty, the cache entry will be NULL */
//            List<CompliedBlock> initial_cbs = new ArrayList<>();
//            List<Map<Integer, Set<Record>>[]> inital_indices_list = new ArrayList<>();
//            initial_cbs.add(null);  // take the position of the head
//            inital_indices_list.add(null);
//            for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < const_restriction_lists.length; pred_idx++) {
//                Predicate predicate = this.structure.get(pred_idx);
//                KbRelation relation = kb.getRelation(predicate.predSymbol);
//                List<ConstRestriction> const_restrictions = const_restriction_lists[pred_idx];
//                Set<Record> records_in_relation = relation.getRecords();
//                if (records_in_relation.isEmpty()) {
//                    initial_cbs = null;
//                    break;
//                }
//                if (null == const_restrictions) {
//                    initial_cbs.add(new CompliedBlock(predicate.predSymbol, new int[predicate.arity()], records_in_relation));
//                    inital_indices_list.add(relation.getArgumentIndices());
//                } else {
//                    Set<Record> records_complied_to_constants = new HashSet<>();
//                    for (Record record : records_in_relation) {
//                        boolean match_all = true;
//                        for (ConstRestriction restriction: const_restrictions) {
//                            if (restriction.constantArg != record.args[restriction.argIdx]) {
//                                match_all = false;
//                                break;
//                            }
//                        }
//                        if (match_all) {
//                            records_complied_to_constants.add(record);
//                        }
//                    }
//                    if (records_complied_to_constants.isEmpty()) {
//                        initial_cbs = null;
//                        break;
//                    }
//
//                    int[] par = new int[predicate.arity()];
//                    for (ConstRestriction restriction: const_restrictions) {
//                        par[restriction.argIdx] = restriction.constantArg;
//                    }
//                    initial_cbs.add(new CompliedBlock(predicate.predSymbol, par, records_complied_to_constants));
//                    inital_indices_list.add(null);
//                }
//            }
//            if (null == initial_cbs) {
//                return;
//            }
//
//            /* Build the caches */
//            /* Build "allCache" first */
//            CacheEntry complete_init_entry = new CacheEntry(initial_cbs, inital_indices_list);
//            CacheEntry init_entry_without_head = new CacheEntry(complete_init_entry);
//            init_entry_without_head.entry.set(HEAD_PRED_IDX, null);
//            init_entry_without_head.argIndicesList.set(HEAD_PRED_IDX, new Map[0]);
//            allCache.add(init_entry_without_head);
//            for (List<ArgLocation> argLocations : lv_id_locs_without_head) {
//                if (null != argLocations) {
//                    allCache = splitCacheEntriesByLvs(allCache, argLocations);
//                }
//            }
//
//            /* No need to construct "posCache" */
//        }
//    }
//
//    /**
//     * Infer new facts from KB w.r.t. a rule.
//     *
//     * @param rule The rule used for inference
//     * @param kb The KB
//     * @return The inferred records that are not in the KB before
//     */
//    static public Set<Record> infer(Rule rule, NumeratedKb kb) {
//        List<Predicate> structure = new ArrayList<>(rule.predicates());
//        for (int i = 0; i < rule.predicates(); i++) {
//            structure.add(new Predicate(rule.getPredicate(i)));
//        }
//        InferenceRuleHandler handler_rule = new InferenceRuleHandler(structure, kb);
//        return handler_rule.getCounterexamples();
//    }
}
