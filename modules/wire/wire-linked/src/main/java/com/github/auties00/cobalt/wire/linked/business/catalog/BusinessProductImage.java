package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Objects;

/**
 * One image asset attached to a {@link BusinessProduct} in a WhatsApp
 * Business catalog.
 *
 * <p>Catalog products carry a primary image and optionally several
 * additional ones; each entry is identified by an opaque server id and
 * exposes two CDN URLs — a {@linkplain #requestedUri() resized URL}
 * matching the dimensions the client asked for at query time and a
 * {@linkplain #fullUri() full-resolution URL} pointing at the original
 * upload. Callers typically render the resized URL in product
 * thumbnails and switch to the full URL when the user opens the
 * product detail view.
 */
@ProtobufMessage(name = "BusinessProductImage")
public final class BusinessProductImage {
    /**
     * Opaque server-assigned identifier for this image asset. Used by
     * downstream surfaces (cart, order, product message) to refer back
     * to the same image without re-uploading the bytes.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * CDN URL of the image rendered at the dimensions the client
     * supplied at query time. Suitable for thumbnail-grade rendering.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final URI requestedUri;

    /**
     * CDN URL of the original-resolution image upload. Suitable for
     * full-screen rendering.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final URI fullUri;

    /**
     * Constructs a new {@code BusinessProductImage} entry. Every
     * argument is required since the WhatsApp catalog backend always
     * publishes the full triple.
     *
     * @param id           the opaque image identifier; never {@code null}
     * @param requestedUri the per-request resized CDN URL; never {@code null}
     * @param fullUri      the original-resolution CDN URL; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    BusinessProductImage(String id, URI requestedUri, URI fullUri) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.requestedUri = Objects.requireNonNull(requestedUri, "requestedUri cannot be null");
        this.fullUri = Objects.requireNonNull(fullUri, "fullUri cannot be null");
    }

    /**
     * Returns the opaque server-assigned identifier for this image.
     *
     * @return the image id; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the per-request resized CDN URL.
     *
     * @return the resized URL; never {@code null}
     */
    public URI requestedUri() {
        return requestedUri;
    }

    /**
     * Returns the original-resolution CDN URL.
     *
     * @return the full URL; never {@code null}
     */
    public URI fullUri() {
        return fullUri;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessProductImage) obj;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.requestedUri, that.requestedUri)
                && Objects.equals(this.fullUri, that.fullUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requestedUri, fullUri);
    }

    @Override
    public String toString() {
        return "BusinessProductImage[" +
                "id=" + id + ", " +
                "requestedUri=" + requestedUri + ", " +
                "fullUri=" + fullUri + ']';
    }
}
