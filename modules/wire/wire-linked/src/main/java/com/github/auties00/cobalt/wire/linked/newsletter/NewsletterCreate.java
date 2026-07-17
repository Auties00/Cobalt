package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.stanza.model.SizedInputStream;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.createNewsletter}. Carries the
 * display name, optional description, and optional profile picture for
 * the new newsletter.
 *
 * <p>{@link #name} is required; {@link #description} and {@link #picture}
 * are optional.
 */
@ProtobufMessage
public final class NewsletterCreate {
    /**
     * Display name of the newsletter.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Optional newsletter description.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Optional JPEG-encoded profile picture supplied as a sized stream. The
     * stream is read fully when the newsletter is created and its advertised
     * length lets the payload be streamed into its Base64 form without
     * buffering it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final SizedInputStream picture;

    /**
     * Constructs a new {@code NewsletterCreate}.
     *
     * @param name        the newsletter display name; required
     * @param description the newsletter description, or {@code null}
     * @param picture     the JPEG-encoded profile picture, or {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    NewsletterCreate(String name, String description, SizedInputStream picture) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = description;
        this.picture = picture;
    }

    /**
     * Returns the display name.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the newsletter description.
     *
     * @return an {@link Optional} carrying the description, or empty
     *         when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the JPEG-encoded profile picture.
     *
     * @return an {@link Optional} carrying the picture, or empty when unset
     */
    public Optional<SizedInputStream> picture() {
        return Optional.ofNullable(picture);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NewsletterCreate) obj;
        // picture is excluded: a sized stream has no meaningful value equality
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public String toString() {
        return "NewsletterCreate[" +
                "name=" + name + ", " +
                "description=" + description + ", " +
                "picture=" + (picture == null ? "unset" : "set") + ']';
    }
}
