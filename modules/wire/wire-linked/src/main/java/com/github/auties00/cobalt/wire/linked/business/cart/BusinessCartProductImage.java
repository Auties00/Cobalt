package com.github.auties00.cobalt.wire.linked.business.cart;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Image attached to a {@link BusinessCartProductMedia} block within a
 * WhatsApp Business shopping cart line item.
 *
 * <p>When a merchant's catalogue product has an image, the server includes
 * a thumbnail descriptor on every refreshed cart entry so the client can
 * render the product visually without re-fetching the full catalogue. The
 * descriptor either references an opaque server-side image identifier or
 * carries a request-resolved URL the client can load directly; either or
 * both fields may be present depending on how the catalogue media is
 * stored.
 */
@ProtobufMessage
public final class BusinessCartProductImage {
    /**
     * Opaque server-side image identifier the client can resolve via the
     * media-download pipeline. Absent when the catalogue product image is
     * exposed only as a direct URL or when the merchant has not configured
     * an image at all.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * Pre-resolved image URL the client can fetch directly without going
     * through the media-download pipeline. Absent when the catalogue
     * exposes the image only via {@link #id()} or when no image is
     * configured.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String requestImageUrl;

    /**
     * Constructs a new {@code BusinessCartProductImage} with the specified
     * image references. Both parameters are optional and may be {@code null}
     * when the corresponding reference is not present.
     *
     * @param id              the opaque server-side image identifier, or {@code null}
     * @param requestImageUrl the pre-resolved image URL, or {@code null}
     */
    BusinessCartProductImage(String id, String requestImageUrl) {
        this.id = id;
        this.requestImageUrl = requestImageUrl;
    }

    /**
     * Returns the opaque server-side image identifier that the client resolves
     * via the media-download pipeline.
     *
     * @return an {@code Optional} containing the identifier, or empty if the
     *         image is exposed only as a direct URL
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the pre-resolved image URL that the client can fetch directly
     * without going through the media-download pipeline.
     *
     * @return an {@code Optional} containing the URL, or empty if the image is
     *         exposed only via {@link #id()}
     */
    public Optional<String> requestImageUrl() {
        return Optional.ofNullable(requestImageUrl);
    }

    /**
     * Sets the opaque server-side image identifier.
     *
     * @param id the identifier to set, or {@code null} to clear
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the pre-resolved image URL.
     *
     * @param requestImageUrl the URL to set, or {@code null} to clear
     */
    public void setRequestImageUrl(String requestImageUrl) {
        this.requestImageUrl = requestImageUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCartProductImage) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.requestImageUrl, that.requestImageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requestImageUrl);
    }

    @Override
    public String toString() {
        return "BusinessCartProductImage[" +
               "id=" + id + ", " +
               "requestImageUrl=" + requestImageUrl + ']';
    }
}
