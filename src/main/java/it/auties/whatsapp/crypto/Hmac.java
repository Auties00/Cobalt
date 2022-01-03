package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@UtilityClass
public class Hmac {
    private final String HMAC_SHA256 = "HmacSHA256";

    @SneakyThrows
    public BinaryArray calculate(@NonNull byte[] plain, @NonNull byte[] key) {
        var localMac = Mac.getInstance(HMAC_SHA256);
        localMac.init(new SecretKeySpec(key, HMAC_SHA256));
        return BinaryArray.of(localMac.doFinal(plain));
    }
}
