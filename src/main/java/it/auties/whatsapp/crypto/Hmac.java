package it.auties.whatsapp.crypto;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public final class Hmac {
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String HMAC_SHA_512 = "HmacSHA512";

    public static byte[] calculateSha256(byte[] plain, byte[] key) {
        return calculate(HMAC_SHA_256, plain, key);
    }

    public static byte[] calculateSha512(byte[] plain, byte[] key) {
        return calculate(HMAC_SHA_512, plain, key);
    }

    private static byte[] calculate(String algorithm, byte[] plain, byte[] key) {
        try {
            var localMac = Mac.getInstance(algorithm);
            localMac.init(new SecretKeySpec(key, algorithm));
            return localMac.doFinal(plain);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot calculate hmac", exception);
        }
    }
}
