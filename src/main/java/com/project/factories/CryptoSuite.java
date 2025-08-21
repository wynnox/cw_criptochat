package main.java.com.project.factories;

import main.java.com.project.ciphers.Algorithm;
import main.java.com.project.modes.CipherMode;
import main.java.com.project.padding.Padding;

public final class CryptoSuite {
    private final Algorithm algorithm;
    private final CipherMode mode;
    private final Padding padding;
    private final int blockSize;

    CryptoSuite(Algorithm algorithm, CipherMode mode, Padding padding) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.padding = padding;
        this.blockSize = algorithm.getBlockSize();
    }

    public byte[] encrypt(byte[] plaintext, byte[] ivOrNonce) {
        byte[] data = (padding != null) ? padding.pad(plaintext, blockSize) : plaintext;
        return mode.encrypt(data, ivOrNonce);
    }

    public byte[] decrypt(byte[] ciphertext, byte[] ivOrNonce) {
        byte[] data = mode.decrypt(ciphertext, ivOrNonce);
        return (padding != null) ? padding.unpad(data) : data;
    }

    public int getBlockSize() {
        return blockSize;
    }
}
