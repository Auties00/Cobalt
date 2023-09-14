package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of content that an interactive
 * message can wrap
 */
public enum InteractiveMessageContentType implements ProtobufEnum {
    /**
     * No content
     */
    NONE(0),
    /**
     * Shop
     */
    SHOP(1),
    /**
     * Collection
     */
    COLLECTION(2),
    /**
     * Native flow
     */
    NATIVE_FLOW(3);

    final int index;

    InteractiveMessageContentType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
