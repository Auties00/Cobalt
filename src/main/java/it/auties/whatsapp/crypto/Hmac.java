package it.auties.whatsapp.crypto;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@UtilityClass
public class Hmac {
    private final String HMAC_SHA_256 = "HmacSHA256";
    private final String HMAC_SHA_512 = "HmacSHA512";

    public byte[] calculateSha256(byte @NonNull [] plain, byte @NonNull [] key) {
        return calculate(HMAC_SHA_256, plain, key);
    }

    public byte[] calculateSha512(byte @NonNull [] plain, byte @NonNull [] key) {
        return calculate(HMAC_SHA_512, plain, key);
    }

    @SneakyThrows
    private byte[] calculate(String algorithm, byte[] plain, byte[] key)  {
        var localMac = Mac.getInstance(algorithm);
        localMac.init(new SecretKeySpec(key, algorithm));
        return localMac.doFinal(plain);
    }
}
