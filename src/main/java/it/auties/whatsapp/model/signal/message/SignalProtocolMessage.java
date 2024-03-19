package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Specification;

public abstract sealed class SignalProtocolMessage<T extends SignalProtocolMessage<T>> implements ProtobufMessage permits SenderKeyMessage, SignalDistributionMessage, SignalMessage, SignalPreKeyMessage {
    private int version;
    protected byte[] serialized;

    public SignalProtocolMessage() {
        this.version = Specification.Signal.CURRENT_VERSION;
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
