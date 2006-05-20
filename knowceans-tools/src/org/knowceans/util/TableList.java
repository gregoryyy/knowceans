/*
 * Created on 20.05.2006
 */
package org.knowceans.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.knowceans.util.Samplers;

/**
 * TableList handles parallel lists whose elements with same index are accessed,
 * sorted etc. simultaneously.
 * <p>
 * This class is optimised for coding rather than runtime efficiency. Sorting,
 * shuffling etc. are provided by the static Collections methods.
 * <p>
 * TODO: alternatively set of HashMaps with TreeMap views for sorting indices
 * 
 * @author gregor
 */
public class TableList extends ArrayList<TableList.RowMap> {

    public static void main(String[] args) {
        int[] a = Samplers.randPerm(100);
        double[] b = Samplers.randDir(0.1, 100);
        List<Integer> aa = Arrays.asList(TableList.convert(a));
        List<Double> bb = Arrays.asList(TableList.convert(b));
        TableList p = new TableList();
        p.addList("indices", aa);
        p.addList("values", bb);
        p.addIndexList("id");
        System.out.println(p.subList(1, 5));
        Collections.sort(p);
        System.out.println(p.subList(1, 5));
        System.out.println(p.subList(1, 5).getList("indices"));
        System.out.println(p.subList(1, 5).getList("id"));
        System.out.println(p.subList(1, 5).getList("values"));
        p.setSortKey("values");
    }

    /**
     * RowMap extends hash map by a comparison capability over the map.
     * 
     * @author gregor
     */
    @SuppressWarnings("serial")
    protected class RowMap extends HashMap<String, Object> implements
        Comparable<RowMap> {

        @SuppressWarnings("unchecked")
        public int compareTo(RowMap o) {
            // return ((CompareMap) get(compareKey)).compareTo((CompareMap) o
            // .get(compareKey));
            Comparable c1 = (Comparable) get(sortKey);
            Comparable c2 = (Comparable) o.get(sortKey);
            return c1.compareTo(c2);
        }
    }

    /**
     * Filter allows to filter entries by calling filter with an implementation
     * of this interface.
     */
    public interface Filter {
        boolean isIn(RowMap row);
    }

    /**
     * 
     */
    public TableList() {
        super();
    }

    /**
     * @param initialCapacity
     */
    public TableList(int initialCapacity) {
        super(initialCapacity);
        // TODO Auto-generated constructor stub
    }

    /**
     * Initialise the parallel list with an existing list. The sorting key is
     * undefined (the first iteration).
     * 
     * @param list
     * @param key
     */
    public TableList(List<RowMap> list) {
        super();
        addAll(list);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 8611765516306513144L;
    private String sortKey;

    /**
     * Set the key by which the list is to be compared. TODO: priority of keys
     * for subcomparisons.
     * 
     * @param key
     */
    void setSortKey(String key) {
        sortKey = key;
    }

    /**
     * Add a list to the internal maps. If no compare key is set, the list is
     * sorted by the first list added by this method.
     * 
     * @param a
     */
    void addList(String key, List< ? extends Object> a) {
        if (size() == 0) {
            for (int i = 0; i < a.size(); i++) {
                RowMap h = new RowMap();
                h.put(key, a.get(i));
                add(h);
            }
        } else if (a.size() != size()) {
            throw new IllegalArgumentException("sizes don't match.");
        } else {
            for (int i = 0; i < size(); i++) {
                get(i).put(key, a.get(i));
            }
        }
        if (sortKey == null) {
            sortKey = key;
        }
    }

    /**
     * Adds an index to the list. After sorting, this way the original sorting
     * order can be tracked.
     * 
     * @param key
     */
    void addIndexList(String key) {
        for (int i = 0; i < size(); i++) {
            get(i).put(key, i);
        }
    }

    /**
     * Remove the list with key from the internal maps.
     * 
     * @param key
     */
    public void removeList(String key) {
        for (int i = 0; i < size(); i++) {
            get(i).remove(key);
        }
    }

    /**
     * Get the list with the specified key.
     * 
     * @param key
     */
    public ArrayList<Object> getList(String key) {
        ArrayList<Object> list = new ArrayList<Object>();
        for (int i = 0; i < size(); i++) {
            list.add(get(i).get(key));
        }
        return list;
    }

    /**
     * Get one element of the list with the specified key.
     * 
     * @param key
     * @param index
     * @return
     */
    public Object get(String key, int index) {
        return get(index).get(key);
    }

    @Override
    public TableList subList(int fromIndex, int toIndex) {
        return new TableList(super.subList(fromIndex, toIndex));
    }

    /**
     * @param filt
     * @return
     */
    public TableList filter(Filter filt) {
        TableList list = new TableList();
        for (int i = 0; i < size(); i++) {
            if (filt.isIn(get(i))) {
                list.add(get(i));
            }
        }
        return list;
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

    public final String getSortKey() {
        return sortKey;
    }

}
