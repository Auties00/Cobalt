package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;

import java.util.Objects;
import com.github.auties00.cobalt.calls.engine.control.ToneType;

/**
 * A {@link ControlCallEvent} instructing the host which call tone to play, if any.
 *
 * <p>The engine plays at most one call tone at a time, the highest priority tone among those currently
 * active. When that selection changes, the engine emits this event carrying the {@link #tone() tone} the
 * host should now play; {@link ToneType#NONE} instructs the host to stop playing any tone. The host owns
 * the actual audio playback, so this event is the engine's single point of control over it.
 *
 * @implNote This implementation carries the already resolved {@link ToneType} rather than a raw tone
 * bitmask. The tone priority selector reduces the set of active tones to the single highest priority tone
 * before emitting this event, so the host never sees a bitmask and never has to break a tie itself.
 * @param tone the tone the host should play, or {@link ToneType#NONE} to play none; never {@code null}
 */
public record PlayCallTone(ToneType tone) implements ControlCallEvent {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code tone} is {@code null}
     */
    public PlayCallTone {
        Objects.requireNonNull(tone, "tone cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#PLAY_CALL_TONE}
     */
    @Override
    public CallEventType type() {
        return CallEventType.PLAY_CALL_TONE;
    }
}
