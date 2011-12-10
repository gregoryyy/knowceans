/*
 * (C) Copyright 2004-2009, Gregor Heinrich (gregor :: arbylon : net) (This file
 * is part of the lda-j (org.knowceans.ldaj.*) experimental software package, a
 * port of lda-c Copyright David Blei.)
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

package org.knowceans.ldaj;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.abs;
import static org.knowceans.util.Gamma.digamma;
import static org.knowceans.util.Gamma.lgamma;
import static org.knowceans.util.Gamma.trigamma;

/**
 * performs optimisation tasks
 * <p>
 * lda-c reference: functions in lda-alpha.c.
 * 
 * @author heinrich
 */
// 2009: lda-alpha.c/.h
public class LdaAlpha {

	public static final double NEWTON_THRESH = 1e-5;
	public static final int MAX_ALPHA_ITER = 1000;

	static double alhood(double a, double ss, int D, int K) {
		return (D * (lgamma(K * a) - K * lgamma(a)) + (a - 1) * ss);
	}

	static double d_alhood(double a, double ss, int D, int K) {
		return (D * (K * digamma(K * a) - K * digamma(a)) + ss);
	}

	static double d2_alhood(double a, int D, int K) {
		return (D * (K * K * trigamma(K * a) - K * trigamma(a)));
	}

	static double opt_alpha(double ss, int D, int K) {
		double a, log_a, init_a = 100;
		double f, df, d2f;
		int iter = 0;

		log_a = log(init_a);
		do {
			iter++;
			a = exp(log_a);
			if (Double.isNaN(a)) {
				init_a = init_a * 10;
				// printf("warning : alpha is nan; new init = %5.5f\n", init_a);
				System.out.println("warning : alpha is nan; new init = "
						+ init_a + "\n");
				a = init_a;
			}
			f = alhood(a, ss, D, K);
			df = d_alhood(a, ss, D, K);
			d2f = d2_alhood(a, D, K);
			log_a = log_a - df / (d2f * a + df);
			// printf("alpha maximization : %5.5f   %5.5f\n", f, df);
			System.out.println("alpha maximization : " + f + "   " + df);
		} while ((abs(df) > NEWTON_THRESH) && (iter < MAX_ALPHA_ITER));
		return (exp(log_a));
	}

	// 2009: void maximize_alpha(double** gamma, lda_model* model, int
	// num_docs);
	static void maximize_alpha(double[][] gamma, LdaModel model, int num_docs) {
		// ldac: unimplemented
	}
}
