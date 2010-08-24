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
     * Sift the twos and sift the threes, / the sieve of Eratosthenes. / When
     * the multiples sublime, / The numbers that remain are prime. ;)
     * <p>
     * code adapted from:
     * http://en.literateprograms.org/Sieve_of_Eratosthenes_%28Java%29
     * 
     * @param N
     * @return
     */
    public static int[] primes(int n) {
        // element k corresponds to number i = 2k + 3
        BitSet sieve = new BitSet((n + 2) >> 1);
        for (int i = 3; i * i <= n; i += 2) {
            // if prime
            if (sieve.get((i - 3) >> 1))
                continue;
            // loop over odd multiples of i
            for (int mi = i * i; mi <= n; mi += i << 1)
                // set prime
                sieve.set((mi - 3) >> 1);
        }
        List<Integer> primes = new ArrayList<Integer>();
        primes.add(2);
        for (int i = 3; i <= n; i += 2)
            if (!sieve.get((i - 3) >> 1))
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
