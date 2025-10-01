package com.project;

import com.project.crypto.ciphers.Magenta;
import com.project.crypto.factories.*;
import com.project.crypto.keyx.DhParams;
import com.project.crypto.keyx.DiffieHellman;
import com.project.crypto.modes.CbcMode;
import com.project.crypto.modes.CfbMode;
import com.project.crypto.modes.EcbMode;
import com.project.crypto.modes.OfbMode;
import com.project.crypto.padding.ISO_10126Padding;
import com.project.crypto.padding.Padding;
import com.project.crypto.padding.Pkcs7Padding;
import com.project.crypto.ciphers.MARS;
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
    void cfb_mode() {
        MARS mars = new MARS();
        mars.setKey(randomKey());

        CfbMode cfb = new CfbMode(mars);

        byte[] iv = randomBytes(mars.getBlockSize());
        byte[] msg = randomBytes(rng.nextInt(1024));

        byte[] enc = cfb.encrypt(msg, iv);
        byte[] dec = cfb.decrypt(enc, iv);

        assertArrayEquals(msg, dec);
    }

    @Test
    void ecb_pkcs7() {
        MARS mars = new MARS();
        mars.setKey(randomKey());

        EcbMode ecb = new EcbMode(mars);
        Pkcs7Padding pkcs7 = new Pkcs7Padding();

        byte[] iv = randomBytes(mars.getBlockSize()); // формально не используется
        byte[] msg = randomBytes(rng.nextInt(1024));

        byte[] enc = ecb.encrypt(pkcs7.pad(msg, mars.getBlockSize()), iv);
        byte[] dec = ecb.decrypt(enc, iv);
        byte[] out = pkcs7.unpad(dec);

        assertArrayEquals(msg, out);
    }

    @Test
    void ofb_mode() {
        MARS mars = new MARS();
        mars.setKey(randomKey());

        OfbMode ofb = new OfbMode(mars);

        byte[] iv = randomBytes(mars.getBlockSize());
        byte[] msg = randomBytes(rng.nextInt(1024));

        byte[] enc = ofb.encrypt(msg, iv);
        byte[] dec = ofb.decrypt(enc, iv);

        assertArrayEquals(msg, dec);
    }

    @Test
    void pcbc_pkcs7() {
        MARS mars = new MARS();
        mars.setKey(randomKey());

        com.project.crypto.modes.PcbcMode pcbc = new com.project.crypto.modes.PcbcMode(mars);
        com.project.crypto.padding.Pkcs7Padding pkcs7 = new com.project.crypto.padding.Pkcs7Padding();

        byte[] iv = randomBytes(mars.getBlockSize());
        byte[] msg = randomBytes(rng.nextInt(1024));

        byte[] enc = pcbc.encrypt(pkcs7.pad(msg, mars.getBlockSize()), iv);
        byte[] dec = pcbc.decrypt(enc, iv);
        byte[] out = pkcs7.unpad(dec);

        assertArrayEquals(msg, out);
    }

    @Test
    void ctr_pkcs7() {
        MARS mars = new MARS();
        mars.setKey(randomKey());

        com.project.crypto.modes.CtrMode ctr = new com.project.crypto.modes.CtrMode(mars);
        com.project.crypto.padding.Pkcs7Padding pkcs7 = new com.project.crypto.padding.Pkcs7Padding();

        byte[] iv = randomBytes(mars.getBlockSize());
        byte[] msg = randomBytes(rng.nextInt(1024));

        byte[] enc = ctr.encrypt(pkcs7.pad(msg, mars.getBlockSize()), iv);
        byte[] dec = ctr.decrypt(enc, iv);
        byte[] out = pkcs7.unpad(dec);

        assertArrayEquals(msg, out);
    }

    @Test
    void random_delta_pkcs7() {
        MARS mars = new MARS();
        mars.setKey(randomKey());

        com.project.crypto.modes.RandomDeltaMode rd = new com.project.crypto.modes.RandomDeltaMode(mars);
        com.project.crypto.padding.Pkcs7Padding pkcs7 = new com.project.crypto.padding.Pkcs7Padding();

        byte[] iv = randomBytes(mars.getBlockSize()); // IV содержит initial и delta
        byte[] msg = randomBytes(rng.nextInt(1024));

        byte[] enc = rd.encrypt(pkcs7.pad(msg, mars.getBlockSize()), iv);
        byte[] dec = rd.decrypt(enc, iv);
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

    @Test
    void diffie_hellman() {
        DhParams params = DhParams.generate(512, 64, rng);
        DiffieHellman alice = new DiffieHellman(params);
        DiffieHellman bob   = new DiffieHellman(params);

        byte[] privA = alice.generatePrivate();
        byte[] pubA  = alice.derivePublic(privA);

        byte[] privB = bob.generatePrivate();
        byte[] pubB  = bob.derivePublic(privB);

        byte[] sharedA = alice.deriveShared(privA, pubB);
        byte[] sharedB = bob.deriveShared(privB, pubA);

        assertArrayEquals(sharedA, sharedB, "DH общий ключ не совпадает");
    }

    @Test
    void magenta_encrypt_decrypt_block() {
        Magenta magenta = new Magenta();

        for (int keyLen : new int[]{16, 24, 32}) {
            byte[] key = randomBytes(keyLen);
            magenta.setKey(key);

            for (int i = 0; i < 8; i++) {
                byte[] pt = randomBytes(magenta.getBlockSize());
                byte[] ct = magenta.encryptBlock(pt);
                byte[] dec = magenta.decryptBlock(ct);

                assertArrayEquals(pt, dec, "Mismatch for keyLen=" + keyLen + ", i=" + i);
            }
        }
    }

    @Test
    void magenta_randomized_stress_test() {
        Magenta magenta = new Magenta();
        byte[] key = randomBytes(32);
        magenta.setKey(key);

        for (int i = 0; i < 1000; i++) {
            byte[] pt = randomBytes(16);
            byte[] ct = magenta.encryptBlock(pt);
            byte[] dec = magenta.decryptBlock(ct);
            assertArrayEquals(pt, dec, "decryption failed at iteration " + i);
        }
    }

    @Test
    void magenta_block_size_constant() {
        Magenta magenta = new Magenta();
        assertEquals(16, magenta.getBlockSize());
    }

    void iso10126_padding() {
        Padding pad = new ISO_10126Padding();
        byte[] data = randomBytes(23);
        int blockSize = 16;

        byte[] padded = pad.pad(data, blockSize);
        byte[] unpadded = pad.unpad(padded);

        assertEquals(0, padded.length % blockSize, "ISO10126: длина не кратна блоку");
        assertArrayEquals(data, unpadded, "ISO10126: неверная распаковка");
    }

    @Test
    void ansi_x923() {
        Padding pad = new com.project.crypto.padding.ANSI_X923Padding();
        int blockSize = 16;

        byte[] data = randomBytes(23);

        byte[] padded = pad.pad(data, blockSize);
        assertEquals(0, padded.length % blockSize, "ANSI X.923: длина не кратна размеру блока");

        int paddingLength = padded[padded.length - 1] & 0xFF;
        for (int i = padded.length - paddingLength; i < padded.length - 1; i++) {
            assertEquals(0, padded[i], "ANSI X.923: неверный байт дополнения (ожидался 0)");
        }

        byte[] unpadded = pad.unpad(padded);
        assertArrayEquals(data, unpadded);
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
