package com.github.auties00.cobalt.wire.linked.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.createCallLink}. Carries the
 * call medium, the JID of the device that creates the call, an
 * optional client-side call identifier, an optional creator username,
 * the waiting-room toggle, and an optional scheduled-start time.
 *
 * <p>{@link #media} and {@link #callCreator} are required. The
 * remaining fields are optional — {@link #callId} and
 * {@link #creatorUsername} default to relay-minted values when unset,
 * {@link #waitingRoomEnabled} defaults to {@code false}, and
 * {@link #eventStartTime} is only set when the link is bound to a
 * scheduled call event.
 */
@ProtobufMessage
public final class CallLinkCreate {
    /**
     * Media kind advertised by the call link (audio or video).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final CallLinkMedia media;

    /**
     * JID of the device that will originate the call when a joiner
     * resolves the link.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid callCreator;

    /**
     * Optional client-side call identifier; {@code null} lets the relay
     * mint one.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String callId;

    /**
     * Optional creator username surfaced in the join preview.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String creatorUsername;

    /**
     * Whether the link's waiting-room is enabled at creation time.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean waitingRoomEnabled;

    /**
     * Optional scheduled start time; non-{@code null} marks the link
     * as bound to a scheduled-call event.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant eventStartTime;

    /**
     * Constructs a new {@code CallLinkCreate}.
     *
     * @param media              the call media kind; required
     * @param callCreator        the call-creator JID; required
     * @param callId             optional client-side call id, or {@code null}
     * @param creatorUsername    optional creator username, or {@code null}
     * @param waitingRoomEnabled whether the waiting-room is enabled
     * @param eventStartTime     optional scheduled-start time, or {@code null}
     * @throws NullPointerException if {@code media} or {@code callCreator}
     *                              is {@code null}
     */
    CallLinkCreate(CallLinkMedia media, Jid callCreator, String callId, String creatorUsername,
                   boolean waitingRoomEnabled, Instant eventStartTime) {
        this.media = Objects.requireNonNull(media, "media cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.callId = callId;
        this.creatorUsername = creatorUsername;
        this.waitingRoomEnabled = waitingRoomEnabled;
        this.eventStartTime = eventStartTime;
    }

    /**
     * Returns the call media kind.
     *
     * @return the media kind, never {@code null}
     */
    public CallLinkMedia media() {
        return media;
    }

    /**
     * Returns the call-creator JID.
     *
     * @return the creator JID, never {@code null}
     */
    public Jid callCreator() {
        return callCreator;
    }

    /**
     * Returns the optional client-side call identifier.
     *
     * @return an {@link Optional} carrying the call id, or empty when unset
     */
    public Optional<String> callId() {
        return Optional.ofNullable(callId);
    }

    /**
     * Returns the optional creator username.
     *
     * @return an {@link Optional} carrying the username, or empty when unset
     */
    public Optional<String> creatorUsername() {
        return Optional.ofNullable(creatorUsername);
    }

    /**
     * Returns whether the waiting-room is enabled.
     *
     * @return {@code true} when the waiting-room is enabled
     */
    public boolean waitingRoomEnabled() {
        return waitingRoomEnabled;
    }

    /**
     * Returns the optional scheduled start time.
     *
     * @return an {@link Optional} carrying the start time, or empty when
     *         the link is not bound to a scheduled event
     */
    public Optional<Instant> eventStartTime() {
        return Optional.ofNullable(eventStartTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CallLinkCreate) obj;
        return media == that.media &&
                Objects.equals(callCreator, that.callCreator) &&
                Objects.equals(callId, that.callId) &&
                Objects.equals(creatorUsername, that.creatorUsername) &&
                waitingRoomEnabled == that.waitingRoomEnabled &&
                Objects.equals(eventStartTime, that.eventStartTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(media, callCreator, callId, creatorUsername, waitingRoomEnabled, eventStartTime);
    }

    @Override
    public String toString() {
        return "CallLinkCreate[" +
                "media=" + media + ", " +
                "callCreator=" + callCreator + ", " +
                "callId=" + callId + ", " +
                "creatorUsername=" + creatorUsername + ", " +
                "waitingRoomEnabled=" + waitingRoomEnabled + ", " +
                "eventStartTime=" + eventStartTime + ']';
    }
}
