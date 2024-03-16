package it.auties.whatsapp.crypto;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.util.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class HttpEce {
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 16;
    private static final int SECRET_LENGTH = 32;
    private static final byte[] AUTH_INFO = "Content-Encoding: auth".getBytes();
    private static final byte[] AES_GCM_INFO = "Content-Encoding: aesgcm".getBytes();
    private static final byte[] IV_INFO = "Content-Encoding: nonce".getBytes();
    private static final int RECORD_SIZE = 4096;

    public static byte[] decrypt(byte[] content, byte[] dh, byte[] publicKey, byte[] privateKey, byte[] salt, byte[] authSecret) {
        var publicKeyLength = publicKey.length;
        var publicKeyLengthBuffer = Bytes.unsignedShortToBytes(publicKeyLength);

        var dhLength = dh.length;
        var dhLengthBuffer = Bytes.unsignedShortToBytes(dhLength);

        var context = Bytes.concat(
                new byte[1],
                publicKeyLengthBuffer,
                publicKey,
                dhLengthBuffer,
                dh
        );

        // TODO: ECDH shared key, curve25519 uses ECDH under the hood so it should be possible to use this
        var sharedKey = Curve25519.sharedKey(publicKey, privateKey);
        var sharedSecret = Hkdf.extractAndExpand(sharedKey, authSecret, AUTH_INFO, SECRET_LENGTH);
        var keyInfo = Bytes.concat(AES_GCM_INFO, context);
        var ivInfo = Bytes.concat(IV_INFO, context);

        var key = Hkdf.extractAndExpand(keyInfo, salt, sharedSecret, KEY_LENGTH);
        var baseIv = Hkdf.extractAndExpand(ivInfo, salt, sharedSecret, IV_LENGTH);

        try(var result = new ByteArrayOutputStream()) {
            var start = 0;
            var counter = 0;
            while (start < content.length) {
                var end = Math.min(start + RECORD_SIZE, content.length);

                var iv = ByteBuffer.allocate(12)
                        .putInt(8, counter)
                        .array();
                xor12(iv, baseIv, iv);
                var chunk = Arrays.copyOfRange(content, start, end);
                result.write(AesGcm.decrypt(iv, chunk, key, null));
                start = end;
                counter++;
            }
            return result.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void xor12(byte[] dst, byte[] a, byte[] b) {
        dst[0] = (byte) (a[0] ^ b[0]);
        dst[1] = (byte) (a[1] ^ b[1]);
        dst[2] = (byte) (a[2] ^ b[2]);
        dst[3] = (byte) (a[3] ^ b[3]);
        dst[4] = (byte) (a[4] ^ b[4]);
        dst[5] = (byte) (a[5] ^ b[5]);
        dst[6] = (byte) (a[6] ^ b[6]);
        dst[7] = (byte) (a[7] ^ b[7]);
        dst[8] = (byte) (a[8] ^ b[8]);
        dst[9] = (byte) (a[9] ^ b[9]);
        dst[10] = (byte) (a[10] ^ b[10]);
        dst[11] = (byte) (a[11] ^ b[11]);
    }
}