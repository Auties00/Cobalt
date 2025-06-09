package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import static it.auties.whatsapp.util.SignalConstants.CURRENT_VERSION;

@ProtobufMessage(name = "SenderKeyDistributionMessage")
public final class SignalDistributionMessage {
    private int version;

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer id;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer iteration;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] chainKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final byte[] signingKey;

    public SignalDistributionMessage(int version, int id, int iteration, byte[] chainKey, byte[] signingKey) {
        this.version = version;
        this.id = id;
        this.iteration = iteration;
        this.chainKey = chainKey;
        this.signingKey = signingKey;
    }

    SignalDistributionMessage(Integer id, Integer iteration, byte[] chainKey, byte[] signingKey) {
        this.id = id;
        this.iteration = iteration;
        this.chainKey = chainKey;
        this.signingKey = signingKey;
    }

    public static SignalDistributionMessage ofSerialized(byte[] serialized) {
        var result = SignalDistributionMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        return result;
    }

    public byte[] serialized() {
        var serialized = new byte[1 + SignalDistributionMessageSpec.sizeOf(this)];
        serialized[0] = serializedVersion();
        SignalDistributionMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        return serialized;
    }

    public int version() {
        if(version == 0) {
            throw new InternalError();
        }

        return version;
    }

    public byte serializedVersion() {
        if(version == 0) {
            throw new InternalError();
        }

        return (byte) (version << 4 | CURRENT_VERSION);
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
