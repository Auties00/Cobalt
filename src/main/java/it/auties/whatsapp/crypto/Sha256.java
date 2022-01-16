package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.security.MessageDigest;

@UtilityClass
public class Sha256 {
    private final String SHA_256 = "SHA-256";

    @SneakyThrows
    public BinaryArray calculate(@NonNull byte[] data) {
        var digest = MessageDigest.getInstance(SHA_256);
        digest.update(data);
        return BinaryArray.of(digest.digest());
    }
}
