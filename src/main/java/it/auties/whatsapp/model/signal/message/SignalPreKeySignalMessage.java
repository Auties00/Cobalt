package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import static it.auties.whatsapp.model.signal.SignalProtocol.CURRENT_VERSION;

@ProtobufMessage(name = "PreKeySignalMessage")
public final class SignalPreKeySignalMessage {
    private Integer version;

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer preKeyId;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final SignalPublicKey baseKey;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final SignalPublicKey identityKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final byte[] serializedSignalMessage;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    final Integer registrationId;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    final Integer signedPreKeyId;

    SignalPreKeySignalMessage(Integer preKeyId, SignalPublicKey baseKey, SignalPublicKey identityKey, byte[] serializedSignalMessage, Integer registrationId, Integer signedPreKeyId) {
        // Don't set the version, it will be set by ofSerialized
        this.preKeyId = preKeyId;
        this.baseKey = baseKey;
        this.identityKey = identityKey;
        this.serializedSignalMessage = serializedSignalMessage;
        this.registrationId = registrationId;
        this.signedPreKeyId = signedPreKeyId;
    }

    public SignalPreKeySignalMessage(Integer version, Integer preKeyId, SignalPublicKey baseKey, SignalPublicKey identityKey, byte[] serializedSignalMessage, Integer registrationId, Integer signedPreKeyId) {
        this.version = version;
        this.preKeyId = preKeyId;
        this.baseKey = baseKey;
        this.identityKey = identityKey;
        this.serializedSignalMessage = serializedSignalMessage;
        this.registrationId = registrationId;
        this.signedPreKeyId = signedPreKeyId;
    }

    public static SignalPreKeySignalMessage ofSerialized(byte[] serialized) {
        var result = SignalPreKeyMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        return result;
    }

    public byte[] serialized() {
        var serialized = new byte[1 + SignalPreKeyMessageSpec.sizeOf(this)];
        if(version == null) {
            throw new InternalError();
        }
        serialized[0] = (byte) (version << 4 | CURRENT_VERSION);
        SignalPreKeyMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        return serialized;
    }

    public Integer version() {
        if(version == null) {
            throw new InternalError();
        }

        return version;
    }

    public SignalMessage signalMessage() {
        return SignalMessage.ofSerialized(serializedSignalMessage);
    }

    public Integer preKeyId() {
        return preKeyId;
    }

    public SignalPublicKey baseKey() {
        return baseKey;
    }

    public SignalPublicKey identityKey() {
        return identityKey;
    }

    public Integer registrationId() {
        return registrationId;
    }

    public Integer signedPreKeyId() {
        return signedPreKeyId;
    }
}
