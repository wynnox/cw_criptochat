package com.project.crypto.modes;

public interface CipherMode {
    byte[] encrypt(byte[] data, byte[] iv);
    byte[] decrypt(byte[] data, byte[] iv);
}