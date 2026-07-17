package com.github.auties00.cobalt.wire.linked.message.poll;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Describes the kind of content that poll options carry.
 *
 * <p>A poll may display its options as plain text labels or as images. This
 * enumeration lets clients distinguish between these presentations when
 * rendering the poll and when interpreting the payload of each option.
 */
@ProtobufEnum(name = "Message.PollContentType")
public enum PollContentType {
    /**
     * Indicates that the poll content type is not specified or is not recognised
     * by the current client. Treat options as text when this value is received.
     */
    UNKNOWN(0),
    /**
     * Indicates that poll options are plain text labels.
     */
    TEXT(1),
    /**
     * Indicates that poll options carry image content.
     */
    IMAGE(2);

    /**
     * Creates a new enum constant for the given protobuf index.
     *
     * @param index the protobuf wire index used for serialization
     */
    PollContentType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire index associated with this constant.
     */
    final int index;

    /**
     * Returns the protobuf wire index associated with this constant.
     *
     * @return the protobuf wire index
     */
    public int index() {
        return this.index;
    }
}
