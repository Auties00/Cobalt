package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage")
public final class PeerDataOperationRequestResponseMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final PeerDataOperationRequestMessage.PeerDataOperationRequestType peerDataOperationRequestType;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String stanzaId;

    public PeerDataOperationRequestResponseMessage(PeerDataOperationRequestMessage.PeerDataOperationRequestType peerDataOperationRequestType, String stanzaId) {
        this.peerDataOperationRequestType = peerDataOperationRequestType;
        this.stanzaId = stanzaId;
    }

    public Optional<PeerDataOperationRequestMessage.PeerDataOperationRequestType> peerDataOperationRequestType() {
        return Optional.ofNullable(peerDataOperationRequestType);
    }
    
    public Optional<String> stanzaId() {
        return Optional.ofNullable(stanzaId);
    }
}
