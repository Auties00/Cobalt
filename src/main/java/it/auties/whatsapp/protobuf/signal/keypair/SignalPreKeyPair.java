package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.socket.Node;
import lombok.NonNull;

public record SignalPreKeyPair(int id, byte @NonNull [] publicKey, byte[] privateKey) implements ISignalKeyPair{
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
        return SignalHelper.toBytes(id, 3);
    }
}
