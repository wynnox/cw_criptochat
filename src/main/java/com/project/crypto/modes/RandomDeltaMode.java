package com.project.crypto.modes;

import com.project.crypto.ciphers.Algorithm;
import com.project.crypto.util.Bytes;

import java.math.BigInteger;
import java.util.Arrays;

public class RandomDeltaMode implements CipherMode {

    private final Algorithm algorithm;

    public RandomDeltaMode(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] iv) {
        int blockSize = algorithm.getBlockSize();
        byte[] result = new byte[data.length];

        BigInteger initial = Bytes.fromUnsigned(Arrays.copyOfRange(iv, 0, blockSize / 2));
        BigInteger delta   = Bytes.fromUnsigned(Arrays.copyOfRange(iv, blockSize / 2, blockSize));

        int blocksCount = (int) Math.ceil((double) data.length / blockSize);

        for (int i = 0; i < blocksCount; i++) {
            int idx = i * blockSize;
            int remaining = Math.min(blockSize, data.length - idx);
            byte[] block = Arrays.copyOfRange(data, idx, idx + remaining);

            BigInteger current = initial.add(delta.multiply(BigInteger.valueOf(i)));
            byte[] deltaBytes = Bytes.toFixed(current, blockSize);

            byte[] xored = Bytes.xor(block, Arrays.copyOf(deltaBytes, remaining));
            byte[] encrypted = algorithm.encryptBlock(Arrays.copyOf(xored, blockSize));

            System.arraycopy(encrypted, 0, result, idx, remaining);
        }

        return result;
    }

    @Override
    public byte[] decrypt(byte[] data, byte[] iv) {
        int blockSize = algorithm.getBlockSize();
        byte[] result = new byte[data.length];

        BigInteger initial = Bytes.fromUnsigned(Arrays.copyOfRange(iv, 0, blockSize / 2));
        BigInteger delta   = Bytes.fromUnsigned(Arrays.copyOfRange(iv, blockSize / 2, blockSize));

        int blocksCount = (int) Math.ceil((double) data.length / blockSize);

        for (int i = 0; i < blocksCount; i++) {
            int idx = i * blockSize;
            int remaining = Math.min(blockSize, data.length - idx);
            byte[] block = Arrays.copyOfRange(data, idx, idx + remaining);

            BigInteger current = initial.add(delta.multiply(BigInteger.valueOf(i)));
            byte[] deltaBytes = Bytes.toFixed(current, blockSize);

            byte[] decryptedBlock = algorithm.decryptBlock(Arrays.copyOf(block, blockSize));
            byte[] plain = Bytes.xor(decryptedBlock, Arrays.copyOf(deltaBytes, remaining));

            System.arraycopy(plain, 0, result, idx, remaining);
        }

        return result;
    }
}
