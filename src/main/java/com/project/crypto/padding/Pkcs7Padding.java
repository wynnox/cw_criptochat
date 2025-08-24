package com.project.crypto.padding;

import java.util.Arrays;

public class Pkcs7Padding implements Padding {

    @Override
    public byte[] pad(byte[] data, int blockSize) {
        int padLen = blockSize - (data.length % blockSize);
        if (padLen == 0) padLen = blockSize;
        byte[] out = Arrays.copyOf(data, data.length + padLen);
        Arrays.fill(out, data.length, out.length, (byte) padLen);
        return out;
    }

    @Override
    public byte[] unpad(byte[] data) {
        int padLen = data[data.length - 1] & 0xFF;
        int start = data.length - padLen;
        return Arrays.copyOf(data, start);
    }
}