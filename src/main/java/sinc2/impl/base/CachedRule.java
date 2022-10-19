package sinc2.impl.base;

import sinc2.common.ArgLocation;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.common.Record;
import sinc2.kb.IntTable;
import sinc2.kb.SimpleKb;
import sinc2.kb.SimpleRelation;
import sinc2.rule.*;
import sinc2.util.MultiSet;

import java.util.*;

/**
 * First-order Horn rule with the compact grounding cache (CGC).
 *
 * Note: The caches (i.e., the lists of cache entries) shall not be modified. Any modification of the cache should follow
 * the copy-on-write strategy, i.e., replace the lists directly with new ones.
 *
 * @since 1.0
 */
public class CachedRule extends Rule {

    /**
     * This class is used for representing the link from a body GV to all GVs in the head.
     *
     * @since 1.0
     */
    static protected class BodyGvLinkInfo {
        /** The predicate index of the GV in the body */
        final int bodyPredIdx;
        /** The argument index of the GV in the body */
        final int bodyArgIdx;
        /** The argument indices of the GVs in the head */
        final Integer[] headVarLocs;

        public BodyGvLinkInfo(int bodyPredIdx, int bodyArgIdx, Integer[] headVarLocs) {
            this.bodyPredIdx = bodyPredIdx;
            this.bodyArgIdx = bodyArgIdx;
            this.headVarLocs = headVarLocs;
        }
    }

    /** The original KB */
    protected final SimpleKb kb;
    /** The cache for the positive entailments (E+-cache) */
    protected List<List<CompliedBlock>> posCache;
    /** The cache for all the entailments (E-cache). the first element should be null to keep the same length as the
     *  E+-cache, as the head should be removed in E-cache */
    protected List<List<CompliedBlock>> allCache;
    /** The list of a PLV in the body. This list should always be of the same length as "limitedVarCnts" */
    protected final List<PlvLoc> plvList = new ArrayList<>();

    /**
     * Initialize the most general rule.
     *
     * @param headRelNum The functor of the head predicate, i.e., the target relation.
     * @param arity The arity of the functor
     * @param fingerprintCache The cache of the used fingerprints
     * @param category2TabuSetMap The tabu set of pruned fingerprints
     * @param kb The original KB
     */
    public CachedRule(
            int headRelNum, int arity, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap, SimpleKb kb
    ) {
        super(headRelNum, arity, fingerprintCache, category2TabuSetMap);
        this.kb = kb;

        /* Initialize the E+-cache */
        SimpleRelation head_relation = kb.getRelation(headRelNum);
        final CompliedBlock cb_head = new CompliedBlock(headRelNum, new int[arity], head_relation.getAllRows(), head_relation);
        final List<CompliedBlock> pos_init_entry = new ArrayList<>();
        pos_init_entry.add(cb_head);
        posCache = new ArrayList<>();
        posCache.add(pos_init_entry);

        /* Initialize the E-cache */
        final List<CompliedBlock> all_init_entry = new ArrayList<>();
        all_init_entry.add(null);  // Keep the same length of the cache entries
        allCache = new ArrayList<>();
        allCache.add(all_init_entry);

        this.eval = calculateEval();
    }

    /**
     * Initialize a cached rule from a list of predicate.
     *
     * @param structure The structure of the rule.
     * @param category2TabuSetMap The tabu set of pruned fingerprints
     * @param kb The original KB
     */
    public CachedRule(
            List<Predicate> structure, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap, SimpleKb kb
    ) {
        super(structure, fingerprintCache, category2TabuSetMap);
        this.kb = kb;
        this.posCache = new ArrayList<>();
        this.allCache = new ArrayList<>();
        for (int i = 0; i < usedLimitedVars(); i++) {
            plvList.add(null);
        }
        constructCache();
        this.eval = calculateEval();
    }

