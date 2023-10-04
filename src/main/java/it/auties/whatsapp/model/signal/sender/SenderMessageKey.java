package it.auties.whatsapp.model.signal.sender;

import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Specification;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record SenderMessageKey(int iteration, byte[] seed, byte[] iv, byte[] cipherKey) {
    public SenderMessageKey(int iteration, byte[] seed) {
        this(iteration, seed, createIv(seed), createCipherKey(seed));
    }

    private static byte[] createIv(byte[] seed) {
        var derivative = getDerivedSeed(seed);
        return Arrays.copyOf(derivative[0], Specification.Signal.IV_LENGTH);
    }

    private static byte[] createCipherKey(byte[] seed) {
        var derivative = getDerivedSeed(seed);
        var data = Arrays.copyOfRange(derivative[0], Specification.Signal.IV_LENGTH, derivative[0].length);
        var concat = BytesHelper.concat(data, derivative[1]);
        return Arrays.copyOf(concat, Specification.Signal.KEY_LENGTH);
    }

    private static byte[][] getDerivedSeed(byte[] seed) {
        return Hkdf.deriveSecrets(seed, "WhisperGroup".getBytes(StandardCharsets.UTF_8));
    }
}