package it.auties.whatsapp.cipher;

import lombok.NonNull;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;

public class AesGmc {
    private GCMBlockCipher cipher;
    public void initialize(byte @NonNull [] key, byte[] data, long ivCounter, boolean encrypt){
        var secretKey = new KeyParameter(key);
        var iv = createIv(ivCounter);
        var cipher = new AESEngine();
        cipher.init(encrypt, secretKey);
        this.cipher = new GCMBlockCipher(cipher);
        var params = new AEADParameters(secretKey, 128, iv, data);
        this.cipher.init(encrypt, params);
    }

    public byte[] processBytes(byte[] bytes) {
        try {
            var outputLength = cipher.getOutputSize(bytes.length);
            var output = new byte[outputLength];
            var outputOffset = cipher.processBytes(bytes, 0, bytes.length, output, 0);
            cipher.doFinal(output, outputOffset);
            return output;
        }catch (InvalidCipherTextException exception){
            throw new IllegalArgumentException("Cannot process provided bytes", exception);
        }
    }

    private byte[] createIv(long count) {
        return ByteBuffer.allocate(12)
                .putLong(4, count)
                .array();
    }
}
