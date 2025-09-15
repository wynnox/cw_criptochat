package com.project.crypto.ciphers;

import java.util.Arrays;
import static java.lang.Byte.toUnsignedInt;

public class Magenta implements Algorithm {

    private byte[] key;

    @Override
    public int getBlockSize() { return 16; }

    @Override
    public void setKey(byte[] key) {
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32))
            throw new IllegalArgumentException("Key length must be 16, 24 or 32 bytes");
        this.key = key;
    }

    @Override
    public byte[] encryptBlock(byte[] block) {
        if (block.length != 16)
            throw new IllegalArgumentException("Block must be 16 bytes");

        byte[][] roundKeys = generateRoundKeys(key);
        byte[] L = Arrays.copyOfRange(block, 0, 8);
        byte[] R = Arrays.copyOfRange(block, 8, 16);

        for (byte[] rk : roundKeys) {
            byte[] f = e(concat(R, rk), 3);
            byte[] newL = xor(L, f);
            L = R;
            R = newL;
        }

        return concat(L, R);
    }

    @Override
    public byte[] decryptBlock(byte[] block) {
        return v(encryptBlock(v(block)));
    }

    private static byte[] v(byte[] x) {
        return concat(Arrays.copyOfRange(x, 8, 16), Arrays.copyOfRange(x, 0, 8));
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] r = new byte[a.length];
        for (int i = 0; i < a.length; i++) r[i] = (byte) (a[i] ^ b[i]);
        return r;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }


    private static final int ALPHA = 0x02;
    private static final int MODULE = 0x165;
    private static final byte[] S = initS();

    private static byte[] initS() {
        byte[] s = new byte[256];
        s[0] = 1;
        for (int i = 1; i < 255; i++)
            s[i] = (byte) mulGF(s[i - 1], ALPHA, MODULE, 8);
        s[255] = 0;
        return s;
    }

    private static int mulGF(int a, int b, int mod, int bits) {
        int res = 0;
        for (int i = 0; i < bits; i++) {
            if ((b & 1) != 0) res ^= a;
            b >>= 1;
            a <<= 1;
            if ((a & (1 << bits)) != 0) a ^= mod;
        }
        return res & ((1 << bits) - 1);
    }

    private static byte f(byte x) { return S[toUnsignedInt(x)]; }

    private static byte a(byte a, byte b) { return f((byte) (a ^ f(b))); }

    private static byte[] pe(byte x, byte y) { return new byte[]{a(x, y), a(y, x)}; }

    private static byte[] pi(byte[] b16) {
        byte[] r = new byte[16];
        for (int i = 0; i < 8; i++) {
            byte[] p = pe(b16[i], b16[i + 8]);
            r[2 * i] = p[0];
            r[2 * i + 1] = p[1];
        }
        return r;
    }

    private static byte[] t(byte[] b16) {
        byte[] r = b16;
        for (int i = 0; i < 4; i++) r = pi(r);
        return r;
    }

    private static byte[] cj(byte[] b16, int r) {
        byte[] c = t(b16);
        for (int i = 1; i < r; i++) {
            byte[] ce = new byte[8], co = new byte[8];
            for (int j = 0; j < 8; j++) {
                ce[j] = c[j * 2];
                co[j] = c[j * 2 + 1];
            }
            c = t(concat(xor(Arrays.copyOfRange(b16, 0, 8), ce),
                    xor(Arrays.copyOfRange(b16, 8, 16), co)));
        }
        return c;
    }

    private static byte[] e(byte[] b16, int r) {
        byte[] c = cj(b16, r);
        byte[] out = new byte[8];
        for (int i = 0; i < 8; i++) out[i] = c[i * 2];
        return out;
    }

    private static byte[][] generateRoundKeys(byte[] key) {
        if (key.length == 16) {
            byte[] k1 = Arrays.copyOfRange(key, 0, 8);
            byte[] k2 = Arrays.copyOfRange(key, 8, 16);
            return new byte[][]{k1, k1, k2, k2, k1, k1};
        } else if (key.length == 24) {
            byte[] k1 = Arrays.copyOfRange(key, 0, 8);
            byte[] k2 = Arrays.copyOfRange(key, 8, 16);
            byte[] k3 = Arrays.copyOfRange(key, 16, 24);
            return new byte[][]{k1, k2, k3, k3, k2, k1};
        } else {
            byte[] k1 = Arrays.copyOfRange(key, 0, 8);
            byte[] k2 = Arrays.copyOfRange(key, 8, 16);
            byte[] k3 = Arrays.copyOfRange(key, 16, 24);
            byte[] k4 = Arrays.copyOfRange(key, 24, 32);
            return new byte[][]{k1, k2, k3, k4, k4, k3, k2, k1};
        }
    }
}
