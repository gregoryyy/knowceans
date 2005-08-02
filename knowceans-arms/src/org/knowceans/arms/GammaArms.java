/*
 * Created on Jul 31, 2005
 */
/*
 * (C) Copyright 2005, Gregor Heinrich (gregor :: arbylon : net) (This file is
 * part of the lda-j (org.knowceans.lda.*) experimental software package.)
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
package org.knowceans.arms;

import org.knowceans.util.Densities;

public class GammaArms extends ArmSampler {

    /**
     * implements the gamma density.
     * 
     * @param x
     * @param params double[]{alpha, beta}
     * @return
     */
    public double logpdf(double x, Object params) {
        double[] pars = (double[]) params;
        return Math.log(Densities.pdfGamma(x, pars[0], pars[1]));
    }

    public static void main(String[] args) {
        GammaArms gars = new GammaArms();
        double[] xprev = new double[] {0.3};
        double[] params = new double[] {40.0, 30.0};
        try {
            double sample = gars.armsSimple(params, 4, new double[] {0.2},
                new double[] {8000.}, true, xprev);
            System.out.println(sample);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
