package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the textual description of a newsletter, along with a stable
 * revision identifier and the moment of its last update.
 *
 * <p>Every revision of a newsletter's description is assigned a new
 * identifier by the server; clients rely on the identifier to detect when
 * the description has changed since the last fetch and to avoid merging
 * stale edits when multiple admins are updating the newsletter
 * concurrently.
 */
@ProtobufMessage
public final class NewsletterDescription {
    /**
     * The revision identifier associated with this description text.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The plain-text description shown on the newsletter profile screen.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * The moment at which this revision of the description was produced.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant updateTimestamp;

    /**
     * Constructs a new {@code NewsletterDescription}. Invoked by the
     * generated protobuf deserializer.
     *
     * @param id              the revision identifier, must not be {@code null}
     * @param text            the description text, must not be {@code null}
     * @param updateTimestamp the revision timestamp, may be {@code null}
     * @throws NullPointerException if {@code id} or {@code text} is
     *                              {@code null}
     */
    NewsletterDescription(String id, String text, Instant updateTimestamp) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.updateTimestamp = updateTimestamp;
    }

    /**
     * Returns the revision identifier associated with this description.
     *
     * @return the revision identifier, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the plain-text description.
     *
     * @return the description text, never {@code null}
     */
    public String text() {
        return text;
    }

    /**
     * Returns the moment at which this revision was produced.
     *
     * @return an {@link Optional} holding the revision timestamp, or empty
     *         if the server has not reported one
     */
    public Optional<Instant> updateTimestamp() {
        return Optional.ofNullable(updateTimestamp);
    }

    /**
     * Sets the revision identifier.
     *
     * @param id the new revision identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the description text.
     *
     * @param text the new description text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the revision timestamp.
     *
     * @param updateTimestamp the new revision timestamp, or {@code null}
     */
    public void setUpdateTimestamp(Instant updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    /**
     * Returns whether this description equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterDescription}
     *         whose fields are all equal to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterDescription that
                            && Objects.equals(id, that.id)
                            && Objects.equals(text, that.text)
                            && Objects.equals(updateTimestamp, that.updateTimestamp);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this description
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, text, updateTimestamp);
    }
}
