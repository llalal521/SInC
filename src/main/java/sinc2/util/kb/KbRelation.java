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
 * A relation can be dumped into local file system as a regular file. This complies with the following format:
 *   - The files are binary files, each of which only contains `arity`x`#records` integers.
 *   - The integers stored in one `.rel` file is row oriented, each row corresponds to one record in the relation. The
 *     records are stored in the file in order, i.e., in the order of: 1st row 1st col, 1st row 2nd col, ..., ith row
 *     jth col, ith row (j+1)th col, ...
 *
 * @since 2.0
 */
public class KbRelation implements Iterable<Record> {

    /** The name of the relation */
    protected final String name;
    /** The id of the relation */
    protected int id;
    /** The arity of the relation */
    protected final int arity;
    /** The set of the records */
    protected final Set<Record> records;

    /**
     * Create an empty relation
     *
     * @param id The numeration of the relation name
     */
    public KbRelation(String name, int id, int arity) {
        this.name = name;
        this.id = id;
        this.arity = arity;
        this.records = new HashSet<>();
    }

    /**
     * Load a single relation from a local file. If the 'numMap' is not NULL, every loaded numeration is checked for
     * validness in the map.
     *
     * @param name The name of the relation
     * @param id The numeration of the relation name
     * @param arity The arity of the relation
     * @param fileName The name of the file
     * @param kbPtah The path to the KB, where the relation file is located
     * @param map The numeration map for validness check
     * @throws IOException File read fails
     * @throws KbException 'map' is not NULL and a loaded numeration is not mapped
     */
    public KbRelation(String name, int id, int arity, String fileName, String kbPtah, NumerationMap map)
            throws IOException, KbException {
        this.name = name;
        this.id = id;
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
        this.id = another.id;
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

    public int getId() {
        return id;
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
        return id == records1.id && arity == records1.arity && Objects.equals(name, records1.name) &&
                Objects.equals(records, records1.records);
    }
}
