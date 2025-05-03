package it.auties.whatsapp.util;

import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageContainerSpec;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

// TODO: Most concat operations can be optimized in the call site
public final class Bytes {
    public static byte[] random(int length) {
        var bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    public static byte[] concat(byte[]... entries) {
        return Arrays.stream(entries)
                .filter(entry -> entry != null && entry.length != 0)
                .reduce(Bytes::concat)
                .orElseGet(() -> new byte[0]);
    }

    public static byte[] concat(byte[] first, byte[] second) {
        if (first == null || first.length == 0) {
            return second;
        }

        if (second == null || second.length == 0) {
            return first;
        }

        var result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static byte[] messageToBytes(MessageContainer container) {
        try {
            if (container.isEmpty()) {
                return null;
            }

            var messageLength = MessageContainerSpec.sizeOf(container);
            var padByte = (byte) SecureRandom.getInstanceStrong().nextInt();
            var padLength = 1 + (15 & padByte);
            var result = new byte[messageLength + padLength];
            MessageContainerSpec.encode(container, ProtobufOutputStream.toBytes(result, 0));
            Arrays.fill(result, messageLength, messageLength + padLength, (byte) padLength);
            return result;
        }catch (Throwable exception) {
            throw new RuntimeException("Cannot encode message", exception);
        }
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

    private static final char[] HEX_ALPHABET = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String randomHex(int i) {
        var result = new char[i];
        var random = new Random();
        while (i-- > 0) {
            result[i] = HEX_ALPHABET[random.nextInt(0, HEX_ALPHABET.length)];
        }
        return new String(result);
    }
}
