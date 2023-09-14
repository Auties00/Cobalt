package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of media that an ad can wrap
 */
public enum ExternalAdReplyInfoMediaType implements ProtobufEnum {
    /**
     * No media
     */
    NONE(0),
    /**
     * Image
     */
    IMAGE(1),
    /**
     * Video
     */
    VIDEO(2);

    final int index;

    ExternalAdReplyInfoMediaType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
