package it.auties.whatsapp.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class AesGcm {
    private static final int TAG_LENGTH = 128;

    private static byte[] cipher(byte[] iv, byte[] input, int offset, int length, byte[] key, byte[] additionalData, boolean encrypt) {
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            var keySpec = new SecretKeySpec(key, "AES");
            var gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if(additionalData != null) {
                cipher.updateAAD(additionalData);
            }
            var outputLength = cipher.getOutputSize(length);
            var output = new byte[outputLength];
            var outputOffset = cipher.update(input, offset, length, output, 0);
            cipher.doFinal(output, outputOffset);
            return output;
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot %s data".formatted(encrypt ? "encrypt" : "decrypt"), throwable);
        }
    }


    private static byte[] toIv(long iv) {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try(var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.write(new byte[4]);
            dataOutputStream.writeLong(iv);
            return byteArrayOutputStream.toByteArray();
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static byte[] encrypt(long iv, byte[] input, byte[] key) {
        return encrypt(iv, input, key, null);
    }

    public static byte[] encrypt(long iv, byte[] input, byte[] key, byte[] additionalData) {
        return cipher(toIv(iv), input, 0, input.length, key, additionalData, true);
    }

    public static byte[] encrypt(byte[] iv, byte[] input, byte[] key, byte[] additionalData) {
        return cipher(iv, input, 0, input.length, key, additionalData, true);
    }

    public static byte[] encrypt(byte[] iv, byte[] input, byte[] key) {
        return cipher(iv, input, 0, input.length, key, null, true);
    }

    public static byte[] decrypt(long iv, byte[] input, byte[] key) {
        return decrypt(iv, input, 0, input.length, key);
    }

    public static byte[] decrypt(long iv, byte[] input, int offset, int length, byte[] key) {
        return decrypt(iv, input, offset, length, key, null);
    }

    public static byte[] decrypt(long iv, byte[] input, byte[] key, byte[] additionalData) {
        return decrypt(iv, input, 0, input.length, key, additionalData);
    }

    public static byte[] decrypt(long iv, byte[] input, int offset, int length, byte[] key, byte[] additionalData) {
        return cipher(toIv(iv), input, offset, length, key, additionalData, false);
    }

    public static byte[] decrypt(byte[] iv, byte[] input, byte[] key, byte[] additionalData) {
        return decrypt(iv, input, 0, input.length, key, additionalData);
    }

    public static byte[] decrypt(byte[] iv, byte[] input, int offset, int length, byte[] key, byte[] additionalData) {
        return cipher(iv, input, offset, length, key, additionalData, false);
    }
}
