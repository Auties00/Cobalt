package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.exchange.Node;
import lombok.NonNull;

import static it.auties.whatsapp.binary.BinaryArray.of;

public record SignalSignedKeyPair(int id, @NonNull SignalKeyPair keyPair, byte @NonNull [] signature) {
    public static SignalSignedKeyPair with(int id, @NonNull SignalKeyPair identityKeyPair){
        var keyPair = SignalKeyPair.random();
        var publicKey = of((byte) 5).append(keyPair.publicKey()).data();
        var signature = Curve.calculateSignature(identityKeyPair.privateKey(), publicKey);
        return new SignalSignedKeyPair(id, keyPair, signature);
    }

    public Node encode(){
        return Node.withChildren("skey",
                Node.with("id", id),
                Node.with("value", keyPair.publicKey()),
                Node.with("signature", signature)
        );
    }

    public byte[] encodedId(){
        return of(id, 3).data();
    }
}
