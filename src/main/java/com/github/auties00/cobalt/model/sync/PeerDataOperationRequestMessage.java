package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "Message.PeerDataOperationRequestMessage")
public final class PeerDataOperationRequestMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final PeerDataOperationRequestType peerDataOperationRequestType;

    public PeerDataOperationRequestMessage(PeerDataOperationRequestType peerDataOperationRequestType) {
        this.peerDataOperationRequestType = peerDataOperationRequestType;
    }

    public Optional<PeerDataOperationRequestType> peerDataOperationRequestType() {
        return Optional.ofNullable(peerDataOperationRequestType);
    }
    
    @ProtobufEnum(name = "Message.PeerDataOperationRequestType")
    public enum PeerDataOperationRequestType {
        UPLOAD_STICKER(0),
        SEND_RECENT_STICKER_BOOTSTRAP(1),
        GENERATE_LINK_PREVIEW(2),
        HISTORY_SYNC_ON_DEMAND(3),
        PLACEHOLDER_MESSAGE_RESEND(4),
        FULL_HISTORY_SYNC_ON_DEMAND(5),
        SYNCD_COLLECTION_FATAL_RECOVERY(6),
        HISTORY_SYNC_CHUNK_RETRY(7),
        GALAXY_FLOW_ACTION(8);

        final int index;

        PeerDataOperationRequestType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
