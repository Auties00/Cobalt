package it.auties.whatsapp.crypto;

import it.auties.whatsapp.util.Bytes;

import javax.crypto.KeyAgreement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public final class HttpEce {
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 16;
    private static final int SECRET_LENGTH = 32;
    private static final byte[] AUTH_INFO = "Content-Encoding: auth\0".getBytes();
    private static final byte[] AES_GCM_INFO = "Content-Encoding: aesgcm\0".getBytes();
    private static final byte[] IV_INFO = "Content-Encoding: nonce\0".getBytes();
    private static final byte[] KEY_LABEL = "P-256".getBytes();
    private static final int RECORD_SIZE = 4112;
    private static final byte[] P256_HEAD = Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE");

    public static byte[] decrypt(byte[] content, byte[] dh, byte[] publicKey, ECPrivateKey privateKey, byte[] salt, byte[] authSecret) {
        var secret = calculateSharedSecret(dh, privateKey);
        var context = Bytes.concat(
                KEY_LABEL,
                new byte[1],
                unsignedShortToBigEndianBytes(publicKey.length),
                publicKey,
                unsignedShortToBigEndianBytes(dh.length),
                dh
        );
        var expandedSecret = Hkdf.extractAndExpand(
                secret,
                authSecret,
                AUTH_INFO,
                SECRET_LENGTH
        );
        var key = Hkdf.extractAndExpand(
                expandedSecret,
                salt,
                Bytes.concat(AES_GCM_INFO, context),
                KEY_LENGTH
        );
        var baseIv = Hkdf.extractAndExpand(
                expandedSecret,
                salt,
                Bytes.concat(IV_INFO, context),
                IV_LENGTH
        );
        try(var result = new ByteArrayOutputStream()) {
            var start = 0;
            var counter = 0;
            while (start < content.length) {
                var end = Math.min(start + RECORD_SIZE, content.length);
                var iv = ByteBuffer.allocate(IV_LENGTH)
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

    private static byte[] calculateSharedSecret(byte[] dh, ECPrivateKey privateKey) {
        try {
            var encodedKey = new byte[P256_HEAD.length + dh.length - 1];
            System.arraycopy(P256_HEAD, 0, encodedKey, 0, P256_HEAD.length);
            System.arraycopy(dh, 1, encodedKey, P256_HEAD.length, dh.length - 1);
            var keyFactory = KeyFactory.getInstance("EC");
            var spec = new X509EncodedKeySpec(encodedKey);
            var jcaPublicKey = (ECPublicKey) keyFactory.generatePublic(spec);
            var keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(jcaPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
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

    private static byte[] unsignedShortToBigEndianBytes(int value) {
        var bytes = new byte[2];
        bytes[0] = (byte) ((value >> 8) & 0xFF);
        bytes[1] = (byte) (value & 0xFF);
        return bytes;
    }
}