package it.auties.whatsapp.model.signal.message;

import it.auties.bytes.Bytes;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.protobuf.api.model.ProtobufSchema;
import it.auties.whatsapp.util.BytesHelper;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.function.Function;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class SignalMessage implements SignalProtocolMessage {
    private int version;

    @ProtobufProperty(index = 1, type = BYTES)
    private byte @NonNull [] ephemeralPublicKey;

    @ProtobufProperty(index = 2, type = UINT32)
    private int counter;

    @ProtobufProperty(index = 3, type = UINT32)
    private int previousCounter;

    @ProtobufProperty(index = 4, type = BYTES)
    private byte @NonNull [] ciphertext;

    private byte[] signature;

    private byte[] serialized;

    @SneakyThrows
    public SignalMessage(byte[] ephemeralPublicKey, int counter, int previousCounter, byte[] ciphertext, Function<byte[], byte[]> signer) {
        this.version = CURRENT_VERSION;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        var encodedMessage = Bytes.of(serializedVersion())
                .append(PROTOBUF.writeValueAsBytes(this));
        this.signature = signer.apply(encodedMessage.toByteArray());
        this.serialized = encodedMessage.append(signature)
                .toByteArray();
    }

    public static SignalMessage ofSerialized(byte[] serialized) {
        try {
            var buffer = Bytes.of(serialized);
            return PROTOBUF.reader()
                    .with(ProtobufSchema.of(SignalMessage.class))
                    .readValue(buffer.slice(1, -MAC_LENGTH).toByteArray(), SignalMessage.class)
                    .version(BytesHelper.bytesToVersion(serialized[0]))
                    .signature(buffer.slice(-MAC_LENGTH).toByteArray())
                    .serialized(serialized);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode SignalMessage", exception);
        }
    }
}
