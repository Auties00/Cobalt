package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One uploaded ad-media asset returned by the advertising-platform media
 * store.
 *
 * <p>When the WhatsApp client uploads click-to-WhatsApp ad media to the
 * advertising-platform store, the server returns a descriptor per uploaded
 * medium: the {@linkplain #url() resulting URL}, the
 * {@linkplain #contentHash() content hash}, the
 * {@linkplain #kind() media kind discriminator}, and, for video uploads, the
 * {@linkplain #videoId() video identifier}.
 *
 * <p>The {@linkplain #kind() kind} value set is server-driven; the WhatsApp
 * client treats it as opaque and keeps it as a {@link String}.
 */
@ProtobufMessage(name = "AdMediaUpload")
public final class AdMediaUpload {
    /**
     * Resulting URL of the uploaded asset, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String url;

    /**
     * Content hash of the uploaded asset, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String contentHash;

    /**
     * Media kind discriminator, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String kind;

    /**
     * Video identifier returned for video uploads, or {@code null} for image
     * uploads or when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String videoId;

    /**
     * Constructs a new {@code AdMediaUpload}. Any reference argument may be
     * {@code null} when the server omitted the corresponding field.
     *
     * @param url         the resulting URL, or {@code null}
     * @param contentHash the content hash, or {@code null}
     * @param kind        the media kind discriminator, or {@code null}
     * @param videoId     the video identifier, or {@code null} for an image
     *                    upload
     */
    AdMediaUpload(String url, String contentHash, String kind, String videoId) {
        this.url = url;
        this.contentHash = contentHash;
        this.kind = kind;
        this.videoId = videoId;
    }

    /**
     * Returns the resulting URL of the uploaded asset.
     *
     * @return the asset URL, or empty when the server omitted it
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the content hash of the uploaded asset.
     *
     * @return the content hash, or empty when the server omitted it
     */
    public Optional<String> contentHash() {
        return Optional.ofNullable(contentHash);
    }

    /**
     * Returns the media kind discriminator.
     *
     * @return the media kind, or empty when the server omitted it
     */
    public Optional<String> kind() {
        return Optional.ofNullable(kind);
    }

    /**
     * Returns the video identifier returned for video uploads.
     *
     * @return the video identifier, or empty for an image upload or when the
     *         server omitted it
     */
    public Optional<String> videoId() {
        return Optional.ofNullable(videoId);
    }
}
