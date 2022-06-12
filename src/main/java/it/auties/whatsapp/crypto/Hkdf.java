package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.util.SignalSpecification;
import it.auties.whatsapp.util.Validate;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;

@UtilityClass
public class Hkdf implements SignalSpecification {
    private final int ITERATION_START_OFFSET = 1; // v3
    private final int HASH_OUTPUT_SIZE = 32;
    private final String HMAC_SHA_256 = "HmacSHA256";

    public byte[][] deriveSecrets(byte[] input, byte[] info) {
        return deriveSecrets(input, info, 3);
    }

    public byte[][] deriveSecrets(byte[] input, byte[] info, int chunks) {
        return deriveSecrets(input, new byte[HASH_OUTPUT_SIZE], info, chunks);
    }

    public byte[][] deriveSecrets(byte[] input, byte[] salt, byte[] info) {
        return deriveSecrets(input, salt, info, 3);
    }

    @SneakyThrows
    public byte[][] deriveSecrets(byte[] input, byte[] salt, byte[] info, int chunks) {
        Validate.isTrue(salt.length == KEY_LENGTH, "Incorrect salt length: %s", salt.length);
        Validate.isTrue(chunks >= 1 && chunks <= 3, "Incorrect number of chunks: %s", chunks);

        var prk = Hmac.calculateSha256(input, salt);
        var result = Bytes.newBuffer(KEY_LENGTH)
                .append(info)
                .append(1)
                .toByteArray();

        var signed = new byte[chunks][];
        var key = Bytes.of(result)
                .slice(KEY_LENGTH)
                .toByteArray();
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

    public byte[] extractAndExpand(byte[] inputKeyMaterial, byte[] info, int outputLength) {
        return extractAndExpand(inputKeyMaterial, new byte[HASH_OUTPUT_SIZE], info, outputLength);
    }

    public byte[] extractAndExpand(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputLength) {
        var prk = extract(salt, inputKeyMaterial);
        return expand(prk, info, outputLength);
    }

    @SneakyThrows
    private byte[] extract(byte[] salt, byte[] inputKeyMaterial) {
        var mac = Mac.getInstance(HMAC_SHA_256);
        mac.init(new SecretKeySpec(salt, HMAC_SHA_256));
        return mac.doFinal(inputKeyMaterial);
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
            if (info != null)
                mac.update(info);
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
