package com.project.crypto.padding;

import java.util.Arrays;
import java.util.Random;

public class ISO_10126Padding implements Padding {

    @Override
    public byte[] pad(byte[] data, int blockSize) {
        Random random = new Random();
        int remainder = data.length % blockSize;
        int paddingLength = (remainder == 0) ? blockSize : (blockSize - remainder);

        byte[] result = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, result, 0, data.length);

        byte[] randomBytes = new byte[paddingLength - 1];
        random.nextBytes(randomBytes);
        System.arraycopy(randomBytes, 0, result, data.length, randomBytes.length);

        result[result.length - 1] = (byte) paddingLength;

        return result;
    }

    @Override
    public byte[] unpad(byte[] data) {
        int paddingLength = data[data.length - 1] & 0xFF;
        if (paddingLength <= 0 || paddingLength > data.length) {
            throw new IllegalArgumentException("Invalid padding length: " + paddingLength);
        }
        return Arrays.copyOf(data, data.length - paddingLength);
    }
}
