package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of attachment that a product
 * header can have
 */
public enum InteractiveHeaderAttachmentType implements ProtobufEnum {
    /**
     * No attachment
     */
    NONE(0),
    /**
     * Document message
     */
    DOCUMENT(3),
    /**
     * Image attachment
     */
    IMAGE(4),
    /**
     * Jpeg attachment
     */
    THUMBNAIL(6),
    /**
     * Video attachment
     */
    VIDEO(7);

    final int index;

    InteractiveHeaderAttachmentType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
