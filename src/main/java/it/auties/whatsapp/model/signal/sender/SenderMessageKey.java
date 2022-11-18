package it.auties.whatsapp.model.signal.sender;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;

@Builder
@Jacksonized
public record SenderMessageKey(int iteration, byte[] seed, byte[] iv, byte[] cipherKey) implements ProtobufMessage, SignalSpecification {
    public SenderMessageKey(int iteration, byte[] seed) {
        this(
                iteration,
                seed,
                createIv(seed),
                createCipherKey(seed)
        );
    }

    private static byte[] createIv(byte[] seed) {
        var derivative = getDerivedSeed(seed);
        return Bytes.of(derivative[0])
                .cut(IV_LENGTH)
                .toByteArray();
    }

    private static byte[] createCipherKey(byte[] seed) {
        var derivative = getDerivedSeed(seed);
        return Bytes.of(derivative[0])
                .slice(IV_LENGTH)
                .append(derivative[1])
                .cut(KEY_LENGTH)
                .toByteArray();
    }

    private static byte[][] getDerivedSeed(byte[] seed) {
        return Hkdf.deriveSecrets(seed, "WhisperGroup".getBytes(StandardCharsets.UTF_8));
    }
}
