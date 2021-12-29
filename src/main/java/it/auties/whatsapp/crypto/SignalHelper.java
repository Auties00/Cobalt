package it.auties.whatsapp.crypto;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * This utility class provides helper functionality to easily encrypt and decrypt data
 * This class should only be used for WhatsappWeb's WebSocket buffer operations
 */
@UtilityClass
public class SignalHelper {
    private final String SHA_PRNG = "SHA1PRNG";

    public byte[] removeKeyHeader(byte[] key) {
        if(key == null){
            return null;
        }

        return switch (key.length){
            case 32 -> key;
            case 33 -> Arrays.copyOfRange(key, 1, key.length);
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    @SneakyThrows
    public int randomRegistrationId() {
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        return secureRandom.nextInt(16380) + 1;
    }

    @SneakyThrows
    public byte[] randomSenderKey() {
        var key = new byte[32];
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        secureRandom.nextBytes(key);
        return key;
    }

    @SneakyThrows
    public int randomSenderKeyId() {
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        return secureRandom.nextInt(2147483647);
    }
}