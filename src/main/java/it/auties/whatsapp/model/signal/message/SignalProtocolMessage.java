package it.auties.whatsapp.model.signal.message;

import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.SignalConstants;

public abstract sealed class SignalProtocolMessage<T extends SignalProtocolMessage<T>> permits SenderKeyMessage, SignalDistributionMessage, SignalMessage, SignalPreKeyMessage {
    protected byte[] serialized;
    private int version;

    public SignalProtocolMessage() {
        this.version = SignalConstants.CURRENT_VERSION;
    }

    public SignalProtocolMessage(int version, byte[] serialized) {
        this.version = version;
        this.serialized = serialized;
    }

    public int version() {
        return version;
    }

    @SuppressWarnings("unchecked")
    public T setVersion(int version) {
        this.version = version;
        return (T) this;
    }

    public byte serializedVersion() {
        return Bytes.versionToBytes(version);
    }

    @SuppressWarnings("unchecked")
    public T setSerialized(byte[] serialized) {
        this.serialized = serialized;
        return (T) this;
    }

    public abstract byte[] serialized();
}
