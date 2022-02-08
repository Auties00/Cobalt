package it.auties.whatsapp.crypto;

import it.auties.whatsapp.util.Validate;
import it.auties.whatsapp.util.VersionProvider;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.Inflater;

/**
 * This utility class provides helper functionality to easily encrypt and decrypt data
 * This class should only be used for WhatsappWeb's WebSocket buffer operations
 */
@UtilityClass
public class SignalHelper implements VersionProvider {
    private final String SHA_PRNG = "SHA1PRNG";

    public byte[] appendKeyHeader(byte[] key){
        if(key == null){
            return null;
        }

        return switch (key.length){
            case 33 -> key;
            case 32 -> writeKeyHeader(key);
            default -> throw new IllegalArgumentException("Invalid key size: %s".formatted(key.length));
        };
    }

    private byte[] writeKeyHeader(byte[] key) {
        Validate.isTrue(key.length == 32,
                "Invalid key size: %s", key.length);
        var result = new byte[33];
        System.arraycopy(key, 0, result, 1, key.length);
        result[0] = 5;
        return result;
    }

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
    public int randomHeader() {
        var key = new byte[1];
        var secureRandom = SecureRandom.getInstance(SHA_PRNG);
        secureRandom.nextBytes(key);
        return 1 + (15 & key[0]);
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

    public byte serialize(int version){
        return (byte) (version << 4 | CURRENT_VERSION);
    }

    public int deserialize(byte version){
        return Byte.toUnsignedInt(version) >> 4;
    }

    @SneakyThrows
    public byte[] deflate(byte[] compressed){
        var decompressor = new Inflater();
        decompressor.setInput(compressed);
        var result = new ByteArrayOutputStream();
        var buffer = new byte[1024];
        while (!decompressor.finished()) {
            var count = decompressor.inflate(buffer);
            result.write(buffer, 0, count);
        }

        return result.toByteArray();
    }

    public byte[] toBytes(long number){
        return ByteBuffer.allocate(Long.BYTES)
                .putLong(number)
                .array();
    }

    public byte[] toBytes(int input, int length) {
        var result = new byte[length];
        for(var i = length - 1; i >= 0; i--){
            result[i] = (byte) (255 & input);
            input >>>= 8;
        }

        return result;
    }
}