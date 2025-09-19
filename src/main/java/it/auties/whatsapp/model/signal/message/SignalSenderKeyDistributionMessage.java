package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import static it.auties.whatsapp.model.signal.SignalProtocol.CURRENT_VERSION;

@ProtobufMessage
public final class SignalSenderKeyDistributionMessage {
    private Integer version;

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer id;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer iteration;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] chainKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final SignalPublicKey signatureKey;

    SignalSenderKeyDistributionMessage(Integer id, Integer iteration, byte[] chainKey, SignalPublicKey signatureKey) {
        // Don't set the version, it will be set by ofSerialized
        this.id = id;
        this.iteration = iteration;
        this.chainKey = chainKey;
        this.signatureKey = signatureKey;
    }

    public SignalSenderKeyDistributionMessage(Integer version, Integer id, Integer iteration, byte[] chainKey, SignalPublicKey signatureKey) {
        this.version = version;
        this.id = id;
        this.iteration = iteration;
        this.chainKey = chainKey;
        this.signatureKey = signatureKey;
    }

    public static SignalSenderKeyDistributionMessage ofSerialized(byte[] serialized) {
        var result = SignalDistributionMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        return result;
    }

    public byte[] serialized() {
        var serialized = new byte[1 + SignalDistributionMessageSpec.sizeOf(this)];
        if(version == null) {
            throw new InternalError();
        }
        serialized[0] = (byte) (version << 4 | CURRENT_VERSION);
        SignalDistributionMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        return serialized;
    }

    public Integer version() {
        if(version == 0) {
            throw new InternalError();
        }

        return version;
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

    public SignalPublicKey signatureKey() {
        return signatureKey;
    }
}
