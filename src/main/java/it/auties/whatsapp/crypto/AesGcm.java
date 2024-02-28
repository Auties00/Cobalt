package it.auties.whatsapp.crypto;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class AesGcm {
    private static final int NONCE = 128;

    private AesGcm() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static byte[] encrypt(long iv, byte[] input, byte[] key) {
        return encrypt(iv, input, key, null);
    }

    public static byte[] encrypt(long iv, byte[] input, byte[] key, byte[] additionalData) {
        return cipher(toIv(iv), input, input.length, key, additionalData, true);
    }

    private static byte[] cipher(byte[] iv, byte[] input, int inputLength, byte[] key, byte[] additionalData, boolean encrypt) {
        try {
            var cipher = new GCMBlockCipher(new AESEngine());
            var parameters = new AEADParameters(new KeyParameter(key), NONCE, iv, additionalData);
            cipher.init(encrypt, parameters);
            var outputLength = cipher.getOutputSize(input.length);
            var output = new byte[outputLength];
            var outputOffset = cipher.processBytes(input, 0, inputLength, output, 0);
            cipher.doFinal(output, outputOffset);
            return output;
        } catch (InvalidCipherTextException exception) {
            throw new RuntimeException("Cannot %s data".formatted(encrypt ? "encrypt" : "decrypt"), exception);
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

    public static byte[] decrypt(long iv, byte[] input, byte[] key) {
        return decrypt(iv, input, input.length, key);
    }

    public static byte[] decrypt(long iv, byte[] input, int inputLength, byte[] key) {
        return cipher(toIv(iv), input, inputLength, key, null, false);
    }

    public static byte[] decrypt(long iv, byte[] input, byte[] key, byte[] additionalData) {
        return cipher(toIv(iv), input, input.length, key, additionalData, false);
    }

    public static byte[] encrypt(byte[] iv, byte[] input, byte[] key, byte[] additionalData) {
        return cipher(iv, input, input.length, key, additionalData, true);
    }

    public static byte[] encrypt(byte[] iv, byte[] input, byte[] key) {
        return cipher(iv, input, input.length, key, null, true);
    }

    public static byte[] decrypt(byte[] iv, byte[] input, byte[] key, byte[] additionalData) {
        return cipher(iv, input, input.length, key, additionalData, false);
    }
}
