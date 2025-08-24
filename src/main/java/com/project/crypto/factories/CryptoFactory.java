package com.project.crypto.factories;

import com.project.crypto.ciphers.Algorithm;
import com.project.crypto.ciphers.MARS;
import com.project.crypto.modes.CbcMode;
import com.project.crypto.modes.CipherMode;
import com.project.crypto.padding.Padding;
import com.project.crypto.padding.Pkcs7Padding;

public class CryptoFactory {
    public final AlgorithmType algorithm;
    public final ModeType mode;
    public final PaddingType padding;

    private CryptoFactory(Builder b) {
        this.algorithm = b.algorithm;
        this.mode = b.mode;
        this.padding = b.padding;
    }

    public static class Builder {
        private AlgorithmType algorithm;
        private ModeType mode;
        private PaddingType padding;
        private byte[] key;

        public Builder algorithm(AlgorithmType a) {
            this.algorithm = a;
            return this;
        }
        public Builder mode(ModeType m) {
            this.mode = m;
            return this;
        }
        public Builder padding(PaddingType p) {
            this.padding = p;
            return this;
        }
        public Builder key(byte[] k) {
            this.key = k;
            return this;
        }

        public CryptoFactory build() {
            return new CryptoFactory(this);
        }

        public CryptoSuite buildSuite() {

            Algorithm alg = switch (algorithm) {
                case MARS -> new MARS();
            };

            alg.setKey(key);

            CipherMode cmode = switch (mode) {
                case CBC -> new CbcMode(alg);
            };

            Padding pad = switch (padding) {
                case PKCS7 -> new Pkcs7Padding();
            };

            return new CryptoSuite(alg, cmode, pad);
        }
    }
}
