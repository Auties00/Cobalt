package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.incall.ScreenShareStanza;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the screen share transition a participant reports.
 *
 * <p>A screen share event reports a transition in a participant's screen sharing stream: it begins, it
 * stops, or it fails to start. Each constant binds the {@link #code() numeric state code} carried by the
 * {@code screenshare_state} attribute of the {@code <screen_share>} action and by the screen share event.
 *
 * @implNote This implementation binds the state codes {@code 1} (started), {@code 2} (stopped), and
 * {@code 3} (failed), the same values {@link ScreenShareStanza#STATE_STARTED},
 * {@link ScreenShareStanza#STATE_STOPPED}, and {@link ScreenShareStanza#STATE_FAILED} carry on the wire.
 * The single stream versus dual stream distinction is a separate {@code version} value, not a state, and is
 * tracked by the screen share controller rather than by this enum.
 * @see ScreenShareStanza
 */
public enum ScreenShareState {
    /**
     * Represents a screen share stream that has started.
     */
    STARTED(ScreenShareStanza.STATE_STARTED),

    /**
     * Represents a screen share stream that has stopped.
     */
    STOPPED(ScreenShareStanza.STATE_STOPPED),

    /**
     * Represents a screen share stream that failed to start.
     */
    FAILED(ScreenShareStanza.STATE_FAILED);

    /**
     * Resolves a numeric state code to its state, backing {@link #ofCode(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves to its
     * state in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, ScreenShareState> BY_CODE;

    static {
        var byCode = new HashMap<Integer, ScreenShareState>();
        for (var state : values()) {
            if (byCode.put(state.code, state) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_CODE = Map.copyOf(byCode);
    }

    /**
     * Holds the numeric state code the wire and the event carry for this transition.
     */
    private final int code;

    /**
     * Constructs a screen share state constant bound to its numeric code.
     *
     * @param code the numeric state code
     */
    ScreenShareState(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric state code the wire and the event carry for this transition.
     *
     * @return the numeric state code ({@code 1} started, {@code 2} stopped, {@code 3} failed)
     */
    public int code() {
        return code;
    }

    /**
     * Looks up the screen share state whose {@linkplain #code() code} equals the given value.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
     * scanning {@link #values()}.
     * @param code the numeric state code to resolve
     * @return the matching state, or an empty result when no state carries the code
     */
    public static Optional<ScreenShareState> ofCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
