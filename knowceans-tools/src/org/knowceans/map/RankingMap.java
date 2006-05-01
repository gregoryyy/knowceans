/*
 * Created on 01.05.2006
 */
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
public class RankingMap<K, V> extends TreeMap<K, V> {

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
        // TODO Auto-generated constructor stub
    }

    /**
     * @param c
     */
    public RankingMap(Comparator< ? super K> c) {
        super(c);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param m
     */
    public RankingMap(Map< ? extends K, ? extends V> m) {
        super(m);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param m
     */
    public RankingMap(SortedMap<K, ? extends V> m) {
        super(m);
        // TODO Auto-generated constructor stub
    }

    public List<K> sortedKeys(int count) {
        ArrayList<K> a = new ArrayList<K>();

        for (K key : keySet()) {
            a.add(key);
            if (--count == 0)
                break;
        }
        return a;
    }

    public List<K> sortedKeys() {
        return sortedKeys(Integer.MAX_VALUE);
    }

    public List<V> sortedValues(int count) {
        ArrayList<V> a = new ArrayList<V>();
        for (V val : values()) {
            a.add(val);
            if (--count == 0)
                break;
        }
        return a;
    }

    public List<V> sortedValues() {
        return sortedValues(Integer.MAX_VALUE);
    }

    void put(K[] keys, V[] values) {
        put(Arrays.asList(keys), Arrays.asList(values));
    }

    /**
     * put keys and values
     * 
     * @param keys
     * @param values
     */
    void put(List<K> keys, List<V> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("lists must have equal size.");
        }
        for (int i = 0; i < keys.size(); i++) {
            super.put(keys.get(i), values.get(i));
        }
    }

    public static Double[] convert(double[] a) {
        Double[] b = new Double[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    public static Integer[] convert(int[] a) {
        Integer[] b = new Integer[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    public static double[] convert(Double[] a) {
        double[] b = new double[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    public static int[] convert(Integer[] a) {
        int[] b = new int[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

}
