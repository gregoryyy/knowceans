/*
 * Created on Jun 30, 2005
 */
package org.knowceans.util;

/**
 * DigitFormat formats double numbers into a specified digit format.
 * 
 * @author heinrich
 */
public class DigitFormat {

    public static void main(String[] args) {
        System.out.println(DigitFormat.format( 100, 2));
    }

    public static double format(double x, int ndigits) {
        int magnitude = (int) (Math.log(x) / Math.log(10));
        double factor = Math.pow(10, ndigits - 1 - magnitude);
        double y = Math.round(x * factor) / factor;

        return y;
    }
}
