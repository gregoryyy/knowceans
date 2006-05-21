/*
 * Created on 21.05.2006
 */
package org.knowceans.util;

import java.lang.reflect.Array;

/**
 * ArrayUtils provides functionality for conversion between primitive and object
 * arrays.
 * 
 * @author gregor heinrich
 */
public class ArrayUtils {

    public static Class[][] types = {
        {Boolean.class, Byte.class, Double.class, Float.class, Integer.class,
            Long.class, Short.class},
        {Boolean.TYPE, Byte.TYPE, Double.TYPE, Float.TYPE, Integer.TYPE,
            Long.TYPE, Short.TYPE}};

    /**
     * Get an array of the number type corresponding to the element type of the
     * argument. The element type is determined from the runtime type of the
     * first element, which, even if the array was initialised as Object[], can
     * be a subclass of Object wrapping a primitive. However, the contract then
     * is that this element runtime type is equal for all elements in the array.
     * If the array has size 0, the class type is determined by the component
     * type, resulting in null return value if the array was not initialised as
     * an Wrapper[] where wrapper is one of the primitive wrapper object types
     * excluding Void.
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
     * Get an array of the number object type corresponding to the primitive
     * element type of the argument.
     * 
     * @param array array of a primitive type
     * @return array of the object type corresponding to the primitive type.
     */
    public static Object[] convert(Object array) {
        Class< ? extends Object> c = array.getClass();
        if (c.isArray() && c.getComponentType().isPrimitive()) {
            int length = Array.getLength(array);
            if (length == 0) {
                return new Object[0];
            }
            // automatic wrapping to object type in Array.get()
            Class< ? extends Object> elementType = Array.get(array, 0)
                .getClass();
            Object[] objects = (Object[]) Array
                .newInstance(elementType, length);
            for (int i = 0; i < length; i++) {
                objects[i] = Array.get(array, i);
            }
            return objects;
        }
        return null;
    }

}
