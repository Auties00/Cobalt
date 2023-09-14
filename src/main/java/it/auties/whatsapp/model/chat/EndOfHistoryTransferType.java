package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of transfers that can regard a
 * chat history sync
 */
public enum EndOfHistoryTransferType implements ProtobufEnum {
    /**
     * Complete, but more messages remain on the phone
     */
    COMPLETE_BUT_MORE_MESSAGES_REMAIN_ON_PRIMARY(0),

    /**
     * Complete and no more messages remain on the phone
     */
    COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY(1);

    final int index;

    EndOfHistoryTransferType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
