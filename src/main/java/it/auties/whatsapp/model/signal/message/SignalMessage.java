package it.auties.whatsapp.model.signal.message;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.util.BytesHelper;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.function.Function;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.whatsapp.util.Specification.Signal.CURRENT_VERSION;
import static it.auties.whatsapp.util.Specification.Signal.MAC_LENGTH;

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
    private Integer counter;

    @ProtobufProperty(index = 3, type = UINT32)
    private Integer previousCounter;

    @ProtobufProperty(index = 4, type = BYTES)
    private byte @NonNull [] ciphertext;

    private byte[] signature;

    private byte[] serialized;

    @SneakyThrows
    public SignalMessage(byte @NonNull [] ephemeralPublicKey, int counter, int previousCounter, byte @NonNull [] ciphertext, Function<byte[], byte[]> signer) {
        this.version = CURRENT_VERSION;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        var encodedMessage = Bytes.of(serializedVersion()).append(PROTOBUF.writeValueAsBytes(this));
        this.signature = signer.apply(encodedMessage.toByteArray());
        this.serialized = encodedMessage.append(signature).toByteArray();
    }

    @SneakyThrows
    public static SignalMessage ofSerialized(byte[] serialized) {
        var buffer = Bytes.of(serialized);
        return PROTOBUF.readMessage(buffer.slice(1, -MAC_LENGTH).toByteArray(), SignalMessage.class)
                .version(BytesHelper.bytesToVersion(serialized[0]))
                .signature(buffer.slice(-MAC_LENGTH).toByteArray())
                .serialized(serialized);
    }
}
