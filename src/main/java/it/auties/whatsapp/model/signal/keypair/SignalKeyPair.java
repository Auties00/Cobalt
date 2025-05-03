package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.SignalConstants;

import java.util.Arrays;

@ProtobufMessage
public record SignalKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] publicKey,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] privateKey
) implements ISignalKeyPair {
    public SignalKeyPair(
            byte[] publicKey,
            byte[] privateKey) {
        this.publicKey = SignalConstants.createCurveKey(publicKey);
        this.privateKey = privateKey;
    }

    public static SignalKeyPair of(byte[] publicKey) {
        return new SignalKeyPair(publicKey, null);
    }

    public static SignalKeyPair random() {
        var privateKey = Curve25519.randomPrivateKey();
        var publicKey = Curve25519.getPublicKey(privateKey);
        return new SignalKeyPair(publicKey, privateKey);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SignalKeyPair(var otherPublicKey, var otherPrivateKey)
                && Arrays.equals(publicKey, otherPublicKey)
                && Arrays.equals(privateKey, otherPrivateKey);
    }

    @Override
    public Node toNode() {
        return Node.of("key", Node.of("value", publicKey()));
    }
}
