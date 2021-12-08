package it.auties.whatsapp.protobuf.key;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.protobuf.model.Node;
import lombok.NonNull;

public record SignedKeyPair(byte @NonNull [] id, @NonNull IdentityKeyPair keyPair, byte @NonNull [] signature) {
    public static SignedKeyPair with(int id, @NonNull IdentityKeyPair identityKeyPair){
        var encodedId = BinaryArray.of(id, 3).data();
        var keyPair = IdentityKeyPair.random();
        var publicKey = BinaryArray.of((byte) 5).append(keyPair.publicKey()).data();
        var signature = Curve.calculateSignature(identityKeyPair.privateKey(), publicKey);
        return new SignedKeyPair(encodedId, keyPair, signature);
    }

    public Node encode(){
        return Node.withChildren("skey",
                Node.with("id", id),
                Node.with("value", keyPair.publicKey()),
                Node.with("signature", signature)
        );
    }
}
