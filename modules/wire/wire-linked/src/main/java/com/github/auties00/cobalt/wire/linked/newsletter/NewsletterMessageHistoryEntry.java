package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single message envelope returned by a newsletter
 * message-history page or message-update delta.
 *
 * <p>Newsletter back-fill queries return only the envelope of every
 * message in the requested window: the stanza id, the server-assigned
 * monotonic id, the send time, and a flag marking messages authored by
 * the connected account. The actual message body is broadcast on the
 * regular newsletter notification stream and reaches the local store
 * via {@link NewsletterMessageInfo}; this type therefore carries the
 * minimum information required to anchor a back-fill into that
 * timeline (so the caller can deduplicate against already-stored
 * messages and request the missing bodies).
 *
 * <p>Both the regular {@code <messages>} page and the {@code <message_updates>}
 * delta variant share the same envelope shape, which is why they
 * collapse to a single domain entry.
 */
@ProtobufMessage
public final class NewsletterMessageHistoryEntry {
    /**
     * The optional client-supplied stanza id of the message, when the
     * relay echoes one.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String stanzaId;

    /**
     * The server-assigned monotonic id within the newsletter.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long serverId;

    /**
     * The original send time of the message, when reported by the
     * relay.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * Whether the message was authored by the connected account.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    boolean fromSelf;

    /**
     * Constructs a new {@code NewsletterMessageHistoryEntry}. Invoked
     * by the generated protobuf deserializer and by the converters
     * that adapt wire responses into the domain model.
     *
     * @param stanzaId  the optional client-supplied stanza id; may be
     *                  {@code null}
     * @param serverId  the server-assigned monotonic id
     * @param timestamp the optional send time; may be {@code null}
     * @param fromSelf  whether the message was authored by the
     *                  connected account
     */
    NewsletterMessageHistoryEntry(String stanzaId, long serverId, Instant timestamp, boolean fromSelf) {
        this.stanzaId = stanzaId;
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.fromSelf = fromSelf;
    }

    /**
     * Returns the optional client-supplied stanza id of the message.
     *
     * @return an {@link Optional} carrying the stanza id, or empty
     *         when the relay omitted it
     */
    public Optional<String> stanzaId() {
        return Optional.ofNullable(stanzaId);
    }

    /**
     * Returns the server-assigned monotonic id within the newsletter.
     *
     * @return the server id
     */
    public long serverId() {
        return serverId;
    }

    /**
     * Returns the original send time of the message.
     *
     * @return an {@link Optional} carrying the send time, or empty
     *         when the relay omitted it
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns whether the message was authored by the connected
     * account.
     *
     * @return {@code true} when the message originated from this
     *         account
     */
    public boolean fromSelf() {
        return fromSelf;
    }

    /**
     * Returns whether this entry equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterMessageHistoryEntry} carrying equal
     *         field values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterMessageHistoryEntry that
                && serverId == that.serverId
                && fromSelf == that.fromSelf
                && Objects.equals(stanzaId, that.stanzaId)
                && Objects.equals(timestamp, that.timestamp);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(stanzaId, serverId, timestamp, fromSelf);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterMessageHistoryEntry[" +
                "stanzaId=" + stanzaId +
                ", serverId=" + serverId +
                ", timestamp=" + timestamp +
                ", fromSelf=" + fromSelf +
                ']';
    }
}
