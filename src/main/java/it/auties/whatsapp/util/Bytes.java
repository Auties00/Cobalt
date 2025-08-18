package it.auties.whatsapp.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public final class Bytes {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final char[] HEX_ALPHABET = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private Bytes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static byte[] random(int length) {
        if(length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }

        if(length == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        try {
            var bytes = new byte[length];
            SecureRandom.getInstanceStrong()
                    .nextBytes(bytes);
            return bytes;
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Cannot generate random bytes", exception);
        }
    }

    public static byte[] concat(byte[]... entries) {
        if(entries == null) {
            return EMPTY_BYTE_ARRAY;
        }

        var length = 0;
        for(var entry : entries) {
            if(entry != null) {
                length += entry.length;
            }
        }
        if(length == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        var result = new byte[length];
        var offset = 0;
        for(var entry : entries) {
            if(entry != null) {
                System.arraycopy(entry, 0, result, offset, entry.length);
                offset += entry.length;
            }
        }
        return result;
    }

    public static String randomHex(int i) {
        var result = new char[i];
        var random = new Random();
        while (i-- > 0) {
            result[i] = HEX_ALPHABET[random.nextInt(0, HEX_ALPHABET.length)];
        }
        return new String(result);
    }
}
