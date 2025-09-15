package com.project.crypto.modes;

import com.project.crypto.ciphers.Algorithm;
import java.util.Arrays;

public class CfbMode implements CipherMode {

    private final Algorithm cipher;

    public CfbMode(Algorithm cipher) {
        this.cipher = cipher;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] iv) {

        int bs = cipher.getBlockSize();

        byte[] out  = new byte[data.length];
        byte[] prev = Arrays.copyOf(iv, bs);
        byte[] buf  = new byte[bs];

        for (int off = 0; off < data.length; off += bs) {
            byte[] enc = cipher.encryptBlock(prev);
            int rem = Math.min(bs, data.length - off);
            System.arraycopy(data, off, buf, 0, rem);
            xorArray(buf, enc);
            System.arraycopy(buf, 0, out, off, rem);
            System.arraycopy(buf, 0, prev, 0, bs);
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
            byte[] enc = cipher.encryptBlock(prev);
            int rem = Math.min(bs, data.length - off);
            System.arraycopy(data, off, buf, 0, rem);
            xorArray(buf, enc);
            System.arraycopy(buf, 0, out, off, rem);
            System.arraycopy(data, off, prev, 0, bs);
        }

        return out;
    }

    private static void xorArray(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) a[i] ^= b[i];
    }
}
