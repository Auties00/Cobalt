package it.auties.whatsapp.model.signal.sender;

import it.auties.bytes.Bytes;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderMessageKey implements ProtobufMessage, SignalSpecification {
    @ProtobufProperty(index = 1, type = UINT32)
    private Integer iteration;

    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] seed;

    private byte[] iv;

    private byte[] cipherKey;

    public SenderMessageKey(int iteration, byte[] seed) {
        var derivative = Hkdf.deriveSecrets(seed, "WhisperGroup".getBytes(StandardCharsets.UTF_8));
        this.iteration = iteration;
        this.seed = seed;
        this.iv = Bytes.of(derivative[0])
                .cut(IV_LENGTH)
                .toByteArray();
        this.cipherKey = Bytes.of(derivative[0])
                .slice(IV_LENGTH)
                .append(derivative[1])
                .cut(KEY_LENGTH)
                .toByteArray();
    }
}