    protected void constructCache() {
        /* Find the variable locations for "posCache" and "allCache" */
        class ConstRestriction {    // This class is used for representing constant restrictions in the rule
            public final int argIdx;    // The argument index of the constant in a predicate
            public final int constantArg;   // The argument of the constant

            public ConstRestriction(int argIdx, int constantArg) {
                this.argIdx = argIdx;
                this.constantArg = constantArg;
            }
        }
        List<ArgLocation>[] lv_id_locs_with_head = new List[usedLimitedVars()];     // LV id as the index of the array
        List<ArgLocation>[] lv_id_locs_without_head = new List[usedLimitedVars()];  // LV id as the index
        List<ConstRestriction>[] const_restriction_lists = new List[structure.size()];   // Predicate index as the index
        Predicate head_pred = getHead();
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            int argument = head_pred.args[arg_idx];
            if (Argument.isVariable(argument)) {
                int id = Argument.decode(argument);
                if (null == lv_id_locs_with_head[id]) {
                    lv_id_locs_with_head[id] = new ArrayList<>();
                }
                lv_id_locs_with_head[id].add(new ArgLocation(HEAD_PRED_IDX, arg_idx));
            } else if (Argument.isConstant(argument)) {
                if (null == const_restriction_lists[HEAD_PRED_IDX]) {
                    const_restriction_lists[HEAD_PRED_IDX] = new ArrayList<>();
                }
                const_restriction_lists[HEAD_PRED_IDX].add(new ConstRestriction(arg_idx, Argument.decode(argument)));
            }
        }
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            Predicate body_pred = structure.get(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                int argument = body_pred.args[arg_idx];
                if (Argument.isVariable(argument)) {
                    int id = Argument.decode(argument);
                    ArgLocation arg_loc = new ArgLocation(pred_idx, arg_idx);
                    if (null == lv_id_locs_with_head[id]) {
                        lv_id_locs_with_head[id] = new ArrayList<>();
                    }
                    lv_id_locs_with_head[id].add(arg_loc);
                    if (null == lv_id_locs_without_head[id]) {
                        lv_id_locs_without_head[id] = new ArrayList<>();
                    }
                    lv_id_locs_without_head[id].add(arg_loc);
                } else if (Argument.isConstant(argument)) {
                    if (null == const_restriction_lists[pred_idx]) {
                        const_restriction_lists[pred_idx] = new ArrayList<>();
                    }
                    const_restriction_lists[pred_idx].add(new ConstRestriction(arg_idx, Argument.decode(argument)));
                }
            }
        }

        /* Find PLVs */
        for (int vid = 0; vid < lv_id_locs_without_head.length; vid++) {
            List<ArgLocation> lv_locs = lv_id_locs_without_head[vid];
            if (null != lv_locs && 1 == lv_locs.size()) {
                ArgLocation plv_loc = lv_locs.get(0);
                ArgLocation plv_loc_in_head = lv_id_locs_with_head[vid].get(0);
                plvList.set(vid, new PlvLoc(plv_loc.predIdx, plv_loc.argIdx, plv_loc_in_head.argIdx));
                lv_id_locs_without_head[vid] = null;
            }
        }

        /* Construct the initial cache entry (where only constant restrictions are applied) */
        /* If any of the compliance sets is empty, the cache entry will be NULL */
        List<CompliedBlock> initial_entry = new ArrayList<>();
        for (int pred_idx = HEAD_PRED_IDX; pred_idx < const_restriction_lists.length; pred_idx++) {
            Predicate predicate = this.structure.get(pred_idx);
            SimpleRelation relation = kb.getRelation(predicate.predSymbol);
            List<ConstRestriction> const_restrictions = const_restriction_lists[pred_idx];
            int[][] records_in_relation = relation.getAllRows();
            if (0 == records_in_relation.length) {
                initial_entry = null;
                break;
            }
            if (null == const_restrictions) {
                initial_entry.add(new CompliedBlock(predicate.predSymbol, new int[predicate.arity()], records_in_relation, relation));
            } else {
                List<int[]> records_complied_to_constants = new ArrayList<>();
                for (int[] record : records_in_relation) {
                    boolean match_all = true;
                    for (ConstRestriction restriction: const_restrictions) {
                        if (restriction.constantArg != record[restriction.argIdx]) {
                            match_all = false;
                            break;
                        }
                    }
                    if (match_all) {
                        records_complied_to_constants.add(record);
                    }
                }
                if (records_complied_to_constants.isEmpty()) {
                    initial_entry = null;
                    break;
                }

                int[] par = new int[predicate.arity()];
                for (ConstRestriction restriction: const_restrictions) {
                    par[restriction.argIdx] = restriction.constantArg;
                }
                initial_entry.add(new CompliedBlock(predicate.predSymbol, par, records_complied_to_constants.toArray(new int[0][])));
            }
        }
        if (null == initial_entry) {
            return;
        }

        /* Build the caches */
        /* Build "allCache" first */
        List<CompliedBlock> init_entry_without_head = new ArrayList<>(initial_entry);
        init_entry_without_head.set(HEAD_PRED_IDX, null);
        allCache.add(init_entry_without_head);
        for (List<ArgLocation> arg_locations : lv_id_locs_without_head) {
            if (null != arg_locations) {
                allCache = splitCacheEntriesByLvs(allCache, arg_locations);
            }
        }

        /* Build "posCache" */
        /* Todo: the "posCache" can be built based on the result of "allCache" but the implementation is too complicated. Optimize in a future enhancement. */
        posCache.add(initial_entry);
        for (List<ArgLocation> arg_locations : lv_id_locs_with_head) {
            if (null != arg_locations) {
                posCache = splitCacheEntriesByLvs(posCache, arg_locations);
            }
        }
    }

    /**
     * Update the cache according to a list of arguments that are assigned by the same LV.
     * No need to copy cache entries in this method.
     * All indices in DBs should be presented.
     *
     * @param cache The cache
     * @param lvLocations The locations of the arguments
     * @return A new list of cache entries complied with the LV restriction
     */
    protected List<List<CompliedBlock>> splitCacheEntriesByLvs(
            List<List<CompliedBlock>> cache, List<ArgLocation> lvLocations
    ) {
        /* Group arguments indices in predicates */
        final int predicates_in_rule = structure.size();
        List<Integer>[] lv_locs_in_preds = new List[predicates_in_rule];  // Predicate index as the array index
        int preds_with_lvs = 0;
        for (ArgLocation lv_loc: lvLocations) {
            if (null == lv_locs_in_preds[lv_loc.predIdx]) {
                lv_locs_in_preds[lv_loc.predIdx] = new ArrayList<>();
                preds_with_lvs++;
            }
            lv_locs_in_preds[lv_loc.predIdx].add(lv_loc.argIdx);
        }
        int[] pred_idxs_with_lvs = new int[preds_with_lvs];
        int[] var_arg_idx_in_pred = new int[preds_with_lvs];
        int pred_idx_with_lv = 0;
        for (int pred_idx = HEAD_PRED_IDX; pred_idx < predicates_in_rule; pred_idx++) {
            List<Integer> lv_arg_idxs = lv_locs_in_preds[pred_idx];
            if (null != lv_arg_idxs) {
                pred_idxs_with_lvs[pred_idx_with_lv] = pred_idx;
                var_arg_idx_in_pred[pred_idx_with_lv] = lv_arg_idxs.get(0);
                pred_idx_with_lv++;
                if (1 < lv_arg_idxs.size()) {
                    /* Filter the CBs in entries */
                    for (List<CompliedBlock> entry : cache) {
                        CompliedBlock cb = entry.get(pred_idx);
                        List<int[]> new_comp_list = new ArrayList<>();
                        for (int[] record : cb.complSet) {
                            boolean all_matched = true;
                            final int argument = record[lv_arg_idxs.get(0)];
                            for (int i = 1; i < lv_arg_idxs.size(); i++) {
                                if (argument != record[lv_arg_idxs.get(i)]) {
                                    all_matched = false;
                                    break;
                                }
                            }
                            if (all_matched) {
                                new_comp_list.add(record);
                            }
                        }
                        int[][] new_comp_list_rows = new_comp_list.toArray(new int[0][]);
                        entry.set(pred_idx, new CompliedBlock(
                                cb.relNum, cb.partAsgnRecord, new_comp_list_rows, new IntTable(new_comp_list_rows)
                        ));
                    }
                }
            }
        }

        /* Create new cache by splitting entries */
        List<List<CompliedBlock>> new_cache = new ArrayList<>();
        for (List<CompliedBlock> cache_entry: cache) {
            /* Match values */
            IntTable[] tables = new IntTable[preds_with_lvs];
            for (int i = 0; i < pred_idxs_with_lvs.length; i++) {
                int pred_idx = pred_idxs_with_lvs[i];
                tables[i] = cache_entry.get(pred_idx).indices;
            }
            List<int[][]>[] slices = IntTable.matchSlices(tables, var_arg_idx_in_pred);
            int matched_values = slices[0].size();
            for (int i = 0; i < matched_values; i++) {
                List<CompliedBlock> new_entry = new ArrayList<>(cache_entry);
                int shared_arg = slices[0].get(i)[0][var_arg_idx_in_pred[0]];
                for (int j = 0; j < pred_idxs_with_lvs.length; j++) {
                    int pred_idx = pred_idxs_with_lvs[j];
                    CompliedBlock cb = cache_entry.get(pred_idx);
                    int[] new_par = cb.partAsgnRecord.clone();
                    for (int arg_idx : lv_locs_in_preds[pred_idx]) {
                        new_par[arg_idx] = shared_arg;
                    }
                    int[][] slice = slices[j].get(i);
                    new_entry.set(pred_idx, new CompliedBlock(cb.relNum, new_par, slice, new IntTable(slice)));
                }
                new_cache.add(new_entry);
            }
        }
        return new_cache;
    }

    /**
     * Copy constructor
     *
     * @param another Another cached rule
     */
    public CachedRule(CachedRule another) {
        super(another);
        this.kb = another.kb;

        /* The caches can be simply copied, as the list should not be modified, but directly replaced (Copy-on-write) */
        this.posCache = another.posCache;
        this.allCache = another.allCache;
        this.plvList.addAll(another.plvList);
    }

    @Override
    public CachedRule clone() {
        return new CachedRule(this);
    }

    /**
     * Calculate the record coverage of the rule.
     */
    @Override
    protected double recordCoverage() {
        final Set<Record> entailed_head = new HashSet<>();
        final SimpleRelation target_relation = kb.getRelation(getHead().predSymbol);
        for (final List<CompliedBlock> cache_entry: posCache) {
            for (int[] record: cache_entry.get(HEAD_PRED_IDX).complSet) {
                if (!target_relation.isEntailed(record)) {
                    entailed_head.add(new Record(record));
                }
            }
        }
        return ((double) entailed_head.size()) / target_relation.totalRows();
    }

    /**
     * Calculate the evaluation of the rule.
     */
    @Override
    protected Eval calculateEval() {
        /* Find all variables in the head */
        final Set<Integer> head_only_lv_args = new HashSet<>();  // For the head only LVs
        int head_uv_cnt = 0;
        final Predicate head_pred = getHead();
        for (int argument: head_pred.args) {
            if (Argument.isEmpty(argument)) {
                head_uv_cnt++;
            } else if (Argument.isVariable(argument)) {
                head_only_lv_args.add(argument);    // The GVs will be removed later
            }
        }

        /* Find the first location of GVs in the body */
        final List<ArgLocation> body_gv_locs = new ArrayList<>();   // PLVs are not included
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            final Predicate body_pred = structure.get(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                final int argument = body_pred.args[arg_idx];
                if (head_only_lv_args.remove(argument) && null == plvList.get(Argument.decode(argument))) {
                    body_gv_locs.add(new ArgLocation(pred_idx, arg_idx));
                }
            }
        }

        /* Count the number of all entailments */
        int body_gv_plv_bindings_cnt = 0;
        if (noPlvInRule()) {
            /* Count all combinations of body GVs */
            final Set<Record> body_gv_bindings = new HashSet<>();
            for (final List<CompliedBlock> cache_entry: allCache) {
                final int[] binding = new int[body_gv_locs.size()];
                for (int i = 0; i < body_gv_locs.size(); i++) {
                    final ArgLocation loc = body_gv_locs.get(i);
                    binding[i] = cache_entry.get(loc.predIdx).partAsgnRecord[loc.argIdx];
                }
                body_gv_bindings.add(new Record(binding));
            }
            body_gv_plv_bindings_cnt = body_gv_bindings.size();
        } else {
            /* List argument indices of PLVs in each predicate */
            final List<Integer>[] plv_arg_index_lists = new List[structure.size()]; // Predicate index is the index of the array
            int preds_containing_plvs = 0;
            for (final PlvLoc plv_loc: plvList) {
                if (null != plv_loc) {
                    if (null == plv_arg_index_lists[plv_loc.bodyPredIdx]) {
                        plv_arg_index_lists[plv_loc.bodyPredIdx] = new ArrayList<>();
                        preds_containing_plvs++;
                    }
                    plv_arg_index_lists[plv_loc.bodyPredIdx].add(plv_loc.bodyArgIdx);
                }
            }

            /* Count the number of the combinations of GVs and PLVs */
            final Map<Record, Set<Record>> body_gv_binding_2_plv_bindings = new HashMap<>();
            for (final List<CompliedBlock> cache_entry: allCache) {
                /* Find the GV combination */
                final int[] gv_binding = new int[body_gv_locs.size()];
                for (int i = 0; i < body_gv_locs.size(); i++) {
                    final ArgLocation loc = body_gv_locs.get(i);
                    gv_binding[i] = cache_entry.get(loc.predIdx).partAsgnRecord[loc.argIdx];
                }

                /* Find the combinations of PLV bindings */
                /* Note: the PLVs in the same predicate should be bind at the same time according to the records in the
                   compliance set, and find the cartesian products of the groups of PLVs bindings. */
                int total_binding_length = 0;
                final Set<Record>[] plv_bindings_within_pred_sets = new Set[preds_containing_plvs];
                {
                    int i = 0;
                    for (int body_pred_idx = FIRST_BODY_PRED_IDX; body_pred_idx < structure.size(); body_pred_idx++) {
                        final List<Integer> plv_arg_idxs = plv_arg_index_lists[body_pred_idx];
                        if (null != plv_arg_idxs) {
                            final Set<Record> plv_bindings = new HashSet<>();
                            for (int[] cs_record : cache_entry.get(body_pred_idx).complSet) {
                                final int[] plv_binding_within_pred = new int[plv_arg_idxs.size()];
                                for (int j = 0; j < plv_binding_within_pred.length; j++) {
                                    plv_binding_within_pred[j] = cs_record[plv_arg_idxs.get(j)];
                                }
                                plv_bindings.add(new Record(plv_binding_within_pred));
                            }
                            plv_bindings_within_pred_sets[i] = plv_bindings;
                            i++;
                            total_binding_length += plv_arg_idxs.size();
                        }
                    }
                }
                final Set<Record> complete_plv_bindings =
                        body_gv_binding_2_plv_bindings.computeIfAbsent(new Record(gv_binding), k -> new HashSet<>());
                addCompleteBodyPlvBindings(
                        complete_plv_bindings, plv_bindings_within_pred_sets, new int[total_binding_length], 0, 0
                );
            }
            for (Set<Record> plv_bindings: body_gv_binding_2_plv_bindings.values()) {
                body_gv_plv_bindings_cnt += plv_bindings.size();
            }
        }
        final double all_entails = body_gv_plv_bindings_cnt * Math.pow(
                kb.totalConstants(), head_uv_cnt + head_only_lv_args.size()
        );
        
        /* Count for the total and new positive entailments */
        final Set<Record> newly_proved = new HashSet<>();
        final Set<Record> already_proved = new HashSet<>();
        final SimpleRelation target_relation = kb.getRelation(getHead().predSymbol);
        if (0 == head_uv_cnt) {
            /* No UV in the head, PAR is the record */
            for (final List<CompliedBlock> cache_entry : posCache) {
                int[] record = cache_entry.get(HEAD_PRED_IDX).partAsgnRecord;
                if (target_relation.isEntailed(record)) {
                    already_proved.add(new Record(record));
                } else {
                    newly_proved.add(new Record(record));
                }
            }
        } else {
            /* UVs in the head, find all records in the CSs */
            for (final List<CompliedBlock> cache_entry: posCache) {
                for (int[] record: cache_entry.get(HEAD_PRED_IDX).complSet) {
                    if (target_relation.isEntailed(record)) {
                        already_proved.add(new Record(record));
                    } else {
                        newly_proved.add(new Record(record));
                    }
                }
            }
        }
        
        /* Update evaluation score */
        /* Those already proved should be excluded from the entire entailment set. Otherwise, they are counted as negative ones */
        return new Eval(eval, newly_proved.size(), all_entails - already_proved.size(), length);
    }

    /**
     * Check if there is no PLV in the body.
     */
    protected boolean noPlvInRule() {
        for (PlvLoc plv_loc: plvList) {
            if (null != plv_loc) {
                return false;
            }
        }
        return true;
    }

    /**
     * Recursively compute the cartesian product of binding values of grouped PLVs and add each combination in the product
     * to the binding set.
     *
     * @param completeBindings the binding set
     * @param plvBindingSets the binding values of the grouped PLVs
     * @param template the template array to hold the binding combination
     * @param bindingSetIdx the index of the binding set in current recursion
     * @param templateStartIdx the starting index of the template for the PLV bindings
     */
    protected void addCompleteBodyPlvBindings(
            Set<Record> completeBindings, Set<Record>[] plvBindingSets, int[] template, int bindingSetIdx, int templateStartIdx
    ) {
        final Set<Record> plv_bindings = plvBindingSets[bindingSetIdx];
        Iterator<Record> itr = plv_bindings.iterator();
        Record plv_binding = itr.next();
        int binding_length = plv_binding.args.length;
        if (bindingSetIdx == plvBindingSets.length - 1) {
            /* Complete each template and add to the set */
            while (true) {
                System.arraycopy(plv_binding.args, 0, template, templateStartIdx, binding_length);
                completeBindings.add(new Record(template.clone()));
                if (!itr.hasNext()) break;
                plv_binding = itr.next();
            }
        } else {
            /* Complete part of the template and move to next recursion */
            while (true) {
                System.arraycopy(plv_binding.args, 0, template, templateStartIdx, binding_length);
                addCompleteBodyPlvBindings(
                        completeBindings, plvBindingSets, template, bindingSetIdx+1, templateStartIdx+binding_length
                );
                if (!itr.hasNext()) break;
                plv_binding = itr.next();
            }
        }
    }

    /**
     * Update the cache indices before specialization. E.g., right after the rule is selected as one of the beams.
     */
    public void updateCacheIndices() {
        for (List<CompliedBlock> entry: posCache) {
            for (CompliedBlock cb: entry) {
                cb.buildIndices();
            }
        }
        for (List<CompliedBlock> entry: allCache) {
            for (CompliedBlock cb: entry) {
                cb.buildIndices();
            }
        }
    }

    /**
     * Update the E+-cache for case 1 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(int predIdx, int argIdx, int varId) {
        boolean found = false;
        final int arg_var = Argument.variable(varId);
        for (int pred_idx = HEAD_PRED_IDX; pred_idx < structure.size() && !found; pred_idx++) {
            final Predicate predicate = structure.get(pred_idx);

            /* Find an argument that shares the same variable with the target */
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (arg_var == predicate.args[arg_idx] && (pred_idx != predIdx || arg_idx != argIdx)) { // Don't compare with the target argument
                    found = true;

                    /* Split */
                    posCache = splitCacheEntries(posCache, predIdx, argIdx, pred_idx, arg_idx);
                    break;
                }
            }
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E-cache for case 1 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(int predIdx, int argIdx, int varId) {
        if (HEAD_PRED_IDX != predIdx) { // No need to update the E-cache if the update is in the head
            PlvLoc plv_loc = plvList.get(varId);
            if (null != plv_loc) {
                /* Match the existing PLV, split */
                plvList.set(varId, null);
                allCache = splitCacheEntries(allCache, predIdx, argIdx, plv_loc.bodyPredIdx, plv_loc.bodyArgIdx);
            } else {
                boolean found = false;
                final int arg_var = Argument.variable(varId);

                /* Find an argument in the body that shares the same variable with the target */
                for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size() && !found; pred_idx++) {
                    final Predicate predicate = structure.get(pred_idx);

                    for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                        if (arg_var == predicate.args[arg_idx] && (pred_idx != predIdx || arg_idx != argIdx)) { // Don't compare with the target argument
                            found = true;

                            /* Split */
                            allCache = splitCacheEntries(allCache, predIdx, argIdx, pred_idx, arg_idx);
                            break;
                        }
                    }
                }

                if (!found) {
                    /* No matching LV in the body, record as a PLV */
                    final Predicate head_pred = structure.get(HEAD_PRED_IDX);
                    for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                        if (arg_var == head_pred.args[arg_idx]) {
                            plvList.set(varId, new PlvLoc(predIdx, argIdx, arg_idx));
                            break;
                        }
                    }
                }
            }
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E+-cache for case 2 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(Predicate newPredicate, int argIdx, int varId) {
        boolean found = false;
        final int arg_var = Argument.variable(varId);
        for (int pred_idx = HEAD_PRED_IDX; pred_idx < structure.size() - 1 && !found; pred_idx++) { // Don't find in the appended predicate
            final Predicate predicate = structure.get(pred_idx);

            /* Find an argument that shares the same variable with the target */
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (arg_var == predicate.args[arg_idx]) {
                    found = true;

                    /* Append + Split */
                    List<List<CompliedBlock>> tmp_cache = appendCacheEntries(posCache, newPredicate.predSymbol);
                    posCache = splitCacheEntries(tmp_cache, structure.size() - 1, argIdx, pred_idx, arg_idx);
                    break;
                }
            }
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E-cache for case 2 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(Predicate newPredicate, int argIdx, int varId) {
        PlvLoc plv_loc = plvList.get(varId);
        if (null != plv_loc) {
            /* Match the found PLV, append then split */
            plvList.set(varId, null);
            List<List<CompliedBlock>> tmp_cache = appendCacheEntries(allCache, newPredicate.predSymbol);
            allCache = splitCacheEntries(
                    tmp_cache, structure.size() - 1, argIdx, plv_loc.bodyPredIdx, plv_loc.bodyArgIdx
            );
        } else {
            boolean found = false;
            final int arg_var = Argument.variable(varId);

            /* Find an argument in the body that shares the same variable with the target */
            for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size() - 1 && !found; pred_idx++) {   // Don't find in the appended predicate
                final Predicate predicate = structure.get(pred_idx);

                for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                    if (arg_var == predicate.args[arg_idx]) {
                        found = true;

                        /* Append + Split */
                        List<List<CompliedBlock>> tmp_cache = appendCacheEntries(allCache, newPredicate.predSymbol);
                        allCache = splitCacheEntries(tmp_cache, structure.size() - 1, argIdx, pred_idx, arg_idx);
                        break;
                    }
                }
            }

            if (!found) {
                /* No matching LV in the body, record as a PLV, and append */
                final Predicate head_pred = structure.get(HEAD_PRED_IDX);
                for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                    if (arg_var == head_pred.args[arg_idx]) {
                        plvList.set(varId, new PlvLoc(structure.size() - 1, argIdx, arg_idx));
                        break;
                    }
                }
                allCache = appendCacheEntries(allCache, newPredicate.predSymbol);
            }
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E+-cache for case 3 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        /* Split */
        posCache = splitCacheEntries(posCache, predIdx1, argIdx1, predIdx2, argIdx2);
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E-cache for case 3 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        if (HEAD_PRED_IDX != predIdx1 || HEAD_PRED_IDX != predIdx2) {   // At least one modified predicate is in the body. Otherwise, nothing should be done.
            if (HEAD_PRED_IDX == predIdx1 || HEAD_PRED_IDX == predIdx2) {   // One is the head and the other is not
                /* The new variable is a PLV */
                if (HEAD_PRED_IDX == predIdx1) {
                    plvList.add(new PlvLoc(predIdx2, argIdx2, argIdx1));
                } else {
                    plvList.add(new PlvLoc(predIdx1, argIdx1, argIdx2));
                }
            } else {    // Both are in the body
                /* The new variable is not a PLV, split */
                plvList.add(null);
                allCache = splitCacheEntries(allCache, predIdx1, argIdx1, predIdx2, argIdx2);
            }
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E+-cache for case 4 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {
        /* Append + Split */
        List<List<CompliedBlock>> tmp_cache = appendCacheEntries(posCache, newPredicate.predSymbol);
        posCache = splitCacheEntries(tmp_cache, structure.size() - 1, argIdx1, predIdx2, argIdx2);
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E-cache for case 4 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {
        if (HEAD_PRED_IDX == predIdx2) {   // One is the head and the other is not
            /* The new variable is a PLV, append */
            plvList.add(new PlvLoc(structure.size() - 1, argIdx1, argIdx2));
            allCache = appendCacheEntries(allCache, newPredicate.predSymbol);
        } else {    // Both are in the body
            /* The new variable is not a PLV, append then split */
            plvList.add(null);
            List<List<CompliedBlock>> tmp_cache = appendCacheEntries(allCache, newPredicate.predSymbol);
            allCache = splitCacheEntries(tmp_cache, structure.size() - 1, argIdx1, predIdx2, argIdx2);
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E+-cache for case 5 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt1Uv2ConstHandlerPreCvg(int predIdx, int argIdx, int constant) {
        /* Assign */
        posCache = assignCacheEntries(posCache, predIdx, argIdx, constant);
        return UpdateStatus.NORMAL;
    }

    /**
     * Update the E-cache for case 5 specialization.
     *
     * Note: all indices should be up-to-date
     */
    @Override
    protected UpdateStatus cvt1Uv2ConstHandlerPostCvg(int predIdx, int argIdx, int constant) {
        if (HEAD_PRED_IDX != predIdx) { // No need to update the E-cache if the update is in the head
            /* Assign */
            allCache = assignCacheEntries(allCache, predIdx, argIdx, constant);
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * Generalization is not applicable in cached rules. This function always returns "UpdateStatus.INVALID"
     *
     * @return UpdateStatus.INVALID
     */
    @Override
    @Deprecated
    protected UpdateStatus rmAssignedArgHandlerPreCvg(int predIdx, int argIdx) {
        return UpdateStatus.INVALID;
    }

    /**
     * Generalization is not applicable in cached rules. This function always returns "UpdateStatus.INVALID"
     *
     * @return UpdateStatus.INVALID
     */
    @Override
    @Deprecated
    protected UpdateStatus rmAssignedArgHandlerPostCvg(int predIdx, int argIdx) {
        return UpdateStatus.INVALID;
    }

    /**
     * Append a raw complied block to each entry of the cache.
     *
     * Note: all indices should be up-to-date in the entries
     *
     * @param cache The original cache
     * @param relNum The numeration of the appended relation
     * @return A new cache containing all updated cache entries
     */
    protected List<List<CompliedBlock>> appendCacheEntries(List<List<CompliedBlock>> cache, int relNum) {
        SimpleRelation relation = kb.getRelation(relNum);
        CompliedBlock cb = new CompliedBlock(relNum, new int[relation.totalCols()], relation.getAllRows(), relation);
        List<List<CompliedBlock>> new_cache = new ArrayList<>();
        for (List<CompliedBlock> entry: cache) {
            List<CompliedBlock> new_entry = new ArrayList<>(entry);
            new_entry.add(cb);
            new_cache.add(new_entry);
        }
        return new_cache;
    }

    /**
     * Split entries in a cache according to two arguments in the rule.
     *
     * Note: all indices should be up-to-date in the entries
     *
     * @param cache The original cache
     * @param predIdx1 The 1st predicate index
     * @param argIdx1 The argument index in the 1st predicate
     * @param predIdx2 The 2nd predicate index
     * @param argIdx2 The argument index in the 2nd predicate
     * @return A new cache containing all updated cache entries
     */
    protected List<List<CompliedBlock>> splitCacheEntries(
            List<List<CompliedBlock>> cache, int predIdx1, int argIdx1, int predIdx2, int argIdx2
    ) {
        List<List<CompliedBlock>> new_cache = new ArrayList<>();
        if (predIdx1 == predIdx2) {
            for (List<CompliedBlock> cache_entry: cache) {
                CompliedBlock cb = cache_entry.get(predIdx1);
                List<int[][]> slices = cb.indices.matchSlices(argIdx1, argIdx2);
                for (int[][] slice: slices) {
                    int matched_val = slice[0][argIdx1];
                    int[] new_par = cb.partAsgnRecord.clone();
                    new_par[argIdx1] = matched_val;
                    new_par[argIdx2] = matched_val;
                    CompliedBlock new_cb = new CompliedBlock(cb.relNum, new_par, slice);
                    List<CompliedBlock> new_entry = new ArrayList<>(cache_entry);
                    new_entry.set(predIdx1, new_cb);
                    new_cache.add(new_entry);
                }
            }
        } else {
            for (List<CompliedBlock> cache_entry : cache) {
                CompliedBlock cb1 = cache_entry.get(predIdx1);
                CompliedBlock cb2 = cache_entry.get(predIdx2);
                IntTable.MatchedSubTables slices = IntTable.matchSlices(cb1.indices, argIdx1, cb2.indices, argIdx2);
                for (int i = 0; i < slices.slices1.size(); i++) {
                    int[][] slice1 = slices.slices1.get(i);
                    int[][] slice2 = slices.slices2.get(i);
                    int matched_val = slice1[0][argIdx1];
                    int[] new_par1 = cb1.partAsgnRecord.clone();
                    int[] new_par2 = cb2.partAsgnRecord.clone();
                    new_par1[argIdx1] = matched_val;
                    new_par2[argIdx2] = matched_val;
                    CompliedBlock new_cb1 = new CompliedBlock(cb1.relNum, new_par1, slice1);
                    CompliedBlock new_cb2 = new CompliedBlock(cb2.relNum, new_par2, slice2);
                    List<CompliedBlock> new_entry = new ArrayList<>(cache_entry);
                    new_entry.set(predIdx1, new_cb1);
                    new_entry.set(predIdx2, new_cb2);
                    new_cache.add(new_entry);
                }
            }
        }
        return new_cache;
    }

    /**
     * Assign a constant to an argument in each cache entry.
     *
     * Note: all indices should be up-to-date in the entries
     *
     * @param cache The original cache
     * @param predIdx The index of the modified predicate
     * @param argIdx The index of the argument in the predicate
     * @param constant The numeration of the constant
     * @return A new cache containing all updated cache entries
     */
    protected List<List<CompliedBlock>> assignCacheEntries(
            List<List<CompliedBlock>> cache, int predIdx, int argIdx, int constant
    ) {
        List<List<CompliedBlock>> new_cache = new ArrayList<>();
        for (List<CompliedBlock> cache_entry: cache) {
            CompliedBlock cb = cache_entry.get(predIdx);
            int[][] slice = cb.indices.getSlice(argIdx, constant);
            if (0 < slice.length) {
                int[] new_par = cb.partAsgnRecord.clone();
                new_par[argIdx] = constant;
                CompliedBlock new_cb = new CompliedBlock(cb.relNum, new_par, slice);
                List<CompliedBlock> new_entry = new ArrayList<>(cache_entry);
                new_entry.set(predIdx, new_cb);
                new_cache.add(new_entry);
            }
        }
        return new_cache;
    }

    /**
     * Find one piece of evidence for each positively entailed record and mark the positive entailments in the KB.
     *
     * @return Batched evidence
     */
    @Override
    public EvidenceBatch getEvidenceAndMarkEntailment() {
        final int[] relations_in_rule = new int[structure.size()];
        for (int i = 0; i < relations_in_rule.length; i++) {
            relations_in_rule[i] = structure.get(i).predSymbol;
        }
        EvidenceBatch evidence_batch = new EvidenceBatch(relations_in_rule);

        SimpleRelation target_relation = kb.getRelation(getHead().predSymbol);
        for (final List<CompliedBlock> cache_entry: posCache) {
            /* Find the grounding body */
            final int[][] grounding_body = new int[relations_in_rule.length][];
            for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
                grounding_body[pred_idx] = cache_entry.get(pred_idx).complSet[0];
            }

            /* Find all entailed records */
            for (int[] head_record: cache_entry.get(HEAD_PRED_IDX).complSet) {
                if (target_relation.entailIfNot(head_record)) {
                    final int[][] grounding = grounding_body.clone();
                    grounding[HEAD_PRED_IDX] = head_record;
                    evidence_batch.evidenceList.add(grounding);
                }
            }
        }
        return evidence_batch;
    }

    /**
     * Find the counterexamples generated by the rule.
     *
     * @return The set of counterexamples
     */
    @Override
    public Set<Record> getCounterexamples() {
        /* Find head-only variables and the locations in the head */
        final Map<Integer, List<Integer>> head_only_var_arg_2_loc_map = new HashMap<>();  // GVs will be removed later
        int uv_id = usedLimitedVars();
        final Predicate head_pred = getHead();
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            final int argument = head_pred.args[arg_idx];
            if (Argument.isEmpty(argument)) {
                List<Integer> list = new ArrayList<>();
                list.add(arg_idx);
                head_only_var_arg_2_loc_map.put(Argument.variable(uv_id), list);
                uv_id++;
            } else if (Argument.isVariable(argument)) {
                head_only_var_arg_2_loc_map.computeIfAbsent(argument, k -> new ArrayList<>()).add(arg_idx);
            }
        }

        /* Find GVs and PLVs in the body and their links to the head */
        /* List and group the argument indices of PLVs in each predicate */
        final Map<Integer, List<BodyGvLinkInfo>> pred_idx_2_plv_links_map = new HashMap<>();
        for (int vid = 0; vid < plvList.size(); vid++) {
            PlvLoc plv_loc = plvList.get(vid);
            if (null != plv_loc) {
                final List<Integer> head_var_locs = head_only_var_arg_2_loc_map.remove(Argument.variable(vid));
                pred_idx_2_plv_links_map.computeIfAbsent(plv_loc.bodyPredIdx, k -> new ArrayList<>())
                        .add(new BodyGvLinkInfo(plv_loc.bodyPredIdx, plv_loc.bodyArgIdx, head_var_locs.toArray(new Integer[0])));
            }
        }
        final List<BodyGvLinkInfo> body_gv_links = new ArrayList<>();
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            final Predicate body_pred = structure.get(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                List<Integer> head_gv_locs = head_only_var_arg_2_loc_map.remove(body_pred.args[arg_idx]);
                if (null != head_gv_locs) {
                    body_gv_links.add(new BodyGvLinkInfo(pred_idx, arg_idx, head_gv_locs.toArray(new Integer[0])));
                }
            }
        }

        /* Bind GVs in the head, producing templates */
        final Set<Record> head_templates = new HashSet<>();
        if (pred_idx_2_plv_links_map.isEmpty()) {
            /* No PLV, bind all GVs as templates */
            for (final List<CompliedBlock> cache_entry: allCache) {
                final int[] new_template = head_pred.args.clone();
                for (final BodyGvLinkInfo gv_link: body_gv_links) {
                    final int argument = cache_entry.get(gv_link.bodyPredIdx).partAsgnRecord[gv_link.bodyArgIdx];
                    for (int head_arg_idx: gv_link.headVarLocs) {
                        new_template[head_arg_idx] = argument;
                    }
                }
                head_templates.add(new Record(new_template));
            }
        } else {
            /* There are PLVs in the body */
            /* Find the bindings combinations of the GVs and the PLVs */
            final int[] base_template = head_pred.args.clone();
            for (final List<CompliedBlock> cache_entry: allCache) {
                /* Bind the GVs first */
                for (final BodyGvLinkInfo gv_loc: body_gv_links) {
                    final int argument = cache_entry.get(gv_loc.bodyPredIdx).partAsgnRecord[gv_loc.bodyArgIdx];
                    for (int head_arg_idx: gv_loc.headVarLocs) {
                        base_template[head_arg_idx] = argument;
                    }
                }

                /* Find the combinations of PLV bindings */
                /* Note: the PLVs in the same predicate should be bind at the same time according to the records in the
                   compliance set, and find the cartesian products of the groups of PLVs bindings. */
                final Set<Record>[] plv_bindings_within_pred_sets = new Set[pred_idx_2_plv_links_map.size()];
                final List<BodyGvLinkInfo>[] plv_link_lists = new List[pred_idx_2_plv_links_map.size()];
                {
                    int i = 0;
                    for (Map.Entry<Integer, List<BodyGvLinkInfo>> entry: pred_idx_2_plv_links_map.entrySet()) {
                        final int body_pred_idx = entry.getKey();
                        final List<BodyGvLinkInfo> plv_links = entry.getValue();
                        final Set<Record> plv_bindings = new HashSet<>();
                        final CompliedBlock cb = cache_entry.get(body_pred_idx);
                        for (int[] cs_record : cb.complSet) {
                            final int[] plv_binding_within_a_pred = new int[plv_links.size()];
                            for (int j = 0; j < plv_binding_within_a_pred.length; j++) {
                                plv_binding_within_a_pred[j] = cs_record[plv_links.get(j).bodyArgIdx];
                            }
                            plv_bindings.add(new Record(plv_binding_within_a_pred));
                        }
                        plv_bindings_within_pred_sets[i] = plv_bindings;
                        plv_link_lists[i] = plv_links;
                        i++;
                    }
                }
                addBodyPlvBindings2HeadTemplates(
                        head_templates, plv_bindings_within_pred_sets, plv_link_lists, base_template, 0
                );
            }
        }

        /* Extend head templates */
        final Set<Record> counter_example_set = new HashSet<>();
        SimpleRelation target_relation = kb.getRelation(head_pred.predSymbol);
        if (head_only_var_arg_2_loc_map.isEmpty()) {
            /* No need to extend UVs */
            for (Record head_template : head_templates) {
                if (!target_relation.hasRow(head_template.args)) {
                    counter_example_set.add(head_template);
                }
            }
        } else {
            /* Extend UVs in the templates */
            List<Integer>[] head_only_var_loc_lists = new List[head_only_var_arg_2_loc_map.size()];
            int i = 0;
            for (List<Integer> head_only_var_loc_list: head_only_var_arg_2_loc_map.values()) {
                head_only_var_loc_lists[i] = head_only_var_loc_list;
                i++;
            }
            for (Record head_template: head_templates) {
                expandHeadUvs4CounterExamples(
                        target_relation, counter_example_set, head_template, head_only_var_loc_lists, 0
                );
            }
        }
        return counter_example_set;
    }

    /**
     * Recursively add PLV bindings to the linked head arguments.
     *
     * @param headTemplates The set of head templates
     * @param plvBindingSets The bindings of PLVs grouped by predicate
     * @param plvLinkLists The linked arguments in the head for each PLV
     * @param template An argument list template
     * @param linkIdx The index of the PLV group
     */
    protected void addBodyPlvBindings2HeadTemplates(
            Set<Record> headTemplates, Set<Record>[] plvBindingSets, List<BodyGvLinkInfo>[] plvLinkLists, int[] template, int linkIdx
    ) {
        final Set<Record> plv_bindings = plvBindingSets[linkIdx];
        final List<BodyGvLinkInfo> plv_links = plvLinkLists[linkIdx];
        if (linkIdx == plvBindingSets.length - 1) {
            /* Finish the last group of PLVs, add to the template set */
            for (Record plv_binding: plv_bindings) {
                for (int i = 0; i < plv_binding.args.length; i++) {
                    BodyGvLinkInfo plv_link = plv_links.get(i);
                    for (int head_arg_idx: plv_link.headVarLocs) {
                        template[head_arg_idx] = plv_binding.args[i];
                    }
                }
                headTemplates.add(new Record(template.clone()));
            }
        } else {
            /* Add current binding to the template and move to the next recursion */
            for (Record plv_binding: plv_bindings) {
                for (int i = 0; i < plv_binding.args.length; i++) {
                    BodyGvLinkInfo plv_link = plv_links.get(i);
                    for (int head_arg_idx: plv_link.headVarLocs) {
                        template[head_arg_idx] = plv_binding.args[i];
                    }
                }
                addBodyPlvBindings2HeadTemplates(headTemplates, plvBindingSets, plvLinkLists, template, linkIdx + 1);
            }
        }
    }

    /**
     * Recursively expand UVs in the template and add to counterexample set
     *
     * @param targetRelation The target relation
     * @param counterexamples The counterexample set
     * @param template The template record
     * @param idx The index of UVs
     * @param varLocs The locations of UVs
     */
    protected void expandHeadUvs4CounterExamples(
            final SimpleRelation targetRelation, final Set<Record> counterexamples, final Record template,
            final List<Integer>[] varLocs, final int idx
    ) {
        final List<Integer> locations = varLocs[idx];
        if (idx < varLocs.length - 1) {
            /* Expand current UV and move to the next recursion */
            for (int constant_symbol: kb.allConstants()) {
                final int argument = Argument.constant(constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = argument;
                }
                expandHeadUvs4CounterExamples(targetRelation, counterexamples, template, varLocs, idx + 1);
            }
        } else {
            /* Expand the last UV and add to counterexample set if it is */
            for (int constant_symbol: kb.allConstants()) {
                final int argument = Argument.constant(constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = argument;
                }
                if (!targetRelation.hasRow(template.args)) {
                    counterexamples.add(new Record(template.args.clone()));
                }
            }
        }
    }
}
