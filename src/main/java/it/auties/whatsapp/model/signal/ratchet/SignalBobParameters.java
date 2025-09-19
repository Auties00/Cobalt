package it.auties.whatsapp.model.signal.ratchet;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalKeyPair;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

@ProtobufMessage
public final class SignalBobParameters {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourIdentityKey;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourSignedPreKey;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourOneTimePreKey;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourRatchetKey;

    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    final SignalPublicKey theirIdentityKey;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    final SignalPublicKey theirBaseKey;

    SignalBobParameters(SignalKeyPair ourIdentityKey, SignalKeyPair ourSignedPreKey, SignalKeyPair ourOneTimePreKey, SignalKeyPair ourRatchetKey, SignalPublicKey theirIdentityKey, SignalPublicKey theirBaseKey) {
        this.ourIdentityKey = ourIdentityKey;
        this.ourSignedPreKey = ourSignedPreKey;
        this.ourOneTimePreKey = ourOneTimePreKey;
        this.ourRatchetKey = ourRatchetKey;
        this.theirIdentityKey = theirIdentityKey;
        this.theirBaseKey = theirBaseKey;
    }

    public SignalKeyPair ourIdentityKey() {
        return ourIdentityKey;
    }

    public SignalKeyPair ourSignedPreKey() {
        return ourSignedPreKey;
    }

    public SignalKeyPair ourOneTimePreKey() {
        return ourOneTimePreKey;
    }

    public SignalKeyPair ourRatchetKey() {
        return ourRatchetKey;
    }

    public SignalPublicKey theirIdentityKey() {
        return theirIdentityKey;
    }

    public SignalPublicKey theirBaseKey() {
        return theirBaseKey;
    }
}
