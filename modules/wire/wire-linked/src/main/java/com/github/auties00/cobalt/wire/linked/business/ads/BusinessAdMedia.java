package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Optional;

/**
 * A resolved media asset displayed in a WhatsApp Business advertisement's
 * preview.
 *
 * <p>While a merchant builds a "Click-to-WhatsApp" ad (a paid promotion that
 * opens a chat with the business when tapped), the editor shows a preview of the
 * image or video the ad will carry. The creative is stored on the advertising
 * platform by hash or identifier rather than by a directly fetchable address, so
 * the editor first asks the server to resolve it into a displayable URL. This
 * model is that resolved asset: the location the preview is fetched from, an
 * optional thumbnail location for a video, and the asset's identifier.
 *
 * <p>{@link #url()} is the resolved location of the asset (the playable URL for a
 * video, the displayable URL for an image); {@link #thumbnailUrl()} is the
 * resolved location of a video's thumbnail when the server reports one; and
 * {@link #id()} is the asset's advertising-platform identifier.
 */
@ProtobufMessage(name = "BusinessAdMedia")
public final class BusinessAdMedia {
    /**
     * Resolved location the preview is fetched from (a playable URL for a video,
     * a displayable URL for an image), or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final URI url;

    /**
     * Resolved location of a video's thumbnail, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final URI thumbnailUrl;

    /**
     * Advertising-platform identifier of the asset. A numeric advertising
     * identifier, not a WhatsApp address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String id;

    /**
     * Constructs a new {@code BusinessAdMedia}. The reference arguments may be
     * {@code null} when the server omitted them.
     *
     * @param url          the resolved asset location, or {@code null}
     * @param thumbnailUrl the resolved thumbnail location, or {@code null}
     * @param id           the asset identifier, or {@code null}
     */
    BusinessAdMedia(URI url, URI thumbnailUrl, String id) {
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.id = id;
    }

    /**
     * Returns the resolved location the preview is fetched from.
     *
     * @return the resolved asset location, or empty when the server omitted it
     */
    public Optional<URI> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the resolved location of a video's thumbnail.
     *
     * @return the resolved thumbnail location, or empty when the server omitted
     *         it
     */
    public Optional<URI> thumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl);
    }

    /**
     * Returns the advertising-platform identifier of the asset.
     *
     * @return the asset id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }
}
