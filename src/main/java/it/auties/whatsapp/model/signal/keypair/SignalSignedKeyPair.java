package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.socket.Node;
import lombok.NonNull;

import java.util.Optional;

public record SignalSignedKeyPair(int id, @NonNull SignalKeyPair keyPair, byte[] signature) implements ISignalKeyPair{
    public static SignalSignedKeyPair of(int id, @NonNull SignalKeyPair identityKeyPair){
        var keyPair = SignalKeyPair.random();
        var signature = Curve25519.calculateSignature(identityKeyPair.privateKey(), keyPair.encodedPublicKey());
        return new SignalSignedKeyPair(id, keyPair, signature);
    }

    public static Optional<SignalSignedKeyPair> of(Node node){
        if(node == null){
            return Optional.empty();
        }

        var id = SignalHelper.fromBytes(node.findNode("id").bytes(), 3);
        var keyPair = new SignalKeyPair(node.findNode("value").bytes(), null);
        var signature = node.findNode("signature");
        return Optional.of(new SignalSignedKeyPair(id, keyPair, signature != null ? signature.bytes() : null));
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
