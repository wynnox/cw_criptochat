package com.project.crypto.ciphers;

public interface Algorithm {
    int getBlockSize();

    void setKey(byte[] key);

    byte[] encryptBlock(byte[] block);
    byte[] decryptBlock(byte[] block);
}