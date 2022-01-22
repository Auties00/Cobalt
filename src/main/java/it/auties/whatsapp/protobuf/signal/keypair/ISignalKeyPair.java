package it.auties.whatsapp.protobuf.signal.keypair;

import it.auties.whatsapp.socket.Node;

public sealed interface ISignalKeyPair permits SignalKeyPair, SignalPreKeyPair, SignalSignedKeyPair{
    Node toNode();
    SignalKeyPair toGenericKeyPair();
}
