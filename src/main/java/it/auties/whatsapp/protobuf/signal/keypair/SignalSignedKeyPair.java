package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.crypto.Curve;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.exchange.Node;
import lombok.NonNull;

public record SignalSignedKeyPair(int id, @NonNull SignalKeyPair keyPair, byte @NonNull [] signature) implements ISignalKeyPair{
    public static SignalSignedKeyPair of(int id, @NonNull SignalKeyPair identityKeyPair){
        var keyPair = SignalKeyPair.random();
        var signature = Curve.calculateSignature(identityKeyPair.privateKey(), keyPair.encodedPublicKey());
        return new SignalSignedKeyPair(id, keyPair, signature);
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return keyPair;
    }

    public Node toNode(){
        return Node.withChildren("skey",
                Node.with("id", encodedId()),
                Node.with("value", keyPair.publicKey()),
                Node.with("signature", signature)
        );
    }

    public byte[] encodedId(){
        return SignalHelper.toBytes(id, 3);
    }
}
