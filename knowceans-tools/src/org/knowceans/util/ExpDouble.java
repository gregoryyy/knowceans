package org.knowceans.util;

import java.util.Arrays;

/**
 * ExpDouble represents a double-precision number by a mantissa, a decimal
 * exponent and the number of digits in the mantissa, in order to allow
 * formatting of the represented double.
 * 
 * @author gregor
 */
public class ExpDouble {

    private static double log10 = Math.log(10);

    public static void main(String[] args) {
        double[] x = new double[] {-1.0099638, 3.56, 30, -300.04, -45383487,
            9.998, 34598E12};
        for (int i = 0; i < x.length; i++) {
            ExpDouble a = new ExpDouble(x[i], 5);
            a.round(3);
            System.out.println(a.debug());
            a.round(1);
            System.out.println(a.toExpString());
        }

    }

    /**
     * Creates an ExpDouble from a double, with the specified number of digits.
     * 
     * @param x
     * @param maxdigits
     */
    public ExpDouble(double x, int maxdigits) {
        magnitude = orderOfMagnitude(x);
        formatExponential(x, maxdigits);
        value = x;
    }

    /**
     * mantissa of number (unsigned)
     */
    long mantissa;
    /**
     * exponent of number
     */
    int exponent;
    /**
     * digits in mantissa
     */
    int digits;
    /**
     * order of magnitude of represented double
     */
    int magnitude;

    /**
     * the original value;
     */
    double value;

    /**
     * negative mantissa
     */
    boolean negative;

    /**
     * Round this instance to the number of significant digits
     * 
     * @param digits
     * @return this
     */
    public ExpDouble round(int ndigits) {
        int diff = digits - ndigits;
        if (diff <= 0) {
            return this;
        }

        // mantissa: ###$ -> ###.$ -> ###
        mantissa = Math.round((double) mantissa / Math.pow(10, diff));
        exponent += diff;
        digits -= diff;
        // case >9.5 -> 10
        if (orderOfMagnitude(mantissa) >= digits) {
            mantissa /= 10;
            exponent++;
            // magnitude changes because we are interested in formatting.
            // the represented value is actually one order below
            magnitude++;
        }
        return this;
    }

    /**
     * @return length as a string
     */
    int strlen() {
        int len = digits;
        // minus sign
        if (negative) {
            len++;
        }
        // contains fractional part
        if (exponent < 0) {
            // point
            len++;
            // trailing zeros
            int tz = -exponent - digits;
            if (tz >= 0) {
                len += tz;
                // 0 left of . as additional char
                len++;
            }
        } else {
            len += exponent;
        }
        return len;
    }

    int strlenexp() {
        int len = minstrlenexp();
        if (digits == 1) {
            return len;
        }
        // digits + point - 1 digit from minlen
        return minstrlenexp() + digits;
    }

    /**
     * @return minimum length of the number in exponential notation 1E3
     */
    int minstrlenexp() {
        // digit and E3
        int len = 1 + 2;
        if (negative) {
            // minus sign
            len++;
        }
        if (value < 1) {
            // neg exponent
            len++;
        }
        if (value < 1E-10 || value > 1E10) {
            // additional exponent digit
            len++;
        }
        return len;
    }

    /**
     * @return string representation of this object
     */
    public String toString() {
        String[] pp = parts();
        if (pp[1] == null) {
            return pp[0];
        }
        return new StringBuilder().append(pp[0]).append(".").append(pp[1])
            .toString();
    }

    /**
     * @return a string representation in exponential notation
     */
    public String toExpString() {
        // #### E 5 = #.### E 5+(digits-1=3)
        // ### E -2 = #.## E -2+(digits-1=2)
        double factor = Math.pow(10, digits - 1);
        double m = mantissa / factor;
        String s = negative ? "-" : "";
        if (digits == 1) {
            s += Long.toString((long) m);
        } else {
            s += Double.toString(m);
        }
        s = s + "E" + (exponent + digits - 1);
        return s;
    }

    /**
     * @return a debug string with the part of this ExpDouble
     */
    public String debug() {
        return toString() + " = " + mantissa + "E" + exponent + " (" + "neg="
            + negative + ",digits=" + digits + ",magnitude=" + magnitude
            + ",strlen=" + strlen() + ")";
    }

    /**
     * @return double representation of this object according to rounding.
     */
    public double toDoubleUnsigned() {
        return mantissa * Math.pow(10, exponent);
    }

    /**
     * @return double representation of this object according to rounding.
     */
    public double toDouble() {
        double x = toDouble();
        return negative ? -x : x;
    }

    /**
     * @return string array of the integer and fraction parts
     */
    protected String[] parts() {

        String[] parts = new String[2];
        // integer part
        parts[0] = Long.toString((long) toDoubleUnsigned());
        // fractional part
        StringBuffer frac = new StringBuffer();
        if (exponent < 0) {
            int trailingZeros = -exponent - digits;
            int zerotail = 0;
            char[] zz;
            if (trailingZeros > 0) {
                zz = new char[trailingZeros];
                Arrays.fill(zz, '0');
                frac.append(zz).append(mantissa);
            } else {
                String s = Long.toString(mantissa).substring(
                    Math.abs(trailingZeros));
                frac.append(s);
                zerotail = digits - (s.length() - trailingZeros);
            }
            // pad tailing zeros if they belong to digits
            // #.###00
            if (zerotail > 0) {
                zz = new char[zerotail];
                Arrays.fill(zz, '0');
                frac.append(zz);
            }
            parts[1] = frac.toString();

        }
        if (negative) {
            parts[0] = "-" + parts[0];
        }
        return parts;
    }

    /**
     * formats the number into a mantissa with interval in interval
     * [1,10)E{ndigits} and an exponent.
     * 
     * @param x the number
     * @param ndigits the number of significant digits
     */
    protected void formatExponential(double x, int ndigits) {

        exponent = magnitude - ndigits + 1;
        digits = ndigits;
        double factor = Math.pow(10, -exponent);
        mantissa = (long) Math.abs(Math.round(x * factor));

        // case >9.5 -> 10
        if (orderOfMagnitude(mantissa) >= digits) {
            mantissa /= 10;
            exponent++;
            // magnitude changes because we are interested in formatting.
            // the represented value is actually one order below
            magnitude++;
        }

        if (x < 0) {
            negative = true;
        }

        // significant digits < max number of digits:
        // this does x=1803,digits=3 => 18E2 with only 2 digits
        // and not 3
        // while ((mantissa % 10) == 0) {
        // digits--;
        // mantissa /= 10;
        // exponent++;
        // }
    }

    /**
     * @param x
     * @return order of magnitue of x, i.e., n : x in [ 10^n, 10^(n+1) )
     */
    public static int orderOfMagnitude(double x) {
        x = Math.abs(x);
        int magnitude = (int) (Math.log(x + 1E-12) / log10);
        if (x < 1)
            magnitude--;
        return magnitude;
    }

}
