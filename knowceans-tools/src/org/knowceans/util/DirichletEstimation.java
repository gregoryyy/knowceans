/*
 * Created on 07.07.2006
 */
package org.knowceans.util;

import static org.knowceans.util.Gamma.digamma;
import static org.knowceans.util.Gamma.invdigamma;
import static org.knowceans.util.Gamma.lgamma;
import static org.knowceans.util.Gamma.trigamma;

/**
 * DirichletEstimation provides a number of methods to estimate parameters of a
 * Dirichlet distribution. Most of the algorithms described in Minka (2003)
 * Estimating a Dirichlet distribution.
 *
 * @author gregor
 */
public class DirichletEstimation {

    /**
     * Estimator for the Dirichlet parameters.
     *
     * @param multinomial parameters p
     * @return ML estimate of the corresponding parameters alpha
     */
    public static double[] estimateAlpha(double[][] pp) {

        // sufficient statistics
        double[] suffstats = suffStats(pp);

        double[] pmean = estMean(pp);

        // initial guess for alpha
        double[] alpha = estAlpha(pp, pmean);
        // System.out.println("initial estimate for alpha: " +
        // System.out.println(Vectors.print(alpha));

        boolean newton = false;
        if (newton) {
            alphaNewton(pp.length, suffstats, alpha);
        } else {
            alphaFixedPoint(suffstats, alpha);
        }
        return alpha;
    }

    /**
     * estimate mean and precision of the observations separately.
     *
     * @param pp input data with vectors in rows
     * @return a vector of the Dirichlet mean in the elements 0..K-2 (the last
     *         element is the difference between the others and 1) and Dirichlet
     *         precision in the element K-1, where K is the dimensionality of
     *         the data, pp[0].length.
     */
    public static double[] estimateMeanPrec(double[][] pp) {
        double[] mean = estMean(pp);
        double[] meansq = colMoments(pp, 2);
        double prec = estPrecision(mean, meansq);
        double[] suffstats = suffStats(pp);

        for (int i = 0; i < 5; i++) {
            prec = precFixedPoint(suffstats, pp.length, mean, prec);
            meanGenNewton(suffstats, mean, prec);
        }
        double[] retval = new double[mean.length];
        System.arraycopy(mean, 0, retval, 0, mean.length - 1);
        retval[mean.length - 1] = prec;
        return retval;
    }

    /**
     * Get the precision out of a mean precision combined vector
     *
     * @param meanPrec
     * @return
     */
    public static double getPrec(double[] meanPrec) {
        return meanPrec[meanPrec.length - 1];
    }

    /**
     * Get the mean out of a mean precision combined vector. The mean vector is
     * copied.
     *
     * @param meanPrec
     * @return
     */
    public static double[] getMean(double[] meanPrec) {
        double[] retval = new double[meanPrec.length];
        System.arraycopy(meanPrec, 0, retval, 0, meanPrec.length - 1);
        double sum = 0;
        for (int k = 0; k < meanPrec.length - 1; k++) {
            sum += meanPrec[k];
        }
        retval[meanPrec.length - 1] = 1 - sum;
        return retval;
    }

    /**
     * Get the alpha vector out of a mean precision combined vector. The vector
     * is copied.
     *
     * @param meanPrec
     * @return
     */
    public static double[] getAlpha(double[] meanPrec) {
        double[] retval = new double[meanPrec.length];
        System.arraycopy(meanPrec, 0, retval, 0, meanPrec.length - 1);
        double sum = 0;
        double prec = meanPrec[meanPrec.length - 1];
        for (int k = 0; k < meanPrec.length - 1; k++) {
            sum += meanPrec[k] * prec;
        }
        retval[meanPrec.length - 1] = 1 - sum;
        return retval;
    }

