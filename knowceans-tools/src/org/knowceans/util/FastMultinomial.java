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
 * index. There are two sampling methods, one that returns the sample as an
 * index of the sorting index array (useful to maintain the sampling order using
 * reorder()) and one that returns the actual sample index in abc.
 * <p>
 * The static functions are proof of concept that does not save much computing
 * time because they don't pre-compute the weights beforehand. Actual speed is
 * gained by subclassing and calculating the weights in getWeights.
 * 
 * @author gregor
 */
public abstract class FastMultinomial {

    public static void main(String[] args) {
        int nsamp = 100000;
        double[] a = new double[] {0.2, 0.1, 0.001, 0.01, 0.02, 0.003, 0.05,
            0.003, 0.2, 0.1};
        double[] b = Vectors.ones(a.length, 3.);
        double[] c = Vectors.ones(a.length, 2.);
        double[][] ww = new double[][] {a, b, c};

        int[] samples = staticMain(nsamp, ww);

        // check empirical distribution
        Histogram.hist(System.out, samples, 10);
    }

    /**
     * static operation of the sampler
     * 
     * @param nsamp
     * @param ww
     * @return
     */
    private static int[] staticMain(int nsamp, double[][] ww) {
        // order weights
        int[] idx = Vectors.range(0, ww[0].length - 1);
        indexsort(ww[0], idx);
        // set up norms
        double[] wwnorm = new double[ww.length];
        for (int i = 0; i < wwnorm.length; i++) {
            wwnorm[i] = sumcube(ww[i]);
        }

        // now sample
        int[] samples = new int[nsamp];
        Random rand = new CokusRandom();
        for (int i = 0; i < nsamp; i++) {
            samples[i] = idx[sampleIdx(ww, wwnorm, idx, rand)];
            samples[i] = sample(ww, wwnorm, idx, rand);
        }
        return samples;

    }

    ////////////// instance members and functions /////////////

    /**
     * number of dimensions
     */
    private int K;

    /**
     * number of factors
     */
    private int I;

    /**
     * power of the norm of abc
     */
    private double[] abcnorm;

    /**
     * rng
     */
    private Random rand;

    /**
     * sorting index into abc to the norm
     */
    private int[] idx;

    /**
     * set up a fast multinomial using a subclass implementation for the
     * 
     * @param I number of factor distributions
     * @param K number of topics
     * @param abcnorm cube norms of factors [I]
     * @param idx descending order of weight factors
     * @param rand random sampler
     */
    public FastMultinomial(int I, int K, Random rand) {
        this.rand = rand;
        this.I = I;
        this.K = K;
    }

    /**
     * Fast sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * Further, the cube norm of each of the factors is given in abcnorm
     * (without the inverse exponential operation), as well as the indexes of
     * the values of abc ordered decending (by one of the factors).
     * 
     * @param I number of factor distributions
     * @param K number of topics
     * @param abcnorm cube norms of factors [I]
     * @param idx descending order of weight factors
     * @param rand random sampler
     * @return a sample as index of idx, use sample() to get the original index
     *         of abc
     */
    public int sampleIdx() {
        int ksorted;
        double Zkprev;
        double Zk = Double.POSITIVE_INFINITY;
        double[] p = new double[K];

        // get norms that are decremented
        // by local elements:
        // norm(a_l:K-1) starting with (a_0:K-1)
        double[] abcl2k = Vectors.copy(abcnorm);

        double u = rand.nextDouble();
        for (int k = 0; k < K; k++) {

            // p[k] = p[k-1] + prod_i abc_ik
            p[k] = k == 0 ? 0 : p[k - 1];
            ksorted = idx[k];

            double[] abc = getWeights(ksorted);
            double abck = abc[0];
            for (int i = 1; i < I; i++) {
                abck *= abc[i];
            }
            p[k] += abck;

            // first reduce the norm by the current 
            // element: norm(a_i,l+1:K) = (-a_i,l^3 + sum_l:K a_i^3)^-3

            // store this in case it was the previous topic
            Zkprev = Zk;
            // Zk = p[k] + prod_i norm(a_i,l+1:K)
            Zk = p[k] + reducenorm(abcl2k, abc);

            if (u > p[k] / Zk) {
                continue;
            } else {
                if (k == 0 || u * Zk > p[k - 1]) {
                    // current topic ? 
                    return k;
                } else {
                    // previous topic (via s_lk) ?
                    // scale and shift u
                    u = (u * Zkprev - p[k - 1]) * Zk / (Zkprev - Zk);
                    for (int kprev = 0; kprev < k; kprev++) {
                        if (p[kprev] >= u) {
                            return kprev;
                        }
                    } // for each previous topic
                } // if current topic
            } // if p too low
        } // for k
        // should never reach this...
        return -1;
    }

    /**
     * This function is supposed to do two things: remove the current element
     * abc from the exponentiated norm and calculate the norm of the remaining
     * elements by the root. This function is implemented using the cube root
     * and should be subclassed for other norms.
     * 
     * @param abcl2kExpnorm [in/out] exponentiated norms from l to K - 1 in each
     *        row
     * @param abcl1 [in] weights to be reduced = abc[][l-1]
     * @return
     */
    public double reducenorm(double[] abcl2kExpnorm, double[] abcl1) {
        double abcl2knorm = 1;
        for (int i = 0; i < I; i++) {
            abcl2kExpnorm[i] -= cube(abcl1[i]);
            if (abcl2kExpnorm[i] < 0) {
                abcl2kExpnorm[i] = 0;
            }
            abcl2knorm *= abcl2kExpnorm[i];
        }
        return cuberoot(abcl2knorm);
    }

    /**
     * Calculate exponentiated norm sum of x for the weights arr. This
     * implementation uses cube norm. Typically, this function is called for
     * each factor once before fast sampling can start. It requires that weights
     * for all k are calculated initially, whereas for the fast sampler this is
     * not necessary.
     * 
     * @param abci factor
     * @return norm (exp)
     */
    public double initnorm(double[] abci) {
        double sum = 0;
        for (int k = 0; k < abci.length; k++) {
            sum += cube(abci[k]);
        }
        return sum;
    }

    /**
     * Get the weight for dimension k for each factor. This is subclassed and
     * contains the actual weights calculations whose number should be reduced
     * by the sorted sampling.
     * 
     * @param k
     * @return
     */
    public abstract double[] getWeights(int k);

    /////////// static functions ///////////

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
     * @return a sample as index of abc
     */
    public static int sample(double[][] abc, double[] abcnorm, int[] idx,
        Random rand) {
        return idx[sampleIdx(abc, abcnorm, idx, rand)];
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
     * @return a sample as index of idx, use sample() to get the original index
     *         of abc
     */
    public static int sampleIdx(double[][] abc, double[] abcnorm, int[] idx,
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
                    return k;
                } else {
                    // previous topic (via s_lk) ?
                    // scale and shift u
                    u = (u * Zkprev - p[k - 1]) * Zk / (Zkprev - Zk);
                    for (int kprev = 0; kprev < k; kprev++) {
                        if (p[kprev] >= u) {
                            return kprev;
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
     * @param kinc element in idx just incremented
     * @param kdec element in idx just decremented
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
