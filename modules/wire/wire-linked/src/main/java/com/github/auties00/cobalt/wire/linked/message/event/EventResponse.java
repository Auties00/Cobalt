package com.github.auties00.cobalt.wire.linked.message.event;

import com.github.auties00.cobalt.wire.core.message.MessageKey;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a single aggregated response entry for a WhatsApp event.
 *
 * <p>Events accumulate responses from all participants of a chat. For each
 * participant, the server aggregates their latest reply into an
 * {@code EventResponse} that pairs:
 * <ul>
 *   <li>the {@link MessageKey} that identifies the specific
 *       {@link EventResponseMessage} the participant sent,</li>
 *   <li>the moment at which the response was recorded,</li>
 *   <li>the decoded {@link EventResponseMessage} itself (RSVP choice and
 *       optional extra guest count), and</li>
 *   <li>a flag marking whether the response is still unread by the event
 *       creator.</li>
 * </ul>
 *
 * <p>A list of these entries typically accompanies an {@link EventMessage}
 * so that clients can render the full participant tally (who is going, who
 * is not, who is tentative) alongside the event itself.
 */
@ProtobufMessage(name = "EventResponse")
public final class EventResponse {
    /**
     * The {@link MessageKey} identifying the concrete response message this
     * entry summarises.
     *
     * <p>Combined with {@link #eventResponseMessage}, it allows clients to
     * link a high-level aggregated response back to the actual
     * {@link EventResponseMessage} that produced it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey eventResponseMessageKey;

    /**
     * The timestamp at which the response was recorded, in milliseconds
     * since the Unix epoch.
     *
     * <p>Stored as an {@link Instant} via {@link InstantMillisMixin}. When a
     * participant changes their RSVP the later timestamp replaces the
     * earlier one.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant timestampMs;

    /**
     * The decoded RSVP payload for this entry.
     *
     * <p>Exposes the response type and, when permitted by the parent event,
     * the number of extra guests the responder is bringing.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    EventResponseMessage eventResponseMessage;

    /**
     * Raw server flag marking whether the response is still unread by the
     * event creator.
     *
     * <p>A value of {@code null} is treated as {@code false} by
     * {@link #unread()}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean unread;

    /**
     * Constructs a new {@code EventResponse} aggregate entry.
     *
     * <p>The constructor is package-private. Application code should build
     * instances through the generated {@code EventResponseBuilder}.
     *
     * @param eventResponseMessageKey the {@link MessageKey} of the underlying
     *                                response message, or {@code null}
     * @param timestampMs             the instant at which the response was
     *                                recorded, or {@code null}
     * @param eventResponseMessage    the decoded RSVP payload, or
     *                                {@code null}
     * @param unread                  the raw unread flag, or {@code null}
     */
    EventResponse(MessageKey eventResponseMessageKey, Instant timestampMs, EventResponseMessage eventResponseMessage, Boolean unread) {
        this.eventResponseMessageKey = eventResponseMessageKey;
        this.timestampMs = timestampMs;
        this.eventResponseMessage = eventResponseMessage;
        this.unread = unread;
    }

    /**
     * Returns the {@link MessageKey} of the underlying response message.
     *
     * @return an {@link Optional} containing the key, or
     *         {@code Optional.empty()} when none was supplied
     */
    public Optional<MessageKey> eventResponseMessageKey() {
        return Optional.ofNullable(eventResponseMessageKey);
    }

    /**
     * Returns the instant at which this response was recorded.
     *
     * @return an {@link Optional} containing the response timestamp, or
     *         {@code Optional.empty()} when no timestamp was supplied
     */
    public Optional<Instant> timestampMs() {
        return Optional.ofNullable(timestampMs);
    }

    /**
     * Returns the decoded RSVP payload for this entry.
     *
     * @return an {@link Optional} containing the underlying
     *         {@link EventResponseMessage}, or {@code Optional.empty()} when
     *         not supplied
     */
    public Optional<EventResponseMessage> eventResponseMessage() {
        return Optional.ofNullable(eventResponseMessage);
    }

    /**
     * Returns whether the response is still unread by the event creator.
     *
     * <p>This is a convenience accessor: a missing field is treated as
     * {@code false}, so callers never need to check for {@code null}.
     *
     * @return {@code true} if the response is unread, {@code false} otherwise
     */
    public boolean unread() {
        return unread != null && unread;
    }

    /**
     * Sets the {@link MessageKey} of the underlying response message.
     *
     * @param eventResponseMessageKey the new response message key, or
     *                                {@code null} to clear the field
     */
    public void setEventResponseMessageKey(MessageKey eventResponseMessageKey) {
        this.eventResponseMessageKey = eventResponseMessageKey;
    }

    /**
     * Sets the instant at which this response was recorded.
     *
     * @param timestampMs the new timestamp, or {@code null} to clear the field
     */
    public void setTimestampMs(Instant timestampMs) {
        this.timestampMs = timestampMs;
    }

    /**
     * Sets the decoded RSVP payload for this entry.
     *
     * @param eventResponseMessage the new response payload, or {@code null}
     *                             to clear the field
     */
    public void setEventResponseMessage(EventResponseMessage eventResponseMessage) {
        this.eventResponseMessage = eventResponseMessage;
    }

    /**
     * Sets the unread flag for this response.
     *
     * @param unread the new unread flag, or {@code null} to clear the field
     */
    public void setUnread(Boolean unread) {
        this.unread = unread;
    }
}
