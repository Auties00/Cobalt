package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of headers that a {@link ButtonsMessage} can have
 */
public enum ButtonsMessageHeaderType implements ProtobufEnum {
    /**
     * Unknown
     */
    UNKNOWN(0),
    /**
     * Empty
     */
    EMPTY(1),
    /**
     * Text message
     */
    TEXT(2),
    /**
     * Document message
     */
    DOCUMENT(3),
    /**
     * Image message
     */
    IMAGE(4),
    /**
     * Video message
     */
    VIDEO(5),
    /**
     * Location message
     */
    LOCATION(6);

    final int index;
    ButtonsMessageHeaderType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public boolean hasMedia() {
        return this == DOCUMENT
                || this == IMAGE
                || this == VIDEO;
    }
}
