package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;

import java.util.Arrays;

@ProtobufMessage
public record SignalKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] publicKey,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] privateKey
) implements ISignalKeyPair {
    public SignalKeyPair(byte[] publicKey, byte[] privateKey) {
        this.publicKey = ISignalKeyPair.toCurveKey(publicKey);
        this.privateKey = privateKey;
    }

    public static SignalKeyPair of(byte[] publicKey) {
        return new SignalKeyPair(publicKey, null);
    }

    public static SignalKeyPair random() {
        var keyPair = Curve25519.randomKeyPair();
        var publicKey = Curve25519.readKey(keyPair.getPublic());
        var privateKey = Curve25519.readKey(keyPair.getPrivate());
        return new SignalKeyPair(publicKey, privateKey);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SignalKeyPair that && Arrays.equals(publicKey(), that.publicKey()) && Arrays.equals(privateKey(), that.privateKey());
    }

    @Override
    public Node toNode() {
        throw new UnsupportedOperationException("Cannot serialize generic signal key pair");
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return this;
    }
}
