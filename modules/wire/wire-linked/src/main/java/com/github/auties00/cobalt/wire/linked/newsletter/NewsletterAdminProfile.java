package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes the profile of the newsletter administrator who authored a
 * particular message.
 *
 * <p>Newsletter messages may be published under an admin persona rather than
 * the admin's personal WhatsApp identity. This type carries the persona's
 * identifier, display name, and profile picture references so that client
 * UIs can render the message attribution independently of the underlying
 * account.
 *
 * <p>All fields are optional because the server may omit any of them when a
 * field has not been configured or is being withheld from the current
 * viewer.
 */
@ProtobufMessage
public final class NewsletterAdminProfile {
    /**
     * The stable identifier of the admin profile assigned by the server.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The display name shown next to messages published by this admin.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * The identifier of the admin profile picture on the media server.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String pictureId;

    /**
     * The direct path used to download the admin profile picture from the
     * media server.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String pictureDirectPath;

    /**
     * Constructs a new {@code NewsletterAdminProfile}. Invoked by the
     * generated protobuf deserializer.
     *
     * @param id                the admin profile identifier, may be {@code null}
     * @param name              the display name, may be {@code null}
     * @param pictureId         the profile picture identifier, may be {@code null}
     * @param pictureDirectPath the profile picture direct path, may be {@code null}
     */
    NewsletterAdminProfile(String id, String name, String pictureId, String pictureDirectPath) {
        this.id = id;
        this.name = name;
        this.pictureId = pictureId;
        this.pictureDirectPath = pictureDirectPath;
    }

    /**
     * Returns the stable identifier of this admin profile.
     *
     * @return an {@link Optional} holding the identifier, or empty if the
     *         server has not reported one
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the display name shown for messages published by this admin.
     *
     * @return an {@link Optional} holding the display name, or empty if none
     *         is configured
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the identifier of the admin profile picture on the media
     * server.
     *
     * @return an {@link Optional} holding the picture identifier, or empty
     *         if no picture is configured
     */
    public Optional<String> pictureId() {
        return Optional.ofNullable(pictureId);
    }

    /**
     * Returns the direct path used to download the admin profile picture.
     *
     * @return an {@link Optional} holding the direct path, or empty if no
     *         picture is configured
     */
    public Optional<String> pictureDirectPath() {
        return Optional.ofNullable(pictureDirectPath);
    }

    /**
     * Sets the admin profile identifier.
     *
     * @param id the new identifier, or {@code null} to clear it
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the admin display name.
     *
     * @param name the new display name, or {@code null} to clear it
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the profile picture identifier.
     *
     * @param pictureId the new picture identifier, or {@code null} to clear it
     */
    public void setPictureId(String pictureId) {
        this.pictureId = pictureId;
    }

    /**
     * Sets the direct path used to download the profile picture.
     *
     * @param pictureDirectPath the new direct path, or {@code null} to clear it
     */
    public void setPictureDirectPath(String pictureDirectPath) {
        this.pictureDirectPath = pictureDirectPath;
    }

    /**
     * Returns whether this profile equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterAdminProfile}
     *         whose fields are all equal to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterAdminProfile that
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(pictureId, that.pictureId)
                && Objects.equals(pictureDirectPath, that.pictureDirectPath);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this profile
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, pictureId, pictureDirectPath);
    }

    /**
     * Returns a debug-oriented string representation of this profile.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterAdminProfile[" +
                "id=" + id +
                ", name=" + name +
                ", pictureId=" + pictureId +
                ", pictureDirectPath=" + pictureDirectPath +
                ']';
    }
}
