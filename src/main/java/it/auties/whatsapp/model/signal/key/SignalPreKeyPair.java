package it.auties.whatsapp.model.signal.key;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Scalar;

import java.util.Objects;

@ProtobufMessage
public record SignalPreKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int id,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        SignalKeyPair keyPair
) implements ISignalKeyPair {
    public SignalPreKeyPair {
        Objects.requireNonNull(keyPair, "keyPair cannot be null");
    }

    public static SignalPreKeyPair random(int id) {
        return new SignalPreKeyPair(id, SignalKeyPair.random());
    }

    public byte[] encodedId() {
        return Scalar.intToBytes(id(), 3);
    }

    @Override
    public SignalPublicKey publicKey() {
        return keyPair.publicKey();
    }

    @Override
    public SignalPrivateKey privateKey() {
        return keyPair.privateKey();
    }
}
