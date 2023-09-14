package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of companion that a
 * {@link AdReplyInfo} can link to
 */
public enum AdReplyInfoMediaType implements ProtobufEnum {
    /**
     * Unknown type
     */
    NONE(0),
    /**
     * Image type
     */
    IMAGE(1),
    /**
     * Video type
     */
    VIDEO(2);

    final int index;

    AdReplyInfoMediaType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
