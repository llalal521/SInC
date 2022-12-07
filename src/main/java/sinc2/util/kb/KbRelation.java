package sinc2.util.kb;

import sinc2.common.Argument;
import sinc2.common.Record;
import sinc2.kb.KbException;
import sinc2.util.LittleEndianIntIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class for the numeration representation of the records in a relation.
 *
 * A relation can be dumped into local file system as a regular file. The complies with the following format:
 *   - The name of the files should be `<relation name>_<arity>_<#records>.rel`.
 *   - The files are binary files, each of which only contains `arity`x`#records` integers.
 *   - The integers stored in one `.rel` file is row oriented, each row corresponds to one record in the relation. The
 *     records are stored in the file in order, i.e., in the order of: 1st row 1st col, 1st row 2nd col, ..., ith row
 *     jth col, ith row (j+1)th col, ...
 *
 * @since 2.0
 */
public class KbRelation implements Iterable<Record> {

    /** A regex pattern used to parse the relation file name */
    protected static final Pattern REL_FILE_NAME_PATTERN = Pattern.compile("(.+)_([0-9]+)_([0-9]+).rel$");

    /** The name of the relation */
    protected final String name;
    /** The numeration of the relation name */
    protected final int numeration;
    /** The arity of the relation */
    protected final int arity;
    /** The set of the records */
    protected final Set<Record> records;

    /**
     * Get the relation file path.
     *
     * @param kbPath The base path of the KB where the relation file locates.
     * @param relName The name of the relation
     * @param arity The arity of the relation
     * @param records the number of records in the relation
     * @return The path to the relation file
     */
    public static Path getRelFilePath(String kbPath, String relName, int arity, int records) {
        return Paths.get(kbPath, String.format("%s_%d_%d.rel", relName, arity, records));
    }

    /**
     * The class for the parsed information of a relation file name
     */
    public static class RelationInfo {
        public final String name;
        public final int arity;
        public final int totalRecords;

