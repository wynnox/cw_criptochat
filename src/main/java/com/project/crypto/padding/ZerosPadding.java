package com.project.crypto.padding;

import java.util.Arrays;

public class ZerosPadding implements Padding{
    @Override
    public byte[] pad(byte[] data, int blockSize) {
        int paddingLength = blockSize - (data.length % blockSize);
        if (paddingLength == 0) {
            paddingLength = blockSize;
        }

        byte[] padded = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, padded, 0, data.length);

        return padded;
    }

    @Override
    public byte[] unpad(byte[] data) {
        int i = data.length - 1;
        while (i >= 0 && data[i] == 0) {
            i--;
        }

        return Arrays.copyOf(data, i + 1);
    }
}
