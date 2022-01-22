package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@UtilityClass
public class Hmac {
    private final String HMAC_SHA_256 = "HmacSHA256";
    private final String HMAC_SHA_512 = "HmacSHA512";

    public BinaryArray calculateSha256(@NonNull byte[] plain, @NonNull byte[] key) {
        return calculate(HMAC_SHA_256, plain, key);
    }

    public BinaryArray calculateSha512(@NonNull byte[] plain, @NonNull byte[] key) {
        return calculate(HMAC_SHA_512, plain, key);
    }

    @SneakyThrows
    private static BinaryArray calculate(String algorithm, byte[] plain, byte[] key)  {
        var localMac = Mac.getInstance(algorithm);
        localMac.init(new SecretKeySpec(key, algorithm));
        return BinaryArray.of(localMac.doFinal(plain));
    }
}
