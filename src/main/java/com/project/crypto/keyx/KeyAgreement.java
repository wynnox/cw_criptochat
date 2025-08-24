package com.project.crypto.keyx;

public interface KeyAgreement {
    byte[] generatePrivate();
    byte[] derivePublic(byte[] privateKey);
    byte[] deriveShared(byte[] myPrivate, byte[] peerPublic);
    int getEncodedLength();
}
