package main.java.com.project.modes;

import main.java.com.project.ciphers.Algorithm;

import java.util.Arrays;

public class CbcMode implements CipherMode{

    private final Algorithm cipher;

    public CbcMode(Algorithm cipher) {
        this.cipher = cipher;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] iv) {

        int bs = cipher.getBlockSize();

        byte[] out  = new byte[data.length];
        byte[] prev = Arrays.copyOf(iv, bs);
        byte[] buf  = new byte[bs];

        for (int off = 0; off < data.length; off += bs) {
            System.arraycopy(data, off, buf, 0, bs);
            xorArray(buf, prev);
            byte[] c = cipher.encryptBlock(buf);
            System.arraycopy(c, 0, out, off, bs);
            prev = c;
        }

        return out;
    }

    @Override
    public byte[] decrypt(byte[] data, byte[] iv) {
        int bs = cipher.getBlockSize();

        byte[] out  = new byte[data.length];
        byte[] prev = Arrays.copyOf(iv, bs);
        byte[] buf  = new byte[bs];

        for (int off = 0; off < data.length; off += bs) {
            System.arraycopy(data, off, buf, 0, bs);
            byte[] d = cipher.decryptBlock(buf);
            xorArray(d, prev);
            System.arraycopy(d, 0, out, off, bs);
            System.arraycopy(buf, 0, prev, 0, bs);
        }
        return out;
    }

    private static void xorArray(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) a[i] ^= b[i];
    }
}
