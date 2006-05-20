package org.knowceans.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.knowceans.util.Cokus;
import org.knowceans.util.TableList;

/**
 * RankingMap allows sorting of two lists of items simultaneously.
 * <p>
 * TODO: build ParallelCollection (or for primitive types a ParallelArray)
 * 
 * @author gregor
 */
public class RankingMap<K, V> extends InvertibleTreeMultiMap<K, V> {

    /**
     * RankEntry is a special pendent to Map.Entry with a wider purpose and
     * validity also after change of the RankingMap
     * 
     * @author gregor
     */
    public class RankEntry {
        private K key;
        private V value;

        public RankEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public final K getKey() {
            return key;
        }

        public final void setKey(K key) {
            this.key = key;
        }

        public final V getValue() {
            return value;
        }

        public final void setValue(V value) {
            this.value = value;
        }

    }

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
        System.out.println("Imagine these were scores:");
        RankingMap<Double, Integer> r = new RankingMap<Double, Integer>(
            Collections.reverseOrder());
        r.put(TableList.convert(a), TableList.convert(b));
        RankingMap<Double, Integer> s = r.headMap(0.8);
        System.out.println("Max 10 best scores above 0.8:");
        System.out.println(s.sortedKeys(10));
        System.out.println(s.sortedValues(10));
        System.out.println("Max 5 best scores below 0.8:");
        RankingMap<Double, Integer> t = r.tailMap(0.8);
        System.out.println(t.sortedKeys(5));
        System.out.println(t.sortedValues(5));

        System.out.println("Imagine these were distances:");
        r = new RankingMap<Double, Integer>();
        r.put(TableList.convert(a), TableList.convert(b));
        s = r.headMap(0.2);
        System.out.println("Max 10 least distances below 0.2:");
        System.out.println(s.sortedKeys(10));
        System.out.println(s.sortedValues(10));
        System.out.println("Max 5 least distances above 0.2:");
        t = r.tailMap(0.2);
        System.out.println(t.sortedKeys(5));
        System.out.println(t.sortedValues(5));

    }

    /**
     * 
     */
    public RankingMap() {
        super();
    }

    /**
     * Init with reverse ordering (reverse is not evaluated).
     */
    @SuppressWarnings("unused")
    public RankingMap(boolean reverse) {
        super(Collections.reverseOrder());
    }

    /**
     * Init and specify Comparator for keys.
     * 
     * @param comp
     */
    public RankingMap(Comparator< ? super K> comp) {
        super(comp);
    }

    /**
     * Create a ranking map from the map.
     * 
     * @param map
     */
    public RankingMap(Map<K, Set<V>> map) {
        putAll(map);
    }

    /**
     * Create a ranking map from the map.
     * 
     * @param map
     */
    public RankingMap(Map<K, Set<V>> map, Comparator< ? super K> comp) {
        super(comp);
        putAll(map);
    }

    /**
     * Get a maximum of count sorted keys. For key-based truncation, cf.
     * headMap; For key-based truncation, cf. headMap and subMap. If scores are
     * doubled, there is no guarantee of the key order.
     * 
     * @param count
     * @return
     */
    public List<K> sortedKeys(int count) {
        ArrayList<K> a = new ArrayList<K>();

        for (K key : keySet()) {
            for (int i = 0; i < get(key).size(); i++) {
                a.add(key);
                if (--count == 0)
                    return a;
            }
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
     * Get sorted key-value pairs with duplicate scores resolved
     * 
     * @param count maximum number of entries returned
     * @return
     */
    public List<RankEntry> entryList(int count) {

        ArrayList<RankEntry> a = new ArrayList<RankEntry>();
        for (K key : keySet()) {
            for (V val : get(key)) {
                a.add(new RankEntry(key, val));
            }
            if (--count == 0) {
                return a;
            }
        }
        return a;

    }
    
    public List<RankEntry> entryList() {
        return entryList(Integer.MAX_VALUE);
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
        for (Collection<V> vals : values()) {
            for (V val : vals) {
                a.add(val);
                if (--count == 0)
                    return a;
            }
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
        RankingMap<K, V> head = new RankingMap<K, V>(comparator());
        for (Map.Entry<K, Set<V>> e : entrySet()) {
            head.put(e.getKey(), e.getValue());
            if (--count == 0)
                break;
        }
        return head;
    }

    @Override
    public RankingMap<K, V> headMap(K fromKey) {
        return new RankingMap<K, V>(super.headMap(fromKey), comparator());
    }

    @Override
    public RankingMap<K, V> tailMap(K fromKey) {
        return new RankingMap<K, V>(super.tailMap(fromKey), comparator());
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
            super.add(keys.get(i), values.get(i));
        }
    }

}
