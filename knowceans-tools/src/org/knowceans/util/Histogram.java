/*
 * Created on Aug 1, 2005
 */
package org.knowceans.util;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

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
        String scale = "";
        for (int i = 1; i < fmax / 10 + 1; i++) {
            scale += "    .    " + i % 10;
        }

        out.println("x" + nf.format(hmax / fmax * 10) + "\t0" + scale);
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
