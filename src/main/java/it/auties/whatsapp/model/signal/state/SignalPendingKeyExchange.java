package it.auties.whatsapp.model.signal.state;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.signal.key.SignalPrivateKey;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import java.util.Objects;

@ProtobufMessage
public final class SignalPendingKeyExchange {
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final int sequence;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final SignalPublicKey localBaseKey;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final SignalPrivateKey localBaseKeyPrivate;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final SignalPublicKey localRatchetKey;

    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    final SignalPrivateKey localRatchetKeyPrivate;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    final SignalPublicKey localIdentityKey;

    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    final SignalPrivateKey localIdentityKeyPrivate;

    SignalPendingKeyExchange(int sequence, SignalPublicKey localBaseKey, SignalPrivateKey localBaseKeyPrivate, SignalPublicKey localRatchetKey, SignalPrivateKey localRatchetKeyPrivate, SignalPublicKey localIdentityKey, SignalPrivateKey localIdentityKeyPrivate) {
        this.sequence = sequence;
        this.localBaseKey = localBaseKey;
        this.localBaseKeyPrivate = localBaseKeyPrivate;
        this.localRatchetKey = localRatchetKey;
        this.localRatchetKeyPrivate = localRatchetKeyPrivate;
        this.localIdentityKey = localIdentityKey;
        this.localIdentityKeyPrivate = localIdentityKeyPrivate;
    }

    public int sequence() {
        return sequence;
    }

    public SignalPublicKey localBaseKey() {
        return localBaseKey;
    }

    public SignalPrivateKey localBaseKeyPrivate() {
        return localBaseKeyPrivate;
    }

    public SignalPublicKey localRatchetKey() {
        return localRatchetKey;
    }

    public SignalPrivateKey localRatchetKeyPrivate() {
        return localRatchetKeyPrivate;
    }

    public SignalPublicKey localIdentityKey() {
        return localIdentityKey;
    }

    public SignalPrivateKey localIdentityKeyPrivate() {
        return localIdentityKeyPrivate;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SignalPendingKeyExchange that
                && sequence == that.sequence
                && Objects.equals(localBaseKey, that.localBaseKey)
                && Objects.equals(localBaseKeyPrivate, that.localBaseKeyPrivate)
                && Objects.equals(localRatchetKey, that.localRatchetKey)
                && Objects.equals(localRatchetKeyPrivate, that.localRatchetKeyPrivate)
                && Objects.equals(localIdentityKey, that.localIdentityKey)
                && Objects.equals(localIdentityKeyPrivate, that.localIdentityKeyPrivate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, localBaseKey, localBaseKeyPrivate, localRatchetKey, localRatchetKeyPrivate, localIdentityKey, localIdentityKeyPrivate);
    }
}