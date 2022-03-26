package it.auties.whatsapp.model.signal.message;

import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.SignalSpecification;

public sealed interface SignalProtocolMessage extends SignalSpecification permits SignalPreKeyMessage, SignalDistributionMessage, SignalMessage {
    int version();
    byte[] serialized();

    default byte serializedVersion(){
        return BytesHelper.versionToBytes(version() == 0 ? CURRENT_VERSION : version());
    }
}
