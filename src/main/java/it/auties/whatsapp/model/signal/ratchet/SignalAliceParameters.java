package it.auties.whatsapp.model.signal.ratchet;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalKeyPair;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

@ProtobufMessage
public final class SignalAliceParameters {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourIdentityKey;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final SignalKeyPair ourBaseKey;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final SignalPublicKey theirIdentityKey;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final SignalPublicKey theirSignedPreKey;

    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    final SignalPublicKey theirOneTimePreKey;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    final SignalPublicKey theirRatchetKey;

    SignalAliceParameters(SignalKeyPair ourIdentityKey, SignalKeyPair ourBaseKey, SignalPublicKey theirIdentityKey, SignalPublicKey theirSignedPreKey, SignalPublicKey theirOneTimePreKey, SignalPublicKey theirRatchetKey) {
        this.ourIdentityKey = ourIdentityKey;
        this.ourBaseKey = ourBaseKey;
        this.theirIdentityKey = theirIdentityKey;
        this.theirSignedPreKey = theirSignedPreKey;
        this.theirOneTimePreKey = theirOneTimePreKey;
        this.theirRatchetKey = theirRatchetKey;
    }

    public SignalKeyPair ourIdentityKey() {
        return ourIdentityKey;
    }

    public SignalKeyPair ourBaseKey() {
        return ourBaseKey;
    }

    public SignalPublicKey theirIdentityKey() {
        return theirIdentityKey;
    }

    public SignalPublicKey theirSignedPreKey() {
        return theirSignedPreKey;
    }

    public SignalPublicKey theirOneTimePreKey() {
        return theirOneTimePreKey;
    }

    public SignalPublicKey theirRatchetKey() {
        return theirRatchetKey;
    }
}
