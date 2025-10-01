package com.project.crypto.padding;

import java.util.Arrays;

public class ANSI_X923Padding implements Padding {

    @Override
    public byte[] pad(byte[] data, int blockSize) {
        int paddingLength = blockSize - (data.length % blockSize);
        if (paddingLength == 0) {
            paddingLength = blockSize;
        }

        byte[] padded = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, padded, 0, data.length);

        padded[padded.length - 1] = (byte) paddingLength;
        return padded;
    }

    @Override
    public byte[] unpad(byte[] data) {
        if (data.length == 0) {
            throw new IllegalArgumentException("Data is empty");
        }

        int paddingLength = data[data.length - 1] & 0xFF;
        if (paddingLength <= 0 || paddingLength > data.length) {
            throw new IllegalArgumentException("Invalid padding length");
        }

        for (int i = data.length - paddingLength; i < data.length - 1; i++) {
            if (data[i] != 0) {
                throw new IllegalArgumentException("Invalid ANSI X.923 padding");
            }
        }

        return Arrays.copyOf(data, data.length - paddingLength);
    }
}
