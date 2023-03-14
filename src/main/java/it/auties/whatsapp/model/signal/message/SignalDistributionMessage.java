package it.auties.whatsapp.model.signal.message;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Spec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class SignalDistributionMessage implements SignalProtocolMessage {
    /**
     * The version of this message
     */
    private int version;

    /**
     * The jid of the sender
     */
    @ProtobufProperty(index = 1, type = UINT32)
    private Integer id;

    /**
     * The iteration of the message
     */
    @ProtobufProperty(index = 2, type = UINT32)
    private Integer iteration;

    /**
     * The value key of the message
     */
    @ProtobufProperty(index = 3, type = BYTES)
    private byte @NonNull [] chainKey;

    /**
     * The signing key of the message
     */
    @ProtobufProperty(index = 4, type = BYTES)
    private byte @NonNull [] signingKey;

    /**
     * This message in a serialized form
     */
    private byte[] serialized;

    public SignalDistributionMessage(int id, int iteration, byte @NonNull [] chainKey, byte @NonNull [] signingKey) {
        this.version = Spec.Signal.CURRENT_VERSION;
        this.id = id;
        this.iteration = iteration;
        this.chainKey = chainKey;
        this.signingKey = signingKey;
        this.serialized = Bytes.of(serializedVersion()).append(Protobuf.writeMessage(this)).toByteArray();
    }

    public static SignalDistributionMessage ofSerialized(byte[] serialized) {
        return Protobuf.readMessage(Bytes.of(serialized).slice(1).toByteArray(), SignalDistributionMessage.class)
                .version(BytesHelper.bytesToVersion(serialized[0]))
                .serialized(serialized);
    }
}
