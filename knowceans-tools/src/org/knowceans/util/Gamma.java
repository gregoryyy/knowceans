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

import static java.lang.Math.log;

/**
 * Gamma represents the Gamma function and its derivatives
 * 
 * @author heinrich
 */

public class Gamma {
    /**
     * Euler-Mascheroni constant
     */
    public static final double GAMMA = 0.57721566490153286;

    /**
     * truncated Taylor series of log Gamma(x). From lda-c
     * 
     * @param x
     * @return
     */
    public static double lgamma(double x) {
        double z;
        assert x > 0;
        z = 1. / (x * x);

        x = x + 6;
        z = (((-0.000595238095238 * z + 0.000793650793651) * z - 0.002777777777778)
            * z + 0.083333333333333)
            / x;
        z = (x - 0.5) * log(x) - x + 0.918938533204673 + z - log(x - 1)
            - log(x - 2) - log(x - 3) - log(x - 4) - log(x - 5) - log(x - 6);
        return z;
    }

    /**
     * gamma function
     * 
     * @param x
     * @return
     */
    public static double fgamma(double x) {
        return Math.exp(lgamma(x));
    }

    /**
     * faculty of an integer.
     * 
     * @param n
     * @return
     */
    public static int faculty(int n) {
        return (int) Math.exp(lgamma(n + 1));
    }

    /**
     * "Dirichlet delta function" is the partition function of the Dirichlet
     * distribution and the k-dimensional generalisation of the beta function.
     * fdelta(a) = prod_k fgamma(a_k) / fgamma( sum_k a_k ) = int_(sum x = 1)
     * prod_k x_k^(a_k-1) dx. See G. Heinrich: Parameter estimation for text
     * analysis (http://www.arbylon.net/publications/text-est.pdf)
     * 
     * @param x
     * @return
     */
    public static double ldelta(double[] x) {
        double lognum = 1;
        double den = 0;
        for (int i = 0; i < x.length; i++) {
            lognum += lgamma(x[i]);
            den += x[i];
        }
        return Math.exp(lognum - lgamma(den));
    }
    
    public static double fdelta(double[] x) {
        return Math.exp(ldelta(x));
    }

    public static double ldelta(int[] x) {
        double lognum = 0;
        double den = 0;
        for (int i = 0; i < x.length; i++) {
            lognum += lgamma(x[i]);
            den += x[i];
        }
        return lognum - lgamma(den);
    }
    
    public static double fdelta(int[] x) {
        return Math.exp(ldelta(x));
    }
        

    /**
     * log Delta function with a symmetric concentration parameter alpha that is
     * added to every element in the vector.
     * 
     * @param x
     * @param alpha
     * @return
     */
    public static double ldelta(int[] x, double alpha) {
        double lognum = 0;
        double den = 0;
        for (int i = 0; i < x.length; i++) {
            lognum += lgamma(x[i] + alpha);
            den += x[i];
        }
        den += alpha * x.length;
        return lognum - lgamma(den);
    }
    
    public static double fdelta(int[] x, double alpha) {
        return Math.exp(ldelta(x, alpha));
    }
        
    
    /**
     * Symmetric version of the log Dirichlet delta function
     * 
     * @param K
     * @param x
     * @return
     */
    public static double ldelta(int K, double x) {
        double delta;
        delta = K * lgamma(x) - lgamma(K * x);
        return delta;
    }
    
    
    public static double fdelta(int K, double x) {
        return Math.exp(ldelta(K, x));
    }
    

    /**
     * truncated Taylor series of Psi(x) = d/dx Gamma(x). From lda-c
     * 
     * @param x
     * @return
     */
    public static double digamma(double x) {
        double p;
        assert x > 0;
        x = x + 6;
        p = 1 / (x * x);
        p = (((0.004166666666667 * p - 0.003968253986254) * p + 0.008333333333333)
            * p - 0.083333333333333)
            * p;
        p = p + log(x) - 0.5 / x - 1 / (x - 1) - 1 / (x - 2) - 1 / (x - 3) - 1
            / (x - 4) - 1 / (x - 5) - 1 / (x - 6);
        return p;
    }

    /**
     * coarse approximation of the inverse of the digamma function (after Eqs.
     * 132-135 in Minka (2003), Estimating a Dirichlet distribution)
     */
    public static double invdigamma(double y) {
        double x = 0;
        // initialisation (135)
        if (y >= -2.22) {
            x = Math.exp(y) + .5;
        } else {
            // gamma = - digamma(1)
            x = -1 / (y + GAMMA);
        }
        // Newton's method (132)
        for (int i = 0; i < 5; i++) {
            x = x - (digamma(x) - y) / trigamma(x);
        }
        return x;
    }

    /**
     * truncated Taylor series of d/dx Psi(x) = d^2/dx^2 Gamma(x). From lda-c
     * 
     * @param x
     * @return
     */
    public static double trigamma(double x) {
        double p;
        int i;

        x = x + 6;
        p = 1 / (x * x);
        p = (((((0.075757575757576 * p - 0.033333333333333) * p + 0.0238095238095238)
            * p - 0.033333333333333)
            * p + 0.166666666666667)
            * p + 1)
            / x + 0.5 * p;
        for (i = 0; i < 6; i++) {
            x = x - 1;
            p = 1 / (x * x) + p;
        }
        return (p);
    }

    /**
     * Recursive implementation of the faculty.
     * 
     * @param i
     * @return
     */
    public static long fak(int i) {
        if (i > 1) {
            return i * fak(i - 1);
        }
        return 1;
    }

    /**
     * Pochhammer function
     * 
     * @author Gregor Heinrich (after Thomas Minka's fastfit implementation)
     * @param x
     * @param n
     * @return
     */
    double pochhammer(double x, int n) {
        double result;
        if (n == 0)
            return 0;
        if (n <= 20) {
            int i;
            double xi = x;
            /* this assumes x is not too large */
            result = xi;
            for (i = n - 1; i > 0; i--) {
                xi = xi + 1;
                result *= xi;
            }
            result = log(result);
        } else if (x >= 1.e4 * n) {
            result = log(x) + (n - 1) * log(x + n / 2);
        } else
            result = lgamma(x + n) - lgamma(x);
        return result;
    }

    /**
     * Pochhammer digamma function
     * 
     * @author Gregor Heinrich (after Thomas Minka's fastfit implementation)
     * @param x
     * @param n
     * @return
     */
    public static double diPochhammer(double x, int n) {
        double result;
        if (n == 0)
            return 0;
        if (n <= 20) {
            int i;
            double xi = x;
            result = 1 / xi;
            for (i = n - 1; i > 0; i--) {
                xi = xi + 1;
                result += 1 / xi;
            }
        } else
            result = digamma(x + n) - digamma(x);
        return result;
    }

    /**
     * Pochhammer trigamma function.
     * 
     * @author Gregor Heinrich (after Thomas Minka's fastfit implementation)
     * @param x
     * @param n
     * @return
     */
    public static double triPochhammer(double x, int n) {
        double result;
        if (n == 0)
            return 0;
        if (n <= 20) {
            result = -1 / (x * x);
            n--;
            while (n > 0) {
                x = x + 1;
                result -= 1 / (x * x);
                n--;
            }
            return result;
        }
        return trigamma(x + n) - trigamma(x);
    }
}
