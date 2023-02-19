package it.auties.whatsapp.model.signal.keypair;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.KeyHelper;

public sealed interface ISignalKeyPair permits SignalKeyPair, SignalPreKeyPair, SignalSignedKeyPair {
    byte[] privateKey();

    Node toNode();

    SignalKeyPair toGenericKeyPair();

    default byte[] encodedPublicKey() {
        return KeyHelper.withHeader(publicKey());
    }

    byte[] publicKey();

    default byte[] encodedId() {
        return BytesHelper.intToBytes(id(), 3);
    }

    default int id() {
        throw new UnsupportedOperationException(getClass().getName() + " doesn't provide an id");
    }
}
