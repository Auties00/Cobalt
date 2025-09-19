package it.auties.whatsapp.model.signal.state;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

@ProtobufMessage
public final class SignalPreKeyBundle {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final int registrationId;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final int deviceId;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    final int preKeyId;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final SignalPublicKey preKeyPublic;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    final int signedPreKeyId;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    final SignalPublicKey signedPreKeyPublic;

    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    final byte[] signedPreKeySignature;

    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    final SignalPublicKey identityKey;

    SignalPreKeyBundle(int registrationId, int deviceId, int preKeyId, SignalPublicKey preKeyPublic, int signedPreKeyId, SignalPublicKey signedPreKeyPublic, byte[] signedPreKeySignature, SignalPublicKey identityKey) {
        this.registrationId = registrationId;
        this.deviceId = deviceId;
        this.preKeyId = preKeyId;
        this.preKeyPublic = preKeyPublic;
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublic = signedPreKeyPublic;
        this.signedPreKeySignature = signedPreKeySignature;
        this.identityKey = identityKey;
    }

    public int registrationId() {
        return registrationId;
    }

    public int deviceId() {
        return deviceId;
    }

    public int preKeyId() {
        return preKeyId;
    }

    public SignalPublicKey preKeyPublic() {
        return preKeyPublic;
    }

    public int signedPreKeyId() {
        return signedPreKeyId;
    }

    public SignalPublicKey signedPreKeyPublic() {
        return signedPreKeyPublic;
    }

    public byte[] signedPreKeySignature() {
        return signedPreKeySignature;
    }

    public SignalPublicKey identityKey() {
        return identityKey;
    }
}
