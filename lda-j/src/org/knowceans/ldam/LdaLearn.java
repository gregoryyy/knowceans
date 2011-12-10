/*
 * Created on Jul 22, 2009
 */
package org.knowceans.ldam;

import java.util.Arrays;

import org.knowceans.lda.Corpus;
import org.knowceans.lda.Document;
import org.knowceans.util.Cokus;
import org.knowceans.util.StopWatch;

public class LdaLearn {

    /**
     * Top-level learning algorithm
     * 
     * @param data corpus
     * @param alpha alpha estimate
     * @param beta beta estimate
     * @param emmax em max iterations
     * @param demmax vb step max iterations
     * @param epsilon em convergence threshold
     */
    public static void ldaLearn(Corpus data, double[] alpha, double[][] beta,
        int emmax, int demmax, double epsilon) {
        double[] gamma, nt, pnt, ap;
        double[][] q, gammas, betas;
        double lik, plik = 0;
        double z;
        int K = alpha.length;
        int V = data.getNumTerms();
        int M = data.getNumDocs();
        int dlenmax = data.getMaxCorpusLength();

        // random seed
        //srand(time(NULL));
        Cokus.seed(4357);

        // initialize parameters
        for (int k = 0; k < K; k++) {
            alpha[k] = Cokus.randDouble();
        }
        z = 0;
        for (int k = 0; k < K; k++) {
            z += alpha[k];
        }
        for (int k = 0; k < K; k++) {
            alpha[k] = alpha[k] / z;
        }
        // sort alpha initially
        Arrays.sort(alpha);

        for (int v = 0; v < V; v++) {
            for (int k = 0; k < K; k++) {
                beta[v][k] = (double) 1 / V;
            }
        }

        // initialize posteriors
        gammas = new double[M][K];
        betas = new double[V][K];

        // initialize buffers
        q = new double[dlenmax][K];
        gamma = new double[K];
        ap = new double[K];
        nt = new double[K];
        pnt = new double[K];

        System.out.println("Number of documents          = " + M);
        System.out.println("Number of words              = " + V);
        System.out.println("Number of latent classes     = " + K);
        System.out.println("Number of outer EM iteration = " + emmax);
        System.out.println("Number of inner EM iteration = " + demmax);
        System.out.println("Convergence threshold        = " + epsilon);

        /*
         * learn main
         */
        StopWatch.start("ldam");
        for (int t = 0; t < emmax; t++) {
            System.out.println("iteration " + (t + 1) + "/" + emmax + "..\t");
            System.out.flush();

            // VB-E step
            // iterate over documents
            for (int d = 0; d < M; d++) {
                Document doc = data.getDoc(d);
                VbEm.vbem(doc, gamma, q, nt, pnt, ap, alpha, beta, demmax);
                accumGammas(gammas, gamma, d);
                accumBetas(betas, q, doc);
            }
            // VB-M step
            // Newton-Raphson for alpha
            Newton.alpha(alpha, gammas, M, K, 0);

            // MLE for beta
            LdamUtils.normalizeMatrixCol(beta, betas);

            // reset buffer
            for (int v = 0; v < V; v++)
                Arrays.fill(betas[v], 0);
            // converged ?
            lik = ldaLik(data, beta, gammas, data.getNumDocs(), K);
            System.out.println("likelihood = " + lik + "\t");
            System.out.flush();
            if ((t > 1) && (Math.abs((lik - plik) / lik) < epsilon)) {
                if (t < 5) {
                    System.out.println("\nearly convergence. restarting.");
                    ldaLearn(data, alpha, beta, emmax, demmax, epsilon);
                    return;
                } else {
                    System.out.println("\nconverged. ["
                        + StopWatch.read("ldam") + "]");
                    break;
                }
            }
            plik = lik;

            // timing
            double eta = StopWatch.lap("ldam");
            System.out.println("ETA:" + StopWatch.read() + " (" + eta
                + " sec/step)");
        }
        return;
    }

    private static double ldaLik(Corpus data, double[][] beta,
        double[][] gammas, int m, int nclass) {
        double[][] egammas;
        double z, lik;
        int j, k;
        int n;
        lik = 0;

        egammas = new double[m][nclass];
        LdamUtils.normalizeMatrixRow(egammas, gammas);

        for (int d = 0; d < data.getNumDocs(); d++) {
            Document doc = data.getDoc(d);
            n = doc.getLength();
            for (j = 0; j < n; j++) {
                for (k = 0, z = 0; k < nclass; k++)
                    z += beta[doc.getWord(j)][k] * egammas[d][k];
                lik += doc.getCount(j) * Math.log(z);
            }
        }

        return lik;
    }

    static void accumGammas(double[][] gammas, double[] gamma, int m) {
        /* gammas(n,:) = gamma for Newton-Raphson of alpha */
        int k;
        for (k = 0; k < gamma.length; k++)
            gammas[m][k] = gamma[k];
        return;
    }

    static void accumBetas(double[][] betas, double[][] q, Document doc) {
        int i, k;
        int n = doc.getLength();
        int K = betas[0].length;

        for (i = 0; i < n; i++)
            for (k = 0; k < K; k++)
                betas[doc.getWord(i)][k] += q[i][k] * doc.getCount(i);
    }

}
