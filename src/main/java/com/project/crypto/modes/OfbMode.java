package com.project.crypto.modes;

import com.project.crypto.ciphers.Algorithm;
import com.project.crypto.util.Bytes;

import java.util.Arrays;

public class OfbMode implements CipherMode {

    private final Algorithm cipher;

    public OfbMode(Algorithm cipher) {
        this.cipher = cipher;
    }

    public byte[] encrypt(byte[] text, byte[] iv) {
        int blockSize = cipher.getBlockSize();
        byte[] result = new byte[text.length];
        byte[] feedback = Arrays.copyOf(iv, iv.length);

        for (int i = 0; i < text.length; i += blockSize) {
            byte[] keystream = cipher.encryptBlock(feedback);
            int blockLen = Math.min(blockSize, text.length - i);

            byte[] partialKey = Arrays.copyOf(keystream, blockLen);
            byte[] block = Arrays.copyOfRange(text, i, i + blockLen);

            byte[] encryptedBlock = Bytes.xor(block, partialKey);

            System.arraycopy(encryptedBlock, 0, result, i, blockLen);
            feedback = keystream;
        }

        return result;
    }

    public byte[] decrypt(byte[] cipherText, byte[] iv) {
        return encrypt(cipherText, iv);
    }
}
