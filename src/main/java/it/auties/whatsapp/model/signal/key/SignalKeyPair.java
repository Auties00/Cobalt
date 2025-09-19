package it.auties.whatsapp.model.signal.key;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage
public record SignalKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        SignalPublicKey publicKey,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        SignalPrivateKey privateKey
) implements ISignalKeyPair {
    public SignalKeyPair(SignalPublicKey publicKey, SignalPrivateKey privateKey) {
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey cannot be null");
        this.privateKey = privateKey;
    }

    public SignalKeyPair(SignalPublicKey publicKey) {
        this(publicKey, null);
    }

    public static SignalKeyPair random() {
        var privateKey = SignalPrivateKey.random();
        var publicKey = SignalPublicKey.of(privateKey);
        return new SignalKeyPair(publicKey, privateKey);
    }
}
