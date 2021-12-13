package it.auties.whatsapp.protobuf.signal.key;

import lombok.NonNull;
import org.whispersystems.libsignal.ecc.DjbECPrivateKey;
import org.whispersystems.libsignal.ecc.DjbECPublicKey;
import org.whispersystems.libsignal.util.KeyHelper;

public record SignalKeyPair(byte @NonNull [] publicKey, byte @NonNull [] privateKey) {
    public static SignalKeyPair random(){
        // TODO: Why doesn't this work?
        // var pair = KeyPairGenerator.getInstance("X25519").generateKeyPair();
        // var privateKey = ((XECPrivateKey) pair.getPrivate()).getScalar().orElseThrow();
        // var publicKey = (X25519PublicKeyParameters) PublicKeyFactory.createKey(pair.getPublic().getEncoded());
        // return new IdentityKeyPair(publicKey.getEncoded(), privateKey);
        var pair = KeyHelper.generateIdentityKeyPair();
        var publicKey = (DjbECPublicKey) pair.getPublicKey().getPublicKey();
        var privateKey = (DjbECPrivateKey) pair.getPrivateKey();
        return new SignalKeyPair(publicKey.getPublicKey(), privateKey.getPrivateKey());
    }
}
