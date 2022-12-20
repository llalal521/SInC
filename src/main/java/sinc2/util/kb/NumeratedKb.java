package sinc2.util.kb;

import sinc2.common.Record;
import sinc2.kb.KbException;
import sinc2.util.MultiSet;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The in-memory KB. Entity name strings are converted to integer numbers to reduce memory cost and improve processing
 * efficiency. A KB is a set of 'NumeratedRelation'. A KB can be dumped into the local file system. A dumped KB is a
 * directory (named by the KB name) that contains multiple files:
 *   - The numeration mapping file: Please refer to class 'sinc2.util.kb.NumerationMap'
 *   - The relation name file "Relations.tsv" : Relation information will be listed in order in this file (i.e., the
 *   line numbers are the IDs of relations). The columns are:
 *     1) relation name
 *     2) arity
 *     3) total records.
 *   - The relation files: The names of the relation files are "<ID>.rel", as the relation names may contain illegal
 *     characters for file names. Please refer to class 'sinc2.util.kb.KbRelation' for structure details.
 *   - Meta information files:
 *       - There may be multiple files with extension `.meta` to store arbitrary meta information of the KB.
 *       - The files are customized by other utilities and are not in a fixed format.
 *
 * The dump of the numerated KB ensures that the numeration of all entities are mapped into a continued integer span:
 * (0, n], where n is the total number of different entity names
 *
 * @since 2.0
 * @see NumerationMap
 * @see KbRelation
 */
public class NumeratedKb {

    /** The file name of the relation name file */
    public static final String REL_INFO_FILE_NAME = "Relations.tsv";

    /** The name of the KB */
    protected final String name;
    /** The mapping from relation names to relation index in the relation list */
    protected final Map<String, KbRelation> relationMap = new HashMap<>();
    /** The list of relations */
    protected final List<KbRelation> relations = new ArrayList<>();
    /** The numeration map */
    protected final NumerationMap numMap;
    /** The multiset of all numerations mapped from entity names */
    protected final MultiSet<Integer> numSet;

    /**
     * Get the path for the files where the KB is dumped.
     *
     * @param kbName The name of the KB
     * @param basePath The path to the dir of KB files
     * @return The path for the files where the KB is dumped.
     */
    public static Path getKbPath(String kbName, String basePath) {
        return Paths.get(basePath, kbName);
    }

    /**
     * Get the path of the relation name file.
     *
     * @param kbName The name of the KB
     * @param basePath The path to the dir of KB files
     */
    public static Path getRelInfoFilePath(String kbName, String basePath) {
        return Paths.get(basePath, kbName, REL_INFO_FILE_NAME);
    }

    /**
     * Get the path to a relation data file.
     *
     * @param relId  The ID of the relation
     */
    public static String getRelDataFileName(int relId) {
        return String.format("%d.rel", relId);
    }

    /**
     * Create an empty KB.
     *
     * @param name The name of the KB
     */
    public NumeratedKb(String name) {
        this.name = name;
        this.numMap = new NumerationMap();
        this.numSet = new MultiSet<>();
    }

    /**
     * Load a KB from files. The loaded records are not checked.
     *
     * @param name The name of the KB
     * @param basePath The base path to the dir of the KB
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     */
    public NumeratedKb(String name, String basePath) throws IOException, KbException {
        this.name = name;
        File kb_dir = getKbPath(name, basePath).toFile();
        String kb_dir_path = kb_dir.getAbsolutePath();
        this.numMap = new NumerationMap(kb_dir_path);
        this.numSet = new MultiSet<>();
        loadAllRelationsHandler(kb_dir_path, getRelInfoFilePath(name, basePath).toFile(), false);
    }

    /**
     * Load a KB from files.
     *
     * @param name The name of the KB
     * @param basePath The base path to the dir of the KB
     * @param check Whether the records are checked when loaded
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     */
    public NumeratedKb(String name, String basePath, boolean check) throws IOException, KbException {
        this.name = name;
        File kb_dir = getKbPath(name, basePath).toFile();
        String kb_dir_path = kb_dir.getAbsolutePath();
        this.numMap = new NumerationMap(kb_dir_path);
        this.numSet = new MultiSet<>();
        loadAllRelationsHandler(kb_dir_path, getRelInfoFilePath(name, basePath).toFile(), check);
    }

