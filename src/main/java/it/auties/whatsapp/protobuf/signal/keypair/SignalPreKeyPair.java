package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.exchange.Node;

import static it.auties.whatsapp.binary.BinaryArray.of;

public record SignalPreKeyPair(int id, byte[] publicKey, byte[] privateKey) implements ISignalKeyPair{
    public static SignalPreKeyPair fromIndex(int index){
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
        return of(id, 3).data();
    }
}
