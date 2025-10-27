package com.github.auties00.cobalt.io.media.stream;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

public abstract class MediaInputStream extends InputStream {
    protected static final int BUFFER_LENGTH = 8192;
    protected static final int MAC_LENGTH = 10;
    protected static final int EXPANDED_SIZE = 112;
    protected static final int KEY_LENGTH = 32;
    protected static final int IV_LENGTH = 16;

    protected final InputStream rawInputStream;
    protected MediaInputStream(InputStream rawInputStream) {
        this.rawInputStream = rawInputStream;
    }

    protected byte[] deriveMediaKeyData(byte[] mediaKey, String mediaKeyName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var hkdf = KDF.getInstance("HKDF-SHA256");
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(new SecretKeySpec(mediaKey, "AES"))
                .thenExpand(mediaKeyName.getBytes(), EXPANDED_SIZE);
        return hkdf.deriveData(params);
    }
}
