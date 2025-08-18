package it.auties.whatsapp.model.signal.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Bytes;

import java.util.Arrays;

@ProtobufMessage(name = "PreKeySignalMessage")
public final class SignalPreKeyMessage extends SignalProtocolMessage<SignalPreKeyMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    private final Integer preKeyId;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    private final byte[] baseKey;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private final byte[] identityKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private final byte[] serializedSignalMessage;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    private final Integer registrationId;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    private final Integer signedPreKeyId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SignalPreKeyMessage(Integer preKeyId, byte[] baseKey, byte[] identityKey, byte[] serializedSignalMessage, int registrationId, int signedPreKeyId) {
        this.preKeyId = preKeyId;
        this.baseKey = baseKey;
        this.identityKey = identityKey;
        this.serializedSignalMessage = serializedSignalMessage;
        this.registrationId = registrationId;
        this.signedPreKeyId = signedPreKeyId;
    }

    public static SignalPreKeyMessage ofSerialized(byte[] serialized) {
        return SignalPreKeyMessageSpec.decode(Arrays.copyOfRange(serialized, 1, serialized.length))
                .setVersion(Bytes.bytesToVersion(serialized[0]))
                .setSerialized(serialized);
    }

    @Override
    public byte[] serialized() {
        if (serialized == null) {
            this.serialized = Bytes.concat(serializedVersion(), SignalPreKeyMessageSpec.encode(this));
        }

        return serialized;
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
