package it.auties.whatsapp.protobuf.signal.message;

public sealed interface SignalProtocolMessage permits SignalPreKeyMessage, SignalDistributionMessage, SignalMessage {
}
