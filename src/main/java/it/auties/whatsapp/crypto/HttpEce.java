package it.auties.whatsapp.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

// TODO: Implement
public class HttpEce {
    private static final int AUTH_TAG_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BYTES = 16;
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int SHA_256_LENGTH_BYTES = 32;

    public static byte[] decrypt(byte[] buffer, byte[] salt, byte[] publicKey, byte[] privateKey, byte[] dh, byte[] authSecret) {
        var keyAndNonce = deriveKeyAndNonce(salt, publicKey, privateKey, dh, authSecret);
        int start = 0;
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        for (int i = 0; start < buffer.length; ++i) {
            int end = start + 4096 + AUTH_TAG_LENGTH_BYTES;
            if (end == buffer.length) {
                throw new IllegalArgumentException("Truncated payload");
            }
            end = Math.min(end, buffer.length);
            if (end - start <= AUTH_TAG_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid block: too small at " + i);
            }

            byte[] record = Arrays.copyOfRange(buffer, start, end);
            try {
                result.write(decryptRecord(keyAndNonce.key, keyAndNonce.nonce, i, record));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            start = end;
        }
        return result.toByteArray();
    }

    private static byte[] decryptRecord(byte[] key, byte[] nonceBase, int counter, byte[] buffer) {
        byte[] nonce = generateNonce(nonceBase, counter);

        byte[] data = AesGcm.decrypt(nonce, buffer, key, null);


        int pad = (int) readLongBE(data, 0, 2); // This cast is OK because the padding is 65537 max
        if (pad + 2 > data.length) {
            throw new IllegalArgumentException("padding exceeds block size");
        }
        for (int i = 2; i < 2 + pad; i++) {
            if (data[i] != 0) {
                throw new IllegalArgumentException("invalid padding");
            }
        }
        return Arrays.copyOfRange(data, 2 + pad, data.length);
    }

    private static KeyAndNonce deriveKeyAndNonce(byte[] salt, byte[] publicKey, byte[] privateKey, byte[] dh, byte[] authSecret) {
        SecretAndContext sc = generateSecretAndContext(publicKey, privateKey, dh);
        if (sc.secret == null) {
            throw new IllegalStateException("Unable to determine the secret");
        }

        sc.expandSecret(authSecret);

        var keyInfo = buildInfo("aesgcm", sc.context);
        var nonceInfo = buildInfo("nonce", sc.context);

        byte[] hkdfKey = hkdfExpand(sc.secret, salt, keyInfo, KEY_LENGTH_BYTES);
        byte[] hkdfNonce = hkdfExpand(sc.secret, salt, nonceInfo, NONCE_LENGTH_BYTES);

        return new KeyAndNonce(hkdfKey, hkdfNonce);
    }

    private record KeyAndNonce(byte[] key, byte[] nonce) {

    }

    // TODO: Implement
    private static SecretAndContext generateSecretAndContext(byte[] publicKey, byte[] privateKey, byte[] otherPublicKey)  {
       throw new UnsupportedOperationException();
    }

    private static byte[] hkdfExpand(byte[] ikm, byte[] salt, byte[] info, int length) {
        return Hkdf.extractAndExpand(ikm, salt, info, length);
    }

    private static byte[] generateNonce(byte[] base, int counter) {
        byte[] nonce = base.clone();
        long m = readLongBE(nonce, base.length - 6, 6);
        long x = ((m ^ counter) & 0xffffff) +
                ((((m / 0x1000000) ^ (counter / 0x1000000)) & 0xffffff) * 0x1000000);
        writeLongBE(nonce, x, base.length - 6, 6);
        return nonce;
    }

    private static byte[] buildInfo(String type, byte[] context) {
        ByteBuffer buffer = ByteBuffer.allocate(19 + type.length() + context.length);
        buffer.put("Content-Encoding: ".getBytes(), 0, 18);
        buffer.put(type.getBytes(), 0, type.length());
        buffer.put(new byte[1], 0, 1);
        buffer.put(context, 0, context.length);
        return buffer.array();
    }

    private static byte[] lengthPrefix(byte[] key) {
        byte[] b = concat(new byte[2], key);
        writeLongBE(b, key.length, 0, 2);
        return b;
    }

    private static byte[] concat(byte[]... arrays) {
        int combinedLength = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                combinedLength += array.length;
            }
        }
        int lastPos = 0;
        byte[] combined = new byte[combinedLength];

        for (byte[] array : arrays) {
            if (array == null) {
                continue;
            }
            System.arraycopy(array, 0, combined, lastPos, array.length);
            lastPos += array.length;
        }

        return combined;
    }

    private static long readLongBE(byte[] buffer, int offset, int len) {
        long result = 0;
        for (int i = 0; i < len; i++) {
            result <<= 8;
            result |= (buffer[i + offset] & 0xFF);
        }
        return result;
    }

    private static void writeLongBE(byte[] buffer, long value, int offset, int len) {
        for (int i = len - 1; i >= 0; i--) {
            buffer[i + offset] = (byte) (value & 0xFF);
            value >>= 8;
        }
    }

    private static class SecretAndContext {
        private byte[] secret;
        private byte[] context;

        public SecretAndContext(byte[] secret, byte[] context) {
            this.secret = secret;
            this.context = context;
        }

        public SecretAndContext(byte[] secret) {
            this(secret, new byte[0]);
        }

        public void expandSecret(byte[] authSecret) {
            this.secret = hkdfExpand(this.secret, authSecret, buildInfo("auth", new byte[0]), SHA_256_LENGTH_BYTES);
        }
    }
}