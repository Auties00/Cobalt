package it.auties.whatsapp.protobuf.model;

import lombok.NonNull;

public record SignedKeyPair(byte @NonNull [] id, @NonNull IdentityKeyPair keyPair, byte @NonNull [] signature) {
    public Node encode(){
        return Node.withChildren("skey",
                Node.with("id", id),
                Node.with("value", keyPair.publicKey()),
                Node.with("signature", signature)
        );
    }
}
