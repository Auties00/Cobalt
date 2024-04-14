package it.auties.whatsapp.crypto;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256 {
    private static final String SHA_256 = "SHA-256";

    public static byte[] calculate(String data) {
        return calculate(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] calculate(byte[] data) {
        return calculate(data, 0, data.length);
    }

    public static byte[] calculate(byte[] data, int offset, int length) {
        try {
            var digest = MessageDigest.getInstance(SHA_256);
            digest.update(data, offset, length);
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing sha256 implementation");
        }
    }
}
