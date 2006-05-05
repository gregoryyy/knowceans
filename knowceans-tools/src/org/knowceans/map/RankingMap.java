package org.knowceans.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.knowceans.util.Cokus;

/**
 * RankingMap allows sorting of two lists of items simultaneously.
 * <p>
 * TODO: build ParallelCollection (or for primitive types a ParallelArray)
 * 
 * @author gregor
 */
public class RankingMap<K, V> extends InvertibleTreeMap<K, V> {

    /**
     * 
     */
    private static final long serialVersionUID = 2428685609778646169L;

    public static void main(String[] args) {
        double[] a = new double[20];
        int[] b = new int[20];
        for (int i = 0; i < 20; i += 2) {
            a[i] = Cokus.randDouble();
            b[i] = (int) Cokus.randUint32();
            // check duplicate scores.
            a[i + 1] = a[i];
            b[i + 1] = b[i] + 1;
            System.out.println(b[i] + " = " + a[i]);
        }
        RankingMap<Double, Integer> r = new RankingMap<Double, Integer>(
            Collections.reverseOrder());
        r.put(convert(a), convert(b));
        System.out.println(r.sortedKeys(5));
        System.out.println(r.sortedValues(5));
    }

    /**
     * 
     */
    public RankingMap() {
        super();
    }

    /**
     * Init and specify whether sorting should be reversed.
     */
    public RankingMap(boolean reverse) {
        super(Collections.reverseOrder());
    }

    /**
     * Init and specify Comparator for keys.
     * 
     * @param comp
     */
    public RankingMap(Comparator<Object> comp) {
        super(comp);
    }

    /**
     * Get a maximum of count sorted keys. For key-based truncation, cf.
     * headMap; For key-based truncation, cf. headMap and subMap
     * 
     * @param count
     * @return
     */
    public List<K> sortedKeys(int count) {
        ArrayList<K> a = new ArrayList<K>();

        for (K key : keySet()) {
            a.add(key);
            if (--count == 0)
                break;
        }
        return a;
    }

    /**
     * Get sorted keys.
     * 
     * @return
     */
    public List<K> sortedKeys() {
        return sortedKeys(Integer.MAX_VALUE);
    }

    /**
     * Get a maximum of count sorted values. For key-based truncation, cf.
     * headMap and subMap;
     * 
     * @param count
     * @return
     */
    public List<V> sortedValues(int count) {
        ArrayList<V> a = new ArrayList<V>();
        for (V val : values()) {
            a.add(val);
            if (--count == 0)
                break;
        }
        return a;
    }

    /**
     * Get sorted values
     * 
     * @return
     */
    public List<V> sortedValues() {
        return sortedValues(Integer.MAX_VALUE);
    }

    /**
     * creates a new map with count values referenced (but not
     * 
     * @param count
     */
    public RankingMap<K, V> headMap(int count) {
        RankingMap<K, V> head = new RankingMap<K, V>();
        for (Map.Entry<K, V> e : entrySet()) {
            head.put(e.getKey(), e.getValue());
            if (--count == 0)
                break;
        }
        return head;
    }

    /**
     * Put the two arrays (of equal size) into the map
     * 
     * @param keys
     * @param values
     */
    public void put(K[] keys, V[] values) {
        put(Arrays.asList(keys), Arrays.asList(values));
    }

    /**
     * put keys and values
     * 
     * @param keys
     * @param values
     */
    public void put(List<K> keys, List<V> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("lists must have equal size.");
        }
        for (int i = 0; i < keys.size(); i++) {
            super.put(keys.get(i), values.get(i));
        }
    }

    /**
     * convert from primitive to Object array
     * 
     * @param a
     * @return
     */
    public static Double[] convert(double[] a) {
        Double[] b = new Double[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    /**
     * convert from primitive to Object array
     * 
     * @param a
     * @return
     */
    public static Integer[] convert(int[] a) {
        Integer[] b = new Integer[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    /**
     * convert from Object to primitive array
     * 
     * @param a
     * @return
     */
    public static double[] convert(Double[] a) {
        double[] b = new double[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    /**
     * convert from Object to primitive array
     * 
     * @param a
     * @return
     */
    public static int[] convert(Integer[] a) {
        int[] b = new int[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

}
