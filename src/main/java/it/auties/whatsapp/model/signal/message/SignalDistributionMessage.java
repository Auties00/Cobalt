package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.BytesHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;

public final class SignalDistributionMessage extends SignalProtocolMessage<SignalDistributionMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    private final Integer id;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    private final Integer iteration;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private final byte @NonNull [] chainKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private final byte @NonNull [] signingKey;

    public SignalDistributionMessage(int id, int iteration, byte @NonNull [] chainKey, byte @NonNull [] signingKey) {
        this.id = id;
        this.iteration = iteration;
        this.chainKey = chainKey;
        this.signingKey = signingKey;
    }

    public static SignalDistributionMessage ofSerialized(byte[] serialized) {
        return SignalDistributionMessageSpec.decode(Arrays.copyOfRange(serialized, 1, serialized.length))
                .setVersion(BytesHelper.bytesToVersion(serialized[0]))
                .setSerialized(serialized);
    }

    @Override
    public byte[] serialized() {
        if(serialized == null) {
            this.serialized = BytesHelper.concat(serializedVersion(), SignalDistributionMessageSpec.encode(this));
        }

        return serialized;
    }

    public Integer id() {
        return id;
    }

    public Integer iteration() {
        return iteration;
    }

    public byte[] chainKey() {
        return chainKey;
    }

    public byte[] signingKey() {
        return signingKey;
    }
}
