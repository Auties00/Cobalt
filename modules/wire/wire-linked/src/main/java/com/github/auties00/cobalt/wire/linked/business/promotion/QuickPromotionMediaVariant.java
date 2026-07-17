package com.github.auties00.cobalt.wire.linked.business.promotion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Banner image variant of a WhatsApp quick-promotion creative for one
 * client colour mode.
 *
 * <p>WhatsApp ships separate light-mode and dark-mode banner images for
 * each quick-promotion creative so the banner renders correctly against
 * the surrounding theme. This model is one of those variants: a
 * base64-encoded JPEG thumbnail the client renders inline.
 */
@ProtobufMessage(name = "QuickPromotionMediaVariant")
public final class QuickPromotionMediaVariant {
    /**
     * Base64-encoded JPEG thumbnail, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String jpegThumbnail;

    /**
     * Constructs a new {@code QuickPromotionMediaVariant}.
     *
     * @param jpegThumbnail the base64-encoded JPEG thumbnail, or
     *                      {@code null} when the server omitted it
     */
    QuickPromotionMediaVariant(String jpegThumbnail) {
        this.jpegThumbnail = jpegThumbnail;
    }

    /**
     * Returns the base64-encoded JPEG thumbnail.
     *
     * @return an {@code Optional} carrying the encoded thumbnail, or
     *         empty when the server omitted it
     */
    public Optional<String> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }
}
