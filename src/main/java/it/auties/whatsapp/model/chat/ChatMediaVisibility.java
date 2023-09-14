package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of media visibility that can be
 * set for a chat
 */
public enum ChatMediaVisibility implements ProtobufEnum {
    /**
     * Default
     */
    DEFAULT(0),
    /**
     * Off
     */
    OFF(1),
    /**
     * On
     */
    ON(2);

    final int index;
    ChatMediaVisibility(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