        public RelationInfo(String name, int arity, int totalRecords) {
            this.name = name;
            this.arity = arity;
            this.totalRecords = totalRecords;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelationInfo that = (RelationInfo) o;
            return arity == that.arity && totalRecords == that.totalRecords && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arity, totalRecords);
        }
    }

    /**
     * Parse the file name of a relation to the components: relation name, arity, total records.
     */
    public static RelationInfo parseRelFilePath(String relFileName) {
        Matcher matcher = REL_FILE_NAME_PATTERN.matcher(relFileName);
        if (matcher.find()) {
            return new RelationInfo(
                    matcher.group(1), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))
            );
        }
        return null;
    }

    /**
     * Create an empty relation
     *
     * @param numeration The numeration of the relation name
     */
    public KbRelation(String name, int numeration, int arity) {
        this.name = name;
        this.numeration = numeration;
        this.arity = arity;
        this.records = new HashSet<>();
    }

    /**
     * Load a single relation from the local file system. If the 'numMap' is not NULL, every loaded numeration is
     * checked for validness in the map.
     *
     * @param name The name of the relation
     * @param numeration The numeration of the relation name
     * @param arity The arity of the relation
     * @param totalRecords The total number of records in the relation
     * @param kbPtah The path to the KB, where the relation file is located
     * @param map The numeration map for validness check
     * @throws IOException File read fails
     * @throws KbException 'map' is not NULL and a loaded numeration is not mapped
     */
    public KbRelation(String name, int numeration, int arity, int totalRecords, String kbPtah, NumerationMap map)
            throws IOException, KbException {
        this.name = name;
        this.numeration = numeration;
        this.arity = arity;
        this.records = new HashSet<>();

        File rel_file = getRelFilePath(kbPtah, name, arity, totalRecords).toFile();
        loadHandler(rel_file, map);
    }

    /**
     * Load a single relation from a local file. If the 'numMap' is not NULL, every loaded numeration is checked for
     * validness in the map.
     *
     * @param name The name of the relation
     * @param numeration The numeration of the relation name
     * @param arity The arity of the relation
     * @param fileName The name of the file
     * @param kbPtah The path to the KB, where the relation file is located
     * @param map The numeration map for validness check
     * @throws IOException File read fails
     * @throws KbException 'map' is not NULL and a loaded numeration is not mapped
     */
    public KbRelation(String name, int numeration, int arity, String fileName, String kbPtah, NumerationMap map)
            throws IOException, KbException {
        this.name = name;
        this.numeration = numeration;
        this.arity = arity;
        this.records = new HashSet<>();

        File rel_file = Paths.get(kbPtah, fileName).toFile();
        loadHandler(rel_file, map);
    }

    /**
     * Copy from another relation.
     */
    public KbRelation(KbRelation another) {
        this.name = another.name;
        this.numeration = another.numeration;
        this.arity = another.arity;
        this.records = new HashSet<>(another.records);
    }

    /**
     * Load a single relation from the local file system. If the 'numMap' is not NULL, every loaded numeration is
     * checked for validness in the map.
     *
     * @param file The relation file
     * @param map The numeration map for validness check
     * @throws IOException File read fails
     * @throws KbException 'map' is not NULL and a loaded numeration is not mapped
     */
    protected void loadHandler(File file, NumerationMap map) throws IOException, KbException {
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[Integer.BYTES];
        int read_args;
        if (null == map) {
            while (true) {
                int[] args = new int[arity];
                for (read_args = 0; read_args < arity && Integer.BYTES == fis.read(buffer); read_args++) {
                    args[read_args] = LittleEndianIntIO.byteArray2LeInt(buffer);
                }
                if (read_args < arity) {
                    break;
                }
                addRecord(new Record(args));
            }
        } else {
            while (true) {
                int[] args = new int[arity];
                for (read_args = 0; read_args < arity && Integer.BYTES == fis.read(buffer); read_args++) {
                    args[read_args] = LittleEndianIntIO.byteArray2LeInt(buffer);
                    if (null == map.num2Name(Argument.decode(args[read_args]))) {
                        fis.close();
                        throw new KbException(String.format("Loaded numeration is not mapped: %d", args[read_args]));
                    }
                }
                if (read_args < arity) {
                    break;
                }
                addRecord(new Record(args));
            }
        }
        fis.close();
    }

    /**
     * Add a record to the relation.
     *
     * @return If the record has been added to the relation before, return false; otherwise, return true.
     * @throws KbException The arity of the record does not match that of the relation.
     */
    public boolean addRecord(Record record) throws KbException {
        if (record.args.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d)", record.args.length, arity
            ));
        }
        return records.add(record);
    }

    /**
     * Add a batch of records to the relation.
     *
     * @throws KbException The arity of the record does not match that of the relation.
     */
    public void addRecords(Iterable<Record> records) throws KbException {
        for (Record record: records) {
            addRecord(record);
        }
    }

    /**
     * Add a batch of records to the relation.
     *
     * @throws KbException The arity of the record does not match that of the relation.
     */
    public void addRecords(Record[] records) throws KbException {
        for (Record record: records) {
            addRecord(record);
        }
    }

    /**
     * Remove a record from the relation
     *
     * @return Whether the record exists in the relation before the removal.
     */
    public boolean removeRecord(Record record) {
        return records.remove(record);
    }

    /**
     * Write the relation to a '.rel' file.
     *
     * @param kbPath The path to the KB, where the relation file should be located.
     * @throws IOException File I/O operation error
     */
    public void dump(String kbPath) throws IOException {
        dumpHandler(getRelFilePath(kbPath, name, arity, records.size()).toFile());
    }

    /**
     * Write the relation to a file.
     *
     * @param kbPath The path to the KB, where the relation file should be located.
     * @param fileName The customized file name.
     * @throws IOException File I/O operation error
     */
    public void dump(String kbPath, String fileName) throws IOException {
        dumpHandler(Paths.get(kbPath, fileName).toFile());
    }

    protected void dumpHandler(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        for (Record record: records) {
            for (int i: record.args) {
                fos.write(LittleEndianIntIO.leInt2ByteArray(i));
            }
        }
        fos.close();
    }

    public boolean hasRecord(Record record) {
        return records.contains(record);
    }

    public Iterator<Record> iterator() {
        return records.iterator();
    }

    public String getName() {
        return name;
    }

    public int getNumeration() {
        return numeration;
    }

    public int getArity() {
        return arity;
    }

    public Set<Record> getRecords() {
        return records;
    }

    public int totalRecords() {
        return records.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbRelation records1 = (KbRelation) o;
        return numeration == records1.numeration && arity == records1.arity && Objects.equals(name, records1.name) &&
                Objects.equals(records, records1.records);
    }
}
