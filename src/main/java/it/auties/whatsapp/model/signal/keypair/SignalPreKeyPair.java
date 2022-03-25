package it.auties.whatsapp.model.signal.keypair;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Keys;
import lombok.NonNull;

public record SignalPreKeyPair(int id, byte @NonNull [] publicKey, byte[] privateKey) implements ISignalKeyPair{
    public SignalPreKeyPair(int id, byte[] publicKey, byte[] privateKey){
        this.id = id;
        this.publicKey = Keys.withoutHeader(publicKey);
        this.privateKey = privateKey;
    }

    public static SignalPreKeyPair ofIndex(int index){
        var keyPair = SignalKeyPair.random();
        return new SignalPreKeyPair(index, keyPair.publicKey(), keyPair.privateKey());
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return new SignalKeyPair(publicKey, privateKey);
    }

    @Override
    public Node toNode(){
        return Node.withChildren("key", Node.with("id", encodedId()),
                Node.with("value", publicKey));
    }

    public byte[] encodedId(){
        return BytesHelper.toBytes(id, 3);
    }
}
