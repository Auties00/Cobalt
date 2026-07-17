package com.github.auties00.cobalt.wire.linked.business.cart;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Media wrapper attached to a {@link BusinessCartProduct} carrying the
 * visual assets associated with a WhatsApp Business catalogue product.
 *
 * <p>The wrapper currently exposes a single {@link BusinessCartProductImage}
 * thumbnail used to render the product line in the in-chat cart drawer.
 * The wrapper exists as a distinct level so future media types (videos,
 * 3D previews) can be added without breaking the cart line shape.
 *
 * <p>Present only when the merchant has attached at least one media asset
 * to the catalogue product.
 */
@ProtobufMessage
public final class BusinessCartProductMedia {
    /**
     * Thumbnail image rendered next to the product in the cart drawer.
     * Absent when the merchant has not configured any image for this
     * catalogue product.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    BusinessCartProductImage image;

    /**
     * Constructs a new {@code BusinessCartProductMedia} with the specified
     * thumbnail image. The parameter is optional and may be {@code null}
     * when no image is attached.
     *
     * @param image the thumbnail image, or {@code null}
     */
    BusinessCartProductMedia(BusinessCartProductImage image) {
        this.image = image;
    }

    /**
     * Returns the thumbnail image rendered next to the product in the cart
     * drawer.
     *
     * @return an {@code Optional} containing the image, or empty if no image
     *         is configured
     */
    public Optional<BusinessCartProductImage> image() {
        return Optional.ofNullable(image);
    }

    /**
     * Sets the thumbnail image rendered next to the product in the cart drawer.
     *
     * @param image the image to set, or {@code null} to clear
     */
    public void setImage(BusinessCartProductImage image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCartProductMedia) obj;
        return Objects.equals(this.image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image);
    }

    @Override
    public String toString() {
        return "BusinessCartProductMedia[" +
               "image=" + image + ']';
    }
}
