package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single status envelope returned by a newsletter
 * status-history page or status-update delta.
 *
 * <p>Status posts on a newsletter are short-lived photo-like updates
 * that surface in the channel's "Status" tray. Like messages, they
 * are back-filled by envelope only: the actual status payload
 * (caption, media, reactions, view counts) is delivered through the
 * regular notification stream. This type therefore carries the
 * minimum information needed to anchor a back-fill into the local
 * status timeline.
 *
 * <p>The {@link #stanzaId()}, {@link #serverId()},
 * {@link #timestamp()} and {@link #fromSelf()} accessors mirror the
 * regular newsletter message envelope, since both kinds of post
 * share the same id/timestamp/sender shape on the wire.
 */
@ProtobufMessage
public final class NewsletterStatusHistoryEntry {
    /**
     * The optional client-supplied stanza id of the status, when the
     * relay echoes one.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String stanzaId;

    /**
     * The server-assigned monotonic id within the newsletter status
     * tray.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long serverId;

    /**
     * The original publish time of the status, when reported by the
     * relay.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * Whether the status was authored by the connected account.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    boolean fromSelf;

    /**
     * Constructs a new {@code NewsletterStatusHistoryEntry}. Invoked
     * by the generated protobuf deserializer and by the converters
     * that adapt wire responses into the domain model.
     *
     * @param stanzaId  the optional client-supplied stanza id; may be
     *                  {@code null}
     * @param serverId  the server-assigned monotonic id
     * @param timestamp the optional publish time; may be {@code null}
     * @param fromSelf  whether the status was authored by the
     *                  connected account
     */
    NewsletterStatusHistoryEntry(String stanzaId, long serverId, Instant timestamp, boolean fromSelf) {
        this.stanzaId = stanzaId;
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.fromSelf = fromSelf;
    }

    /**
     * Returns the optional client-supplied stanza id of the status.
     *
     * @return an {@link Optional} carrying the stanza id, or empty
     *         when the relay omitted it
     */
    public Optional<String> stanzaId() {
        return Optional.ofNullable(stanzaId);
    }

    /**
     * Returns the server-assigned monotonic id within the status
     * tray.
     *
     * @return the server id
     */
    public long serverId() {
        return serverId;
    }

    /**
     * Returns the original publish time of the status.
     *
     * @return an {@link Optional} carrying the publish time, or empty
     *         when the relay omitted it
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns whether the status was authored by the connected
     * account.
     *
     * @return {@code true} when the status originated from this
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
     *         {@code NewsletterStatusHistoryEntry} carrying equal
     *         field values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterStatusHistoryEntry that
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
        return "NewsletterStatusHistoryEntry[" +
                "stanzaId=" + stanzaId +
                ", serverId=" + serverId +
                ", timestamp=" + timestamp +
                ", fromSelf=" + fromSelf +
                ']';
    }
}
