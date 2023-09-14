package it.auties.whatsapp.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class KeyHelper {
    private static final String SHA_PRNG = "SHA1PRNG";

    public static byte[] withHeader(byte[] key) {
        if (key == null) {
            return null;
        }
        return switch (key.length) {
            case 33 -> key;
            case 32 -> writeKeyHeader(key);
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    private static byte[] writeKeyHeader(byte[] key) {
        Validate.isTrue(key.length == 32, "Invalid key size: %s", key.length);
        var result = new byte[33];
        System.arraycopy(key, 0, result, 1, key.length);
        result[0] = 5;
        return result;
    }

    public static byte[] withoutHeader(byte[] key) {
        if (key == null) {
            return null;
        }
        return switch (key.length) {
            case 32 -> key;
            case 33 -> Arrays.copyOfRange(key, 1, key.length);
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    public static int header() {
        try {
            var key = new byte[1];
            var secureRandom = SecureRandom.getInstance(SHA_PRNG);
            secureRandom.nextBytes(key);
            return 1 + (15 & key[0]);
        }catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing ShaPRNG implementation");
        }
    }

    public static int registrationId() {
        try {
            var secureRandom = SecureRandom.getInstance(SHA_PRNG);
            return secureRandom.nextInt(16380) + 1;
        }catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing ShaPRNG implementation");
        }
    }

    public static String identityId() {
        return HexFormat.of().formatHex(BytesHelper.random(20));
    }

    public static String deviceId() {
        return Base64.getUrlEncoder().encodeToString(BytesHelper.random(16));
    }

    public static String phoneId() {
        return UUID.randomUUID().toString();
    }

    public static byte[] senderKey() {
        try{
            var key = new byte[32];
            var secureRandom = SecureRandom.getInstance(SHA_PRNG);
            secureRandom.nextBytes(key);
            return key;
        }catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing ShaPRNG implementation");
        }
    }

    public static int senderKeyId() {
        try{
            var secureRandom = SecureRandom.getInstance(SHA_PRNG);
            return secureRandom.nextInt(0, 2147483647);
        }catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing ShaPRNG implementation");
        }
    }

    public static int agent() {
        return ThreadLocalRandom.current().nextInt(800_000_000, 900_000_000);
    }

    public static byte[] appKeyId() {
        return BytesHelper.intToBytes(ThreadLocalRandom.current().nextInt(19000, 20000), 6);
    }
}