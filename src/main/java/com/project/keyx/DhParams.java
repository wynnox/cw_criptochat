package main.java.com.project.keyx;

import java.math.BigInteger;
import java.security.SecureRandom;

public class DhParams {
    public final BigInteger p;
    public final BigInteger q;
    public final BigInteger g;
    public final int encodedLen;

    private DhParams(BigInteger p, BigInteger q, BigInteger g) {
        this.p = p;
        this.q = q;
        this.g = g;
        this.encodedLen = (p.bitLength() + 7) / 8;
    }

    public static DhParams generate(int bitLength, int certainty, SecureRandom rng) {
        BigInteger q, p;

        while (true) {
            q = new BigInteger(bitLength - 1, certainty, rng);
            p = q.shiftLeft(1).add(BigInteger.ONE);
            if (p.isProbablePrime(certainty)) break;
        }

        BigInteger g = findGenerator(p, q, rng);

        return new DhParams(p, q, g);
    }

    private static BigInteger findGenerator(BigInteger p, BigInteger q, SecureRandom rng) {
        while (true) {
            // 2 <= g <= p-2
            BigInteger g = new BigInteger(p.bitLength() - 1, rng).mod(p.subtract(BigInteger.TWO)).add(BigInteger.TWO);

            // g^2 != 1 mod p и g^q != 1 mod p
            if (!g.modPow(BigInteger.TWO, p).equals(BigInteger.ONE)
                    && !g.modPow(q, p).equals(BigInteger.ONE)) {
                return g;
            }
        }
    }
}
