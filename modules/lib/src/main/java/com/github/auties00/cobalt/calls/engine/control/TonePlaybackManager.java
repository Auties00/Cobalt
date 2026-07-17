package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import com.github.auties00.cobalt.calls.engine.control.event.PlayCallTone;

/**
 * Tracks which call tones are active and tells the host which single tone to play.
 *
 * <p>Several call conditions can each want a tone at once: a call connecting, a peer alerting, a busy
 * signal. This manager keeps the set of active tones as a bitmask and plays only the one with the highest
 * priority, so a busy signal overrides a ringback and a ringback overrides a connecting tone.
 * {@link #activate(ToneType)} sets a tone's bit and {@link #deactivate(ToneType)} clears it; whenever the
 * active tone with the highest priority changes, the manager emits a {@link PlayCallTone} carrying the tone
 * the host should now play, or {@link ToneType#NONE} when the last tone clears.
 *
 * <p>The bitmask and the last played tone are guarded by a lock so a concurrent activate and deactivate
 * never emit a tone out of order. The manager is bound to its event sink at construction; it owns no
 * timers. The host performs the actual audio playback; this manager is the engine's single point of
 * control over which tone is audible.
 *
 * @implNote This implementation holds the active tones as a bitmask, ORing in each tone by its
 * {@link ToneType#bit() priority bit}, resolving the tone to play through
 * {@link ToneType#highestPriority(int)}, and emitting the resolved {@link ToneType} rather than the raw
 * mask. Mutual exclusion over the mask and the last played tone is provided by a {@link ReentrantLock}.
 */
public final class TonePlaybackManager {
    /**
     * The logger for {@link TonePlaybackManager}.
     */
    private static final System.Logger LOGGER = Log.get(TonePlaybackManager.class);

    /**
     * The event sink the tone to play event is emitted into.
     */
    private final CallEventSink events;

    /**
     * Guards the active tone bitmask and the last played tone.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The bitmask of currently active tones, an OR of their {@linkplain ToneType#bit() priority bits}.
     */
    private int activeMask;

    /**
     * The tone most recently reported to the host through a {@link PlayCallTone} event.
     */
    private ToneType playing = ToneType.NONE;

    /**
     * Constructs a tone playback manager bound to its event sink.
     *
     * @param events the event sink to emit the tone to play event into; never {@code null}
     * @throws NullPointerException if {@code events} is {@code null}
     */
    public TonePlaybackManager(CallEventSink events) {
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Activates a tone, emitting a new tone to play event if it becomes the active tone with the highest
     * priority.
     *
     * <p>Sets the tone's {@linkplain ToneType#bit() priority bit} in the active mask and re evaluates the
     * active tone with the highest priority; a change emits a {@link PlayCallTone}. Activating
     * {@link ToneType#NONE} has no effect, since it carries no bit.
     *
     * @param tone the tone to activate; never {@code null}
     * @throws NullPointerException if {@code tone} is {@code null}
     */
    public void activate(ToneType tone) {
        Objects.requireNonNull(tone, "tone cannot be null");
        lock.lock();
        try {
            activeMask |= tone.bit();
            reevaluate();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Deactivates a tone, emitting a new tone to play event if the active tone with the highest priority
     * changes.
     *
     * <p>Clears the tone's {@linkplain ToneType#bit() priority bit} from the active mask and re evaluates
     * the active tone with the highest priority; a change emits a {@link PlayCallTone}, which is
     * {@link ToneType#NONE} when the last tone clears. Deactivating {@link ToneType#NONE} has no effect.
     *
     * @param tone the tone to deactivate; never {@code null}
     * @throws NullPointerException if {@code tone} is {@code null}
     */
    public void deactivate(ToneType tone) {
        Objects.requireNonNull(tone, "tone cannot be null");
        lock.lock();
        try {
            activeMask &= ~tone.bit();
            reevaluate();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears every active tone, emitting a stop ({@link ToneType#NONE}) if a tone was playing.
     *
     * <p>Used at teardown so no tone is left audible; emits a {@link PlayCallTone} carrying
     * {@link ToneType#NONE} only when a tone was playing.
     */
    public void clear() {
        lock.lock();
        try {
            activeMask = 0;
            reevaluate();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the tone currently selected for playback.
     *
     * @return the tone the host should be playing, or {@link ToneType#NONE} when none; never {@code null}
     */
    public ToneType playing() {
        lock.lock();
        try {
            return playing;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Re evaluates the active tone with the highest priority and emits a change.
     *
     * <p>Resolves the highest set bit of the active mask through {@link ToneType#highestPriority(int)};
     * when it differs from the last played tone, records it and emits a {@link PlayCallTone}. Called while
     * holding the lock.
     */
    private void reevaluate() {
        var next = ToneType.highestPriority(activeMask);
        if (next != playing) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tone playback {0} -> {1}", playing, next);
            playing = next;
            events.emit(new PlayCallTone(next));
        }
    }
}