    /**
     * Estimate the dirichlet parameters using the moments method
     *
     * @param pp data with items in rows and dimensions in cols
     * @param pmean first moment of pp
     * @return
     */
    public static double[] estAlpha(double[][] pp, double[] pmean) {
        // first and second moments of the columns of p

        int K = pp[0].length;
        double[] pmeansq = colMoments(pp, 2);

        // init alpha_k using moments method (19-21)
        double[] alpha = Vectors.copy(pmean);
        double precision = estPrecision(pmean, pmeansq);
        precision /= K;
        // System.out.println("precision = " + precision);
        // alpha_k = mean_k * precision
        for (int k = 0; k < K; k++) {
            alpha[k] = pmean[k] * precision;
        }
        return alpha;
    }

    /**
     * Estimate the Dirichlet mean of the data along columns
     *
     * @param pp
     * @return
     */
    public static double[] estMean(double[][] pp) {
        return colMoments(pp, 1);
    }

    /**
     * Estimate the mean given the data and a guesses of the mean and precision.
     * This uses the gradient ascent method described in Minka (2003) and Huang
     * (2004).
     *
     * @param suffstats
     * @param mean [in / out]
     * @param prec
     */
    private static void meanGenNewton(double[] suffstats, double[] mean,
        double prec) {

        double[] alpha = new double[mean.length];

        for (int i = 0; i < 100; i++) {

            for (int k = 0; k < mean.length; k++) {
                for (int j = 0; j < alpha.length; j++) {
                    alpha[k] += mean[j]
                        * (suffstats[j] - digamma(prec * mean[j]));
                }
                alpha[k] = invdigamma(suffstats[k] - alpha[k]);
            }
            double sumalpha = Vectors.sum(alpha);
            for (int k = 0; k < alpha.length; k++) {
                mean[k] = alpha[k] / sumalpha;
            }
        }
    }

    /**
     * Estimate the precision given the data and a guesses of the mean and
     * precision. This uses the gradient ascent method described in Minka (2003)
     * and Huang (2004).
     *
     * @param suffstats
     * @param N
     * @param mean
     * @param prec
     */
    private static double precFixedPoint(double[] suffstats, int N,
        double[] mean, double prec) {
        double dloglik = 0;
        for (int k = 0; k < mean.length; k++) {
            dloglik += mean[k] * (digamma(prec * mean[k]) + suffstats[k]);
        }
        dloglik = N * (digamma(prec) - dloglik);
        double ddloglik = 0;
        for (int k = 0; k < mean.length; k++) {
            ddloglik += mean[k] * mean[k] * trigamma(prec * mean[k]);
        }
        ddloglik = N * (trigamma(prec) - dloglik);
        double precinv = 1 / prec + dloglik / (prec * prec * ddloglik);
        return 1 / precinv;

    }

    /**
     * Estimate the Dirichlet precision using a simple and crude moments method.
     *
     * @param pmean
     * @param pmeansq
     * @return
     */
    public static double estPrecision(double[] pmean, double[] pmeansq) {
        double precision = 0;

        int K = pmean.length;

        // estimate s for each dimension (21) and take the mean
        for (int k = 0; k < K; k++) {
            precision += (pmean[k] - pmeansq[k])
                / (pmeansq[k] - pmean[k] * pmean[k]);
        }
        return precision / pmean.length;
    }

