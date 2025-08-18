package it.auties.whatsapp.model.signal.keypair;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.SignalConstants;

public sealed interface ISignalKeyPair permits SignalKeyPair, SignalPreKeyPair, SignalSignedKeyPair {
    byte[] privateKey();

    Node toNode();

    byte[] publicKey();

    default byte[] signalPublicKey() {
        return SignalConstants.createSignalKey(publicKey());
    }
}
