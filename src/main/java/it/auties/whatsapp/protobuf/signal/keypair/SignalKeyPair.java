package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.socket.Node;
import lombok.NonNull;
import org.whispersystems.libsignal.ecc.DjbECPrivateKey;
import org.whispersystems.libsignal.ecc.DjbECPublicKey;
import org.whispersystems.libsignal.util.KeyHelper;

import java.util.Arrays;

public record SignalKeyPair(byte @NonNull [] publicKey, byte @NonNull [] privateKey) implements ISignalKeyPair{
/*
private static final int PUBLIC_HEADER_LENGTH = 12;
private static final int PRIVATE_HEADER_LENGTH = 16;
private static final int KEY_SIZE = 32;

@SneakyThrows
public static SignalKeyPair random() {
var pair = KeyPairGenerator.getInstance("X25519").generateKeyPair();
var publicKey = copyOfRange(pair.getPublic().getEncoded(), PUBLIC_HEADER_LENGTH, PUBLIC_HEADER_LENGTH + KEY_SIZE);
var privateKey = copyOfRange(pair.getPrivate().getEncoded(), PRIVATE_HEADER_LENGTH, PRIVATE_HEADER_LENGTH + KEY_SIZE);
return new SignalKeyPair(publicKey, privateKey);
}
 */

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
