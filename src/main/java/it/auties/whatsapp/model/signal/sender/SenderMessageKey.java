package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.SignalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static it.auties.whatsapp.util.SignalConstants.IV_LENGTH;

@ProtobufMessage
public record SenderMessageKey(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int iteration,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] seed,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] iv,
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte[] cipherKey
) {
    public SenderMessageKey(int iteration, byte[] seed) {
        this(iteration, seed, createIv(seed), createCipherKey(seed));
    }

    private static byte[] createIv(byte[] seed) {
        var chunks = Hkdf.deriveSecrets(seed, "WhisperGroup".getBytes(StandardCharsets.UTF_8), 1);
        return Arrays.copyOf(chunks[0], IV_LENGTH);
    }

    private static byte[] createCipherKey(byte[] seed) {
        var derived = Hkdf.deriveSecrets(seed, "WhisperGroup".getBytes(StandardCharsets.UTF_8));
        var result = new byte[SignalConstants.KEY_LENGTH];
        System.arraycopy(derived[0], IV_LENGTH, result, 0, derived[0].length - IV_LENGTH);
        System.arraycopy(derived[1], 0, result, derived[0].length - IV_LENGTH, SignalConstants.KEY_LENGTH - derived[0].length + IV_LENGTH);
        return result;
    }
}