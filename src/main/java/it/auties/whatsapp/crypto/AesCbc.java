package it.auties.whatsapp.crypto;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@UtilityClass
public class AesCbc {
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private final String AES = "AES";

    @SneakyThrows
    public byte[] encrypt(byte[] iv, byte [] decrypted, byte[] encKey) {
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(encKey, AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(decrypted);
    }

    @SneakyThrows
    public byte[] decrypt(byte[] iv, byte[] encrypted, byte[] secretKey) {
        var cipher = Cipher.getInstance(AES_CBC);
        var keySpec = new SecretKeySpec(secretKey, AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
}
