package sinc2.kb;

import sinc2.common.Argument;
import sinc2.common.Record;
import sinc2.rule.Rule;
import sinc2.util.LittleEndianIntIO;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This class is for the compressed KB. It extends the numerated KB in three perspectives:
 *   1. The compressed KB contains counterexample relations:
 *      - The counterexamples are stored into '.ceg' files. The file format is the same as '.rel' files. The names of the
 *        files are '<relation name>_<arity>_<#records>.ceg'.
 *      - If there is no counterexample in a relation, the counterexample relation file is not created.
 *   2. The compressed KB contains a hypothesis set:
 *      - The hypothesis set is stored into a 'rules.hyp' file. The rules are written in the form of plain text, one per
 *        line.
 *      - If there is no rule in the hypothesis set, the file will not be created.
 *   3. The compressed KB contains a supplementary constant set:
 *      - The supplementary constant set is stored into a 'supplementary.cst' file. Constant numerations in the mapping
 *        are stored in the file.
 *        Note: The numeration mapping in a compressed KB contains all mappings as the original KB does.
 *      - If there is no element in the supplementary set, the file will not be created.
 * The necessary facts are stored as is in the original KB.
 *
 * @since 2.1
 * @see sinc2.util.kb.KbRelation
 * @see sinc2.util.kb.NumeratedKb
 */
public class SimpleCompressedKb {
    /** A regex pattern used to parse the counterexample file name */
    protected static final Pattern COUNTEREXAMPLE_FILE_NAME_PATTERN = Pattern.compile("(.+)_([0-9]+)_([0-9]+).ceg$");
    /** The name of the hypothesis file */
    public static final String HYPOTHESIS_FILE_NAME = "rules.hyp";
    /** The name of the second mapping file for the supplementary constants */
    public static final String SUPPLEMENTARY_CONSTANTS_FILE_NAME = "supplementary.cst";
    /** The number of bit flags in one integer */
    protected static final int BITS_PER_INT = Integer.BYTES * 8;

    public static Path getCounterexampleFilePath(String kbPath, String relName, int arity, int records) {
        return Paths.get(kbPath, String.format("%s_%d_%d.ceg", relName, arity, records));
    }

    /** The name of the compressed KB */
    protected final String name;
    /** The reference to the original KB. The original KB is used for determining the necessary records and the missing
     *  constants. */
    protected final SimpleKb originalKb;
    /** The hypothesis set, i.e., a list of rules */
    protected List<Rule> hypothesis = new ArrayList<>();
    /** The records included by FVS in each corresponding relation */
    protected final List<int[]>[] fvsRecords;
    /** The counterexample sets. The ith set is correspondent to the ith relation in the original KB. */
    protected final Set[] counterexampleSets;
    /** The constants marked in a supplementary set. Otherwise, they are lost due to removal of facts. */
    protected Set<Integer> supplementaryConstants = null;

    /**
     * Construct an empty compressed KB.
     *
     * @param name       The name of the compressed KB
     * @param originalKb The original KB
     */
    public SimpleCompressedKb(String name, SimpleKb originalKb) {
        this.name = name;
        this.originalKb = originalKb;
        this.fvsRecords = new List[originalKb.totalRelations()];
        this.counterexampleSets = new Set[originalKb.totalRelations()];
        for (int i = 0; i < counterexampleSets.length; i++) {
            fvsRecords[i] = new ArrayList<>();
            counterexampleSets[i] = new HashSet<Record>();
        }
    }

    public void addFvsRecord(int relId, int[] record) {
        fvsRecords[relId].add(record);
    }

    /**
     * Add some counterexamples to the KB.
     *
     * @param relId   The id of the corresponding relation
     * @param records The counterexamples
     */
    public void addCounterexamples(int relId, Collection<Record> records) {
        Set<Record> set = counterexampleSets[relId];
        for (Record record: records) {
            set.add(new Record(record.args));
        }
    }

    public void addHypothesisRules(Collection<Rule> rules) {
        hypothesis.addAll(rules);
    }

