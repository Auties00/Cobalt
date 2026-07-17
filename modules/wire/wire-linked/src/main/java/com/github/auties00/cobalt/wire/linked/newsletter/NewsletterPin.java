package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;

/**
 * A model representing a single pinned WhatsApp Channel (newsletter).
 *
 * <p>WhatsApp lets users pin a Channel to the top of the Updates tab. Each
 * pin entry pairs the Channel's {@linkplain #newsletterJid() newsletter JID}
 * with the {@linkplain #pinnedAt() instant} at which the pin was applied,
 * so the UI can sort pinned Channels by pin time.
 *
 * <p>Cobalt persists each pin independently so callers can resolve the pin
 * state of a single Channel without iterating the whole pin map. The
 * matching sync action updates the record whenever the pin is applied or
 * removed.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class NewsletterPin {
    /**
     * The non-{@code null} JID of the pinned Channel. Used as the primary
     * key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid newsletterJid;

    /**
     * The non-{@code null} instant at which the pin was applied. The UI
     * sorts pinned Channels by this value.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant pinnedAt;

    /**
     * Constructs a new pin entry with the given Channel JID and pin
     * timestamp.
     *
     * @param newsletterJid the non-{@code null} newsletter JID
     * @param pinnedAt      the non-{@code null} pin timestamp
     */
    NewsletterPin(Jid newsletterJid, Instant pinnedAt) {
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        this.pinnedAt = Objects.requireNonNull(pinnedAt, "pinnedAt cannot be null");
    }

    /**
     * Returns the non-{@code null} JID of the pinned Channel.
     *
     * @return the newsletter JID
     */
    public Jid newsletterJid() {
        return newsletterJid;
    }

    /**
     * Returns the non-{@code null} instant at which the pin was applied.
     *
     * @return the pin timestamp
     */
    public Instant pinnedAt() {
        return pinnedAt;
    }

    /**
     * Updates the pin timestamp.
     *
     * @param pinnedAt the non-{@code null} new pin timestamp
     * @return this pin instance for method chaining
     */
    public NewsletterPin setPinnedAt(Instant pinnedAt) {
        this.pinnedAt = Objects.requireNonNull(pinnedAt, "pinnedAt cannot be null");
        return this;
    }

    /**
     * Returns a hash code derived from this pin's
     * {@linkplain #newsletterJid() newsletter JID}.
     *
     * @return the hash code of the newsletter JID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(newsletterJid);
    }

    /**
     * Returns whether this pin is equal to the given object.
     *
     * <p>Two pins are considered equal when they share the same
     * {@linkplain #newsletterJid() newsletter JID}, regardless of the pin
     * timestamp.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code NewsletterPin}
     *         with the same newsletter JID
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof NewsletterPin that && Objects.equals(this.newsletterJid, that.newsletterJid);
    }
}
