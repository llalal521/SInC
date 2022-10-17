package sinc2.kb.compact;

import sinc2.common.Argument;
import sinc2.kb.CompressedKb;
import sinc2.kb.KbException;
import sinc2.kb.Record;
import sinc2.rule.Rule;
import sinc2.util.LittleEndianIntIO;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class is used to store the components in the compressed KB.
 *
 * @since 2.1
 */
public class SimpleCompressedKb {
    /** The name of the compressed KB */
    protected final String name;
    /** The reference to the original KB. The original KB is used for determining the necessary records and the missing
     *  constants. */
    protected final SimpleKb originalKb;
    /** The hypothesis set, i.e., a list of rules */
    protected List<Rule> hypothesis = new ArrayList<>();
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
        this.counterexampleSets = new Set[originalKb.totalRelations()];
        for (int i = 0; i < counterexampleSets.length; i++) {
            counterexampleSets[i] = new HashSet<Record>();
        }
    }

    /**
     * Add some counterexamples to the KB.
     *
     * @param relId   The id of the corresponding relation
     * @param records The counterexamples
     */
    public void addCounterexamples(int relId, Collection<int[]> records) {
        if (0 <= relId && relId < counterexampleSets.length) {
            Set<Record> set = counterexampleSets[relId];
            for (int[] record: records) {
                set.add(new Record(record));
            }
        }
    }

    /**
     * Add some counterexamples to the KB.
     *
     * @param relName The name of the corresponding relation
     * @param records The counterexamples
     */
    public void addCounterexamples(String relName, Collection<int[]> records) {
        SimpleRelation relation = originalKb.getRelation(relName);
        if (null != relation) {
            Set<Record> set = counterexampleSets[relation.id];
            for (int[] record: records) {
                set.add(new Record(record));
            }
        }
    }

    public void addHypothesisRules(Collection<Rule> rules) {
        hypothesis.addAll(rules);
    }

    /**
     * Update the supplementary constant set.
     */
    public void updateSupplementaryConstants() {
        /* Find all constants */
        Set<Integer> lost_constants = originalKb.allConstants();

        /* Remove all occurred arguments*/
        for (SimpleRelation relation: originalKb.getRelations()) {
            relation.removeReservedConstants(lost_constants);
        }
        for (Rule rule: hypothesis) {
            for (int pred_idx = 0; pred_idx < rule.predicates(); pred_idx++) {
                for (int argument: rule.getPredicate(pred_idx).args) {
                    if (Argument.isConstant(argument)) {
                        lost_constants.remove(Argument.decode(argument));
                    }
                }
            }
        }
        for (Set<Record> counterexample_set: counterexampleSets) {
            for (Record record: counterexample_set) {
                for (int argument: record.args) {
                    lost_constants.remove(argument);
                }
            }
        }
        supplementaryConstants = lost_constants;
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
            Set<Record> counterexample_set = counterexampleSets[i];
            relation.dumpUnentailedRecords(dir_path);
            if (0 < counterexample_set.size()) {
                /* Dump only non-empty relations */
                try {
                    FileOutputStream fos = new FileOutputStream(Paths.get(
                            dir_path, CompressedKb.getCounterexampleFileName(relation.name)
                    ).toFile());
                    for (Record counterexample: counterexample_set) {
                        for (int arg: counterexample.args) {
                            fos.write(LittleEndianIntIO.leInt2ByteArray(arg));
                        }
                    }
                    fos.close();
                } catch (IOException e) {
                    throw new KbException(e);
                }
            }
        }

        /* Dump hypothesis */
        if (0 < hypothesis.size()) {
            try {
                PrintWriter writer = new PrintWriter(
                        Paths.get(dir_path, CompressedKb.HYPOTHESIS_FILE_NAME).toFile()
                );
                for (Rule rule : hypothesis) {
                    writer.println(rule.toDumpString());
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
                        dir_path, CompressedKb.SUPPLEMENTARY_CONSTANTS_FILE_NAME
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
}