    /**
     * Copy from another KB and assign a new name.
     *
     * @param newName The new name for this KB
     */
    public NumeratedKb(NumeratedKb another, String newName) {
        this.name = newName;
        for (int rel_idx = 0; rel_idx < another.relations.size(); rel_idx++) {
            KbRelation new_relation = new KbRelation(another.relations.get(rel_idx));
            relations.add(new_relation);
            relationMap.put(new_relation.getName(), new_relation);
        }
        numMap = new NumerationMap(another.numMap);
        numSet = new MultiSet<>(another.numSet);
    }

    /**
     * Load all relations in the directory of the KB.
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     */
    protected void loadAllRelationsHandler(String kbDirPath, File relInfoFile, boolean check) throws IOException, KbException {
        /* Load relation information */
        BufferedReader reader = new BufferedReader(new FileReader(relInfoFile));
        String line;
        while (null != (line = reader.readLine())) {
            String[] components = line.split("\t"); // relation name, arity, total records
            int rel_id = relations.size();
            File rel_file = Paths.get(kbDirPath, getRelDataFileName(rel_id)).toFile();
            KbRelation relation;
            if (rel_file.exists()) {
                /* Load from file */
                loadRelation(kbDirPath, rel_file.getName(), components[0], Integer.parseInt(components[1]), check);
            } else {
                /* No relation data file means the relation is empty. Create an empty relation */
                relation = new KbRelation(components[0], rel_id, Integer.parseInt(components[1]));
                relations.add(relation);
                relationMap.put(relation.getName(), relation);
            }
        }
        reader.close();
    }

    /**
     * Dump the KB to the local file system. This will call "tidyUp()" method to rearrange numeration mappings and relation
     * IDs.
     *
     * @param basePath The path to the KB directory
     * @throws IOException Thrown when KB directory creation failed or errors occur in the dump of other files
     */
    public void dump(String basePath) throws IOException {
        tidyUp();

        /* Check & create dir */
        Path kb_dir = getKbPath(name, basePath);
        File kb_dir_file = kb_dir.toFile();
        if (!kb_dir_file.exists() && !kb_dir_file.mkdirs()) {
            throw new IOException("KB directory creation failed: " + kb_dir_file.getAbsolutePath());
        }

        /* Dump */
        String kb_dir_path = kb_dir_file.getAbsolutePath();
        numMap.dump(kb_dir_path);
        PrintWriter writer = new PrintWriter(getRelInfoFilePath(name, basePath).toFile());
        for (int rel_id = 0; rel_id < relations.size(); rel_id++) {
            KbRelation relation = relations.get(rel_id);
            writer.printf("%s\t%d\t%d\n", relation.getName(), relation.getArity(), relation.totalRecords());
            if (0 < relation.totalRecords()) {
                /* Dump only non-empty relations */
                relation.dump(kb_dir_path, getRelDataFileName(rel_id));
            }
        }
        writer.close();
    }

    /**
     * Return a newly created relation of 'relName' in the KB or the existing one.
     *
     * @param relName The name of the relation
     * @param arity The arity of the relation
     * @return The created relation object
     * @throws KbException The relation has already been created with a different arity
     */
    public KbRelation createRelation(String relName, int arity) throws KbException {
        KbRelation relation = relationMap.get(relName);
        if (null != relation) {
            if (relation.getArity() != arity) {
                throw new KbException(String.format(
                        "The relation has already been created: %s (arity=%d, instead of %d)",
                        relName, relation.getArity(), arity
                ));
            }
        } else {
            relation = new KbRelation(relName, relations.size(), arity);
            relationMap.put(relName, relation);
            relations.add(relation);
        }
        return relation;
    }

    /**
     * Load a relation into the KB from a '.rel' file.  If the name 'relName' has been used, raise a KbException.
     *
     * @param relBasePath The path where the file is stored.
     * @param fileName    The name of the relation data file
     * @param relName     The name of the relation
     * @param arity       The arity of the relation
     * @param check       Whether loaded records are checked in the mapping
     * @return The loaded relation object
     * @throws KbException The name 'relName' has already been used; numerations in loaded records are not mapped if check=true.
     * @throws IOException File I/O errors
     */
    public KbRelation loadRelation(String relBasePath, String fileName, String relName, int arity, boolean check)
            throws KbException, IOException {
        KbRelation relation = relationMap.get(relName);
        if (null != relation) {
            throw new KbException("The relation has already been created in the KB: " + relName);
        }
        relation = new KbRelation(relName, relations.size(), arity, fileName, relBasePath, check ? numMap:null);
        relationMap.put(relName, relation);
        relations.add(relation);
        for (Record record: relation) {
            for (int arg: record.args) {
                numSet.add(arg);
            }
        }
        return relation;
    }

