package it.auties.whatsapp.model.signal.ratchet;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalKeyPair;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

@ProtobufMessage
public final class SignalSymmetricParameters {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourBaseKey;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourRatchetKey;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourIdentityKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final SignalPublicKey theirBaseKey;

    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    final SignalPublicKey theirRatchetKey;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    final SignalPublicKey theirIdentityKey;

    SignalSymmetricParameters(SignalKeyPair ourBaseKey, SignalKeyPair ourRatchetKey, SignalKeyPair ourIdentityKey, SignalPublicKey theirBaseKey, SignalPublicKey theirRatchetKey, SignalPublicKey theirIdentityKey) {
        this.ourBaseKey = ourBaseKey;
        this.ourRatchetKey = ourRatchetKey;
        this.ourIdentityKey = ourIdentityKey;
        this.theirBaseKey = theirBaseKey;
        this.theirRatchetKey = theirRatchetKey;
        this.theirIdentityKey = theirIdentityKey;
    }

    public SignalKeyPair ourBaseKey() {
        return ourBaseKey;
    }

    public SignalKeyPair ourRatchetKey() {
        return ourRatchetKey;
    }

    public SignalKeyPair ourIdentityKey() {
        return ourIdentityKey;
    }

    public SignalPublicKey theirBaseKey() {
        return theirBaseKey;
    }

    public SignalPublicKey theirRatchetKey() {
        return theirRatchetKey;
    }

    public SignalPublicKey theirIdentityKey() {
        return theirIdentityKey;
    }
}
