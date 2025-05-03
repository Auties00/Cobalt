package it.auties.whatsapp.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

// TODO: Inline me
public final class AesCbc {
    public static byte[] encrypt(byte[] iv, byte[] plaintext, byte[] key) {
        try {
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    public static byte[] decrypt(byte[] encrypted, byte[] key) {
        try {
            if (encrypted.length % 16 != 0) {
                throw new IllegalArgumentException("Invalid encrypted size");
            }
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(encrypted, 0, 16));
            return cipher.doFinal(encrypted, 16, encrypted.length - 16);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot decrypt data", exception);
        }
    }

    public static byte[] decrypt(byte[] iv, byte[] encrypted, byte[] key) {
        try {
            if (iv.length != 16) {
                throw new IllegalArgumentException("Invalid iv size: expected %s, got %s".formatted(16, iv.length));
            }
            if (encrypted.length % 16 != 0) {
                throw new IllegalArgumentException("Invalid encrypted size");
            }
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot decrypt data", exception);
        }
    }
}
