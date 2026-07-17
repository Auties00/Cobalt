package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;
import com.github.auties00.cobalt.calls.engine.control.ScreenShareState;

/**
 * A {@link ControlCallEvent} reporting that a participant's screen share stream changed.
 *
 * <p>The engine emits this when a participant starts, stops, or fails to start screen sharing. It carries
 * the {@link #sharer() sharer} device JID, the new {@link #state() screen share state}, and the negotiated
 * {@link #version() screen share protocol version} that distinguishes the single stream version 2 path from
 * the dual stream version 3 path. The event type reported by {@link #type()} is always
 * {@link CallEventType#SCREEN_SHARE}.
 *
 * @param sharer  the device JID of the participant whose screen share state changed; never {@code null}
 * @param state   the new screen share state; never {@code null}
 * @param version the negotiated screen share protocol version
 */
public record ScreenShareEvent(Jid sharer, ScreenShareState state, int version) implements ControlCallEvent {
    /**
     * Validates the record components, rejecting a {@code null} sharer or state.
     *
     * @throws NullPointerException if {@code sharer} or {@code state} is {@code null}
     */
    public ScreenShareEvent {
        Objects.requireNonNull(sharer, "sharer cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#SCREEN_SHARE}
     */
    @Override
    public CallEventType type() {
        return CallEventType.SCREEN_SHARE;
    }
}
