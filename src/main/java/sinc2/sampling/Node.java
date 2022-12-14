package sinc2.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A node structure (with two linked lists of in- and out-edges)
 */
class Node {
    public final int num;
    public final List<EdgeInNode> inEdgeInNodes = new ArrayList<>();
    public final List<EdgeInNode> outEdgeInNodes = new ArrayList<>();

    public Node(int num) {
        this.num = num;
    }

    public int totalEdges() {
        return inEdgeInNodes.size() + outEdgeInNodes.size();
    }

    public Edge getEdgeAt(int idx) {
        if (idx < inEdgeInNodes.size()) {
            EdgeInNode edge = inEdgeInNodes.get(idx);
            return new Edge(edge.neighbourNum, edge.pred, num);
        } else {
            EdgeInNode edge = outEdgeInNodes.get(idx - inEdgeInNodes.size());
            return new Edge(num, edge.pred, edge.neighbourNum);
        }
    }

    public Edge[] randEdges(int edges) {
        Random random = new Random();
        int total_edges = totalEdges();
        Edge[] rand_edges = new Edge[edges];
        for (int i = 0; i < edges; i++) {
            int rand_idx = random.nextInt(total_edges);
            rand_edges[i] = getEdgeAt(rand_idx);
        }
        return rand_edges;
    }
}
