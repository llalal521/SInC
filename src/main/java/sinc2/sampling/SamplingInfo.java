package sinc2.sampling;

import sinc2.kb.SimpleKb;

/**
 * Information of sampled KB and the mapping of constant integers.
 *
 * @since 2.1
 */
public class SamplingInfo {
    /** Sampled KB */
    public final SimpleKb sampledKb;
    /** The mapping from the integers in the sampled KB to the original KB. I.e., constMap[i] is the numeration of i in
     *  the original KB */
    public final int[] constMap;

    public SamplingInfo(SimpleKb sampledKb, int[] constMap) {
        this.sampledKb = sampledKb;
        this.constMap = constMap;
    }
}