    /**
     * Update the supplementary constant set.
     */
    public void updateSupplementaryConstants() {
        /* Build flags for all constants */
        int[] flags = new int[originalKb.totalConstants() / BITS_PER_INT + ((0 == originalKb.totalConstants() % BITS_PER_INT) ? 0 : 1)];

        /* Remove all occurred arguments*/
        for (int i = 0; i < originalKb.totalRelations(); i++) {
            SimpleRelation relation = originalKb.getRelation(i);
            relation.setFlagOfReservedConstants(flags);
            for (int[] record: fvsRecords[i]) {
                for (int arg: record) {
                    flags[arg / BITS_PER_INT] |= 1 << (arg % BITS_PER_INT);
                }
            }
        }
        for (Rule rule: hypothesis) {
            for (int pred_idx = 0; pred_idx < rule.predicates(); pred_idx++) {
                for (int argument: rule.getPredicate(pred_idx).args) {
                    if (Argument.isConstant(argument)) {
                        int arg = Argument.decode(argument);
                        flags[arg / BITS_PER_INT] |= 1 << (arg % BITS_PER_INT);
                    }
                }
            }
        }
        for (Set<Record> counterexample_set: counterexampleSets) {
            for (Record record: counterexample_set) {
                for (int arg: record.args) {
                    flags[arg / BITS_PER_INT] |= 1 << (arg % BITS_PER_INT);
                }
            }
        }

        /* Find all integers that are not flagged */
        supplementaryConstants = new HashSet<>();
        int base_offset = 0;
        for (int sub_flag: flags) {
            for (int i = 0; i < BITS_PER_INT && base_offset + i < originalKb.totalConstants(); i++) {
                if (0 == (sub_flag & 1)) {
                    supplementaryConstants.add(base_offset + i);
                }
                sub_flag >>= 1;
            }
            base_offset += BITS_PER_INT;
        }
        supplementaryConstants.remove(0);   // 0 is added in the set and should be discarded
    }

    /**
     * Write the compressed KB to the local file system.
     *
     * @param basePath The path to where the KB directory should locate
     * @throws KbException Dump failed
     */
    public void dump(String basePath) throws KbException {
        File dir = Paths.get(basePath, name).toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new KbException("KB directory creation failed.");
        }

        /* Dump necessary records & counterexamples */
        String dir_path = dir.getAbsolutePath();
        for (int i = 0; i < counterexampleSets.length; i++) {
            SimpleRelation relation = originalKb.getRelation(i);
            relation.dumpNecessaryRecords(dir_path, fvsRecords[i]);
            relation.dumpCounterexamples(dir_path, counterexampleSets[i]);
        }

        /* Dump hypothesis */
        if (0 < hypothesis.size()) {
            try {
                PrintWriter writer = new PrintWriter(
                        Paths.get(dir_path, HYPOTHESIS_FILE_NAME).toFile()
                );
                for (Rule rule : hypothesis) {
                    writer.println(rule.toDumpString(originalKb));
                }
                writer.close();
            } catch (FileNotFoundException e) {
                throw new KbException(e);
            }
        }

        /* Dump supplementary constants */
        updateSupplementaryConstants();
        if (0 < supplementaryConstants.size()) {
            try {
                FileOutputStream fos = new FileOutputStream(Paths.get(
                        dir_path, SUPPLEMENTARY_CONSTANTS_FILE_NAME
                ).toFile());
                for (int i : supplementaryConstants) {
                    fos.write(LittleEndianIntIO.leInt2ByteArray(i));
                }
                fos.close();
            } catch (IOException e) {
                throw new KbException(e);
            }
        }
    }

    public List<Rule> getHypothesis() {
        return hypothesis;
    }

    public int totalNecessaryRecords() {
        int cnt = 0;
        for (SimpleRelation relation: originalKb.getRelations()) {
            cnt += relation.totalRows() - relation.totalEntailedRecords();
        }
        return cnt + totalFvsRecords();
    }

    public int totalFvsRecords() {
        int cnt = 0;
        for (List<int[]> fvs_records: fvsRecords) {
            cnt += fvs_records.size();
        }
        return cnt;
    }

    public int totalCounterexamples() {
        int cnt = 0;
        for (Set<Record> counterexample_set: counterexampleSets) {
            cnt += counterexample_set.size();
        }
        return cnt;
    }

    public int totalHypothesisSize() {
        int cnt = 0;
        for (Rule rule: hypothesis) {
            cnt += rule.length();
        }
        return cnt;
    }

    public int totalSupplementaryConstants() {
        if (null == supplementaryConstants) {
            updateSupplementaryConstants();
        }
        return supplementaryConstants.size();
    }

    public String getName() {
        return name;
    }
}
