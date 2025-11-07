package com.github.auties00.cobalt.media;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

abstract class MediaInputStream extends InputStream {
    static final int BUFFER_LENGTH = 8192;
    static final int MAC_LENGTH = 10;
    static final int EXPANDED_SIZE = 112;
    static final int KEY_LENGTH = 32;
    static final int IV_LENGTH = 16;

    final InputStream rawInputStream;
    boolean finished;
    MediaInputStream(InputStream rawInputStream) {
        this.rawInputStream = Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
    }

    byte[] deriveMediaKeyData(byte[] mediaKey, String mediaKeyName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var hkdf = KDF.getInstance("HKDF-SHA256");
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(new SecretKeySpec(mediaKey, "AES"))
                .thenExpand(mediaKeyName.getBytes(), EXPANDED_SIZE);
        return hkdf.deriveData(params);
    }

    @Override
    public void close() throws IOException {
        rawInputStream.close();
    }
}
