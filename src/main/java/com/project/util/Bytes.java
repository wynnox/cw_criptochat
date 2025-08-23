package main.java.com.project.util;

import java.math.BigInteger;

public class Bytes {
    private Bytes(){}

    public static byte[] toFixed(BigInteger x, int len) {
        byte[] v = x.toByteArray();
        if (v.length == len) return v;
        byte[] out = new byte[len];

        if (v[0]==0 && v.length - 1 <= len) {
            System.arraycopy(v, 1, out, len-(v.length - 1), v.length - 1);
        } else if (v.length <= len) {
            System.arraycopy(v, 0, out, len-v.length, v.length);
        } else {
            System.arraycopy(v, v.length-len, out, 0, len);
        }
        return out;
    }

    public static BigInteger fromUnsigned(byte[] be) {
        return new BigInteger(1, be);
    }
}
