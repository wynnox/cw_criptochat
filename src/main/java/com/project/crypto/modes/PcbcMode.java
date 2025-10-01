package com.project.crypto.modes;

import com.project.crypto.ciphers.Algorithm;
import com.project.crypto.util.Bytes;

import java.util.Arrays;

public class PcbcMode implements CipherMode {

    private final Algorithm algorithm;

    public PcbcMode(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] iv) {
        int blockSize = algorithm.getBlockSize();
        int blocksCount = data.length / blockSize;
        byte[] result = new byte[data.length];
        byte[] prevBlock = Arrays.copyOf(iv, iv.length);

        for (int i = 0; i < blocksCount; i++) {
            int idx = i * blockSize;
            byte[] block = Arrays.copyOfRange(data, idx, idx + blockSize);
            byte[] xored = Bytes.xor(block, prevBlock);
            byte[] encrypted = algorithm.encryptBlock(xored);
            System.arraycopy(encrypted, 0, result, idx, blockSize);
            prevBlock = Bytes.xor(block, encrypted);
        }

        return result;
    }

    @Override
    public byte[] decrypt(byte[] data, byte[] iv) {
        int blockSize = algorithm.getBlockSize();
        int blocksCount = data.length / blockSize;
        byte[] result = new byte[data.length];
        byte[] prevBlock = Arrays.copyOf(iv, iv.length);

        for (int i = 0; i < blocksCount; i++) {
            int idx = i * blockSize;
            byte[] block = Arrays.copyOfRange(data, idx, idx + blockSize);
            byte[] decrypted = algorithm.decryptBlock(block);
            byte[] plain = Bytes.xor(decrypted, prevBlock);
            System.arraycopy(plain, 0, result, idx, blockSize);
            prevBlock = Bytes.xor(block, plain);
        }

        return result;
    }
}
