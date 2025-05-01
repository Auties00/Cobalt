package it.auties.whatsapp.crypto;

import it.auties.whatsapp.util.Bytes;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class AesCbc {
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";
    private static final int AES_BLOCK_SIZE = 16;

    public static byte[] encryptAndPrefix(byte[] plaintext, byte[] key) {
        var iv = Bytes.random(AES_BLOCK_SIZE);
        var encrypted = encrypt(iv, plaintext, key);
        return Bytes.concat(iv, encrypted);
    }

    public static byte[] encrypt(byte[] iv, byte[] plaintext, byte[] key) {
        try {
            var cipher = Cipher.getInstance(AES_CBC);
            var keySpec = new SecretKeySpec(key, AES);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    public static byte[] decrypt(byte[] encrypted, byte[] key) {
        var iv = Arrays.copyOfRange(encrypted, 0, AES_BLOCK_SIZE);
        var encryptedNoIv = Arrays.copyOfRange(encrypted, iv.length, encrypted.length);
        return decrypt(iv, encryptedNoIv, key);
    }

    public static byte[] decrypt(byte[] iv, byte[] encrypted, byte[] key) {
        try {
            if (iv.length != AES_BLOCK_SIZE) {
                throw new IllegalArgumentException("Invalid iv size: expected %s, got %s".formatted(AES_BLOCK_SIZE, iv.length));
            }
            if (encrypted.length % AES_BLOCK_SIZE != 0) {
                throw new IllegalArgumentException("Invalid encrypted size");
            }
            var cipher = Cipher.getInstance(AES_CBC);
            var keySpec = new SecretKeySpec(key, AES);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }
}
