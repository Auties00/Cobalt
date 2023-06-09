package it.auties.whatsapp.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class KeyHelper {
    private final String SHA_PRNG = "SHA1PRNG";

    public byte[] withHeader(byte[] key) {
        if (key == null) {
            return null;
        }
        return switch (key.length) {
            case 33 -> key;
            case 32 -> writeKeyHeader(key);
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    private byte[] writeKeyHeader(byte[] key) {
        Validate.isTrue(key.length == 32, "Invalid key size: %s", key.length);
        var result = new byte[33];
        System.arraycopy(key, 0, result, 1, key.length);
        result[0] = 5;
        return result;
    }

    public byte[] withoutHeader(byte[] key) {
        if (key == null) {
            return null;
        }
        return switch (key.length) {
            case 32 -> key;
            case 33 -> Arrays.copyOfRange(key, 1, key.length);
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    @SneakyThrows
    public int header() {
        var key = new byte[1];
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        secureRandom.nextBytes(key);
        return 1 + (15 & key[0]);
    }

    @SneakyThrows
    public int registrationId() {
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        return secureRandom.nextInt(16380) + 1;
    }

    public String identityId() {
        return HexFormat.of().formatHex(BytesHelper.random(20));
    }

    public String deviceId() {
        return Base64.getUrlEncoder().encodeToString(BytesHelper.random(16));
    }

    public String phoneId() {
        return UUID.randomUUID().toString();
    }

    @SneakyThrows
    public byte[] senderKey() {
        var key = new byte[32];
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        secureRandom.nextBytes(key);
        return key;
    }

    @SneakyThrows
    public int senderKeyId() {
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        return secureRandom.nextInt(0, 2147483647);
    }

    public int agent() {
        return ThreadLocalRandom.current().nextInt(800_000_000, 900_000_000);
    }

    public byte[] appKeyId() {
        return BytesHelper.intToBytes(ThreadLocalRandom.current().nextInt(19000, 20000), 6);
    }
}