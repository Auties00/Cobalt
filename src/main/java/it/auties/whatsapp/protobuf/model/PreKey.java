package it.auties.whatsapp.protobuf.model;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.cipher.Cipher;

public record PreKey(byte[] id, byte[] publicKey) {
    public static PreKey fromIndex(int index){
        return new PreKey(BinaryArray.of(index, 3).data(), Cipher.randomKeyPair().publicKey());
    }

    public Node encode(){
        return Node.withChildren("key", Node.with("id", id), Node.with("value", publicKey));
    }
}