    /**
     * Moment of each column in an element of the returned vector
     *
     * @param xx
     * @param order
     * @return
     */
    private static double[] colMoments(double[][] xx, int order) {
        int K = xx[0].length;
        int N = xx.length;

        double[] pmean2 = new double[K];
        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                double element = xx[i][k];
                for (int d = 1; d < order; d++) {
                    element *= element;
                }
                pmean2[k] += element;
            }
        }
        for (int k = 0; k < K; k++) {
            pmean2[k] /= N;
        }
        return pmean2;
    }

    /**
     * Dirichlet sufficient statistics 1/N sum log p
     *
     * @param pp
     * @return
     */
    private static double[] suffStats(double[][] pp) {
        int K = pp[0].length;
        int N = pp.length;

        double[] suffstats = new double[K];

        for (int i = 0; i < N; i++) {
            for (int k = 0; k < K; k++) {
                suffstats[k] += Math.log(pp[i][k]);
            }
        }
        for (int k = 0; k < K; k++) {
            suffstats[k] /= N;
        }
        return suffstats;
    }

    // FIXME: doesn't work yet.
    public static void alphaNewton(int N, double[] suffstats, double[] alpha) {
        int K = alpha.length;

        // initial likelihood (4)
        double loglik = 0;
        double loglikold = 0;
        double[] grad = new double[K];
        double alphasum = Vectors.sum(alpha);
        double[] alphaold = new double[K];
        double lgasum = 0;
        double asssum = 0;
        int iterations = 1000;
        double epsilon = 1e-6;
        for (int i = 0; i < iterations; i++) {
            System.arraycopy(alpha, 0, alphaold, 0, K);

            for (int k = 0; k < K; k++) {
                lgasum += lgamma(alpha[k]);
                asssum += (alpha[k] - 1) * suffstats[k];
                grad[k] = N
                    * (digamma(alphasum) - digamma(alpha[k]) + suffstats[k]);
            }
            loglik = N * (lgamma(alphasum) - lgasum + asssum);
            // System.out.println(loglik);
            if (Math.abs(loglikold - loglik) < epsilon) {
                break;
            }
            loglikold = loglik;

            // invhessian x grad and diag Q (could be omitted by calculating 17
            // and 15 below inline)
            double[] hinvg = new double[K];
            double[] qdiag = new double[K];
            double bnum = 0;
            double bden = 0;

            // (14)
            double z = N * trigamma(alphasum);

            // (18)
            for (int k = 0; k < K; k++) {
                qdiag[k] = -N * trigamma(alpha[k]);
                bnum += grad[k] / qdiag[k];
                bden += 1 / qdiag[k];
            }
            double b = bnum / (1 / z + bden);

            for (int k = 0; k < K; k++) {
                // (17)
                hinvg[k] = (grad[k] - b) / qdiag[k];
                // (15)
                alpha[k] -= hinvg[k];
            }
            // System.out.println("hinv g = " + Vectors.print(hinvg));
            // System.out.println("alpha = " + Vectors.print(alpha));
        }
    }

    /**
     * fixpoint iteration on alpha.
     *
     * @param suffstats
     * @param alpha [in/out]
     */
    public static void alphaFixedPoint(double[] suffstats, double[] alpha) {
        int K = alpha.length;

        // using (9)
        int fixits = 500;
        double[] alphadiff = new double[K];
        for (int i = 0; i < fixits; i++) {
            System.arraycopy(alpha, 0, alphadiff, 0, K);
            double sumalpha = Vectors.sum(alpha);
            for (int k = 0; k < K; k++) {
                alpha[k] = invdigamma(digamma(sumalpha) + suffstats[k]);
                alphadiff[k] = Math.abs(alpha[k] - alphadiff[k]);
            }
            if (Vectors.sum(alphadiff) < 1e-4) {
                // System.out.println(i);
                break;
            }
        }
    }

    public static void main(String[] args) {
        // testing estimation of alpha from p
        double[] alpha = {0.495, 1.10, 0.69};
        double[][] pp = Samplers.randDir(alpha, 10000);
        double[] alphaguess = estimateAlpha(pp);
        System.out.println("estimated alpha");
        System.out.println(Vectors.print(alphaguess));

        System.out.println("estimated mean");
        double[] mean = estMean(pp);
        double[] suffstats = suffStats(pp);
        System.out.println(Vectors.print(mean));

        System.out.println("estimated mean (Newton)");
        meanGenNewton(suffstats, mean, 2.5);
        System.out.println(Vectors.print(mean));

        System.out.println("estimated mean / precision");
        double[] mp = estimateMeanPrec(pp);
        System.out.println(Vectors.print(mp));
    }
}
