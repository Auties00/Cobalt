package it.auties.whatsapp.protobuf.signal.key;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.exchange.Node;
import lombok.NonNull;

public record SignalSignedKeyPair(byte @NonNull [] id, @NonNull SignalKeyPair keyPair, byte @NonNull [] signature) {
    public static SignalSignedKeyPair with(int id, @NonNull SignalKeyPair identityKeyPair){
        var encodedId = BinaryArray.of(id, 3).data();
        var keyPair = SignalKeyPair.random();
        var publicKey = BinaryArray.of((byte) 5).append(keyPair.publicKey()).data();
        var signature = Curve.calculateSignature(identityKeyPair.privateKey(), publicKey);
        return new SignalSignedKeyPair(encodedId, keyPair, signature);
    }

    public Node encode(){
        return Node.withChildren("skey",
                Node.with("id", id),
                Node.with("value", keyPair.publicKey()),
                Node.with("signature", signature)
        );
    }
}
