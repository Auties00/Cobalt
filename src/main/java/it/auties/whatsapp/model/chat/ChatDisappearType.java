package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various actors that can initialize
 * disappearing messages in a chat
 */
public enum ChatDisappearType implements ProtobufEnum {
    /**
     * Changed in chat
     */
    CHANGED_IN_CHAT(0),
    /**
     * Initiated by me
     */
    INITIATED_BY_ME(1),
    /**
     * Initiated by other
     */
    INITIATED_BY_OTHER(2);

    final int index;

    ChatDisappearType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
