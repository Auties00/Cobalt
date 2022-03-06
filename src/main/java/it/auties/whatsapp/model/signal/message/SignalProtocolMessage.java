package it.auties.whatsapp.model.signal.message;

import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.util.SignalSpec;

public sealed interface SignalProtocolMessage extends SignalSpec permits SignalPreKeyMessage, SignalDistributionMessage, SignalMessage {
    int version();
    byte[] serialized();

    default byte serializedVersion(){
        return SignalHelper.serialize(version() == 0 ? CURRENT_VERSION : version());
    }
}
