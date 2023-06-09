package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Spec;

public sealed interface SignalProtocolMessage extends ProtobufMessage permits SenderKeyMessage, SignalDistributionMessage, SignalMessage, SignalPreKeyMessage {
    int version();

    byte[] serialized();

    default byte serializedVersion() {
        return BytesHelper.versionToBytes(version() == 0 ? Spec.Signal.CURRENT_VERSION : version());
    }
}
