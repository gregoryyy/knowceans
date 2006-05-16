/*
 * Created on 16.05.2006
 */
package org.knowceans.map;

import java.util.Comparator;

/**
 * IndexSorter sorts Integer indices by their scores.
 *
 * @author gregor
 */
public class IndexSorter extends RankingMap<Double, Integer> {

    /**
     * 
     */
    public IndexSorter() {
        super();
    }

    /**
     * @param reverse
     */
    public IndexSorter(boolean reverse) {
        super(reverse);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param comp
     */
    public IndexSorter(Comparator<Double> comp) {
        super(comp);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param map
     * @param comp
     */
    public IndexSorter(IndexSorter map, Comparator<Double> comp) {
        super(comp);
        putAll(map);
    }

    /**
     * @param map
     */
    public IndexSorter(IndexSorter map) {
        putAll(map);
    }
    
}
