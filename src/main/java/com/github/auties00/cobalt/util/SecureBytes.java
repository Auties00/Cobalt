package com.github.auties00.cobalt.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public final class SecureBytes {
    private static final SecureRandom RANDOM;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final char[] HEX_ALPHABET = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    static {
        try {
            RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private SecureBytes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static byte[] random(int from, int to) {
        if(from < 0) {
            throw new IllegalArgumentException("From cannot be negative: " + from);
        }

        if(from > to) {
            throw new IllegalArgumentException("From cannot be greater than to: " + from + " > " + to);
        }

        var bytes = new byte[to - from];
        RANDOM.nextBytes(bytes);
        System.arraycopy(bytes, 0, bytes, from, bytes.length);
        return bytes;
    }

    public static byte[] random(int length) {
        if(length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }

        if(length == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        var bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static void random(byte[] bytes, int offset, int length) {
        if(length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }

        var payload = new byte[length];
        RANDOM.nextBytes(payload);
        System.arraycopy(payload, 0, bytes, offset, length);
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
        while (i-- > 0) {
            var index = RANDOM.nextInt(0, HEX_ALPHABET.length);
            result[i] = HEX_ALPHABET[index];
        }
        return new String(result);
    }

    public static byte[] intToBytes(int input, int length) {
        var result = new byte[length];
        for (var i = length - 1; i >= 0; i--) {
            result[i] = (byte) (255 & input);
            input >>>= 8;
        }
        return result;
    }

    public static int bytesToInt(byte[] bytes, int length) {
        var result = 0;
        for (var i = 0; i < length; i++) {
            result = 256 * result + Byte.toUnsignedInt(bytes[i]);
        }
        return result;
    }

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static String randomSid() {
        return Clock.nowSeconds()
               + "-" + ThreadLocalRandom.current().nextLong(1_000_000_000, 9_999_999_999L)
               + "-" + ThreadLocalRandom.current().nextInt(0, 1000);
    }
}
