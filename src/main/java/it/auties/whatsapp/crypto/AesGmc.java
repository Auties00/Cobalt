package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public record AesGmc(@NonNull GCMBlockCipher cipher) {
    private static final int NONCE = 128;

    public static AesGmc of(@NonNull Bytes key, long ivCounter, boolean encrypt) {
        return of(key, null, ivCounter, encrypt);
    }

    public static AesGmc of(@NonNull Bytes key, byte[] data, long ivCounter, boolean encrypt) {
        var cipher = new GCMBlockCipher(new AESEngine());
        var parameters = new AEADParameters(new KeyParameter(key.toByteArray()), NONCE, createIv(ivCounter), data);
        cipher.init(encrypt, parameters);
        return new AesGmc(cipher);
    }

    private static byte[] createIv(long count) {
        return Bytes.newBuffer(4)
                .appendLong(count)
                .assertSize(12)
                .toByteArray();
    }

    @SneakyThrows
    public byte[] encrypt(byte[] bytes) {
        var outputLength = cipher.getOutputSize(bytes.length);
        var output = new byte[outputLength];
        var outputOffset = cipher.processBytes(bytes, 0, bytes.length, output, 0);
        cipher.doFinal(output, outputOffset);
        return output;
    }
}
