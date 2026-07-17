package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * The end-of-video card shown after a video message finishes playing.
 *
 * <p>End cards typically advertise the video's author (username, profile picture)
 * together with a caption and a thumbnail image so that viewers can follow up or
 * share the content. Every field on this class is mandatory.
 */
@ProtobufMessage(name = "Message.VideoEndCard")
public final class MessageVideoEndCard implements Message {
    /**
     * Display username of the video's author.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String username;

    /**
     * Caption text shown on the end card.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String caption;

    /**
     * URL of the thumbnail image shown on the end card.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String thumbnailImageUrl;

    /**
     * URL of the author's profile picture shown on the end card.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String profilePictureUrl;


    /**
     * Constructs a new video end card with all mandatory fields populated.
     *
     * @param username          the author username
     * @param caption           the caption text
     * @param thumbnailImageUrl the thumbnail image URL
     * @param profilePictureUrl the author profile picture URL
     * @throws NullPointerException if any argument is {@code null}
     */
    MessageVideoEndCard(String username, String caption, String thumbnailImageUrl, String profilePictureUrl) {
        this.username = Objects.requireNonNull(username);
        this.caption = Objects.requireNonNull(caption);
        this.thumbnailImageUrl = Objects.requireNonNull(thumbnailImageUrl);
        this.profilePictureUrl = Objects.requireNonNull(profilePictureUrl);
    }

    /**
     * Returns the author's username.
     *
     * @return the username
     */
    public String username() {
        return username;
    }

    /**
     * Returns the caption text shown on the end card.
     *
     * @return the caption
     */
    public String caption() {
        return caption;
    }

    /**
     * Returns the thumbnail image URL.
     *
     * @return the URL
     */
    public String thumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    /**
     * Returns the author's profile picture URL.
     *
     * @return the URL
     */
    public String profilePictureUrl() {
        return profilePictureUrl;
    }

    /**
     * Updates the author's username.
     *
     * @param username the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Updates the caption text.
     *
     * @param caption the new caption
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Updates the thumbnail image URL.
     *
     * @param thumbnailImageUrl the new URL
     */
    public void setThumbnailImageUrl(String thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    /**
     * Updates the author's profile picture URL.
     *
     * @param profilePictureUrl the new URL
     */
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
