package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.curve25519.XecUtils;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.KeyHelper;
import lombok.NonNull;

import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.util.Arrays;

public record SignalKeyPair(byte @NonNull [] publicKey, byte[] privateKey) implements ISignalKeyPair{
    public SignalKeyPair(byte[] publicKey, byte[] privateKey) {
        this.publicKey = KeyHelper.withoutHeader(publicKey);
        this.privateKey = privateKey;
    }

    public static SignalKeyPair of(byte[] publicKey){
        return new SignalKeyPair(publicKey, null);
    }

    public static SignalKeyPair random(){
        var keyPair = Curve25519.randomKeyPair();
        var publicKey = XecUtils.toBytes((XECPublicKey) keyPair.getPublic());
        var privateKey = XecUtils.toBytes((XECPrivateKey) keyPair.getPrivate());
        return new SignalKeyPair(publicKey, privateKey);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SignalKeyPair that
                && Arrays.equals(publicKey(), that.publicKey()) && Arrays.equals(privateKey(), that.privateKey());
    }

    @Override
    public Node toNode() {
        throw new UnsupportedOperationException("Cannot serialize generic signal key pair");
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return this;
    }
}
