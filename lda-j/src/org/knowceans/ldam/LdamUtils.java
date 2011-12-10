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

/**
 * Utility functions
 * <p>
 * lda-c reference: functions in utils.c
 * 
 * @author heinrich
 */
public class LdamUtils {

    static boolean converged(double[] u, double[] v, int n, double threshold) {
        /* return 1 if |a - b|/|a| < threshold */
        double us = 0;
        double ds = 0;
        double d;
        int i;

        for (i = 0; i < n; i++)
            us += u[i] * u[i];

        for (i = 0; i < n; i++) {
            d = u[i] - v[i];
            ds += d * d;
        }

        if (Math.sqrt(ds / us) < threshold)
            return true;
        else
            return false;

    }

    static void normalizeMatrixCol(double[][] dst, double[][] src) {
        /* column-wise normalize from src -> dst */
        double z;
        int i, j;

        int rows = src.length;
        int cols = src[0].length;
        for (j = 0; j < cols; j++) {
            for (i = 0, z = 0; i < rows; i++)
                z += src[i][j];
            for (i = 0; i < rows; i++)
                dst[i][j] = src[i][j] / z;
        }
    }

    static void normalizeMatrixRow(double[][] dst, double[][] src) {
        /* row-wise normalize from src -> dst */
        int i, j;
        double z;

        int rows = src.length;
        int cols = src[0].length;
        for (i = 0; i < rows; i++) {
            for (j = 0, z = 0; j < cols; j++)
                z += src[i][j];
            for (j = 0; j < cols; j++)
                dst[i][j] = src[i][j] / z;
        }
    }

}
