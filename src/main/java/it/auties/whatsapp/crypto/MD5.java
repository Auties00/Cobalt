package it.auties.whatsapp.crypto;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MD5 {
    private static final String MD5 = "MD5";

    public static byte[] calculate(String data) {
        return calculate(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] calculate(byte[] data) {
        try {
            var digest = MessageDigest.getInstance(MD5);
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing md5 implementation", exception);
        }
    }
}
