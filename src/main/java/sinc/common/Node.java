package sinc.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Node {
    final public ArrayList<Set<Node>> adjacent = new ArrayList<>();
    final public ArrayList<Set<Node>> reverse = new ArrayList<>();
    public String name;

    public Node(String name){
        this.name = name;
    }

    public void setNum(int relation_num){
        for(int i = 0; i < relation_num; ++i){
            adjacent.add(null);
        }
        for(int i = 0; i < relation_num; ++i){
            reverse.add(null);
        }
    }

    public void setObject(int pos, Node node){
        if(adjacent.get(pos) == null){
            adjacent.set(pos, new HashSet<>());
        }
        Set<Node> objs = adjacent.get(pos);
        objs.add(node);
    }

    public void setSubject(int pos, Node node){
        if(reverse.get(pos) == null){
            reverse.set(pos, new HashSet<>());
        }
        Set<Node> subs = reverse.get(pos);
        subs.add(node);
    }
}