    /**
     * Remove a relation from the KB by the ID of the relation.
     *
     * @return The removed relation, or NULL if no such relation
     */
    public KbRelation deleteRelation(int relId) {
        if (relId >= relations.size() || 0 > relId) {
            return null;
        }
        KbRelation relation = relations.get(relId);
        if (null != relation) {
            relationMap.remove(relation.getName());
            relations.set(relId, null);
            for (Record record: relation) {
                for (int arg: record.args) {
                    if (0 == numSet.remove(arg)) {
                        numMap.unmapNumeration(arg);
                    }
                }
            }
        }
        return relation;
    }

    /**
     * Remove a relation from the KB by the relation name.
     *
     * @return The removed relation, or NULL if no such relation
     */
    public KbRelation deleteRelation(String relName) {
        KbRelation relation = relationMap.remove(relName);
        if (null != relation) {
            relations.set(relation.getId(), null);
            for (Record record: relation) {
                for (int arg: record.args) {
                    if (0 == numSet.remove(arg)) {
                        numMap.unmapNumeration(arg);
                    }
                }
            }
        }
        return relation;
    }

    /**
     * Fetch the relation object from the KB by relation name. NULL if no such relation.
     */
    public KbRelation getRelation(String relName) {
        return relationMap.get(relName);
    }

    /**
     * Fetch the relation object from the KB by relation name numeration. NULL if no such relation.
     */
    public KbRelation getRelation(int relNum) {
        return (relNum >= 0 && relNum < relations.size())? relations.get(relNum) : null;
    }

    /**
     * Check the existence of the relation by the name.
     */
    public boolean hasRelation(String relName) {
        return relationMap.containsKey(relName);
    }

    /**
     * Check the existence of the relation by the name numeration.
     */
    public boolean hasRelation(int relNum) {
        return relNum >= 0 && relNum < relations.size() && (null != relations.get(relNum));
    }

    /**
     * Get the arity of the relation. -1 if no such relation.
     */
    public int getRelationArity(String relName) {
        KbRelation relation = getRelation(relName);
        return (null == relation) ? -1 : relation.getArity();
    }

    /**
     * Get the arity of the relation. -1 if no such relation.
     */
    public int getRelationArity(int relNum) {
        KbRelation relation = getRelation(relNum);
        return (null == relation) ? -1 : relation.getArity();
    }

    /**
     * Add a record where arguments are name strings to the KB. The names will be converted to numerations (or be added
     * to the mapping first) before the record is added. A new KbRelation wil be created If the relation does not exist
     * in the KB.
     *
     * @param relName The name of the relation
     * @param argNames The names of the arguments
     * @throws KbException Record arity does not match the relation
     */
    public void addRecord(String relName, String[] argNames) throws KbException {
        KbRelation relation = createRelation(relName, argNames.length);

        int[] arg_nums = new int[argNames.length];
        for (int i = 0; i < argNames.length; i++) {
            arg_nums[i] = numMap.mapName(argNames[i]);
        }
        addRecordHandler(relation, new Record(arg_nums));
    }

    /**
     * Add a record where arguments are numbers to the KB. A new KbRelation will be created If the relation does not
     * exist in the KB. A KbException will be raised if a number is not mapped to any string in the KB.
     *
     * @throws KbException Record arity does not match the relation; Number is not mapped to any string
     */
    public void addRecord(String relName, int[] record) throws KbException {
        KbRelation relation = createRelation(relName, record.length);

        for (int arg: record) {
            if (null == numMap.num2Name(arg)) {
                throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
            }
        }

        addRecordHandler(relation, new Record(record));
    }

    public void addRecord(String relName, Record record) throws KbException {
        KbRelation relation = createRelation(relName, record.args.length);

        for (int arg: record.args) {
            if (null == numMap.num2Name(arg)) {
                throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
            }
        }

        addRecordHandler(relation, record);
    }

