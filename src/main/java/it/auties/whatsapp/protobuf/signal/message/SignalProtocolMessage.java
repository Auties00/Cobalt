package it.auties.whatsapp.protobuf.signal.message;

import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.util.VersionProvider;

public sealed interface SignalProtocolMessage extends VersionProvider permits SignalPreKeyMessage, SignalDistributionMessage, SignalMessage {
    int version();
    byte[] serialized();

    default byte serializedVersion(){
        return SignalHelper.serialize(version() == 0 ? CURRENT_VERSION : version());
    }
}
