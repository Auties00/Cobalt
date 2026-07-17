package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Creative story specification of a Click-to-WhatsApp ad group.
 *
 * <p>An ad group's creative is described by an object story spec: which page and Instagram account
 * publish it and the creative content itself. This model carries the keys WhatsApp Web's creative
 * builder populates: the {@link #pageId() page identifier}, the {@link #instagramActorId() Instagram
 * actor} and {@link #instagramUserId() Instagram user} identifiers, and the {@link #linkData() link}
 * or {@link #videoData() video} creative content.
 */
@ProtobufMessage(name = "ObjectStorySpec")
public final class ObjectStorySpec {
    /**
     * Identifier of the Facebook page the creative is published from. A Facebook page identifier (a
     * numeric string), not a WhatsApp address. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String pageId;

    /**
     * Identifier of the Instagram actor the creative is published from. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String instagramActorId;

    /**
     * Identifier of the Instagram user the creative is published from. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String instagramUserId;

    /**
     * Link-format creative content. Empty when the creative is not a link creative.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final LinkData linkData;

    /**
     * Video-format creative content. Empty when the creative is not a video creative.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final VideoData videoData;

    /**
     * Constructs a new {@code ObjectStorySpec}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param pageId           the publishing page identifier, or {@code null}
     * @param instagramActorId the Instagram actor identifier, or {@code null}
     * @param instagramUserId  the Instagram user identifier, or {@code null}
     * @param linkData         the link-format creative content, or {@code null}
     * @param videoData        the video-format creative content, or {@code null}
     */
    ObjectStorySpec(String pageId, String instagramActorId, String instagramUserId, LinkData linkData,
                    VideoData videoData) {
        this.pageId = pageId;
        this.instagramActorId = instagramActorId;
        this.instagramUserId = instagramUserId;
        this.linkData = linkData;
        this.videoData = videoData;
    }

    /**
     * Returns the identifier of the Facebook page the creative is published from.
     *
     * @return an {@link Optional} carrying the page identifier, or empty when unset
     */
    public Optional<String> pageId() {
        return Optional.ofNullable(pageId);
    }

    /**
     * Returns the identifier of the Instagram actor the creative is published from.
     *
     * @return an {@link Optional} carrying the Instagram actor identifier, or empty when unset
     */
    public Optional<String> instagramActorId() {
        return Optional.ofNullable(instagramActorId);
    }

    /**
     * Returns the identifier of the Instagram user the creative is published from.
     *
     * @return an {@link Optional} carrying the Instagram user identifier, or empty when unset
     */
    public Optional<String> instagramUserId() {
        return Optional.ofNullable(instagramUserId);
    }

    /**
     * Returns the link-format creative content.
     *
     * @return an {@link Optional} carrying the link creative, or empty when unset
     */
    public Optional<LinkData> linkData() {
        return Optional.ofNullable(linkData);
    }

    /**
     * Returns the video-format creative content.
     *
     * @return an {@link Optional} carrying the video creative, or empty when unset
     */
    public Optional<VideoData> videoData() {
        return Optional.ofNullable(videoData);
    }
}
