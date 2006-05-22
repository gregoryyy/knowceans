/*
 * Created on 21.05.2006
 */
package org.knowceans.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * ArrayUtils provides functionality for conversion between primitive and object
 * arrays and lists.
 * 
 * @author gregor heinrich
 */
public class ArrayUtils {

    public static void main(String[] args) {
        Integer[] a = new Integer[] {1, 2, 3};
        Object b = convert(a);
        System.out.println(Vectors.print((int[]) b) + " "
            + b.getClass().getComponentType());
        Object[] c = convert(b);
        System.out.println(Vectors.print(c) + " "
            + c.getClass().getComponentType());
        int[] d = new int[0];
        Object[] e = convert(d);
        System.out.println(Vectors.print(e) + " "
            + e.getClass().getComponentType());
        Object f = convert(e);
        System.out.println(Vectors.print((int[]) f) + " "
            + f.getClass().getComponentType());
        List< ? > h = asList(b);
        System.out.println(h);
        int[] i = (int[]) asPrimitiveArray(h);
        System.out.println(Vectors.print(i));
    }

    public static final Class[][] types = {
        {Boolean.class, Byte.class, Double.class, Float.class, Integer.class,
            Long.class, Short.class},
        {Boolean.TYPE, Byte.TYPE, Double.TYPE, Float.TYPE, Integer.TYPE,
            Long.TYPE, Short.TYPE}};

    /**
     * Get an array of the number type corresponding to the element type of the
     * argument. The element type is determined from the runtime type of the
     * first element, which, even if the array was initialised as Object[], can
     * be a subclass of Object wrapping a primitive.
     * <p>
     * The contract is that the element runtime type is equal for all elements
     * in the array and no null element is included, otherwise runtime
     * exceptions will be thrown. If the array has size 0, the class type is
     * determined by the component type, resulting in null return value if the
     * array was not initialised as an Wrapper[] where wrapper is one of the
     * primitive wrapper object types excluding Void.
     * 
     * @param objects object array with elements of primitive wrapper classes
     *        excluding Void.
     * @return an array of the relevant primitive type or null if size = 0 and
     *         or invalid type.
     */
    public static Object convert(Object[] objects) {
        Class< ? extends Object> c = null;
        if (objects.length == 0) {
            c = objects.getClass().getComponentType();
        } else {
            c = objects[0].getClass();
        }
        // no standard method found to get primitive types from their wrappers
        for (int i = 0; i < types[0].length; i++) {
            if (c.equals(types[0][i])) {
                Object array = Array.newInstance(types[1][i], objects.length);
                for (int j = 0; j < objects.length; j++) {
                    Array.set(array, j, objects[j]);
                }
                return array;
            }
        }
        return null;
    }

    /**
     * Get an array of the wrapper type corresponding to the primitive element
     * type of the argument.
     * 
     * @param array array of a primitive type
     * @return array of the object type corresponding to the primitive type or
     *         null if invalid element type or no array in argument.
     */
    @SuppressWarnings("unchecked")
    public static Object[] convert(Object array) {
        Class< ? extends Object> c = array.getClass();
        if (c.isArray() && c.getComponentType().isPrimitive()) {
            int len = Array.getLength(array);
            // handle zero-length arrays
            if (len == 0) {
                // automatic wrapping to object type in native Array.get()
                c = array.getClass().getComponentType();
                for (int i = 0; i < types[1].length; i++) {
                    if (c.equals(types[1][i])) {
                        c = types[0][i];
                        return (Object[]) Array.newInstance(c, 0);
                    }
                }
            }
            // automatic wrapping to object type in native Array.get()
            c = Array.get(array, 0).getClass();
            Object[] objects = (Object[]) Array.newInstance(c, len);
            for (int i = 0; i < len; i++) {
                objects[i] = Array.get(array, i);
            }
            return objects;
        }
        return null;
    }

    /**
     * Convert an array of primitive-type elements into a list of its
     * wrapper-type elements.
     * 
     * @param array
     * @return the array or null if invalid
     */
    public static List< ? > asList(Object array) {
        Object[] a = convert(array);
        if (a == null) {
            return null;
        }
        return Arrays.asList(a);
    }

    /**
     * Convert an list of objects into an array of primitive types. This extends
     * the functionality of the List.toArray() method to primitive types. If the
     * list does not consist of wrapper-type elements or if it has zero length,
     * null is returned (in the second case because the element type cannot be
     * determined).
     * 
     * @param objects
     * @return array of primitive types or null if invalid.
     */
    public static Object asPrimitiveArray(List<? extends Object> objects) {
        Class< ? extends Object> c = null;
        if (objects.size() == 0) {
            return null;
        } else {
            c = objects.get(0).getClass();
        }
        // no standard method found to get primitive types from their wrappers
        for (int i = 0; i < types[0].length; i++) {
            if (c.equals(types[0][i])) {
                Object array = Array.newInstance(types[1][i], objects.size());
                for (int j = 0; j < objects.size(); j++) {
                    Array.set(array, j, objects.get(j));
                }
                return array;
            }
        }
        return null;
    }
}
