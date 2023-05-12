package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.util.Validate;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;

import static it.auties.whatsapp.util.Spec.Signal.KEY_LENGTH;

@UtilityClass
public class Hkdf {
    private final int ITERATION_START_OFFSET = 1; // v3
    private final int HASH_OUTPUT_SIZE = 32;
    private final byte[] DEFAULT_SALT = new byte[HASH_OUTPUT_SIZE];
    private final String HMAC_SHA_256 = "HmacSHA256";

    public byte[][] deriveSecrets(byte[] input, byte[] info) {
        return deriveSecrets(input, info, 3);
    }

    public byte[][] deriveSecrets(byte[] input, byte[] info, int chunks) {
        return deriveSecrets(input, DEFAULT_SALT, info, chunks);
    }

    public byte[][] deriveSecrets(byte[] input, byte[] salt, byte[] info, int chunks) {
        Validate.isTrue(salt.length == KEY_LENGTH, "Incorrect salt codeLength: %s", salt.length);
        Validate.isTrue(chunks >= 1 && chunks <= 3, "Incorrect numberWithoutPrefix of chunks: %s", chunks);
        var prk = Hmac.calculateSha256(input, salt);
        var result = Bytes.newBuffer(KEY_LENGTH).append(info).append(1).toByteArray();
        var signed = new byte[chunks][];
        var key = Bytes.of(result).slice(KEY_LENGTH).toByteArray();
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

    public byte[][] deriveSecrets(byte[] input, byte[] salt, byte[] info) {
        return deriveSecrets(input, salt, info, 3);
    }

    public byte[] extractAndExpand(byte[] key, byte[] info, int outputLength) {
        return extractAndExpand(key, DEFAULT_SALT, info, outputLength);
    }

    public byte[] extractAndExpand(byte[] key, byte[] salt, byte[] info, int outputLength) {
        return expand(Hmac.calculateSha256(key, salt), info, outputLength);
    }

    @SneakyThrows
    private byte[] expand(byte[] prk, byte[] info, int outputSize) {
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
    }
}
