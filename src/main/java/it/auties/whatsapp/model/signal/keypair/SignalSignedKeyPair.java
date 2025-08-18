package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Scalar;
import it.auties.whatsapp.util.SignalConstants;

import java.util.NoSuchElementException;
import java.util.Optional;

@ProtobufMessage
public record SignalSignedKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int id,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] publicKey,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] privateKey,
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte[] signature
) implements ISignalKeyPair {
    public SignalSignedKeyPair(
            int id,
            byte[] publicKey,
            byte[] privateKey,
            byte[] signature) {
        this.id = id;
        this.publicKey = SignalConstants.createCurveKey(publicKey);
        this.privateKey = privateKey;
        this.signature = signature;
    }

    public static SignalSignedKeyPair of(int id, SignalKeyPair identityKeyPair) {
        var privateKey = Curve25519.randomPrivateKey();
        var message = new byte[SignalConstants.KEY_LENGTH + 1];
        message[0] = SignalConstants.KEY_TYPE;
        Curve25519.getPublicKey(privateKey, message, 1);
        var signature = Curve25519.sign(identityKeyPair.privateKey(), message);
        return new SignalSignedKeyPair(id, message, privateKey, signature);
    }

    public static Optional<SignalSignedKeyPair> of(Node node) {
        if (node == null) {
            return Optional.empty();
        }
        var id = node.findChild("id")
                .flatMap(Node::contentAsBytes)
                .map(bytes -> Scalar.bytesToInt(bytes, 3))
                .orElseThrow(() -> new NoSuchElementException("Missing id in SignalSignedKeyPair"));
        var publicKey = node.findChild("value")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new NoSuchElementException("Missing publicKey in SignalSignedKeyPair"));
        var signature = node.findChild("signature")
                .flatMap(Node::contentAsBytes)
                .orElse(null);
        return Optional.of(new SignalSignedKeyPair(id, publicKey, null, signature));
    }

    @Override
    public Node toNode() {
        return Node.of("skey", Node.of("id", encodedId()), Node.of("value", publicKey()), Node.of("signature", signature()));
    }

    public byte[] encodedId() {
        return Scalar.intToBytes(id(), 3);
    }

    public SignalKeyPair toGenericKeyPair() {
        return new SignalKeyPair(publicKey, privateKey);
    }
}
