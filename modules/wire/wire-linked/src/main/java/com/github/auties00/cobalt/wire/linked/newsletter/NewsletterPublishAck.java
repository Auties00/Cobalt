package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents the relay's acknowledgement of a successful newsletter
 * publish.
 *
 * <p>Three flavours of newsletter publish share this acknowledgement
 * shape: a brand-new newsletter message, a brand-new newsletter
 * status post, and a question-response/reaction/poll-vote bound to
 * an existing message. The relay always replies with the publish
 * timestamp, and — depending on the publish kind — may additionally
 * return a server-assigned monotonic id (for brand-new posts) or a
 * response server id string (for question responses).
 *
 * <p>The {@link #serverId()} accessor exposes the server-assigned
 * monotonic id assigned to a brand-new post; the
 * {@link #responseServerId()} accessor exposes the opaque per-response
 * identifier returned for a question-response publish; both are empty
 * when the publish did not produce them.
 */
@ProtobufMessage
public final class NewsletterPublishAck {
    /**
     * The optional server-assigned monotonic id of the published
     * post, present on brand-new message and status publishes.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    Long serverId;

    /**
     * The optional opaque per-response identifier returned for a
     * question-response publish.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String responseServerId;

    /**
     * The moment at which the publish was acknowledged.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * Constructs a new {@code NewsletterPublishAck}. Invoked by the
     * generated protobuf deserializer and by the converters that
     * adapt wire responses into the domain model.
     *
     * @param serverId         the optional server-assigned id; may be
     *                         {@code null}
     * @param responseServerId the optional response server id; may be
     *                         {@code null}
     * @param timestamp        the ack time; must not be {@code null}
     * @throws NullPointerException if {@code timestamp} is
     *                              {@code null}
     */
    NewsletterPublishAck(Long serverId, String responseServerId, Instant timestamp) {
        this.serverId = serverId;
        this.responseServerId = responseServerId;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }

    /**
     * Returns the server-assigned monotonic id of the published
     * post.
     *
     * @return an {@link OptionalLong} carrying the server id, or
     *         empty for non-brand-new publishes
     */
    public OptionalLong serverId() {
        return serverId == null ? OptionalLong.empty() : OptionalLong.of(serverId);
    }

    /**
     * Returns the opaque per-response identifier returned for a
     * question-response publish.
     *
     * @return an {@link Optional} carrying the identifier, or empty
     *         for non-question-response publishes
     */
    public Optional<String> responseServerId() {
        return Optional.ofNullable(responseServerId);
    }

    /**
     * Returns the moment at which the publish was acknowledged.
     *
     * @return the ack time, never {@code null}
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns whether this ack equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterPublishAck} carrying equal field
     *         values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterPublishAck that
                && Objects.equals(serverId, that.serverId)
                && Objects.equals(responseServerId, that.responseServerId)
                && Objects.equals(timestamp, that.timestamp);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(serverId, responseServerId, timestamp);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterPublishAck[" +
                "serverId=" + serverId +
                ", responseServerId=" + responseServerId +
                ", timestamp=" + timestamp +
                ']';
    }
}
