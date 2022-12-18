package it.auties.whatsapp.crypto;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@UtilityClass
public class Sha256 {
    private final String SHA_256 = "SHA-256";

    public byte[] calculate(@NonNull String data) {
        return calculate(data.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public byte[] calculate(byte @NonNull [] data) {
        var digest = MessageDigest.getInstance(SHA_256);
        digest.update(data);
        return digest.digest();
    }
}
