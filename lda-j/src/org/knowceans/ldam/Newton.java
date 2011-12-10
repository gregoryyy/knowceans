/*
 * Created on Jul 22, 2009
 */
package org.knowceans.ldam;

import static org.knowceans.util.Gamma.*;

public class Newton {

    public static int MAX_RECURSION_LIMIT = 20;
    public static int MAX_NEWTON_ITERATION = 20;

    /**
     * this is for vectorial alpha
     * 
     * @param alpha
     * @param gammas
     * @param M
     * @param K
     * @param level
     */
    static void alpha(double[] alpha, double[][] gammas, int M, int K, int level) {
        int i, j, t;
        double[] g, h, pg, palpha;
        double z, sh, hgz;
        double psg, spg, gs;
        double alpha0, palpha0;

        // allocate arrays
        g = new double[K];
        h = new double[K];
        pg = new double[K];
        palpha = new double[K];

        // initialize
        if (level == 0) {
            for (i = 0; i < K; i++) {
                for (j = 0, z = 0; j < M; j++) {
                    z += gammas[j][i];
                }
                alpha[i] = z / (M * K);
            }
        } else {
            for (i = 0; i < K; i++) {
                for (j = 0, z = 0; j < M; j++) {
                    z += gammas[j][i];
                }
                alpha[i] = z / (M * K * Math.pow(10, level));
            }
        }

        psg = 0;
        for (i = 0; i < M; i++) {
            for (j = 0, gs = 0; j < K; j++) {
                gs += gammas[i][j];
            }
            psg += digamma(gs);
        }
        for (i = 0; i < K; i++) {
            for (j = 0, spg = 0; j < M; j++) {
                spg += digamma(gammas[j][i]);
            }
            pg[i] = spg - psg;
        }

        /* main iteration */
        for (t = 0; t < MAX_NEWTON_ITERATION; t++) {
            for (i = 0, alpha0 = 0; i < K; i++) {
                alpha0 += alpha[i];
            }
            palpha0 = digamma(alpha0);

            for (i = 0; i < K; i++) {
                g[i] = M * (palpha0 - digamma(alpha[i])) + pg[i];
            }
            for (i = 0; i < K; i++) {
                h[i] = -1 / trigamma(alpha[i]);
            }
            for (i = 0, sh = 0; i < K; i++) {
                sh += h[i];
            }

            for (i = 0, hgz = 0; i < K; i++)
                hgz += g[i] * h[i];
            hgz /= (1 / trigamma(alpha0) + sh);

            for (i = 0; i < K; i++)
                alpha[i] = alpha[i] - h[i] * (g[i] - hgz) / M;

            for (i = 0; i < K; i++) {
                if (alpha[i] < 0) {
                    if (level >= MAX_RECURSION_LIMIT) {
                        System.err
                            .println("newton:: maximum recursion limit reached.");
                        System.exit(1);
                    } else {
                        alpha(alpha, gammas, M, K, 1 + level);
                        return;
                    }
                }
            }

            if ((t > 0) && LdamUtils.converged(alpha, palpha, K, 1.0e-4)) {
                return;
            } else {
                for (i = 0; i < K; i++) {
                    palpha[i] = alpha[i];
                }
            }
        }
        System.err.println("newton:: maximum iteration reached. t = " + t);
    }
    
    /**
     * this is for scalar alpha 
     * 
     * @param alpha
     * @param gammas
     * @param M
     * @param K
     * @param level
     */
    static void alpha(double alpha, double[][] gammas, int M, int K, int level) {
        
    }
}
