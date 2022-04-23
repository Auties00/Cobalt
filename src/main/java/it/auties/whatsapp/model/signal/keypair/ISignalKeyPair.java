package it.auties.whatsapp.model.signal.keypair;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Keys;

public sealed interface ISignalKeyPair permits SignalKeyPair, SignalPreKeyPair, SignalSignedKeyPair {
    default int id(){
        throw new UnsupportedOperationException(getClass().getName() + " doesn't provide an id");
    }

    byte[] publicKey();
    byte[] privateKey();
    Node toNode();
    SignalKeyPair toGenericKeyPair();

    default byte[] encodedPublicKey(){
        return Keys.withHeader(publicKey());
    }

    default byte[] encodedId(){
        return BytesHelper.intToBytes(id(), 3);
    }
}
