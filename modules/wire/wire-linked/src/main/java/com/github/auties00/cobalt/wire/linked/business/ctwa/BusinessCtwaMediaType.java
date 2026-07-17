package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Media-asset type carried in a Click-to-WhatsApp (CTWA) ad context.
 *
 * <p>When a WhatsApp user opens a chat by tapping a Facebook or Instagram
 * "Click to WhatsApp" ad, the resulting conversation carries an attached
 * ad context that may reference either a still image or a video creative.
 * This enum captures that two-state media discriminator so callers can
 * branch on the asset kind without inspecting the underlying CTWA payload.
 *
 * <p>The discriminator is populated only when the ad context actually
 * carries a thumbnail; if the upstream response has no thumbnail child the
 * media type is omitted from the parent {@link BusinessCtwaContext}.
 */
@ProtobufEnum
public enum BusinessCtwaMediaType {
    /**
     * The CTWA ad creative is a still image. Selected when the upstream
     * response carries a thumbnail child but no video sub-asset.
     */
    IMAGE(0),

    /**
     * The CTWA ad creative is a video. Selected when the upstream response
     * carries a thumbnail with an inner video URL grandchild.
     */
    VIDEO(1);

    /**
     * The wire-level integer index assigned to this enum constant by the
     * protobuf encoding.
     */
    final int index;

    /**
     * Constructs a {@code BusinessCtwaMediaType} bound to the given
     * protobuf wire index.
     *
     * @param index the protobuf wire index for this constant
     */
    BusinessCtwaMediaType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire index assigned to this constant.
     *
     * @return the protobuf wire index
     */
    public int index() {
        return index;
    }
}
