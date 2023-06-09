package it.auties.whatsapp.crypto;

import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Validate;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

@UtilityClass
public class AesCbc {
    private final String AES_CBC = "AES/CBC/PKCS5Padding";
    private final String AES = "AES";
    private final int AES_BLOCK_SIZE = 16;

    public byte[] encryptAndPrefix(byte[] plaintext, byte[] key) {
        var iv = BytesHelper.random(AES_BLOCK_SIZE);
        var encrypted = encrypt(iv, plaintext, key);
        return BytesHelper.concat(iv, encrypted);
    }

    @SneakyThrows
    public byte[] encrypt(byte[] iv, byte[] plaintext, byte[] key) {
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(key, AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(plaintext);
    }

    public byte[] decrypt(byte[] encrypted, byte[] key) {
        var iv = Arrays.copyOfRange(encrypted, 0, 16);
        var encryptedNoIv = Arrays.copyOfRange(encrypted, iv.length, encrypted.length);
        return decrypt(iv, encryptedNoIv, key);
    }

    @SneakyThrows
    public byte[] decrypt(byte[] iv, byte[] encrypted, byte[] key) {
        Validate.isTrue(iv.length == AES_BLOCK_SIZE, "Invalid iv size: expected %s, got %s", AES_BLOCK_SIZE, iv.length);
        Validate.isTrue(encrypted.length % AES_BLOCK_SIZE == 0, "Invalid encrypted size");
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(key, AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
}
