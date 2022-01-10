package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;

public record AesGmc(@NonNull GCMBlockCipher cipher) {
    private static final int NONCE = 128;

    public static AesGmc with(@NonNull BinaryArray key, long ivCounter, boolean encrypt) {
        return with(key, null, ivCounter, encrypt);
    }

    public static AesGmc with(@NonNull BinaryArray key, byte[] data, long ivCounter, boolean encrypt) {
        var cipher = new GCMBlockCipher(new AESEngine());
        var parameters = new AEADParameters(new KeyParameter(key.data()), NONCE, createIv(ivCounter), data);
        cipher.init(encrypt, parameters);
        return new AesGmc(cipher);
    }

    private static byte[] createIv(long count) {
        return ByteBuffer.allocate(12).putLong(4, count).array();
    }

    @SneakyThrows
    public synchronized byte[] process(byte[] bytes) {
        var outputLength = cipher.getOutputSize(bytes.length);
        var output = new byte[outputLength];
        var outputOffset = cipher.processBytes(bytes, 0, bytes.length, output, 0);
        cipher.doFinal(output, outputOffset);
        return output;
    }
}
