package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.exchange.Node;

public record SignalPreKey(byte[] id, byte[] publicKey) {
    public static SignalPreKey fromIndex(int index){
        return new SignalPreKey(BinaryArray.of(index, 3).data(), SignalKeyPair.random().publicKey());
    }

    public Node encode(){
        return Node.withChildren("key", Node.with("id", id), Node.with("value", publicKey));
    }
}
