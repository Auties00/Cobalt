package it.auties.whatsapp.model.signal.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Bytes;

import java.util.Arrays;

@ProtobufMessage(name = "SenderKeyDistributionMessage")
public final class SignalDistributionMessage extends SignalProtocolMessage<SignalDistributionMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    private final Integer id;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    private final Integer iteration;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private final byte[] chainKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private final byte[] signingKey;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SignalDistributionMessage(int id, int iteration, byte[] chainKey, byte[] signingKey) {
        this.id = id;
        this.iteration = iteration;
        this.chainKey = chainKey;
        this.signingKey = signingKey;
    }

    public static SignalDistributionMessage ofSerialized(byte[] serialized) {
        return SignalDistributionMessageSpec.decode(Arrays.copyOfRange(serialized, 1, serialized.length))
                .setVersion(Bytes.bytesToVersion(serialized[0]))
                .setSerialized(serialized);
    }

    @Override
    public byte[] serialized() {
        if (serialized == null) {
            this.serialized = Bytes.concat(serializedVersion(), SignalDistributionMessageSpec.encode(this));
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
