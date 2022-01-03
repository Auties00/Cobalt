package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.exchange.Node;

public sealed interface ISignalKeyPair permits SignalKeyPair, SignalPreKeyPair, SignalSignedKeyPair{
    Node toNode();
    SignalKeyPair toGenericKeyPair();
}
