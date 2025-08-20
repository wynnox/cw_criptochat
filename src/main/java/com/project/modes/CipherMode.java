package main.java.com.project.modes;

public interface CipherMode {
    byte[] encrypt(byte[] data, byte[] iv);
    byte[] decrypt(byte[] data, byte[] iv);
}