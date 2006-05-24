/*
 * Created on Aug 1, 2005
 */
/*
 * Copyright (c) 2005-2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.util;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 * Histogram is a static class to output histogram data graphically on an output
 * print stream. It uses lines as columns for the bins.
 * 
 * @author gregor
 */
public class Histogram {

    /**
     * print histogram of the data
     * 
     * @param out stream to print to
     * @param data vector of evidence
     * @param low lower bin limit
     * @param high upper bin limit
     * @param bins # of bins
     * @param fmax max frequency in display
     * @return the scaled histogram bin values
     */
    public static double[] hist(PrintStream out, double[] data, double low,
        double high, int bins, int fmax) {

        double binwidth = (high - low) / bins;

        // create bins
        double[] binhigh = new double[bins];
        double[] hist = new double[bins];
        binhigh[0] = low + binwidth;
        for (int i = 1; i < bins; i++) {
            binhigh[i] = binhigh[i - 1] + binwidth;
        }

        for (int i = 0; i < data.length; i++) {
            int c = 0;
            if (data[i] >= high) {
                hist[bins - 1]++;
                continue;
            }
            for (c = 0; c < bins; c++) {
                if (data[i] < binhigh[c])
                    break;
            }
            // out.println(data[i] + " -> " + c);
            hist[c]++;
        }

        // scale maximum
        double hmax = 0;
        for (int i = 0; i < hist.length; i++) {
            hmax = Math.max(hist[i], hmax);
        }
        double shrink = fmax / hmax;
        for (int i = 0; i < hist.length; i++) {
            hist[i] = shrink * hist[i];
        }

        NumberFormat nf = new DecimalFormat("0.00");
        String scale = "0";
        for (int i = 1; i < fmax / 10 + 1; i++) {
            scale += "    .    " + i % 10;
        }
        String legend = "x" + nf.format(hmax / fmax * 10) + " ";
        if (legend.length() < 9) {
            char[] fill = new char[8-legend.length()];
            Arrays.fill(fill, ' ');
            legend += new String(fill);
        } else {
            scale = scale.substring(legend.length() - 8);
        }
        out.println(legend + scale);
        out.println(low + "\t.");
        for (int i = 0; i < hist.length; i++) {
            String x = nf.format(binhigh[i] - binwidth / 2);
            out.print(x + "\t|");
            for (int j = 0; j < Math.round(hist[i]); j++) {
                if ((j + 1) % 10 == 0)
                    out.print("]");
                else
                    out.print("|");
            }
            out.println();
        }
        out.println(high + "\t.");
        return hist;
    }

    /**
     * print histogram that spans the complete data set and chooses the
     * proportion of the histogram to be good at a given size (max freqency).
     * 
     * @param out output stream
     * @param data data vector
     * @param size controls max frequency and bin number
     * @return the scaled histogram bin values
     */
    public static double[] hist(PrintStream out, double[] data, int size) {
        // determine low, high, bins and fmax

        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (double x : data) {
            low = Math.min(x, low);
            high = Math.max(x, high);
        }
        int bins = (int) (0.7 * size);
        low -= (high - low) * 2 / bins;
        high += (high - low) * 2 / bins;
        return hist(out, data, low, high, bins, size);
    }

}
