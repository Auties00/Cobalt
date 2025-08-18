package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import static it.auties.whatsapp.util.SignalConstants.CURRENT_VERSION;

@ProtobufMessage(name = "PreKeySignalMessage")
public final class SignalPreKeyMessage {
    private int version;

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer preKeyId;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] baseKey;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] identityKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final byte[] serializedSignalMessage;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    final Integer registrationId;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    final Integer signedPreKeyId;

    public SignalPreKeyMessage(int version, Integer preKeyId, byte[] baseKey, byte[] identityKey, byte[] serializedSignalMessage, int registrationId, int signedPreKeyId) {
        this.version = version;
        this.preKeyId = preKeyId;
        this.baseKey = baseKey;
        this.identityKey = identityKey;
        this.serializedSignalMessage = serializedSignalMessage;
        this.registrationId = registrationId;
        this.signedPreKeyId = signedPreKeyId;
    }

    SignalPreKeyMessage(Integer preKeyId, byte[] baseKey, byte[] identityKey, byte[] serializedSignalMessage, Integer registrationId, Integer signedPreKeyId) {
        this.preKeyId = preKeyId;
        this.baseKey = baseKey;
        this.identityKey = identityKey;
        this.serializedSignalMessage = serializedSignalMessage;
        this.registrationId = registrationId;
        this.signedPreKeyId = signedPreKeyId;
    }

    public static SignalPreKeyMessage ofSerialized(byte[] serialized) {
        var result = SignalPreKeyMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        return result;
    }

    public byte[] serialized() {
        var serialized = new byte[1 + SignalPreKeyMessageSpec.sizeOf(this)];
        serialized[0] = serializedVersion();
        SignalPreKeyMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
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

    public SignalMessage signalMessage() {
        return SignalMessage.ofSerialized(serializedSignalMessage);
    }

    public byte[] serializedSignalMessage() {
        return serializedSignalMessage;
    }

    public Integer preKeyId() {
        return preKeyId;
    }

    public byte[] baseKey() {
        return baseKey;
    }

    public byte[] identityKey() {
        return identityKey;
    }

    public Integer registrationId() {
        return registrationId;
    }

    public Integer signedPreKeyId() {
        return signedPreKeyId;
    }
}
