package test.java.com.yourproject;

import main.java.com.project.ciphers.MARS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

public class MARSTest {

    private SecureRandom rng;

    @BeforeEach
    void setup() {
        rng = new SecureRandom();
    }

    @Test
    void Test() {
        final int KEY_ITERATIONS   = 32;
        final int BLOCK_ITERATIONS = 64;

        MARS mars = new MARS();
        SecureRandom rng = new SecureRandom();

        for (int t = 0; t < KEY_ITERATIONS; t++) {
            int nWords = 4 + rng.nextInt(11);
            byte[] key = new byte[nWords * 4];
            rng.nextBytes(key);
            mars.setKey(key);

            for (int i = 0; i < BLOCK_ITERATIONS; i++) {
                byte[] pt = new byte[16];
                rng.nextBytes(pt);

                byte[] ct  = mars.encryptBlock(pt);
                byte[] dec = mars.decryptBlock(ct);

                assertArrayEquals(
                        pt, dec,
                        "Roundtrip failed (t="+t+", i="+i+") key="+toHex(key)+" pt="+toHex(pt)
                );
            }
        }
    }

    private static String toHex(byte[] a){
        StringBuilder sb = new StringBuilder();
        for (byte b: a) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
