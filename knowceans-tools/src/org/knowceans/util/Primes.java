package org.knowceans.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.knowceans.util.primes.BigIntegerRSA;
import org.knowceans.util.primes.ECM;
import org.knowceans.util.primes.FactoringAlgo;

/**
 * simple prime number generator and factoriser
 * 
 * @author gregor
 * 
 */
public class Primes {

    public static void main(String[] args) {
        int[] a = primes(100000, 20000000);
        // System.out.println(Vectors.print(a));
        // int b = a[a.length - 1];

        BigIntegerRSA rsa = new BigIntegerRSA();
        rsa.setBitLength(86);
        rsa.generateNewPrimes();
        BigInteger b = rsa.getPQ();

        BigInteger[] ii = factorsElliptic(b);
        System.out.println(ii);
        // System.out.println(Vectors.print(factorsNaive(b - 1)));
        // System.out.println(Vectors.print(factors(BigInteger.valueOf(b))));
        // System.out.println(Vectors.print(factors(b - 1)));
    }

    /**
     * Primes in the interval [nlow, nhigh].
     * <p>
     * Sift the twos and sift the threes, / the sieve of Eratosthenes. / When
     * the multiples sublime, / The numbers that remain are prime. ;)
     * <p>
     * code adapted from:
     * http://en.literateprograms.org/Sieve_of_Eratosthenes_%28Java%29
     * 
     * @param nlow
     * @param nhigh
     * @return array of prime numbers
     */
    public static int[] primes(int nlow, int nhigh) {
        // element k corresponds to number i = 2k + 3
        BitSet sieve = new BitSet((nhigh + 2) >> 1);
        for (int i = 3; i * i <= nhigh; i += 2) {
            // if prime
            if (sieve.get((i - 3) >> 1))
                continue;
            // loop over odd multiples of i
            for (int mi = i * i; mi <= nhigh; mi += i << 1)
                // set prime
                sieve.set((mi - 3) >> 1);
        }
        List<Integer> primes = new ArrayList<Integer>();
        if (nlow <= 2)
            primes.add(2);
        for (int i = 3; i <= nhigh; i += 2)
            if (!sieve.get((i - 3) >> 1) && i >= nlow)
                primes.add(i);

        return (int[]) ArrayUtils.asPrimitiveArray(primes);
    }

    /**
     * Primes up to n
     * <p>
     * Sift the twos and sift the threes, / the sieve of Eratosthenes. / When
     * the multiples sublime, / The numbers that remain are prime. ;)
     * <p>
     * code adapted from:
     * http://en.literateprograms.org/Sieve_of_Eratosthenes_%28Java%29
     * 
     * @param N
     * @return array of prime numbers, size approx. 1.11 / log n ~ Pi(n)
     */
    public static int[] primes(int n) {
        return primes(2, n);
    }

    /**
     * factors the number n into primes. For more efficient approaches, use
     * elliptic curves etc.: http://www.alpertron.com.ar/ECM.HTM
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

    /**
     * Pollard's rho algorithm
     * 
     * @param n
     * @return a nontrivial prime factor
     */
    public static long factorRho(long n) {
        return factorRho(n, 1l);
    }

    /**
     * Pollard's rho algorithm with controllable pseudo-random function: x^2 +
     * add
     * 
     * @param n
     * @param add
     * @return a nontrivial prime factor
     */
    public static long factorRho(long n, long add) {
        long x = 2;
        long y = 2;
        long d = 1;
        while (d == 1) {
            x = f(x, n, add);
            y = f(f(y, n, add), n, add);
            d = gcd(Math.abs(x - y), n);
        }
        if (d == n) {
            if (add < MAXADD) {
                return factorBrent(n, add + 1);
            } else {
                return 1;
            }
        } else {
            return d;
        }
    }

    /**
     * maximum variants of the Pollard and Brent pseudo-random functions
     */
    public static long MAXADD = 10;

    /**
     * prime factorisation using Brent's variant of Pollard's Rho algorithm
     * <p>
     * Web: http://www.worldlingo.com/ma/enwiki/en/Pollard
     * 's_rho_algorithm#Richard_Brent.27s_variant
     * <p>
     * for values beyond the range of Java's 64 bit long, this can be adapted to
     * BigInteger
     * 
     * @return
     */
    public static long factorBrent(long n) {
        return factorBrent(n, 1l);
    }

    /**
     * Brent's algorithm with parameter for controllable pseudo-random function:
     * x^2 + add.
     * 
     * @param n
     * @param add
     * @return
     */
    public static long factorBrent(long n, long add) {

        long x0 = Samplers.randUniform(100) * n / 100l;
        long m = Samplers.randUniform(100) * n / 100l;
        long y = x0;
        long r = 1l;
        long q = 1l;

        long ys = 0l;
        long g = 0;
        long x = 0;
        do {
            x = y;
            for (long i = 1; i < r; i++)
                y = f(y, n, add);

            long k = 0;
            do {
                ys = x0;
                for (long i = 1; i < Math.min(m, r - k); i++) {
                    y = f(y, n, add);
                    q *= Math.abs(x - y) % n;
                }
                g = gcd(q, n);
                k += m;
            } while (k >= r || g >= 1);
            r <<= 1;
        } while (g > 1);

        if (g == n) {
            do {
                ys = f(ys, n, add);
                g = gcd(Math.abs(x - ys), n);
            } while (g > 1);
        }

        if (g == 1 && add < MAXADD) {
            return factorBrent(n, add + 1);
        }
        return g;
    }

    /**
     * greatest common denominator, using BigInteger maths.
     * 
     * @param q
     * @param n
     * @return
     */
    static long gcd(long q, long n) {
        return BigInteger.valueOf(q).gcd(BigInteger.valueOf(n)).longValue();
    }

    /**
     * pseudo random function modulo n
     * 
     * @param x
     * @param n
     * @return
     */
    private static long f(long x, long n, long add) {
        return (x * x + add) % n;
    }

    /**
     * factorises n using elliptic curves
     * 
     * @param n
     * @return
     */
    public static BigInteger[] factorsElliptic(BigInteger n) {
        BigInteger[] factor1 = new BigInteger[2];
        // EcFactors ecm = new EcFactors();
        // ECM ecm = new ECM();

        // Factor
        FactoringAlgo algo = new ECM();
        factor1[0] = algo.factor(n);
        factor1[1] = n.divide(factor1[0]);

        return factor1;
    }
}
