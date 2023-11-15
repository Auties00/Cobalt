package it.auties.whatsapp.crypto;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha1 {
    private static final String SHA_1 = "SHA-1";

    public static byte[] calculate(String data) {
        return calculate(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] calculate(byte[] data) {
        try {
            var digest = MessageDigest.getInstance(SHA_1);
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing sha1 implementation");
        }
    }
}
