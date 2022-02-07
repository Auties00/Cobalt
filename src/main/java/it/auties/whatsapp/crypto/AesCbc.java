package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.util.Validate;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static it.auties.whatsapp.binary.BinaryArray.random;

@UtilityClass
public class AesCbc {
    private final String AES_CBC = "AES/CBC/PKCS5Padding";
    private final String AES = "AES";
    private final int AES_BLOCK_SIZE = 16;

    public byte[] encrypt(byte [] decrypted, byte[] encKey) {
        return encrypt(random(AES_BLOCK_SIZE).data(), decrypted, encKey);
    }

    @SneakyThrows
    public byte[] encrypt(byte[] iv, byte [] decrypted, byte[] encKey) {
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(encKey, AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(decrypted);
    }

    public byte[] decrypt(byte[] encrypted, byte[] secretKey) {
        var binary = BinaryArray.of(encrypted);
        var iv = binary.cut(16)
                .data();
        var encryptedNoIv = binary.slice(16)
                .data();
        return decrypt(iv, encryptedNoIv, secretKey);
    }

    @SneakyThrows
    public byte[] decrypt(byte[] iv, byte[] encrypted, byte[] secretKey) {
        Validate.isTrue(iv.length == AES_BLOCK_SIZE,
                "Invalid iv size: expected %s, got %s",
                AES_BLOCK_SIZE,
                iv.length);
        Validate.isTrue(encrypted.length % AES_BLOCK_SIZE == 0,
                "Invalid encrypted size");
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(secretKey, AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
}