    /**
     * Add a record where arguments are name strings to the KB. The names will be converted to numerations (or be added
     * to the mapping first) before the record is added. A KbException will be raised If the relation does not exist in
     * the KB.
     *
     * @param relNum The numeration of the relation
     * @throws KbException The relation is not in the KB; Record arity does not match the relation.
     */
    public void addRecord(int relNum, String[] argNames) throws KbException {
        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        if (argNames.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", argNames.length, arity,
                    Arrays.toString(argNames)
            ));
        }

        int[] arg_nums = new int[argNames.length];
        for (int i = 0; i < arity; i++) {
            arg_nums[i] = numMap.mapName(argNames[i]);
        }
        addRecordHandler(relation, new Record(arg_nums));
    }

    /**
     * Add a record where arguments are numbers to the KB.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addRecord(int relNum, int[] record) throws KbException {
        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        if (record.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", record.length, arity, Arrays.toString(record)
            ));
        }

        for (int arg: record) {
            if (null == numMap.num2Name(arg)) {
                throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
            }
        }

        addRecordHandler(relation, new Record(record));
    }

    public void addRecord(int relNum, Record record) throws KbException {
        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        if (record.args.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", record.args.length, arity, record
            ));
        }

        for (int arg: record.args) {
            if (null == numMap.num2Name(arg)) {
                throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
            }
        }

        addRecordHandler(relation, record);
    }

    /**
     * Add records. Relation will be created if not exist.
     *
     * @throws KbException Record arity does not match the relation
     */
    public void addRecords(String relName, String[][] argNamesArray) throws KbException {
        if (0 == argNamesArray.length) {
            return;
        }

        KbRelation relation = createRelation(relName, argNamesArray[0].length);

        int arity = relation.getArity();
        for (String[] arg_names: argNamesArray) {
            if (arg_names.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", arg_names.length, arity,
                        Arrays.toString(arg_names)
                ));
            }

            int[] arg_nums = new int[arg_names.length];
            for (int i = 0; i < arity; i++) {
                arg_nums[i] = numMap.mapName(arg_names[i]);
            }
            addRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Add records. Relation will be created if not exist.
     *
     * @throws KbException Record arity does not match the relation; Number is not mapped to any string
     */
    public void addRecords(String relName, int[][] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = createRelation(relName, records[0].length);

        int arity = relation.getArity();
        for (int[] record: records) {
            if (record.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", record.length, arity, Arrays.toString(record)
                ));
            }

            for (int arg : record) {
                if (null == numMap.num2Name(arg)) {
                    throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
                }
            }

            addRecordHandler(relation, new Record(record));
        }
    }

    public void addRecords(String relName, Record[] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = createRelation(relName, records[0].args.length);

        int arity = relation.getArity();
        for (Record record: records) {
            if (record.args.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", record.args.length, arity, record
                ));
            }

            for (int arg : record.args) {
                if (null == numMap.num2Name(arg)) {
                    throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
                }
            }

            addRecordHandler(relation, record);
        }
    }

    /**
     * Add records.
     *
     * @throws KbException The relation is not in the KB; Record arity does not match the relation.
     */
    public void addRecords(int relNum, String[][] argNamesArray) throws KbException {
        if (0 == argNamesArray.length) {
            return;
        }

        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        for (String[] arg_names: argNamesArray) {
            if (arg_names.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", arg_names.length, arity,
                        Arrays.toString(arg_names)
                ));
            }

            int[] arg_nums = new int[arg_names.length];
            for (int i = 0; i < arity; i++) {
                arg_nums[i] = numMap.mapName(arg_names[i]);
            }
            addRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Add records.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addRecords(int relNum, int[][] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        for (int[] record: records) {
            if (record.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", record.length, arity, Arrays.toString(record)
                ));
            }

            for (int arg : record) {
                if (null == numMap.num2Name(arg)) {
                    throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
                }
            }

            addRecordHandler(relation, new Record(record));
        }
    }

    /**
     * Add records.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addRecords(int relNum, Record[] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        for (Record record: records) {
            if (record.args.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", record.args.length, arity, record
                ));
            }

            for (int arg : record.args) {
                if (null == numMap.num2Name(arg)) {
                    throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
                }
            }

            addRecordHandler(relation, record);
        }
    }

    /**
     * Add a record to the relation.
     *
     * @throws KbException Record arity does not match the relation
     */
    protected void addRecordHandler(KbRelation relation, Record record) throws KbException {
        if (relation.addRecord(record)) {
            for (int arg : record.args) {
                numSet.add(arg);
            }
        }
    }

    /**
     * Remove a record from the relation. No exception is thrown if the relation is not in the KB nor the record is not
     * in the relation.
     *
     * @param relName The name of the relation
     * @param argNames The names of the arguments
     */
    public void removeRecord(String relName, String[] argNames) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = numMap.name2Num(argNames[i]);
            }
            removeRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Remove a record form the relation.
     */
    public void removeRecord(String relName, int[] record) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            removeRecordHandler(relation, new Record(record));
        }
    }

    public void removeRecord(String relName, Record record) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            removeRecordHandler(relation, record);
        }
    }

    /**
     * Remove a record form the relation.
     */
    public void removeRecord(int relNum, String[] argNames) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = numMap.name2Num(argNames[i]);
            }
            removeRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Remove a record form the relation.
     */
    public void removeRecord(int relNum, int[] record) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            removeRecordHandler(relation, new Record(record));
        }
    }

    public void removeRecord(int relNum, Record record) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            removeRecordHandler(relation, record);
        }
    }

    protected void removeRecordHandler(KbRelation relation, Record record) {
        if (relation.removeRecord(record)) {
            for (int arg: record.args) {
                if (0 == numSet.remove(arg)) {
                    numMap.unmapNumeration(arg);
                }
            }
        }
    }

    /**
     * Check whether a record is in the KB.
     *
     * @param relName The name of the relation
     * @param argNames The names of the arguments
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(String relName, String[] argNames) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = numMap.name2Num(argNames[i]);
            }
            return relation.hasRecord(new Record(arg_nums));
        }
        return false;
    }

    /**
     * Check whether a record is in the KB.
     *
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(String relName, int[] record) {
        KbRelation relation = getRelation(relName);
        return null != relation && relation.hasRecord(new Record(record));
    }

    public boolean hasRecord(String relName, Record record) {
        KbRelation relation = getRelation(relName);
        return null != relation && relation.hasRecord(record);
    }

    /**
     * Check whether a record is in the KB.
     *
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(int relNum, String[] argNames) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = numMap.name2Num(argNames[i]);
            }
            return relation.hasRecord(new Record(arg_nums));
        }
        return false;
    }

    /**
     * Check whether a record is in the KB.
     *
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(int relNum, int[] record) {
        KbRelation relation = getRelation(relNum);
        return null != relation && relation.hasRecord(new Record(record));
    }

    public boolean hasRecord(int relNum, Record record) {
        KbRelation relation = getRelation(relNum);
        return null != relation && relation.hasRecord(record);
    }

    /**
     * Get the mapped name for number 'num'.
     *
     * @return The mapped name of the number, NULL if the number is not mapped in the KB.
     */
    public String num2Name(int num) {
        return numMap.num2Name(num);
    }

    /**
     * Get the mapped integer of a name string.
     *
     * @return The mapped number for the name. 0 if the name is not mapped in the KB.
     */
    public int name2Num(String name) {
        return numMap.name2Num(name);
    }

    public String getName() {
        return name;
    }

    public Collection<KbRelation> getRelations() {
        return relationMap.values();
    }

    public NumerationMap getNumerationMap() {
        return numMap;
    }

    public int totalMappings() {
        return numMap.totalMappings();
    }

    public int totalRelations() {
        return relationMap.size();
    }

    public int totalRecords() {
        int cnt = 0;
        for (KbRelation relation: relationMap.values()) {
            cnt += relation.totalRecords();
        }
        return cnt;
    }

    /**
     * Tidy up the mapping and records because there may be many mappings that are not used due to removal of relations
     * and records. Reuse the IDs of deleted, not empty, relations.
     */
    public void tidyUp() {
        /* Reuse IDs of deleted relations */
        int last_rel_idx = relations.size() - 1;
        for (int rel_idx = 0; rel_idx <= last_rel_idx; rel_idx++) {
            KbRelation relation = relations.get(rel_idx);
            if (null == relation) {
                for (; last_rel_idx >= rel_idx; last_rel_idx--) {
                    KbRelation replacement = relations.get(last_rel_idx);
                    if (null != replacement) {
                        replacement.id = rel_idx;
                        relations.set(rel_idx, replacement);
                        relations.set(last_rel_idx, null);
                    }
                }
            }
        }
        relations.subList(last_rel_idx + 1, relations.size()).clear();

        /* Remove invalid mappings */
        Map<Integer, Integer> remap_map = numMap.replaceEmpty();
        if (!remap_map.isEmpty()) {
            for (KbRelation relation: relations) {
                for (Record record: relation) {
                    for (int i = 0; i < relation.getArity(); i++) {
                        record.args[i] = remap_map.getOrDefault(record.args[i], record.args[i]);
                    }
                }
            }
        }
    }

    /**
     * Rearrange the mapping between name strings and numerations. The method will be skipped if the size of the rearrangement
     * does not match the original mapping.
     *
     * @param oldNum2New The mapping from old numeration to new. I.e., oldNum2New[old_num] = new_num.
     */
    public void rearrangeMapping(int[] oldNum2New) {
        numMap.rearrange(oldNum2New);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumeratedKb that = (NumeratedKb) o;
        return Objects.equals(name, that.name) && Objects.equals(relationMap, that.relationMap) &&
                Objects.equals(numMap, that.numMap);
    }
}
