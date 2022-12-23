package sinc2.util.io;

import sinc2.util.LittleEndianIntIO;

import java.io.*;

/**
 * This class is used for reading integers from binary files. Given that the users of this class always know the total
 * number of integers that should be read from files, the reading method does not check the return value for better
 * performance.
 *
 * @since 2.1
 */
public class IntReader implements AutoCloseable {
    /** The size of buffer */
    public static final int BUF_SIZE = IntWriter.BUF_SIZE;

    /** The buffered input stream */
    protected BufferedInputStream is;

    /**
     * Construct a reader by the path to the target file
     */
    public IntReader(String filePath) throws FileNotFoundException {
        is = new BufferedInputStream(new FileInputStream(filePath), BUF_SIZE);
    }

    /**
     * Construct a reader by the target file
     */
    public IntReader(File file) throws FileNotFoundException {
        is = new BufferedInputStream(new FileInputStream(file), BUF_SIZE);
    }

    /**
     * Read an integer from file
     */
    public int next() throws IOException {
        byte[] buf = new byte[Integer.BYTES];
        is.read(buf);   // The return value is not checked here because the caller always read a known number of integers from files
        return LittleEndianIntIO.byteArray2LeInt(buf);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }
}
