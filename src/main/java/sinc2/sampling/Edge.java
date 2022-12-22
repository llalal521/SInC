package sinc2.sampling;

/**
 * A complete edge structure (a triple).
 */
public class Edge {
    public final int subj;
    public final int pred;
    public final int obj;

    public Edge(int subj, int pred, int obj) {
        this.subj = subj;
        this.pred = pred;
        this.obj = obj;
    }
}
