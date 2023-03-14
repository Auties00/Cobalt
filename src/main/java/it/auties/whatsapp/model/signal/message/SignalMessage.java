package it.auties.whatsapp.model.signal.message;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.util.BytesHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.function.Function;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.whatsapp.util.Spec.Signal.CURRENT_VERSION;
import static it.auties.whatsapp.util.Spec.Signal.MAC_LENGTH;

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

    public SignalMessage(byte @NonNull [] ephemeralPublicKey, int counter, int previousCounter, byte @NonNull [] ciphertext, Function<byte[], byte[]> signer) {
        this.version = CURRENT_VERSION;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        var encodedMessage = Bytes.of(serializedVersion()).append(Protobuf.writeMessage(this));
        this.signature = signer.apply(encodedMessage.toByteArray());
        this.serialized = encodedMessage.append(signature).toByteArray();
    }

    public static SignalMessage ofSerialized(byte[] serialized) {
        var buffer = Bytes.of(serialized);
        return Protobuf.readMessage(buffer.slice(1, -MAC_LENGTH).toByteArray(), SignalMessage.class)
                .version(BytesHelper.bytesToVersion(serialized[0]))
                .signature(buffer.slice(-MAC_LENGTH).toByteArray())
                .serialized(serialized);
    }
}
