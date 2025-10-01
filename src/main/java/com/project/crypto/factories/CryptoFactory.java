package com.project.crypto.factories;

import com.project.crypto.ciphers.Algorithm;
import com.project.crypto.ciphers.MARS;
import com.project.crypto.ciphers.Magenta;
import com.project.crypto.modes.*;
import com.project.crypto.padding.*;

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
                case MAGENTA -> new Magenta();
            };

            alg.setKey(key);

            CipherMode cmode = switch (mode) {
                case CBC -> new CbcMode(alg);
                case CFB -> new CfbMode(alg);
                case ECB -> new EcbMode(alg);
                case OFB -> new OfbMode(alg);
                case PCBC -> new PcbcMode(alg);
                case CTR -> new CtrMode(alg);
                case Random_Delta -> new RandomDeltaMode(alg);
            };

            Padding pad = switch (padding) {
                case PKCS7 -> new Pkcs7Padding();
                case ISO_10126 -> new ISO_10126Padding();
                case ANSI_X923 -> new ANSI_X923Padding();
                case Zeros -> new ZerosPadding();
            };

            return new CryptoSuite(alg, cmode, pad);
        }
    }
}
