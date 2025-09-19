package it.auties.whatsapp.model.signal.state;


import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import java.util.Objects;

@ProtobufMessage
public final class SignalPendingPreKey {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer preKeyId;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final SignalPublicKey baseKey;

    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final Integer signedKeyId;

    SignalPendingPreKey(Integer preKeyId, SignalPublicKey baseKey, Integer signedKeyId) {
        this.preKeyId = preKeyId;
        this.baseKey = baseKey;
        this.signedKeyId = signedKeyId;
    }

    public Integer preKeyId() {
        return preKeyId;
    }

    public SignalPublicKey baseKey() {
        return baseKey;
    }

    public Integer signedKeyId() {
        return signedKeyId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SignalPendingPreKey that
                && Objects.equals(preKeyId, that.preKeyId)
                && Objects.equals(baseKey, that.baseKey)
                && Objects.equals(signedKeyId, that.signedKeyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preKeyId, baseKey, signedKeyId);
    }
}