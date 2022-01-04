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

@UtilityClass
public class AesCbc {
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";

    @SneakyThrows
    public byte[] cipher(byte[] key, byte[] data, byte[] iv, boolean encrypt) {
        var cipher = Cipher.getInstance(AES_CBC);
        var mode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        var keySpec = new SecretKeySpec(key, "AES");
        var ivSpec = new IvParameterSpec(iv);
        cipher.init(mode, keySpec, ivSpec);
        return cipher.doFinal(data);
    }
}
