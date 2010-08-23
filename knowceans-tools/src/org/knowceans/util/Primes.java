package org.knowceans.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * simple prime number generator and factoriser
 * 
 * @author gregor
 * 
 */
public class Primes {

    public static void main(String[] args) {
        int[] a = primes(1000000);
        System.out.println(Vectors.print(a));
        int b = a[a.length - 1];
        System.out.println(Vectors.print(factors(b)));
        System.out.println(Vectors.print(factors(b - 1)));
    }

    /**
     * Sieve of Erastothenes method
     * 
     * @param N
     * @return
     */
    public static int[] primes(int n) {
        BitSet sieve = new BitSet((n + 2) >> 1);
        for (int i = 3; i * i <= n; i += 2) {
            if (sieve.get((i - 3) / 2))
                continue;
            for (int mi = i * i; mi <= n; mi += i << 1)
                sieve.set((mi - 3) / 2);

        }
        List<Integer> primes = new ArrayList<Integer>();
        primes.add(2);
        for (int i = 3; i <= n; i += 2)
            if (!sieve.get((i - 3) / 2))
                primes.add(i);

        return (int[]) ArrayUtils.asPrimitiveArray(primes);
    }

    /**
     * factors the number n into primes
     * 
     * @param n
     * @return
     */
    public static int[] factors(int n) {
        List<Integer> factors = new ArrayList<Integer>();
        for (int i = 2; i <= n; i++) {
            while (n % i == 0) {
                factors.add(i);
                n /= i;
            }
        }
        return (int[]) ArrayUtils.asPrimitiveArray(factors);
    }

}
