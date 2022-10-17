package sinc2.kb.compact;

import sinc2.kb.KbRelation;
import sinc2.kb.NumeratedKb;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A simple in-memory KB. The values in the KB are converted to integers so each relation in the KB is a 2D table of
 * integers. It is the same as "NumeratedKb", except that the simple KB is read-only. Due to its simplicity, the memory
 * cost is much lower than NumeratedKb. The estimated size of the memory cost, at the worst case, is about 3 times the
 * size of the disk space taken by all relation files. For more information, please refer to "NumeratedKB".
 *
 * @see NumeratedKb
 * @since 2.1
 */
public class SimpleKb {

    /** The name of the KB */
    protected final String name;
    /** The list of relations. The ID of each relation is its index in the list */
    protected final SimpleRelation[] relations;
    /** The map from relation names to IDs */
    protected final Map<String, Integer> relationNameMap;

    /**
     * Load a KB from local file system.
     *
     * @param name The name of the KB
     * @param basePath The base path to the dir of the KB
     * @throws IOException
     */
    public SimpleKb(String name, String basePath) throws IOException {
        this.name = name;
        List<SimpleRelation> relations = new ArrayList<>();
        this.relationNameMap = new HashMap<>();
        File kb_dir = NumeratedKb.getKbPath(name, basePath).toFile();
        String kb_dir_path = kb_dir.getAbsolutePath();
        File[] files = kb_dir.listFiles();
        if (null != files) {
            for (File f: files) {
                KbRelation.RelationInfo rel_info = KbRelation.parseRelFilePath(f.getName());
                if (null != rel_info) {
                    SimpleRelation relation = new SimpleRelation(
                            rel_info.name, relations.size(), rel_info.arity, rel_info.totalRecords, kb_dir_path
                    );
                    relations.add(relation);
                    relationNameMap.put(relation.name, relation.id);
                }
            }
        }
        this.relations = relations.toArray(new SimpleRelation[0]);
    }

    public SimpleRelation getRelation(String name) {
        Integer idx = relationNameMap.get(name);
        return (null == idx) ? null : relations[idx];
    }

    public SimpleRelation getRelation(int id) {
        return (id >=0 && id < relations.length) ? relations[id] : null;
    }

    public boolean hasRecord(String relationName, int[] record) {
        Integer idx = relationNameMap.get(relationName);
        return (null != idx) && relations[idx].hasRow(record);
    }

    public boolean hasRecord(int relationId, int[] record) {
        return relationId >= 0 && relationId < relations.length && relations[relationId].hasRow(record);
    }

    public void setAsEntailed(String relationName, int[] record) {
        Integer idx = relationNameMap.get(relationName);
        if (null != idx) {
            relations[idx].setAsEntailed(record);
        }
    }

    public void setAsEntailed(int relationId, int[] record) {
        if (relationId >= 0 && relationId < relations.length) {
            relations[relationId].setAsEntailed(record);
        }
    }

    public void setAsNotEntailed(String relationName, int[] record) {
        Integer idx = relationNameMap.get(relationName);
        if (null != idx) {
            relations[idx].setAsNotEntailed(record);
        }
    }

    public void setAsNotEntailed(int relationId, int[] record) {
        if (relationId >= 0 && relationId < relations.length) {
            relations[relationId].setAsNotEntailed(record);
        }
    }

    public int[][] getPromisingConstants(int relId) {
        SimpleRelation relation = getRelation(relId);
        return (null == relation) ? null : relation.getPromisingConstants();
    }

    public String getName() {
        return name;
    }

    public SimpleRelation[] getRelations() {
        return relations.clone();
    }

    public int totalRelations() {
        return relations.length;
    }

    public int totalRecords() {
        int cnt = 0;
        for (SimpleRelation relation: relations) {
            cnt += relation.totalRows();
        }
        return cnt;
    }

    public Set<Integer> allConstants() {
        Set<Integer> constants = new HashSet<>();
        for (SimpleRelation relation: relations) {
            relation.collectConstants(constants);
        }
        return constants;
    }
}
