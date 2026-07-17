package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Objects;

/**
 * One video asset attached to a {@link BusinessProduct} in a WhatsApp
 * Business catalog.
 *
 * <p>Catalog products may carry zero or more video clips alongside the
 * static image assets. Each entry pairs the original-resolution video
 * URL with a still-image thumbnail URL so clients can render the
 * thumbnail in the catalog grid and stream the underlying video only
 * when the user opens the product detail view.
 */
@ProtobufMessage(name = "BusinessProductVideo")
public final class BusinessProductVideo {
    /**
     * Opaque server-assigned identifier for this video asset. Used by
     * downstream surfaces to refer back to the same upload without
     * re-publishing the bytes.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * CDN URL of the original-resolution video upload.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final URI videoUri;

    /**
     * CDN URL of the still-image thumbnail rendered for this video.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final URI thumbnailUri;

    /**
     * Constructs a new {@code BusinessProductVideo} entry. Every
     * argument is required since the WhatsApp catalog backend always
     * publishes the full triple.
     *
     * @param id           the opaque video identifier; never {@code null}
     * @param videoUri     the original-resolution video URL; never {@code null}
     * @param thumbnailUri the still-image thumbnail URL; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    BusinessProductVideo(String id, URI videoUri, URI thumbnailUri) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.videoUri = Objects.requireNonNull(videoUri, "videoUri cannot be null");
        this.thumbnailUri = Objects.requireNonNull(thumbnailUri, "thumbnailUri cannot be null");
    }

    /**
     * Returns the opaque server-assigned identifier for this video.
     *
     * @return the video id; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the original-resolution video CDN URL.
     *
     * @return the video URL; never {@code null}
     */
    public URI videoUri() {
        return videoUri;
    }

    /**
     * Returns the still-image thumbnail CDN URL.
     *
     * @return the thumbnail URL; never {@code null}
     */
    public URI thumbnailUri() {
        return thumbnailUri;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessProductVideo) obj;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.videoUri, that.videoUri)
                && Objects.equals(this.thumbnailUri, that.thumbnailUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, videoUri, thumbnailUri);
    }

    @Override
    public String toString() {
        return "BusinessProductVideo[" +
                "id=" + id + ", " +
                "videoUri=" + videoUri + ", " +
                "thumbnailUri=" + thumbnailUri + ']';
    }
}
