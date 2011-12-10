/*
 * (C) Copyright 2004-2009, Gregor Heinrich (gregor :: arbylon : net) 
 * (This file is part of the lda-j (org.knowceans.ldaj.*) experimental software 
 * package, a port of lda-c Copyright David Blei.)
 */
/*
 * lda-j is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 */
/*
 * lda-j is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on Jan 4, 2005
 */
package org.knowceans.ldaj;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static org.knowceans.lda.Utils.digamma;
import static org.knowceans.lda.Utils.lgamma;
import static org.knowceans.lda.Utils.logSum;

import org.knowceans.lda.Document;

/**
 * lda inference functions
 * <p>
 * lda-c reference: functions in lda-inference.c. TODO: merge with model?
 * 
 * @author heinrich
 */
public class LdaInference {

    public static float VAR_CONVERGED;

    public static int VAR_MAX_ITER;

    /*
     * variational inference
     */
    // 2009: double lda_inference(document* doc, lda_model* model, double* var_gamma, double** phi)
    public static double ldaInference(Document doc, LdaModel model,
        double[] varGamma, double[][] phi) {
        double converged = 1;
        double phisum = 0, likelihood = 0, likelihoodOld = 0;
        double[] oldphi = new double[model.getNumTopics()];
        int k, n, varIter;
        double[] digammaGam = new double[model.getNumTopics()];

        // compute posterior dirichlet
        for (k = 0; k < model.getNumTopics(); k++) {
            varGamma[k] = model.getAlpha() + doc.getTotal()
                / (double) model.getNumTopics();
            digammaGam[k] = digamma(varGamma[k]);
            for (n = 0; n < doc.getLength(); n++)
                phi[n][k] = 1.0 / model.getNumTopics();
        }
        varIter = 0;

        while ((converged > VAR_CONVERGED) && (varIter < VAR_MAX_ITER)
            || (VAR_MAX_ITER == -1)) {
            varIter++;
            for (n = 0; n < doc.getLength(); n++) {
                phisum = 0;
                for (k = 0; k < model.getNumTopics(); k++) {
                    oldphi[k] = phi[n][k];

                    assert varGamma[k] != 0;
                    phi[n][k] = digammaGam[k]
                        + model.logProbW[k][doc.getWord(n)];

                    if (k > 0) {
                        phisum = logSum(phisum, phi[n][k]);
                    } else {
                        phisum = phi[n][k];
                    }
                }
                for (k = 0; k < model.getNumTopics(); k++) {
                    phi[n][k] = exp(phi[n][k] - phisum);
                    varGamma[k] = varGamma[k] + doc.getCount(n)
                        * (phi[n][k] - oldphi[k]);
                }
            }
            likelihood = computeLikelihood(doc, model, phi, varGamma);
            assert !Double.isNaN(likelihood);
            converged = (likelihoodOld - likelihood) / likelihoodOld;
            likelihoodOld = likelihood;
        }
        return likelihood;
    }

    /*
     * compute likelihood bound
     */
    public static double computeLikelihood(Document doc, LdaModel model,
        double[][] phi, double[] varGamma) {
        double likelihood = 0, digsum = 0, varGammaSum = 0;// , x;
        double[] dig = new double[model.getNumTopics()];
        int k, n, message = 0;

        for (k = 0; k < model.getNumTopics(); k++) {
            dig[k] = digamma(varGamma[k]);
            varGammaSum += varGamma[k];
        }
        digsum = digamma(varGammaSum);

        likelihood = lgamma(model.getAlpha() * model.getNumTopics())
            - model.getNumTopics() * lgamma(model.getAlpha())
            - (lgamma(varGammaSum));

        assert likelihood != Double.NaN;
        for (k = 0; k < model.getNumTopics(); k++) {
            likelihood += (model.getAlpha() - 1) * (dig[k] - digsum)
                + lgamma(varGamma[k]) - (varGamma[k] - 1) * (dig[k] - digsum);

            for (n = 0; n < doc.getLength(); n++) {
                if (phi[n][k] > 0) {
                    likelihood += doc.getCount(n)
                        * (phi[n][k] * ((dig[k] - digsum) - log(phi[n][k])
                            + log(phi[n][k]) - model.logProbW[k][doc.getWord(n)]));

                }
            }
        }
        return likelihood;
    }
}
