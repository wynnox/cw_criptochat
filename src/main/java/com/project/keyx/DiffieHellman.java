package main.java.com.project.keyx;

import main.java.com.project.util.Bytes;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class DiffieHellman implements KeyAgreement {
    private final DhParams params;
    private final SecureRandom rng = new SecureRandom();

    public DiffieHellman(DhParams params) {
        this.params = params;
    }

    @Override
    public byte[] generatePrivate() {
        BigInteger x;
        do {
            x = new BigInteger(params.q.bitLength(), rng);
        } while (x.compareTo(BigInteger.TWO) < 0 || x.compareTo(params.q) >= 0);

        return Bytes.toFixed(x, params.encodedLen);
    }

    @Override
    public byte[] derivePublic(byte[] privateKey) {
        BigInteger x = Bytes.fromUnsigned(privateKey);
        // y = g^x mod p
        BigInteger y = params.g.modPow(x, params.p);
        return Bytes.toFixed(y, params.encodedLen);
    }

    @Override
    public byte[] deriveShared(byte[] myPrivate, byte[] peerPublic) {
        BigInteger x = Bytes.fromUnsigned(myPrivate);
        BigInteger Y = Bytes.fromUnsigned(peerPublic);

        // 2 <= Y <= p-2
        if (Y.compareTo(BigInteger.TWO) < 0 || Y.compareTo(params.p.subtract(BigInteger.TWO)) > 0) {
            throw new IllegalArgumentException("Неверный публичный ключ DH");
        }

        // s = Y^x mod p
        BigInteger s = Y.modPow(x, params.p);
        return Bytes.toFixed(s, params.encodedLen);
    }

    @Override
    public int getEncodedLength() {
        return params.encodedLen;
    }

    public DhParams getParams() {
        return params;
    }
}
