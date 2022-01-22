package it.auties.whatsapp.crypto;

import it.auties.whatsapp.util.Validate;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static java.util.Arrays.copyOfRange;

@UtilityClass
public class Hkdf {
    private final int ITERATION_START_OFFSET = 1; // v3
    private final int HASH_OUTPUT_SIZE = 32;
    private final String HMAC_SHA_256 = "HmacSHA256";

    public byte[][] deriveSecrets(byte[] inputKeyMaterial, byte[] info) {
        return deriveSecrets(inputKeyMaterial, info, 3);
    }

    public byte[][] deriveSecrets(byte[] inputKeyMaterial, byte[] info, int chunks) {
        return deriveSecrets(inputKeyMaterial, new byte[HASH_OUTPUT_SIZE], info, chunks);
    }

    public byte[][] deriveSecrets(byte[] inputKeyMaterial, byte[] salt, byte[] info) {
        return deriveSecrets(inputKeyMaterial, salt, info, 3);
    }

    @SneakyThrows
    public byte[][] deriveSecrets(byte[] input, byte[] salt, byte[] info, int chunks) {
        Validate.isTrue(salt.length == 32,
                "Incorrect salt length: %s", salt.length);
        Validate.isTrue(chunks >= 1 && chunks <= 3,
                "Incorrect number of chunks: %s", chunks);

       var prk = Hmac.calculateSha256(input, salt);
       var resultStream = new ByteArrayOutputStream();
        resultStream.write(new byte[32]);
        resultStream.write(info);
        resultStream.write(1);
        var result = resultStream.toByteArray();

       var signed = new ArrayList<byte[]>();
       var first = Hmac.calculateSha256(copyOfRange(result, 32, result.length), prk.data());
       signed.add(first.data());

        if (chunks > 1) {
            var source = signed.get(0);
            System.arraycopy(source, 0, result, 0, source.length);
            result[result.length - 1] = 2;
            var second = Hmac.calculateSha256(result, prk.data());
            signed.add(second.data());
        }

        if (chunks > 2) {
            var source = signed.get(1);
            System.arraycopy(source, 0, result, 0, source.length);
            result[result.length - 1] = 3;
            var third = Hmac.calculateSha256(result, prk.data());
            signed.add(third.data());
        }

        return signed.toArray(byte[][]::new);
    }

    public byte[] extractAndExpand(byte[] inputKeyMaterial, byte[] info, int outputLength) {
        return extractAndExpand(inputKeyMaterial, new byte[HASH_OUTPUT_SIZE], info, outputLength);
    }

    public byte[] extractAndExpand(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputLength) {
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
