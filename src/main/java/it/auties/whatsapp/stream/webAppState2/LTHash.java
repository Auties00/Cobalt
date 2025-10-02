package it.auties.whatsapp.stream.webAppState2;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class LTHash {
    private final byte[] hkdfInfo;
    private final int hkdfSize;
    private final KDF hkdf;

    public LTHash(byte[] hkdfInfo, int hkdfSize) {
        try {
            this.hkdfInfo = hkdfInfo;
            this.hkdfSize = hkdfSize;
            this.hkdf = KDF.getInstance("HKDF-SHA256");
        }catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }


    public void subtractThenAdd(byte[] base, byte[][] subtract, byte[][] add) throws GeneralSecurityException {
        Objects.requireNonNull(base, "base cannot be null");

        if ((base.length & 1) != 0) {
            throw new IllegalArgumentException("baseLen must be even");
        }

        if (subtract != null && subtract.length != 0) {
            multipleOp(base, subtract, true);
        }

        if (add != null && add.length != 0) {
            multipleOp(base, add, false);
        }
    }

    private void multipleOp(byte[] base, byte[][] inputs, boolean subtract) throws GeneralSecurityException {
        for (var input : inputs) {
            var spec = HKDFParameterSpec.ofExtract()
                    .addIKM(input)
                    .thenExpand(hkdfInfo, hkdfSize);
            var derived = hkdf.deriveData(spec);
            performPointwiseWithOverflow(base, derived, subtract);
        }
    }

    private static void performPointwiseWithOverflow(byte[] base, byte[] inputBuf, boolean subtract) {
        var baseLen = base.length;
        if (inputBuf.length < baseLen) {
            throw new IllegalArgumentException("input too short for baseLen");
        }

        var bo = 0;
        var io = 0;
        while (bo < baseLen) {
            int x = (base[bo] & 0xFF) | ((base[bo + 1] & 0xFF) << 8);
            int y = (inputBuf[io] & 0xFF) | ((inputBuf[io + 1] & 0xFF) << 8);
            int r = subtract ? (x - y) : (x + y);
            r &= 0xFFFF;
            base[bo] = (byte) (r & 0xFF);
            base[bo + 1] = (byte) ((r >>> 8) & 0xFF);
            bo += 2;
            io += 2;
        }
    }
}
