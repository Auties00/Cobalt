package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the display name of a newsletter together with a stable revision
 * identifier and the moment of its last change.
 *
 * <p>Every time an admin renames the newsletter, the server assigns a new
 * revision identifier so that clients can detect whether a locally cached
 * name is still current. The identifier also prevents stale edits from
 * overwriting newer ones when several admins rename the newsletter
 * concurrently.
 */
@ProtobufMessage
public final class NewsletterName {
    /**
     * The revision identifier associated with this name.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The plain-text display name shown on the newsletter profile and
     * channel list.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * The moment at which this revision of the name was produced.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant updateTime;

    /**
     * Constructs a new {@code NewsletterName}. Invoked by the generated
     * protobuf deserializer.
     *
     * @param id         the revision identifier, must not be {@code null}
     * @param text       the display text, must not be {@code null}
     * @param updateTime the revision timestamp, may be {@code null}
     * @throws NullPointerException if {@code id} or {@code text} is
     *                              {@code null}
     */
    NewsletterName(String id, String text, Instant updateTime) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.updateTime = updateTime;
    }

    /**
     * Returns the revision identifier associated with this name.
     *
     * @return the revision identifier, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the plain-text display name.
     *
     * @return the display name, never {@code null}
     */
    public String text() {
        return text;
    }

    /**
     * Returns the moment at which this revision of the name was produced.
     *
     * @return an {@link Optional} holding the revision timestamp, or empty
     *         if the server has not reported one
     */
    public Optional<Instant> updateTimeSeconds() {
        return Optional.ofNullable(updateTime);
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
     * Sets the display text.
     *
     * @param text the new display text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the revision timestamp.
     *
     * @param updateTime the new revision timestamp, or {@code null}
     */
    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * Returns whether this name equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterName} whose
     *         fields are all equal to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterName that
                            && Objects.equals(id, that.id)
                            && Objects.equals(text, that.text)
                            && Objects.equals(updateTime, that.updateTime);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this name
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, text, updateTime);
    }
}
