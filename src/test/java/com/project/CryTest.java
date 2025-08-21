package test.java.com.project;

import main.java.com.project.ciphers.MARS;
import main.java.com.project.factories.*;
import main.java.com.project.modes.CbcMode;
import main.java.com.project.padding.Pkcs7Padding;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;


public class CryTest {

    private final SecureRandom rng = new SecureRandom();


    @Test
    void mars() {
        MARS mars = new MARS();
        byte[] key = randomKey();
        mars.setKey(key);

        for (int i = 0; i < 8; i++) {
            byte[] pt = randomBytes(16);
            byte[] ct = mars.encryptBlock(pt);
            byte[] dec = mars.decryptBlock(ct);
            assertArrayEquals(pt, dec);
        }
    }

    @Test
    void cbc_pkcs7() {
        MARS mars = new MARS();
        mars.setKey(randomKey());

        CbcMode cbc = new CbcMode(mars);
        Pkcs7Padding pkcs7 = new Pkcs7Padding();

        byte[] iv = randomBytes(mars.getBlockSize());
        byte[] msg = randomBytes(rng.nextInt(1024));

        byte[] enc = cbc.encrypt(pkcs7.pad(msg, mars.getBlockSize()), iv);
        byte[] dec = cbc.decrypt(enc, iv);
        byte[] out = pkcs7.unpad(dec);

        assertArrayEquals(msg, out);
    }

    @Test
    void pkcs7() {
        Pkcs7Padding pad = new Pkcs7Padding();
        byte[] data = new byte[16];
        byte[] p = pad.pad(data, 16);
        assertEquals(32, p.length);
        for (int i = 16; i < 32; i++)
            assertEquals(16, p[i] & 0xff);
        assertArrayEquals(data, pad.unpad(p));
    }

    @Test
    void end_to_end_via_factory() {
        byte[] key = randomKey();

        CryptoSuite suite = new CryptoFactory.Builder()
                .algorithm(AlgorithmType.MARS)
                .mode(ModeType.CBC)
                .padding(PaddingType.PKCS7)
                .key(key)
                .buildSuite();

        for (int len : new int[]{0, 5, 16, 31, 64, 257}) {
            byte[] iv  = randomBytes(suite.getBlockSize());
            byte[] msg = randomBytes(len);

            byte[] ct  = suite.encrypt(msg, iv);
            byte[] dec = suite.decrypt(ct, iv);

            assertArrayEquals(msg, dec, "factory failed (len="+len+")");
        }
    }

    private byte[] randomKey() {
        int nWords = 4 + rng.nextInt(11);
        byte[] key = new byte[nWords * 4];
        rng.nextBytes(key);
        return key;
    }
    private byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        rng.nextBytes(b);
        return b;
    }
}
