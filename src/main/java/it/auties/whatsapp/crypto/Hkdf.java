package it.auties.whatsapp.crypto;

import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@UtilityClass
public class Hkdf {
    private final int ITERATION_START_OFFSET = 1; // v3
    private final int HASH_OUTPUT_SIZE = 32;
    private final String HMAC_SHA_256 = "HmacSHA256";

    public byte[] deriveSecrets(byte[] inputKeyMaterial, byte[] info, int outputLength) {
        return deriveSecrets(inputKeyMaterial, new byte[HASH_OUTPUT_SIZE], info, outputLength);
    }

    public byte[] deriveSecrets(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputLength) {
        var prk = extract(salt, inputKeyMaterial);
        return expand(prk, info, outputLength);
    }

    public byte[] extract(byte[] salt, byte[] inputKeyMaterial) {
        try {
            var mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(salt, HMAC_SHA_256));
            return mac.doFinal(inputKeyMaterial);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new RuntimeException("Cannot hkdf extract", exception);
        }
    }

    public byte[] expand(byte[] prk, byte[] info, int outputSize) {
        try {
            var iterations = (int) Math.ceil((double) outputSize / (double) HASH_OUTPUT_SIZE);
            var mixin = new byte[0];
            var results = new ByteArrayOutputStream();
            for (var index = ITERATION_START_OFFSET; index < iterations + ITERATION_START_OFFSET; index++) {
                var mac = Mac.getInstance(HMAC_SHA_256);
                mac.init(new SecretKeySpec(prk, HMAC_SHA_256));
                mac.update(mixin);
                if (info != null) mac.update(info);
                mac.update((byte) index);

                var stepResult = mac.doFinal();
                var stepSize = Math.min(outputSize, stepResult.length);
                results.write(stepResult, 0, stepSize);

                mixin = stepResult;
                outputSize -= stepSize;
            }

            return results.toByteArray();
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new RuntimeException("Cannot hkdf expand", exception);
        }
    }
}
