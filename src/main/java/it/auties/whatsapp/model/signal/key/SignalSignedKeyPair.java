package it.auties.whatsapp.model.signal.key;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Scalar;

import java.util.Objects;

@ProtobufMessage
public record SignalSignedKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int id,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        SignalKeyPair keyPair,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] signature
) implements ISignalKeyPair {
    public SignalSignedKeyPair {
        Objects.requireNonNull(signature, "signature cannot be null");
        Objects.requireNonNull(keyPair, "keyPair cannot be null");
    }

    public static SignalSignedKeyPair of(int id, SignalKeyPair signatureKey) {
        var keypair = SignalKeyPair.random();
        var privateKey = signatureKey.privateKey().encodedPoint();
        var message = keypair.publicKey().serialized();
        var signature = Curve25519.sign(privateKey, message);
        return new SignalSignedKeyPair(id, keypair, signature);
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
