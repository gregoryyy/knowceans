/*
 * Created on Dec 10, 2005
 */
package org.knowceans.util;

import java.io.Serializable;
import java.util.Collection;

/**
 * ItemList represents a circular list of items that can be browsed with next
 * and prev commands in jsp pages (eg, result sets). The list ensures uniqueness
 * of its elements with the conservative strategy of the SetVector class. This
 * allows to control the list via its element values. E.g., in a result set with
 * paging, if links such as [first, back, forward, last] in a web page are used,
 * the item associated with the new page can be directly found via this element.
 * 
 * @author heinrich
 */
public class ItemList<E> implements Serializable /* extends Vector<T> */{

    /*
     * Note: This class cannot be a subclass of [Set]Vector, as would be nice
     * design. JSTL/EL operator translation precedence does not recognise
     * constructs like ${items.next} as items.getNext() but rather as string
     * index items.get("next").
     */
    SetVector<E> data;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * the state of the current list.
     */
    private int state;

    /**
     * 
     */
    public ItemList() {
        // super();
        data = new SetVector<E>();
        state = 0;
    }

    /**
     * @param c
     */
    public ItemList(Collection< ? extends E> c) {
        // super(c);
        data = new SetVector<E>(c);
        state = 0;
    }

    /**
     * @param initialCapacity
     * @param capacityIncrement
     */
    public ItemList(int initialCapacity, int capacityIncrement) {
        // super(initialCapacity, capacityIncrement);
        data = new SetVector<E>(initialCapacity, capacityIncrement);
        state = 0;
    }

    /**
     * @param initialCapacity
     */
    public ItemList(int initialCapacity) {
        // super(initialCapacity);
        data = new SetVector<E>(initialCapacity);
        state = 0;
    }

    /**
     * get the state of the list
     * 
     * @return
     */
    public final int getState() {
        return state;
    }

    /**
     * sets the state of the list, if the state is larger than the size, it is
     * set to zero
     * 
     * @param newstate
     */
    public final void setState(int newstate) {
        if (newstate > data.size() - 1) {
            newstate = 0;
        }
        if (newstate < 0) {
            newstate = data.size() - 1;
        }
        System.out.println("State = " + state + ", Value = " + data.get(state));
        this.state = newstate;
    }

    /**
     * set the new state of the variable
     * 
     * @param newvalue
     * @return the state
     */
    public final int setStateByValue(E newvalue) {
        int temp = data.indexOf(newvalue);
        if (temp != -1) {
            state = temp;
        }
        return state;
    }

    /**
     * @return the next item or the first one or null for zero size
     */
    public E getNext() {
        if (data.size() == 0) {
            return null;
        }
        if (state < data.size() - 1) {
            return data.get(state + 1);
        } else {
            return data.get(0);
        }
    }

    /**
     * @return the next item or the first one or null for zero size
     */
    public E goNext() {
        if (data.size() == 0) {
            return null;
        }
        setState(state + 1);
        return data.get(state);
    }

    /**
     * @return the current item or null for zero size
     */
    public E getCurr() {
        if (data.size() == 0) {
            return null;
        }
        return data.get(state);
    }

    /**
     * @return the previous item or the last one or null for zero size
     */
    public E getPrev() {
        if (data.size() == 0) {
            return null;
        }
        if (state > 0) {
            return data.get(state - 1);
        } else {
            return data.get(data.size() - 1);
        }
    }

    /**
     * @return the previous item or the last one or null for zero size
     */
    public E goPrev() {
        if (data.size() == 0) {
            return null;
        }
        setState(state - 1);
        return data.get(state);
    }

    /**
     * @return the last item or null for zero size
     */
    public E getLast() {
        if (data.size() == 0) {
            return null;
        }
        return data.get(data.size() - 1);
    }

    /**
     * @return the last item or null for zero size
     */
    public E goLast() {
        if (data.size() == 0) {
            return null;
        }
        setState(data.size() - 1);
        return data.get(state);
    }

    /**
     * @return the first item or null for zero size
     */
    public E getFirst() {
        if (data.size() == 0) {
            return null;
        }
        return data.get(0);
    }

    /**
     * @return the first item or null for zero size
     */
    public E goFirst() {
        if (data.size() == 0) {
            return null;
        }
        setState(0);
        return data.get(0);
    }

    /**
     * the size of the data vector
     * 
     * @return
     */
    public int getSize() {
        return data.size();
    }

    /**
     * get the data vector
     * 
     * @return
     */
    public final SetVector<E> getData() {
        return data;
    }

    /**
     * set the data vector
     * 
     * @param data
     */
    public final void setData(SetVector<E> data) {
        this.data = data;
    }

}
