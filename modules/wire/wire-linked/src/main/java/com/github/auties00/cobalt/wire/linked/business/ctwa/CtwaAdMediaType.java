package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Media-kind classifier for a Click-to-WhatsApp ad creative.
 *
 * <p>The CTWA native-ad media-upload endpoint registers ad assets as
 * either still images or video clips. Each ad slot identifies its
 * payload by an opaque relay-allocated identifier paired with one of
 * these two enum values, so the rendering surface knows how to render
 * the asset.
 */
@ProtobufEnum
public enum CtwaAdMediaType {
    /**
     * Still-image creative, served as JPEG/PNG/WebP.
     */
    IMAGE(0),

    /**
     * Video creative, served as MP4.
     */
    VIDEO(1);

    /**
     * The protobuf wire-format index associated with this media type.
     */
    final int index;

    /**
     * Constructs a new {@code CtwaAdMediaType} with the supplied
     * protobuf index.
     *
     * @param index the protobuf wire-format index
     */
    CtwaAdMediaType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire-format index associated with this value.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }
}
