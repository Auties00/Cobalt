package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.Bytes;

import java.util.NoSuchElementException;
import java.util.Optional;

@ProtobufMessage
public record SignalSignedKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int id,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        SignalKeyPair keyPair,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] signature
) implements ISignalKeyPair {
    public static SignalSignedKeyPair of(int id, SignalKeyPair identityKeyPair) {
        var keyPair = SignalKeyPair.random();
        var signature = Curve25519.sign(identityKeyPair.privateKey(), keyPair.signalPublicKey(), true);
        return new SignalSignedKeyPair(id, keyPair, signature);
    }

    public static Optional<SignalSignedKeyPair> of(Node node) {
        if (node == null) {
            return Optional.empty();
        }
        var id = node.findChild("id")
                .flatMap(Node::contentAsBytes)
                .map(bytes -> Bytes.bytesToInt(bytes, 3))
                .orElseThrow(() -> new NoSuchElementException("Missing id in SignalSignedKeyPair"));
        var publicKey = node.findChild("value")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new NoSuchElementException("Missing publicKey in SignalSignedKeyPair"));
        var keyPair = new SignalKeyPair(publicKey, null);
        var signature = node.findChild("signature").flatMap(Node::contentAsBytes).orElse(null);
        return Optional.of(new SignalSignedKeyPair(id, keyPair, signature));
    }

    @Override
    public byte[] privateKey() {
        return keyPair.privateKey();
    }

    public Node toNode() {
        return Node.of("skey", Node.of("id", encodedId()), Node.of("value", publicKey()), Node.of("signature", signature()));
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return keyPair();
    }

    @Override
    public byte[] publicKey() {
        return keyPair.publicKey();
    }
}
