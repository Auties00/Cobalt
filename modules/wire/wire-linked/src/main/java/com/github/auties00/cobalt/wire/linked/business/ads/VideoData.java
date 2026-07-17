package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Video-format creative content of a Click-to-WhatsApp ad's object story spec.
 *
 * <p>A single-video ad renders its creative through video data. This model carries the fields WhatsApp
 * Web populates: the {@link #callToAction() call-to-action}, the {@link #videoId() video} and its
 * {@link #imageHash() thumbnail} or {@link #imageUrl() thumbnail URL}, the {@link #title() title}, the
 * {@link #message() primary text}, and the {@link #linkDescription() link description}.
 */
@ProtobufMessage(name = "VideoData")
public final class VideoData {
    /**
     * Call-to-action button of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CallToAction callToAction;

    /**
     * Hash of the video thumbnail. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String imageHash;

    /**
     * URL of the video thumbnail. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String imageUrl;

    /**
     * Description shown next to the link. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String linkDescription;

    /**
     * Primary text of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String message;

    /**
     * Headline title of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String title;

    /**
     * Identifier of the creative video. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String videoId;

    /**
     * Constructs a new {@code VideoData}. Every argument may be {@code null} to leave the corresponding
     * field unset.
     *
     * @param callToAction    the call-to-action button, or {@code null}
     * @param imageHash       the thumbnail hash, or {@code null}
     * @param imageUrl        the thumbnail URL, or {@code null}
     * @param linkDescription the link description, or {@code null}
     * @param message         the primary text, or {@code null}
     * @param title           the headline title, or {@code null}
     * @param videoId         the video identifier, or {@code null}
     */
    VideoData(CallToAction callToAction, String imageHash, String imageUrl, String linkDescription,
              String message, String title, String videoId) {
        this.callToAction = callToAction;
        this.imageHash = imageHash;
        this.imageUrl = imageUrl;
        this.linkDescription = linkDescription;
        this.message = message;
        this.title = title;
        this.videoId = videoId;
    }

    /**
     * Returns the call-to-action button of the creative.
     *
     * @return an {@link Optional} carrying the call-to-action, or empty when unset
     */
    public Optional<CallToAction> callToAction() {
        return Optional.ofNullable(callToAction);
    }

    /**
     * Returns the hash of the video thumbnail.
     *
     * @return an {@link Optional} carrying the thumbnail hash, or empty when unset
     */
    public Optional<String> imageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * Returns the URL of the video thumbnail.
     *
     * @return an {@link Optional} carrying the thumbnail URL, or empty when unset
     */
    public Optional<String> imageUrl() {
        return Optional.ofNullable(imageUrl);
    }

    /**
     * Returns the description shown next to the link.
     *
     * @return an {@link Optional} carrying the link description, or empty when unset
     */
    public Optional<String> linkDescription() {
        return Optional.ofNullable(linkDescription);
    }

    /**
     * Returns the primary text of the creative.
     *
     * @return an {@link Optional} carrying the message, or empty when unset
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns the headline title of the creative.
     *
     * @return an {@link Optional} carrying the title, or empty when unset
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the identifier of the creative video.
     *
     * @return an {@link Optional} carrying the video identifier, or empty when unset
     */
    public Optional<String> videoId() {
        return Optional.ofNullable(videoId);
    }
}
