/*
 * Created on Sep 23, 2009
 */
package org.knowceans.util;

import java.util.Random;

/**
 * FastMultinomial implements a fast multinomial sampler that exploits
 * heavy-tail distribution of the probability masses. It assumes that the
 * weights are based on a product term and uses a series of bounds on the
 * normalisation constant of the sampling distribution that allows to calculate
 * the terms iteratively. The algorithm was described for a fast LDA Gibbs
 * sampler in Porteous, Newman and Welling (KDD 2008) and is here used more
 * generically.
 * <p>
 * The core data structure is the weights array abc that contains a multinomial
 * factor distribution in each of its rows. Operation requires to keep an index
 * of sorted elements of abc, for which the dominant element should be chosen
 * with a call to indexsort(abc[dominantindex]). Further, a norm of all of the
 * rows of abc should be calculated using sumcube(abc[i]), not including the
 * root operation. Finally, samples can be taken from the distribution, updating
 * the state of the sampler in all three parts, abc, the norm and the sort
 * index.
 * <p>
 * This class is a proof of concept that does not save much computing time
 * because it does pre-compute the weights beforehand.
 * 
 * @author gregor
 */
public class FastMultinomial {

    public static void main(String[] args) {
        int nsamp = 100000;
        double[] a = new double[] {0.2, 0.1, 0.001, 0.01, 0.02, 0.003, 0.05,
            0.003, 0.2, 0.1};
        double[] b = Vectors.ones(a.length, 3.);
        double[] c = Vectors.ones(a.length, 2.);
        double[][] ww = new double[][] {a, b, c};

        // order weights
        int[] idx = Vectors.range(0, a.length - 1);
        indexsort(a, idx);
        // set up norms
        double[] wwnorm = new double[ww.length];
        for (int i = 0; i < wwnorm.length; i++) {
            wwnorm[i] = sumcube(ww[i]);
        }

        // now sample
        int[] samples = new int[nsamp];
        Random rand = new CokusRandom();
        for (int i = 0; i < nsamp; i++) {
            samples[i] = sample(ww, wwnorm, idx, rand);
        }

        // check empirical distribution
        Histogram.hist(System.out, samples, 10);
    }

    /**
     * Fast sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * Further, the cube norm of each of the factors is given in abcnorm
     * (without the inverse exponential operation), as well as the indexes of
     * the values of abc ordered decending (by one of the factors).
     * 
     * @param abc weight factors of multinomial masses [I x K]
     * @param abcnorm cube norms of factors [I]
     * @param idx descending order of weight factors
     * @param rand random sampler
     * @return
     */
    public static int sample(double[][] abc, double[] abcnorm, int[] idx,
        Random rand) {
        int I = abc.length;
        int K = abc[0].length;
        int ksorted;
        double Zkprev;
        double Zk = Double.POSITIVE_INFINITY;
        double[] p = new double[K];

        // get norms that are decremented
        // by local elements:
        // norm(a_l:K-1) starting with (a_0:K-1)
        double[] abcl2k = new double[I];
        for (int i = 0; i < I; i++) {
            abcl2k[i] = abcnorm[i];
        }

        double u = rand.nextDouble();
        for (int k = 0; k < K; k++) {

            // p[k] = p[k-1] + prod_i abc_ik
            p[k] = k == 0 ? 0 : p[k - 1];
            ksorted = idx[k];
            double abck = abc[0][ksorted];
            for (int i = 1; i < I; i++) {
                abck *= abc[i][ksorted];
            }
            p[k] += abck;

            // first reduce the norm by the current 
            // element: norm(a_i,l+1:K) = (-a_i,l^3 + sum_l:K a_i^3)^-3
            double abcl2knorm = 1;
            for (int i = 0; i < I; i++) {
                abcl2k[i] -= cube(abc[i][ksorted]);
                if (abcl2k[i] < 0) {
                    abcl2k[i] = 0;
                }
                abcl2knorm *= abcl2k[i];
            }

            // store this in case it was the previous topic
            Zkprev = Zk;
            // Zk = p[k] + prod_i norm(a_i,l+1:K)
            Zk = p[k] + cuberoot(abcl2knorm);

            if (u > p[k] / Zk) {
                continue;
            } else {
                if (k == 0 || u * Zk > p[k - 1]) {
                    // current topic ? 
                    return ksorted;
                } else {
                    // previous topic (via s_lk) ?
                    // scale and shift u
                    u = (u * Zkprev - p[k - 1]) * Zk / (Zkprev - Zk);
                    for (int kprev = 0; kprev < k; kprev++) {
                        if (p[kprev] >= u) {
                            return idx[kprev];
                        }
                    } // for each previous topic
                } // if current topic
            } // if p too low
        } // for k
        // should never reach this...
        return -1;
    }

    /**
     * sort idx according to reverse order of elements of x, leaving x
     * untouched.
     * 
     * @param x [in] unsorted array
     * @param idx [in/out] index array reordered so x[idx[0..K-1]] has
     *        descending order.
     */
    public static void indexsort(double[] x, int[] idx) {
        IndexQuickSort.sort(x, idx);
        IndexQuickSort.reverse(idx);
    }

    /**
     * reorder the index of a sorted array after element kinc had been
     * incremented and kdec decremented (referring to the indices in idx). This
     * is a minimal form of quicksort.
     * 
     * @param x weights array
     * @param idx indices from x to idx
     * @param kinc element just incremented
     * @param kdec element just decremented
     */
    public static void reorder(double[] x, int[] idx, int kinc, int kdec) {

        while (kinc > 0 && x[idx[kinc]] > x[idx[kinc - 1]]) {
            IndexQuickSort.swap(idx, kinc, kinc - 1);
            kinc--;
        }

        while (kdec < x.length - 1 && x[idx[kdec]] < x[idx[kdec + 1]]) {
            IndexQuickSort.swap(idx, kdec, kdec + 1);
            kdec++;
        }
    }

    /**
     * cube of the argument
     * 
     * @param x
     * @return
     */
    public static double cube(double x) {
        return x * x * x;
    }

    /**
     * cube root of argument
     * 
     * @param x
     * @return
     */
    public static double cuberoot(double x) {
        return Math.pow(x, 0.33333333333);
    }

    /**
     * calculate sum of cubes of argument
     * 
     * @param x
     * @return sum x^3
     */
    public static double sumcube(double[] x) {
        int i;
        double sum = 0;
        for (i = 0; i < x.length; i++) {
            sum += cube(x[i]);
        }
        return sum;
    }

}
