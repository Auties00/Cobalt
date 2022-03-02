package it.auties.whatsapp.crypto;

import it.auties.buffer.ByteBuffer;
import it.auties.whatsapp.util.Validate;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static it.auties.buffer.ByteBuffer.random;

@UtilityClass
public class AesCbc {
    private final String AES_CBC = "AES/CBC/PKCS5Padding";
    private final String AES = "AES";
    private final int AES_BLOCK_SIZE = 16;

    public byte[] encrypt(byte [] plaintext, byte[] key) {
        return encrypt(random(AES_BLOCK_SIZE).toByteArray(), plaintext, key);
    }

    @SneakyThrows
    public byte[] encrypt(byte[] iv, byte [] plaintext, byte[] key) {
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(key, AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(plaintext);
    }

    public byte[] decrypt(byte[] encrypted, byte[] key) {
        var binary = ByteBuffer.of(encrypted);
        var iv = binary.cut(16)
                .toByteArray();
        var encryptedNoIv = binary.slice(16)
                .toByteArray();
        return decrypt(iv, encryptedNoIv, key);
    }

    @SneakyThrows
    public byte[] decrypt(byte[] iv, byte[] encrypted, byte[] key) {
        Validate.isTrue(iv.length == AES_BLOCK_SIZE,
                "Invalid iv size: expected %s, got %s",
                AES_BLOCK_SIZE,
                iv.length);
        Validate.isTrue(encrypted.length % AES_BLOCK_SIZE == 0,
                "Invalid encrypted size");
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(key, AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
}
