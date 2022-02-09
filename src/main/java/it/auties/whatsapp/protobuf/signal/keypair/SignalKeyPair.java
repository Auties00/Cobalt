package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.socket.Node;
import lombok.NonNull;
import org.whispersystems.libsignal.ecc.DjbECPrivateKey;
import org.whispersystems.libsignal.ecc.DjbECPublicKey;
import org.whispersystems.libsignal.util.KeyHelper;

import java.util.Arrays;

public record SignalKeyPair(byte @NonNull [] publicKey, byte[] privateKey) implements ISignalKeyPair{
    public static SignalKeyPair random(){
        var pair = KeyHelper.generateIdentityKeyPair();
        var publicKey = (DjbECPublicKey) pair.getPublicKey().getPublicKey();
        var privateKey = (DjbECPrivateKey) pair.getPrivateKey();
        return new SignalKeyPair(publicKey.getPublicKey(), privateKey.getPrivateKey());
    }

    public byte[] encodedPublicKey(){
        return SignalHelper.appendKeyHeader(publicKey);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SignalKeyPair that
                && Arrays.equals(publicKey, that.publicKey) && Arrays.equals(privateKey, that.privateKey);
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
