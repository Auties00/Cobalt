package it.auties.whatsapp.protobuf.key;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.CipherHelper;
import it.auties.whatsapp.protobuf.model.Node;

public record PreKey(byte[] id, byte[] publicKey) {
    public static PreKey fromIndex(int index){
        return new PreKey(BinaryArray.of(index, 3).data(), CipherHelper.randomKeyPair().publicKey());
    }

    public Node encode(){
        return Node.withChildren("key", Node.with("id", id), Node.with("value", publicKey));
    }
}
