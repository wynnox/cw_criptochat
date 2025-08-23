package main.java.com.project.keyx;

public interface KeyAgreement {
    byte[] generatePrivate();
    byte[] derivePublic(byte[] privateKey);
    byte[] deriveShared(byte[] myPrivate, byte[] peerPublic);
    int getEncodedLength();
}
