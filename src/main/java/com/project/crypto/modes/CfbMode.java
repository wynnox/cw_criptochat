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

        for (int off = 0; off < data.length; off += bs) {
            byte[] enc = cipher.encryptBlock(prev);
            int rem = Math.min(bs, data.length - off);

            for (int i = 0; i < rem; i++) {
                out[off + i] = (byte) (data[off + i] ^ enc[i]);
            }

            System.arraycopy(out, off, prev, 0, rem);
        }

        return out;
    }

    @Override
    public byte[] decrypt(byte[] data, byte[] iv) {
        int bs = cipher.getBlockSize();

        byte[] out  = new byte[data.length];
        byte[] prev = Arrays.copyOf(iv, bs);

        for (int off = 0; off < data.length; off += bs) {
            byte[] enc = cipher.encryptBlock(prev);
            int rem = Math.min(bs, data.length - off);

            for (int i = 0; i < rem; i++) {
                out[off + i] = (byte) (data[off + i] ^ enc[i]);
            }

            System.arraycopy(data, off, prev, 0, rem);
        }

        return out;
    }
}