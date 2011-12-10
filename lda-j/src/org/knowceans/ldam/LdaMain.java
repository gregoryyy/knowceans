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
package org.knowceans.ldam;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.knowceans.lda.Corpus;
import org.knowceans.util.Arguments;
import org.knowceans.util.ArrayIo;

// lda.c/.h
public class LdaMain {

    private static final boolean BINOUT = false;
    public static int CLASS_DEFAULT = 50;
    public static int EMMAX_DEFAULT = 100;
    public static int DEMMAX_DEFAULT = 20;
    public static double EPSILON_DEFAULT = 1.0e-4;

    public static void main(String[] args) {
        Corpus data;
        double[] alpha;
        double[][] beta;
        int V;

        String options = "k|topics=i {number of topics, default = 50}" + //
            "i|iter=i {maximum iterations in outer loop} " + // 
            "d|diter=i {maximum iterations in VB loop}" + //
            "e|eps=f {convergence tolerance}";
        String types = "s {corpusfile} s {modelbase}";
        Arguments a = new Arguments(options, types);
        a.parse(args);

        data = new Corpus((String) a.getArgument(1, "./nips/nips.corpus"));
        String model = (String) a.getArgument(2, "nips.ldam");

        int K = (Integer) a.getOption("k", CLASS_DEFAULT);
        int emmax = (Integer) a.getOption("i", EMMAX_DEFAULT);
        int demmax = (Integer) a.getOption("d", DEMMAX_DEFAULT);
        double epsilon = (Double) a.getOption("e", EPSILON_DEFAULT);

        V = data.getNumTerms();

        alpha = new double[K];
        // NOTE: dmatrix constructor swaps dims
        // beta = dmatrix(nlex, nclass)
        beta = new double[V][K];

        LdaLearn.ldaLearn(data, alpha, beta, emmax, demmax, epsilon);

        ldaWrite(model, alpha, beta);

    }

    private static void ldaWrite(String model, double[] alpha, double[][] beta) {
        // open model outputs
        try {

            if (BINOUT) {
                DataOutputStream ap = ArrayIo
                    .openOutputStream(model + ".final");
                ArrayIo.writeDoubleVector(ap, alpha);
                ArrayIo.writeDoubleMatrix(ap, beta);
                ArrayIo.closeOutputStream(ap);
            } else {
                BufferedWriter ap = new BufferedWriter(new FileWriter(model
                    + ".alpha"));
                for (int k = 0; k < alpha.length; k++) {
                    ap.write(Double.toString(alpha[k]));
                    ap.write('\n');
                }
                ap.close();
                ArrayIo.saveAscii(model + ".beta", beta);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
