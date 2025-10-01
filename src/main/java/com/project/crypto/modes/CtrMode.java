package com.project.crypto.modes;

import com.project.crypto.ciphers.Algorithm;
import com.project.crypto.util.Bytes;

import java.util.Arrays;

public class CtrMode implements CipherMode {

    private final Algorithm algorithm;

    public CtrMode(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] iv) {
        int blockSize = algorithm.getBlockSize();
        byte[] result = new byte[data.length];

        int blocksCount = (int) Math.ceil((double) data.length / blockSize);
        byte[] counterBlock = Arrays.copyOf(iv, blockSize);

        for (int i = 0; i < blocksCount; i++) {
            int idx = i * blockSize;
            int remaining = Math.min(blockSize, data.length - idx);

            byte[] keystream = algorithm.encryptBlock(counterBlock);
            byte[] block = Arrays.copyOfRange(data, idx, idx + remaining);

            byte[] encrypted = Bytes.xor(block, Arrays.copyOf(keystream, remaining));
            System.arraycopy(encrypted, 0, result, idx, remaining);

            incrementCounter(counterBlock);
        }

        return result;
    }

    @Override
    public byte[] decrypt(byte[] data, byte[] iv) {
        return encrypt(data, iv);
    }

    private void incrementCounter(byte[] counter) {
        for (int i = counter.length - 1; i >= 0; i--) {
            counter[i]++;
            if (counter[i] != 0) break;
        }
    }
}
