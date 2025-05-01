package it.auties.whatsapp.crypto;

import it.auties.whatsapp.util.Bytes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static it.auties.whatsapp.util.SignalConstants.KEY_LENGTH;

public final class Hkdf {
    private static final int ITERATION_START_OFFSET = 1; // v3
    private static final int HASH_OUTPUT_SIZE = 32;
    private static final byte[] DEFAULT_SALT = new byte[HASH_OUTPUT_SIZE];
    private static final String HMAC_SHA_256 = "HmacSHA256";

    public static byte[][] deriveSecrets(byte[] input, byte[] info) {
        return deriveSecrets(input, info, 3);
    }

    public static byte[][] deriveSecrets(byte[] input, byte[] info, int chunks) {
        return deriveSecrets(input, DEFAULT_SALT, info, chunks);
    }

    public static byte[][] deriveSecrets(byte[] input, byte[] salt, byte[] info, int chunks) {
        if (salt.length != KEY_LENGTH) {
            throw new IllegalArgumentException("Incorrect salt codeLength: %s".formatted(salt.length));
        }

        if (chunks < 1 || chunks > 3) {
            throw new IllegalArgumentException("Incorrect number of chunks: %s".formatted(chunks));
        }

        var prk = Hmac.calculateSha256(input, salt);
        var result = Bytes.concat(new byte[KEY_LENGTH], info, new byte[]{1});
        var signed = new byte[chunks][];
        var key = Arrays.copyOfRange(result, KEY_LENGTH, result.length);
        var first = Hmac.calculateSha256(key, prk);
        signed[0] = first;
        if (chunks > 1) {
            System.arraycopy(first, 0, result, 0, first.length);
            result[result.length - 1] = 2;
            signed[1] = Hmac.calculateSha256(result, prk);
        }
        if (chunks > 2) {
            System.arraycopy(signed[1], 0, result, 0, signed[1].length);
            result[result.length - 1] = 3;
            signed[2] = Hmac.calculateSha256(result, prk);
        }
        return signed;
    }

    public static byte[][] deriveSecrets(byte[] input, byte[] salt, byte[] info) {
        return deriveSecrets(input, salt, info, 3);
    }

    public static byte[] extractAndExpand(byte[] key, byte[] info, int outputLength) {
        return extractAndExpand(key, DEFAULT_SALT, info, outputLength);
    }

    public static byte[] extractAndExpand(byte[] key, byte[] salt, byte[] info, int outputLength) {
        return expand(Hmac.calculateSha256(key, salt), info, outputLength);
    }

    private static byte[] expand(byte[] prk, byte[] info, int outputSize) {
        try {
            var iterations = (int) Math.ceil((double) outputSize / (double) HASH_OUTPUT_SIZE);
            var mixin = new byte[0];
            var results = new ByteArrayOutputStream();
            for (var index = ITERATION_START_OFFSET; index < iterations + ITERATION_START_OFFSET; index++) {
                var mac = Mac.getInstance(HMAC_SHA_256);
                mac.init(new SecretKeySpec(prk, HMAC_SHA_256));
                mac.update(mixin);
                if (info != null) {
                    mac.update(info);
                }
                mac.update((byte) index);
                var stepResult = mac.doFinal();
                var stepSize = Math.min(outputSize, stepResult.length);
                results.write(stepResult, 0, stepSize);
                mixin = stepResult;
                outputSize -= stepSize;
            }
            return results.toByteArray();
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot expand data", exception);
        }
    }
}
