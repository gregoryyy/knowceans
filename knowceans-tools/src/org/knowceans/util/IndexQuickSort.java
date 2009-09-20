/*
 * Created on Sep 20, 2009
 */
package org.knowceans.util;

import org.knowceans.util.Vectors;

/**
 * IndexQuickSort sorts indices of an array <br>
 * TODO: generalise to comparable items
 * 
 * @author gregor
 * @author original code at
 *         http://www.cs.princeton.edu/introcs/42sort/QuickSort.java.html
 */
public class IndexQuickSort {

    public static void main(String[] args) {
        double[] weights = new double[] {0.1, 0.1, 0.001, 0.05, 0.2, 0.3, 0.5,
            0.03, 0.02, 0.1};
        int[] index = Vectors.range(0, weights.length - 1);
        quicksort2(weights, index);
        for (int i = 0; i < index.length; i++) {
            System.out.println(i + "\t" + weights[i] + "\t" + index[i]);
        }
    }

    /**
     * just sort indices
     * 
     * @param main
     * @param index
     */
    public static void quicksort2(double[] main, int[] index) {
        idxqsort(main, index, 0, index.length - 1);
    }

    ///////////////////

    // quicksort a[left] to a[right]
    public static void idxqsort(double[] a, int[] index, int left, int right) {
        if (right <= left)
            return;
        int i = idxpartition(a, index, left, right);
        idxqsort(a, index, left, i - 1);
        idxqsort(a, index, i + 1, right);
    }

    // partition a[left] to a[right], assumes left < right
    private static int idxpartition(double[] a, int[] index, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (a[index[++i]] < a[index[right]])
                // find item on left to swap
                // a[right] acts as sentinel
                ;
            while (a[index[right]] < a[index[--j]])
                // find item on right to swap
                if (j == left)
                    // don't go out-of-bounds
                    break;
            if (i >= j)
                // check if pointers cross
                break;
            // swap two elements into place
            swap(index, i, j);
        }
        // swap with partition element
        swap(index, i, right);
        return i;
    }

    // swap indices i and j
    private static void swap(int[] index, int i, int j) {
        int b = index[i];
        index[i] = index[j];
        index[j] = b;
    }

}
