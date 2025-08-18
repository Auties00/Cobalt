package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Scalar;
import it.auties.whatsapp.util.SignalConstants;

@ProtobufMessage
public record SignalPreKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int id,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] publicKey,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] privateKey
) implements ISignalKeyPair {
    public SignalPreKeyPair(
            int id,
            byte[] publicKey,
            byte[] privateKey) {
        this.id = id;
        this.publicKey = SignalConstants.createCurveKey(publicKey);
        this.privateKey = privateKey;
    }

    public static SignalPreKeyPair random(int id) {
        var privateKey = Curve25519.randomPrivateKey();
        var publicKey = Curve25519.getPublicKey(privateKey);
        return new SignalPreKeyPair(id, publicKey, privateKey);
    }

    @Override
    public Node toNode() {
        return Node.of("key", Node.of("id", encodedId()), Node.of("value", publicKey()));
    }

    public byte[] encodedId() {
        return Scalar.intToBytes(id(), 3);
    }

    public SignalKeyPair toGenericKeyPair() {
        return new SignalKeyPair(publicKey, privateKey);
    }
}
