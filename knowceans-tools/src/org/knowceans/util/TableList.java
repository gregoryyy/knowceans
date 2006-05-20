package org.knowceans.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TableList handles parallel lists whose elements with same index are accessed,
 * sorted etc. simultaneously. Internally, each element of the list is a list on
 * its own, representing the fields of the list. Filtering operations are
 * provided via the filter method and Filter interface.
 * <p>
 * This class is optimised for coding rather than runtime efficiency.
 * Particularly, manipulating the structure of the fields (columns) is expensive
 * as it iterates through all rows. Sorting, shuffling etc. are provided by the
 * static Collections methods. To find rows of large lists, first sort and then
 * do binary search via the collections interface.
 * <p>
 * TODO: alternatively set of HashMaps with TreeMap views for sorting indices
 * 
 * @author gregor
 */
public class TableList extends ArrayList<TableList.Rows> {

    public static void main(String[] args) {
        int[] a = Samplers.randPerm(100000);
        double[] b = Samplers.randDir(0.1, 100000);
        System.out.println(Which.usedMemory());
        List<Integer> aa = Arrays.asList(TableList.convert(a));
        List<Double> bb = Arrays.asList(TableList.convert(b));
        StopWatch.start();
        final TableList p = new TableList();
        p.addList("index", aa);
        p.addList("value", bb);
        p.addIndexList("id");
        System.out.println(p.subList(1, 5));
        Collections.sort(p);
        System.out.println(p.subList(1, 5));
        System.out.println(p.subList(1, 5).getList("index"));
        System.out.println(p.subList(1, 5).getList("id"));
        System.out.println(p.subList(1, 5).getList("value"));
        p.setSortField("value");
        Collections.sort(p, Collections.reverseOrder());

        TableList p2 = p.filter(new Filter() {

            public boolean valid(Rows row) {
                int id = (Integer) row.get(p.getFields().indexOf("id"));
                if (id > 2000 && id < 3000)
                    return true;
                return false;
            }
        });

        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(p2.subList(1, 5).getList("index"));
        System.out.println(p2.subList(1, 5).getList("id"));
        System.out.println(p2.subList(1, 5).getList("value"));
        System.out.println(Which.usedMemory());

    }

    // helper classes

    /**
     * RowMap extends hash map by a comparison capability over the map.
     * 
     * @author gregor
     */
    @SuppressWarnings("serial")
    protected class Rows extends ArrayList<Object> implements Comparable<Rows> {

        @SuppressWarnings("unchecked")
        public int compareTo(Rows rr) {
            Comparable c1 = (Comparable) get(sortCol);
            Comparable c2 = (Comparable) rr.get(sortCol);
            return c1.compareTo(c2);
        }
    }

    /**
     * Filter allows to filter entries by calling filter with an implementation
     * of this interface.
     */
    public interface Filter {
        boolean valid(Rows row);
    }

    // fields

    /**
     * 
     */
    private static final long serialVersionUID = 8611765516306513144L;
    private int sortCol = 0;
    private List<String> fields = null;

    // constructors

    /**
     * 
     */
    public TableList() {
        super();
        init();
    }

    /**
     * @param initialCapacity
     */
    public TableList(int initialCapacity) {
        super(initialCapacity);
        init();
    }

    /**
     * Initialise the parallel list with an existing list. The sorting key is
     * set to 0 -- the first field.
     * 
     * @param list
     * @param key
     */
    public TableList(List<Rows> list, List<String> fields) {
        super();
        this.fields = fields;
        addAll(list);
    }

    /**
     * Initialise fields
     */
    private void init() {
        fields = new ArrayList<String>();
    }

    // methods

    /**
     * Add a list to the internal maps.
     * 
     * @param a
     */
    void addList(String key, List< ? extends Object> a) {
        fields.add(key);
        if (size() == 0) {
            for (int i = 0; i < a.size(); i++) {
                Rows h = new Rows();
                h.add(a.get(i));
                add(h);
            }
        } else if (a.size() != size()) {
            throw new IllegalArgumentException("sizes don't match.");
        } else {
            for (int i = 0; i < size(); i++) {
                get(i).add(a.get(i));
            }
        }
    }

    /**
     * Adds an index to the list. After sorting, this way the original sorting
     * order can be tracked.
     * 
     * @param key
     */
    void addIndexList(String key) {
        fields.add(key);
        for (int i = 0; i < size(); i++) {
            get(i).add(i);
        }
    }

    /**
     * Remove the list with key from the internal maps.
     * 
     * @param key
     */
    public void removeList(String key) {
        int index = fields.indexOf(key);
        fields.remove(index);
        for (int i = 0; i < size(); i++) {
            get(i).remove(index);
        }
    }

    /**
     * Get the list with the specified key.
     * 
     * @param index
     */
    public ArrayList<Object> getList(String key) {
        return getList(fields.indexOf(key));
    }

    /**
     * Get the list with the specified key.
     * 
     * @param index
     */
    public ArrayList<Object> getList(int index) {

        ArrayList<Object> list = new ArrayList<Object>();
        for (int i = 0; i < size(); i++) {
            list.add(get(i).get(index));
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
    public Object get(int field, int index) {
        return get(index).get(field);
    }

    /**
     * Get one element of the list with the specified key.
     * 
     * @param key
     * @param index
     * @return
     */
    public Object get(String key, int index) {
        return get(index).get(fields.indexOf(key));
    }

    /**
     * Get a sublist of this list according to the indices of the current
     * sorting. The actual values and field names are referenced.
     * 
     * @param filt
     * @return
     */
    @Override
    public TableList subList(int fromIndex, int toIndex) {
        return new TableList(super.subList(fromIndex, toIndex), fields);
    }

    /**
     * Get a sublist of this list according to the filter criterion. The actual
     * values and field names are referenced.
     * 
     * @param filt
     * @return
     */
    public TableList filter(Filter filt) {
        TableList list = new TableList();
        for (int i = 0; i < size(); i++) {
            if (filt.valid(get(i))) {
                list.add(get(i));
            }
        }
        list.fields = fields;
        return list;
    }

    /**
     * Set the key by which the list is to be compared. TODO: priority of keys
     * for subcomparisons.
     * 
     * @param key
     */
    public void setSortField(String key) {
        setSortField(fields.indexOf(key));
    }

    /**
     * Set the key by which the list is to be compared. TODO: priority of keys
     * for subcomparisons.
     * 
     * @param key
     */
    public void setSortField(int key) {
        sortCol = key;
    }

    /**
     * Get field index of key.
     * 
     * @param key
     * @return
     */
    public int getField(String key) {
        return fields.indexOf(key);
    }

    /**
     * Get key of field index.
     * 
     * @param key
     * @return
     */
    public String getField(int key) {
        return fields.get(key);
    }

    /**
     * Get the field names of the table list.
     * 
     * @return
     */
    public List<String> getFields() {
        return fields;
    }

    // static helpers

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
