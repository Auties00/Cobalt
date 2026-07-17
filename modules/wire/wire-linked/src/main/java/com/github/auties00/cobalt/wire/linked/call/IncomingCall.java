package com.github.auties00.cobalt.wire.linked.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a pending inbound call offer, surfaced to the user before any media is exchanged.
 *
 * <p>An offer is delivered to the application's call listener when an inbound call offer arrives from
 * the peer. The listener responds by accepting or rejecting the offer through the client; an
 * unanswered offer expires on its own once the WhatsApp-imposed timeout of around thirty seconds
 * elapses.
 *
 * <p>This class is a pure value type carrying the protocol metadata of the offer: call identifier,
 * peer JID, timestamps, and the group and video flags. It holds no transport state and no media
 * ports; those exist only after the offer is accepted and a {@link Call} is established.
 *
 * @apiNote Prefer accepting as audio-only (passing no video stream) even when {@link #videoOffered()}
 * is {@code true} if the local side has no camera to stream; the caller's video offer does not
 * obligate the callee to send video.
 */
public final class IncomingCall {
    /**
     * Holds the unique identifier for this call, assigned by the caller's device and shared with the
     * message-key id of the corresponding call-log chat entry.
     */
    private final String callId;

    /**
     * Holds the {@link Jid} of the user who initiated the call, the peer the local user is talking
     * to.
     */
    private final Jid peer;

    /**
     * Holds the chat where this call's log entry appears: the peer's JID for one-to-one calls or the
     * group JID for group calls.
     */
    private final Jid chatJid;

    /**
     * Holds the instant at which the offer was received locally.
     */
    private final Instant timestamp;

    /**
     * Indicates whether the caller offered video; {@code false} denotes an audio-only call.
     */
    private final boolean videoOffered;

    /**
     * Indicates whether this is a group call.
     */
    private final boolean group;

    /**
     * Holds the group's {@link Jid} for group calls; {@code null} for one-to-one calls.
     */
    private final Jid groupJid;

    /**
     * Indicates whether the offer was server-replayed because the local device was disconnected when
     * the call originally rang.
     *
     * <p>Such a notification is informational only: the call cannot be answered in real time once an
     * offline offer's natural timeout has elapsed.
     */
    private final boolean offlineOffer;

    /**
     * Constructs an offer.
     *
     * @param callId       the unique call identifier
     * @param peer         the caller's JID
     * @param chatJid      the chat the offer arrived in: the peer JID for one-to-one calls, the group
     *                     JID for group calls
     * @param timestamp    when the offer was received
     * @param videoOffered whether video was offered
     * @param group        whether this is a group call
     * @param groupJid     the group JID, or {@code null} for one-to-one
     * @param offlineOffer whether the offer was server-replayed
     * @throws NullPointerException if {@code callId}, {@code peer}, {@code chatJid}, or
     *                              {@code timestamp} is {@code null}
     */
    public IncomingCall(String callId, Jid peer, Jid chatJid, Instant timestamp,
                        boolean videoOffered, boolean group, Jid groupJid,
                        boolean offlineOffer) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.peer = Objects.requireNonNull(peer, "peer cannot be null");
        this.chatJid = Objects.requireNonNull(chatJid, "chatJid cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.videoOffered = videoOffered;
        this.group = group;
        this.groupJid = groupJid;
        this.offlineOffer = offlineOffer;
    }

    /**
     * Returns the unique call identifier.
     *
     * @return the call id
     */
    public String callId() {
        return callId;
    }

    /**
     * Returns the JID of the peer who placed the call.
     *
     * @return the peer JID
     */
    public Jid peer() {
        return peer;
    }

    /**
     * Returns the chat the offer arrived in: the peer JID for one-to-one calls, the group JID for
     * group calls.
     *
     * @return the chat JID
     */
    public Jid chatJid() {
        return chatJid;
    }

    /**
     * Returns the instant the offer was received locally.
     *
     * @return the offer timestamp
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns whether the caller offered video.
     *
     * <p>The local side may still accept as audio-only by passing no video stream when accepting the
     * call.
     *
     * @return {@code true} if video was offered
     */
    public boolean videoOffered() {
        return videoOffered;
    }

    /**
     * Returns whether this is a group call.
     *
     * @return {@code true} for group calls
     */
    public boolean group() {
        return group;
    }

    /**
     * Returns the group's JID for group calls.
     *
     * @return the group JID, or {@link Optional#empty()} for one-to-one calls
     */
    public Optional<Jid> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns whether the offer was server-replayed because the local device was offline at ring
     * time.
     *
     * @return {@code true} for offline-replayed offers
     */
    public boolean offlineOffer() {
        return offlineOffer;
    }
}
