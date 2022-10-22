package sinc2;

import sinc2.kb.KbException;
import sinc2.kb.SimpleCompressedKb;
import sinc2.kb.SimpleKb;

/**
 * The recovery class, retuning a compressed KB to the original version.
 * 
 * @since 1.0
 */
public abstract class SincRecovery {
    /**
     * Decompress the KB to the original form.
     *
     * @param decompressedName The name of the decompressed KB.
     */
    abstract public SimpleKb recover(SimpleCompressedKb compressedKb, String decompressedName) throws KbException;
}
