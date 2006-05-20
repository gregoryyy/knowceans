package org.knowceans.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * TableList handles parallel lists whose elements with same index can be
 * accessed, sorted, filtered etc. simultaneously. Internally, each element of
 * the list is a list on its own, representing the fields of the list. The
 * contract is that these element lists are of equal size when manipulating
 * single elements. Filtering operations are provided via the filter method and
 * Filter interface.
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
public class TableList extends ArrayList<TableList.Fields> {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int[] a = Samplers.randPerm(1000);
        double[] b = Samplers.randDir(0.1, 1000);
        System.out.println(Which.usedMemory());
        List<Integer> aa = Arrays.asList(TableList.convert(a));
        List<Double> bb = Arrays.asList(TableList.convert(b));
        StopWatch.start();

        TableList list = new TableList();
        list.addList("key", aa);
        list.addList("value", bb);
        list.addIndexList("index");

        System.out.println(list);

        list.sort("key", false);

        System.out.println(list);

        list.sort("value", true);

        System.out.println(list);

        TableList list2 = list.getSubList(0, 3);

        list2.sort("value", false);

        System.out.println(list2);

        TableList list3 = list.filter(
            list.new FieldBetween("value", 0.001, 0.01, false)).sort("key",
            false);

        System.out.println(list3);

        list3.addAll(list2);

        list3.sort("index", false);

        System.out.println(list3);

        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(Which.usedMemory());

    }

    // helper classes

    /**
     * FieldSorter sorts fields according to a numeric field.
     * 
     * @author gregor
     */
    public class FieldSorter implements Comparator<Fields> {
        private int field;
        private boolean reverse;

        /**
         * Initialise the sorter using the field to sort by and the direction.
         * 
         * @param field
         * @param reverse
         */
        public FieldSorter(int field, boolean reverse) {
            this.field = field;
            this.reverse = reverse;
        }

        @SuppressWarnings("unchecked")
        public int compare(Fields o1, Fields o2) {
            Comparable c1 = (Comparable) o1.get(field);
            Comparable c2 = (Comparable) o2.get(field);
            // multifield sorting: if cmp == 0, take next field
            return c1.compareTo(c2) * (reverse ? -1 : 1);
        }
    }

    /**
     * Fields extends an array list by a comparison capability over the map
     * list. By default, the field in index 0 is the order key when using
     * Collections.sort(). For other fields, use a FieldSorter instance.
     * 
     * @author gregor
     */
    @SuppressWarnings("serial")
    public class Fields extends ArrayList<Object> {
        @SuppressWarnings("unchecked")
        public int compareTo(Fields rr) {
            Comparable c1 = (Comparable) get(0);
            Comparable c2 = (Comparable) rr.get(0);
            return c1.compareTo(c2);
        }
    }

    /**
     * Filter allows to filter entries by calling filter with an implementation
     * of this interface.
     */
    public interface Filter {
        boolean valid(Fields row);
    }

    /**
     * SingleFieldFilter represents the common case of filtering according to
     * the value of one field.
     * 
     * @author gregor
     */
    public abstract class SingleFieldFilter implements Filter {

        protected int field;
        protected Object value;

        public SingleFieldFilter(String field, Object value) {
            this.field = fields.indexOf(field);
            this.value = value;
        }
    }

    /**
     * FieldEquals is an equals condition
     * 
     * @author gregor
     */
    public class FieldEquals extends SingleFieldFilter {

        public FieldEquals(String field, Object value) {
            super(field, value);
        }

        public boolean valid(Fields row) {
            return row.get(field).equals(value);
        }
    }

    /**
     * FieldLessThan checks if field less than.
     * 
     * @author gregor
     */
    public class FieldLessThan extends SingleFieldFilter {

        protected boolean allowsEqual;

        public FieldLessThan(String field, Object value, boolean orEqual) {
            super(field, value);
            allowsEqual = orEqual;
        }

        @SuppressWarnings("unchecked")
        public boolean valid(Fields row) {
            int a = ((Comparable) row.get(field)).compareTo(value);
            return a < 0 ? true : a == 0 ? allowsEqual : false;
        }
    }

    /**
     * FieldLargerThan checks if field larger than value.
     * 
     * @author gregor
     */
    public class FieldGreaterThan extends FieldLessThan {

        public FieldGreaterThan(String field, Object value, boolean orEqual) {
            super(field, value, orEqual);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean valid(Fields row) {
            int a = ((Comparable) row.get(field)).compareTo(value);
            return a > 0 ? true : a == 0 ? allowsEqual : false;
        }

    }

    /**
     * FieldBetween checks if the field is between low and high value.
     * <p>
     * TODO: with null values could be a generalisation of less and larger than.
     * 
     * @author gregor
     */
    public class FieldBetween extends FieldLessThan {

        private Object value2;

        public FieldBetween(String field, Object low, Object high,
            boolean orEqual) {
            super(field, low, orEqual);
            this.value2 = high;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean valid(Fields row) {
            int a = ((Comparable) row.get(field)).compareTo(value);
            if (a < 0 || a == 0 && !allowsEqual)
                return false;
            int b = ((Comparable) row.get(field)).compareTo(value2);
            if (b > 0 || a == 0 && !allowsEqual)
                return false;
            return true;
        }
    }

    // fields
    /**
     * 
     */
    private static final long serialVersionUID = 8611765516306513144L;
    private List<String> fields = null;

    // constructors

    /**
     * 
     */
    public TableList() {
        super();
        fields = new ArrayList<String>();
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
     * @param field
     */
    public TableList(List<Fields> list, List<String> fields) {
        this(fields);
        addAll(list);
    }

    /**
     * Copy constructor.
     * 
     * @param list
     */
    public TableList(TableList list) {
        this(list.fields);
        addAll(list);
    }

    /**
     * Inner constructor to prepare list copying. The field names are copied.
     * 
     * @param fields
     * @param sortField
     */
    protected TableList(List<String> fields) {
        super();
        this.fields = new ArrayList<String>(fields.size());
        this.fields.addAll(fields);
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
                Fields h = new Fields();
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
     * @param field
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
     * sorting, as a copy. The actual values are referenced, field names are
     * copied.
     * 
     * @param filt
     * @return
     */
    public TableList getSubList(int fromIndex, int toIndex) {
        TableList ll = new TableList(fields);
        ll.addAll(subList(fromIndex, toIndex));
        return ll;
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
     * Get a sublist of this list according to the filter criterion. The actual
     * values and field names are copied.
     * 
     * @param filt
     * @return
     */
    public TableList getFilterCopy(Filter filt) {
        TableList filtered = filter(filt);
        return new TableList(filtered);
    }

    /**
     * Sort the table list by the specified field. Use the Collections.sort() or
     * sort(Comparator<Fields>) method for other comparators.
     * 
     * @param field
     * @param reverse
     * @return
     */
    public synchronized TableList sort(String field, boolean reverse) {
        Collections.sort(this, new FieldSorter(fields.indexOf(field), reverse));
        return this;
    }

    /**
     * Sort the table with the specific comparator given. Alternative to
     * Collections.sort().
     * 
     * @param comp
     * @return
     */
    public synchronized TableList sort(Comparator<Fields> comp) {
        Collections.sort(this, comp);
        return this;
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

    // static helpers for primitive arrays.

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
