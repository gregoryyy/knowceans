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
        List<Integer> a = primes(1000000);
        System.out.println(a);
        int b = a.get(a.size() - 1);
        System.out.println(factors(b));
        System.out.println(factors(b - 1));
    }

    /**
     * Sieve of Erastothenes method
     * 
     * @param N
     * @return
     */
    public static List<Integer> primes(int n) {
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

        return primes;
    }

    /**
     * factors the number n into primes
     * 
     * @param n
     * @return
     */
    public static List<Integer> factors(int n) {
        List<Integer> factors = new ArrayList<Integer>();
        for (int i = 2; i <= n; i++) {
            while (n % i == 0) {
                factors.add(i);
                n /= i;
            }
        }
        return factors;
    }

}
