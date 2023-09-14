package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * Enum representing the errorReason for a past participant leaving the chat.
 */
public enum ParticipantLeaveReason implements ProtobufEnum {
    /**
     * The past participant left the chat voluntarily.
     */
    LEFT(0),
    /**
     * The past participant was removed from the chat.
     */
    REMOVED(1);

    final int index;

    ParticipantLeaveReason(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
