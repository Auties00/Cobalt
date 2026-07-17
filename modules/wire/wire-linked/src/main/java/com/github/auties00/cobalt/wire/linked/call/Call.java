package com.github.auties00.cobalt.wire.linked.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single in-progress or recently-ended call as a plain data view.
 *
 * <p>A call instance exists from the moment an outbound call is placed or an inbound offer is
 * accepted until the call ends. It carries only descriptive state: the immutable identity of the
 * call ({@linkplain #callId() id}, {@linkplain #peer() peer}, {@linkplain #chatJid() chat},
 * {@linkplain #creator() creator}, {@linkplain #isOutgoing() direction}, {@linkplain #isGroup()
 * topology}, {@linkplain #isVideo() video}) and the mutable runtime state the call engine updates
 * as the call progresses ({@linkplain #state() phase}, {@linkplain #isAudioMuted() audio-mute} and
 * {@linkplain #isVideoMuted() video-mute} flags, and the {@linkplain #endReason() end reason}).
 *
 * <p>This type holds no media ports, no transport, no locks, and no lifecycle logic: it is not
 * closeable and performs no I/O. Operations on a live call (mute, hang up, request a video upgrade,
 * share a screen, send a reaction) are exposed as methods on the client, and media frames flow
 * through the dedicated call media streams; both are owned by the library, not by this model. The
 * mutating accessors exist so the call engine can publish state transitions onto a shared instance
 * the application also observes; application code reads them but is not expected to call them.
 */
public final class Call {
    /**
     * Holds the unique identifier of this call, assigned by the caller's device.
     */
    private final String callId;

    /**
     * Holds the {@link Jid} of the peer the local user is talking to.
     */
    private final Jid peer;

    /**
     * Holds the chat the call belongs to: the peer JID for one-to-one calls or the group JID for
     * group calls.
     */
    private final Jid chatJid;

    /**
     * Holds the {@link Jid} of the user who created the call, with its device suffix.
     */
    private final Jid creator;

    /**
     * Indicates whether the local user placed this call ({@code true}) or received it
     * ({@code false}).
     */
    private final boolean outgoing;

    /**
     * Indicates whether this is a group call rather than a one-to-one call.
     */
    private final boolean group;

    /**
     * Indicates whether this call was placed or accepted with video enabled rather than audio-only.
     */
    private final boolean video;

    /**
     * Holds the current lifecycle phase of the call, updated by the call engine.
     */
    private volatile CallState state;

    /**
     * Indicates whether the local outbound audio is currently muted.
     */
    private volatile boolean audioMuted;

    /**
     * Indicates whether the local outbound video is currently muted.
     */
    private volatile boolean videoMuted;

    /**
     * Holds the reason the call ended, or {@code null} while the call has not ended.
     */
    private volatile CallEndReason endReason;

    /**
     * Constructs a call in an initial lifecycle phase.
     *
     * @param callId   the unique call identifier
     * @param peer     the peer JID
     * @param chatJid  the chat the call belongs to
     * @param creator  the creator JID with device suffix
     * @param outgoing whether the local user placed the call
     * @param group    whether this is a group call
     * @param video    whether the call was placed or accepted with video enabled
     * @param state    the initial lifecycle phase
     * @throws NullPointerException if {@code callId}, {@code peer}, {@code chatJid},
     *                              {@code creator}, or {@code state} is {@code null}
     */
    public Call(String callId, Jid peer, Jid chatJid, Jid creator, boolean outgoing, boolean group,
                boolean video, CallState state) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.peer = Objects.requireNonNull(peer, "peer cannot be null");
        this.chatJid = Objects.requireNonNull(chatJid, "chatJid cannot be null");
        this.creator = Objects.requireNonNull(creator, "creator cannot be null");
        this.outgoing = outgoing;
        this.group = group;
        this.video = video;
        this.state = Objects.requireNonNull(state, "state cannot be null");
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
     * Returns the JID of the peer the local user is talking to.
     *
     * @return the peer JID
     */
    public Jid peer() {
        return peer;
    }

    /**
     * Returns the chat the call belongs to: the peer JID for one-to-one calls or the group JID for
     * group calls.
     *
     * @return the chat JID
     */
    public Jid chatJid() {
        return chatJid;
    }

    /**
     * Returns the JID of the user who created the call, with its device suffix.
     *
     * @return the creator JID
     */
    public Jid creator() {
        return creator;
    }

    /**
     * Returns whether the local user placed this call.
     *
     * @return {@code true} for an outbound call, {@code false} for an inbound one
     */
    public boolean isOutgoing() {
        return outgoing;
    }

    /**
     * Returns whether this is a group call.
     *
     * @return {@code true} for a group call, {@code false} for a one-to-one call
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * Returns whether this call was placed or accepted with video enabled rather than audio-only.
     *
     * @return {@code true} for a video call, {@code false} for an audio-only call
     */
    public boolean isVideo() {
        return video;
    }

    /**
     * Returns the current lifecycle phase of the call.
     *
     * @return the call state
     */
    public CallState state() {
        return state;
    }

    /**
     * Returns whether the local outbound audio is currently muted.
     *
     * @return {@code true} if audio is muted
     */
    public boolean isAudioMuted() {
        return audioMuted;
    }

    /**
     * Returns whether the local outbound video is currently muted.
     *
     * @return {@code true} if video is muted
     */
    public boolean isVideoMuted() {
        return videoMuted;
    }

    /**
     * Returns the reason the call ended.
     *
     * @return the end reason, or {@link Optional#empty()} while the call has not ended
     */
    public Optional<CallEndReason> endReason() {
        return Optional.ofNullable(endReason);
    }

    /**
     * Publishes a new lifecycle phase for this call.
     *
     * <p>Invoked by the call engine as the call progresses; application code observes the result
     * through {@link #state()} or the corresponding listener callbacks.
     *
     * @param state the new lifecycle phase
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public void setState(CallState state) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * Publishes the local audio-mute flag for this call.
     *
     * @param audioMuted whether the local outbound audio is muted
     */
    public void setAudioMuted(boolean audioMuted) {
        this.audioMuted = audioMuted;
    }

    /**
     * Publishes the local video-mute flag for this call.
     *
     * @param videoMuted whether the local outbound video is muted
     */
    public void setVideoMuted(boolean videoMuted) {
        this.videoMuted = videoMuted;
    }

    /**
     * Publishes the reason the call ended.
     *
     * @param endReason the end reason
     */
    public void setEndReason(CallEndReason endReason) {
        this.endReason = endReason;
    }
}
