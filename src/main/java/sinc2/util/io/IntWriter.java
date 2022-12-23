package sinc2.util.io;

import sinc2.util.LittleEndianIntIO;

import java.io.*;

/**
 * This class is used for writing integers into binary files.
 *
 * @since 2.1
 */
public class IntWriter implements AutoCloseable {
    /** The size of buffer */
    public static int BUF_SIZE = 4096*4;

    /** The buffered output stream */
    protected final BufferedOutputStream os;

    /**
     * Construct a writer by the path to the target file
     */
    public IntWriter(String filePath) throws FileNotFoundException {
        os = new BufferedOutputStream(new FileOutputStream(filePath), BUF_SIZE);
    }

    /**
     * Construct a writer by the target file
     */
    public IntWriter(File file) throws FileNotFoundException {
        os = new BufferedOutputStream(new FileOutputStream(file), BUF_SIZE);
    }

    /**
     * Write an integer to the file
     */
    public void write(int i) throws IOException {
        os.write(LittleEndianIntIO.leInt2ByteArray(i));
    }

    public void close() throws IOException {
        os.close();
    }
}
