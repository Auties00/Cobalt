package it.auties.whatsapp.model.signal.keypair;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.node.Node;

@ProtobufMessage
public record SignalPreKeyPair(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        int id,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] publicKey,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] privateKey
) implements ISignalKeyPair {
    public SignalPreKeyPair(int id, byte[] publicKey, byte[] privateKey) {
        this.id = id;
        this.publicKey = ISignalKeyPair.toCurveKey(publicKey);
        this.privateKey = privateKey;
    }

    public static SignalPreKeyPair random(int id) {
        var keyPair = SignalKeyPair.random();
        return new SignalPreKeyPair(id, keyPair.publicKey(), keyPair.privateKey());
    }

    @Override
    public Node toNode() {
        return Node.of("key", Node.of("id", encodedId()), Node.of("value", publicKey()));
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return new SignalKeyPair(publicKey(), privateKey());
    }
}
