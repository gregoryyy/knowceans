/*
 * Created on Feb 28, 2010
 */
package org.knowceans.dsp;

import java.util.Arrays;

import org.knowceans.util.Vectors;

/**
 * IirFilter implements a simple IIR filter using a cascade of biquad filters
 * (second-order sections).
 * 
 * @author gregor
 */
public class IirFilter {

    public IirFilter(int channels, int stages, int framesize) {
        v = new double[stages][channels * 3];
        init(channels, stages, framesize);
    }

    public void init(int channels, int stages, int framesize) {
        channel_num = channels;
        stage_num = stages;
        samples_num = framesize;
        status_flag = 0;
        for (int i = 0; i < stage_num; i++) {
            Arrays.fill(v[i], 0);
        }
    }

    int channel_num;
    int stage_num;
    double[][] a;
    double[][] b;
    double[][] v;
    int samples_num;
    int status_flag;

    /**
     * perform processing for one frame
     * 
     * @param inframe
     * @param outframe
     */
    public void nextFrame(double[] inframe, double[] outframe, double gain) {
        Vectors.setFormat(10, 5);
        for (int i = 0; i < outframe.length; i++) {
            outframe[i] = inframe[i];
            for (int k = 0; k < stage_num; k++) {
                v[k][0] = outframe[i] - a[k][0] * v[k][1] - a[k][1] * v[k][2];
                outframe[i] = b[k][0] * v[k][0] + b[k][1] * v[k][1] + b[k][2]
                    * v[k][2];
                v[k][2] = v[k][1];
                v[k][1] = v[k][0];
            }
            outframe[i] *= gain;
        }
    }

    public static void main(String[] args) {

        // a0 omitted: always 1
        double[][] aa = { {-1.867051864128537, 0.875313511144923},
            {-1.921144065558925, 0.946622554622648}};
        double[][] bb = { {1.0, 2.0, 1.0}, {1.0, 2.0, 1.0}};
        double gain = 1.241996358804925e-05;

        int N = 100;
        int i;
        double[] x = new double[N];
        double[] y = new double[N];
        // create test signal
        for (i = 0; i < N; i++) {
            x[i] = Math.sin(Math.PI * i / 20) + Math.sin(Math.PI * i / 12);
        }

        System.out.println("*** original ***");
        FirFilter.rprintf(x, N);

        // assemble filter
        IirFilter filter = new IirFilter(1, 2, 10);
        filter.a = aa;
        filter.b = bb;

        double[] inframe = new double[filter.samples_num];
        double[] outframe = new double[filter.samples_num];

        // process signal
        for (i = 0; i < N; i += filter.samples_num) {
            System.arraycopy(x, i, inframe, 0, filter.samples_num);
            filter.nextFrame(inframe, outframe, gain);
            System.arraycopy(outframe, 0, y, i, filter.samples_num);
        }

        System.out.println("*** filtered ***");
        FirFilter.rprintf(y, N);
    }
}
