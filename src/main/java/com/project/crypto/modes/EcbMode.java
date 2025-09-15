package com.project.crypto.modes;

import com.project.crypto.ciphers.Algorithm;

public class EcbMode implements CipherMode {

    private final Algorithm cipher;

    public EcbMode(Algorithm cipher) {
        this.cipher = cipher;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] iv) {
        int bs = cipher.getBlockSize();

        byte[] out = new byte[data.length];
        byte[] buf = new byte[bs];

        for (int off = 0; off < data.length; off += bs) {
            System.arraycopy(data, off, buf, 0, bs);
            byte[] c = cipher.encryptBlock(buf);
            System.arraycopy(c, 0, out, off, bs);
        }

        return out;
    }

    @Override
    public byte[] decrypt(byte[] data, byte[] iv) {
        int bs = cipher.getBlockSize();

        byte[] out = new byte[data.length];
        byte[] buf = new byte[bs];

        for (int off = 0; off < data.length; off += bs) {
            System.arraycopy(data, off, buf, 0, bs);
            byte[] d = cipher.decryptBlock(buf);
            System.arraycopy(d, 0, out, off, bs);
        }

        return out;
    }
}
