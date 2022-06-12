package it.auties.whatsapp.model.signal.keypair;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.KeyHelper;
import lombok.NonNull;

public record SignalPreKeyPair(int id, byte @NonNull [] publicKey, byte[] privateKey) implements ISignalKeyPair {
    public SignalPreKeyPair(int id, byte[] publicKey, byte[] privateKey) {
        this.id = id;
        this.publicKey = KeyHelper.withoutHeader(publicKey);
        this.privateKey = privateKey;
    }

    public static SignalPreKeyPair random(int id) {
        var keyPair = SignalKeyPair.random();
        return new SignalPreKeyPair(id, keyPair.publicKey(), keyPair.privateKey());
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return new SignalKeyPair(publicKey(), privateKey());
    }

    @Override
    public Node toNode() {
        return Node.withChildren("key", Node.with("id", encodedId()), Node.with("value", publicKey()));
    }
}
