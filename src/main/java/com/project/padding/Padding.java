package main.java.com.project.padding;

public interface Padding {
    byte[] pad(byte[] data, int blockSize);
    byte[] unpad(byte[] data);
}