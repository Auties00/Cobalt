package it.auties.whatsapp.model.media;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The constants of this enumerated type describe the various types of media visibility that can be
 * set for a chat
 */
@ProtobufEnum(name = "MediaVisibility")
public enum MediaVisibility {
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

    MediaVisibility(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
