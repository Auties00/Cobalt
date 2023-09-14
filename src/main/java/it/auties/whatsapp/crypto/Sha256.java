package it.auties.whatsapp.crypto;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256 {
    private static final String SHA_256 = "SHA-256";

    public static byte[] calculate(@NonNull String data) {
        return calculate(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] calculate(byte @NonNull [] data) {
        try {
            var digest = MessageDigest.getInstance(SHA_256);
            digest.update(data);
            return digest.digest();
        }catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing sha256 implementation");
        }
    }
}
