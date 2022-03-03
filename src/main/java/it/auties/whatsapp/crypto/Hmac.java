package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@UtilityClass
public class Hmac {
    private final String HMAC_SHA_256 = "HmacSHA256";
    private final String HMAC_SHA_512 = "HmacSHA512";

    public Bytes calculateSha256(@NonNull byte[] plain, @NonNull byte[] key) {
        return calculate(HMAC_SHA_256, plain, key);
    }

    public Bytes calculateSha512(@NonNull byte[] plain, @NonNull byte[] key) {
        return calculate(HMAC_SHA_512, plain, key);
    }

    @SneakyThrows
    private static Bytes calculate(String algorithm, byte[] plain, byte[] key)  {
        var localMac = Mac.getInstance(algorithm);
        localMac.init(new SecretKeySpec(key, algorithm));
        return Bytes.of(localMac.doFinal(plain));
    }
}
