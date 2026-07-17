package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Describes a picture associated with a newsletter, such as its profile
 * photo.
 *
 * <p>WhatsApp serves newsletter pictures in two resolutions: a
 * full-resolution {@code "image"} used for the profile screen and a smaller
 * {@code "preview"} thumbnail used in list cells. Both resolutions share
 * the same layout and are represented by this class.
 *
 * <p>Downloading the picture bytes requires the combination of the
 * {@linkplain #id() picture id} and the {@linkplain #directPath() direct
 * path} reported by the media server.
 */
@ProtobufMessage
public final class NewsletterPicture {
    /**
     * The identifier of the picture on the media server.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The resolution type, typically {@code "image"} for the full-size
     * picture or {@code "preview"} for the thumbnail.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String type;

    /**
     * The direct path used to download the picture bytes from the media
     * server.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String directPath;

    /**
     * Constructs a new {@code NewsletterPicture}. Invoked by the generated
     * protobuf deserializer.
     *
     * @param id         the picture identifier, must not be {@code null}
     * @param type       the resolution type, must not be {@code null}
     * @param directPath the download direct path, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    NewsletterPicture(String id, String type, String directPath) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.directPath = Objects.requireNonNull(directPath, "directPath cannot be null");
    }

    /**
     * Returns the identifier of this picture on the media server.
     *
     * @return the picture identifier, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the resolution type of this picture.
     *
     * @return the type, typically {@code "image"} or {@code "preview"},
     *         never {@code null}
     */
    public String type() {
        return type;
    }

    /**
     * Returns the direct path used to download the picture bytes from the
     * media server.
     *
     * @return the direct path, never {@code null}
     */
    public String directPath() {
        return directPath;
    }

    /**
     * Sets the identifier of this picture on the media server.
     *
     * @param id the new identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the resolution type of this picture.
     *
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Sets the direct path used to download the picture bytes.
     *
     * @param directPath the new direct path
     */
    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    /**
     * Returns whether this picture equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterPicture}
     *         whose fields are all equal to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterPicture that
                            && Objects.equals(id, that.id)
                            && Objects.equals(type, that.type)
                            && Objects.equals(directPath, that.directPath);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this picture
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, type, directPath);
    }
}
