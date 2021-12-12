package it.auties.whatsapp.protobuf.signal.key;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.exchange.Node;

public record PreKey(byte[] id, byte[] publicKey) {
    public static PreKey fromIndex(int index){
        return new PreKey(BinaryArray.of(index, 3).data(), IdentityKeyPair.random().publicKey());
    }

    public Node encode(){
        return Node.withChildren("key", Node.with("id", id), Node.with("value", publicKey));
    }
}
