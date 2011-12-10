/*
 * Created on Jul 22, 2009
 */
package org.knowceans.ldam;

import static org.knowceans.util.Gamma.digamma;

import org.knowceans.lda.Document;

// vbem.c
public class VbEm {

    /**
     * variational E step
     * 
     * @param doc document
     * @param gamma var param for document
     * @param q var param for tokens
     * @param nt (local) words per topic
     * @param pnt (local) saved words per topic (for convergence monitoring)
     * @param ap (local)
     * @param alpha alpha estimate
     * @param beta beta estimate
     * @param demmax max iterations
     */
    public static void vbem(Document doc, double[] gamma, double[][] q,
        double[] nt, double[] pnt, double[] ap, double[] alpha,
        double[][] beta, int demmax) {
        int r, k, t;
        double qsum;
        int Tm = doc.getLength();
        int K = gamma.length;

        for (k = 0; k < K; k++) {
            nt[k] = (double) Tm / K;
        }

        for (r = 0; r < demmax; r++) {
            // vb-estep
            for (k = 0; k < K; k++) {
                ap[k] = Math.exp(digamma(alpha[k] + nt[k]));
            }
            // accumulate q
            for (t = 0; t < Tm; t++) {
                for (k = 0; k < K; k++) {
                    q[t][k] = beta[doc.getWord(t)][k] * ap[k];
                }
            }
            // normalize q
            for (t = 0; t < Tm; t++) {
                qsum = 0;
                for (k = 0; k < K; k++) {
                    qsum += q[t][k];
                }
                for (k = 0; k < K; k++) {
                    q[t][k] /= qsum;
                }
            }
            // vb-mstep
            for (k = 0; k < K; k++) {
                qsum = 0;
                for (t = 0; t < Tm; t++)
                    qsum += q[t][k] * doc.getCount(t);
                nt[k] = qsum;
            }
            // converged ?
            if ((r > 0) && LdamUtils.converged(nt, pnt, K, 1.0e-2))
                break;
            for (k = 0; k < K; k++) {
                pnt[k] = nt[k];
            }
        }
        // update gamma
        for (k = 0; k < K; k++) {
            gamma[k] = alpha[k] + nt[k];
        }

        return;
    }
}
