package it.auties.whatsapp.model.signal.sender;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.SignalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
        var derivative = getDerivedSeed(seed);
        return Arrays.copyOf(derivative[0], SignalConstants.IV_LENGTH);
    }

    private static byte[] createCipherKey(byte[] seed) {
        var derivative = getDerivedSeed(seed);
        var data = Arrays.copyOfRange(derivative[0], SignalConstants.IV_LENGTH, derivative[0].length);
        var concat = Bytes.concat(data, derivative[1]);
        return Arrays.copyOf(concat, SignalConstants.KEY_LENGTH);
    }

    private static byte[][] getDerivedSeed(byte[] seed) {
        return Hkdf.deriveSecrets(seed, "WhisperGroup".getBytes(StandardCharsets.UTF_8));
    }
}