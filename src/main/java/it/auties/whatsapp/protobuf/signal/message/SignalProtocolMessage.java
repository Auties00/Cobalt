package it.auties.whatsapp.protobuf.signal.message;

import it.auties.whatsapp.crypto.SignalHelper;

public sealed interface SignalProtocolMessage permits SignalPreKeyMessage, SignalDistributionMessage, SignalMessage {
    int CURRENT_VERSION = 3;

    int version();
    byte[] serialized();

    default byte serializedVersion(){
        return SignalHelper.serialize(version() == 0 ? CURRENT_VERSION : version());
    }
}
