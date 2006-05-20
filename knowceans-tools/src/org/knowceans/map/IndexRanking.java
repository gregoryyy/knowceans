/*
 * Created on 16.05.2006
 */
package org.knowceans.map;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * IndexSorter is a convenience class that sorts Integer indices (values) by
 * their scores (keys).
 * 
 * @author gregor
 */
public class IndexRanking extends RankingMap<Double, Integer> {

    /**
     * 
     */
    public IndexRanking() {
        super();
    }

    /**
     * @param reverse
     */
    public IndexRanking(boolean reverse) {
        super(reverse);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param comp
     */
    public IndexRanking(Comparator<? super Double> comp) {
        super(comp);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param map
     * @param comp
     */
    public IndexRanking(IMultiMap<Double, Integer> map, Comparator<? super Double> comp) {
        super(comp);
        putAll(map);
    }

    /**
     * @param map
     */
    public IndexRanking(IMultiMap<Double, Integer> map) {
        putAll(map);
    }


    /**
     * creates a new map with count values referenced (but not
     * 
     * @param count
     */
    public IndexRanking  headMap(int count) {
        IndexRanking  head = new IndexRanking (comparator());
        for (Map.Entry<Double, Set<Integer>> e : entrySet()) {
            head.put(e.getKey(), e.getValue());
            if (--count == 0)
                break;
        }
        return head;
    }

    @Override
    public IndexRanking  headMap(Double fromKey) {
        return new IndexRanking (super.headMap(fromKey), comparator());
    }

    @Override
    public IndexRanking  tailMap(Double fromKey) {
        return new IndexRanking (super.tailMap(fromKey), comparator());
    }


}
