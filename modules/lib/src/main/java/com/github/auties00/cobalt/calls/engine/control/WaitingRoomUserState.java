package com.github.auties00.cobalt.calls.engine.control;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the lifecycle state the engine tracks for a participant waiting in a call's lobby.
 *
 * <p>While a participant in the waiting room moves from requesting admission to joining or being turned
 * away, the engine stores a small integer state for it. Each constant binds the {@link #code() integer code}
 * the engine stores. The state advances as the host acts on the participant: a fresh request is
 * {@link #OUTGOING}, a delivered request is {@link #RECEIPT}, an admitted participant that joins becomes
 * {@link #JOINED}, and a denied or expired participant becomes {@link #TERMINATED}.
 *
 * @implNote This implementation binds the codes {@code OUTGOING=1}, {@code RECEIPT=2}, {@code TERMINATED=3},
 * {@code JOINED=4}. The codes start at {@code 1}; there is no zero state, so {@link #ofCode(int)} yields an
 * empty result for {@code 0} and any value outside {@code 1..4}.
 */
public enum WaitingRoomUserState {
    /**
     * Represents an admission request the host has issued but not yet had delivered.
     */
    OUTGOING(1),

    /**
     * Represents an admission request whose delivery the engine has acknowledged.
     */
    RECEIPT(2),

    /**
     * Represents a participant that has been denied admission or whose request expired.
     */
    TERMINATED(3),

    /**
     * Represents a participant that has been admitted and has joined the call.
     */
    JOINED(4);

    /**
     * Resolves an engine code to its state, backing {@link #ofCode(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves to its
     * state in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, WaitingRoomUserState> BY_CODE;

    static {
        var byCode = new HashMap<Integer, WaitingRoomUserState>();
        for (var state : values()) {
            if (byCode.put(state.code, state) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_CODE = Map.copyOf(byCode);
    }

    /**
     * Holds the integer code the engine stores for this state.
     */
    private final int code;

    /**
     * Constructs a state constant bound to its engine code.
     *
     * @param code the integer code the engine stores
     */
    WaitingRoomUserState(int code) {
        this.code = code;
    }

    /**
     * Returns the integer code the engine stores for this state.
     *
     * @return the engine code, in the range {@code 1} to {@code 4}
     */
    public int code() {
        return code;
    }

    /**
     * Looks up the state whose {@linkplain #code() code} equals the given value.
     *
     * <p>The codes start at {@code 1}; a value of {@code 0} or any value outside {@code 1..4} yields an
     * empty result.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
     * scanning {@link #values()}.
     * @param code the engine code to resolve
     * @return the matching state, or an empty result when no state carries the code
     */
    public static Optional<WaitingRoomUserState> ofCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
