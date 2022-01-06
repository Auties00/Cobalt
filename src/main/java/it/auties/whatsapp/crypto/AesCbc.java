package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;

@UtilityClass
public class AesCbc {
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private final String AES = "AES";

    @SneakyThrows
    public byte[] encrypt(byte[] iv, byte [] decrypted, byte[] encKey) {
        final var cipher = Cipher.getInstance(AES_CBC);
        final var keySpec = new SecretKeySpec(encKey, AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(decrypted);
    }

    @SneakyThrows
    public byte[] decrypt(byte[] iv, byte[] encrypted, byte[] secretKey) {
        final var cipher = Cipher.getInstance(AES_CBC);
        final var keySpec = new SecretKeySpec(secretKey, AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
}
