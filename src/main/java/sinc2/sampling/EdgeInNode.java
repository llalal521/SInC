package sinc2.sampling;

/**
 * In/Out edge information linked to/from a node in KG.
 */
public class EdgeInNode {
    public final int pred;
    public final int neighbourNum;

    public EdgeInNode(int pred, int neighbourNum) {
        this.pred = pred;
        this.neighbourNum = neighbourNum;
    }
}
