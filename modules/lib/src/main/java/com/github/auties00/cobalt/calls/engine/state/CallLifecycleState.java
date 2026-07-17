package com.github.auties00.cobalt.calls.engine.state;

import com.github.auties00.cobalt.wire.linked.call.CallState;

import java.util.Optional;

/**
 * Enumerates the fifteen internal lifecycle states of the voip call engine.
 *
 * <p>The engine drives every call through a richer state machine than the five public phases of
 * {@link CallState}: it distinguishes the caller and callee setup legs, the two established states (peer
 * media flowing versus connected while still alone), the group call link join phase, and the broadcast
 * call start. Each constant binds the {@link #wireOrdinal()} the engine stores in the call context and a
 * projection to the public {@link CallState} via {@link #toPublic()}. This enum is the engine view of a
 * call; {@link CallState} is the coarser view delivered to listeners.
 *
 * <p>The {@link #wireOrdinal()} coincides with this enum's {@link Enum#ordinal()} because the state
 * values are contiguous from {@code 0} to {@code 14} and are declared here in numeric order; the explicit
 * accessor is nonetheless the contract, since declaration order is not a protocol guarantee. State
 * transitions are governed elsewhere (the transition guard, the timers); this type only names the states
 * and projects them.
 *
 * @implNote This implementation admits only the fifteen known values; {@link #ofWireOrdinal(int)} resolves
 * any value outside the range {@code 0} to {@code 14} to an empty result rather than fabricating a state.
 */
public enum CallLifecycleState {
    /**
     * Represents the absence of an active call: the idle state and the terminal state after teardown.
     */
    NONE(0),

    /**
     * Represents an outbound call whose offer has been sent and whose peer is being notified.
     */
    CALLING(1),

    /**
     * Represents an outbound call for which an early acceptance arrived: the peer device is alerting but
     * the peer user has not answered.
     */
    PREACCEPT_RECEIVED(2),

    /**
     * Represents an inbound call whose offer has been received and is ringing locally.
     */
    RECEIVED_CALL(3),

    /**
     * Represents the local user having accepted an inbound call, with transport and media bring up
     * underway and the accept sent.
     */
    ACCEPT_SENT(4),

    /**
     * Represents an outbound call whose accept arrived, with transport and media bring up underway.
     */
    ACCEPT_RECEIVED(5),

    /**
     * Represents an established call with peer media flowing.
     */
    CALL_ACTIVE(6),

    /**
     * Represents a call that was answered or otherwise handled on another device of the same account.
     */
    CALL_ACTIVE_ELSEWHERE(7),

    /**
     * Represents an inbound call received without a media descriptor, so no early media can be prepared.
     */
    RECEIVED_CALL_WITHOUT_OFFER(8),

    /**
     * Represents a loss of the in call network path with relay or DTLS renegotiation underway.
     */
    REJOINING(9),

    /**
     * Represents the group call link join handshake phase, whose sub state is tracked separately.
     */
    LINK(10),

    /**
     * Represents an established call that is connected but has no peer connected yet: the group
     * "lonely" state.
     */
    CONNECTED_LONELY(11),

    /**
     * Represents the transient state entered before an outbound offer is built.
     */
    PRE_CALLING(12),

    /**
     * Represents a call that is tearing down toward {@link #NONE}.
     */
    ENDING(13),

    /**
     * Represents the start of a broadcast or business call.
     */
    BCALL_STARTING(14);

    /**
     * Holds the numeric state value the engine stores in the call context.
     */
    private final int wireOrdinal;

    /**
     * Constructs a constant bound to its native state value.
     *
     * @param wireOrdinal the numeric state value stored in the call context
     */
    CallLifecycleState(int wireOrdinal) {
        this.wireOrdinal = wireOrdinal;
    }

    /**
     * Returns the numeric state value the engine stores in the call context.
     *
     * @return the native state value, in the range {@code 0} to {@code 14}
     */
    public int wireOrdinal() {
        return wireOrdinal;
    }

    /**
     * Projects this internal state onto the user facing {@link CallState}.
     *
     * <p>The fifteen internal states collapse onto the five public phases as follows. The setup legs
     * before media bring up ({@link #CALLING}, {@link #PRE_CALLING}, {@link #RECEIVED_CALL},
     * {@link #RECEIVED_CALL_WITHOUT_OFFER}, {@link #PREACCEPT_RECEIVED}, {@link #BCALL_STARTING}) map to
     * {@link CallState#RINGING}. The legs with transport and media bring up underway
     * ({@link #ACCEPT_SENT}, {@link #ACCEPT_RECEIVED}, {@link #LINK}) map to {@link CallState#CONNECTING}.
     * The two established states ({@link #CALL_ACTIVE} and {@link #CONNECTED_LONELY}) map to
     * {@link CallState#ACTIVE}, since from the local user's perspective the call is connected even while
     * alone in a group call. {@link #REJOINING} maps to {@link CallState#RECONNECTING}. The terminal and
     * elsewhere states ({@link #NONE}, {@link #ENDING}, {@link #CALL_ACTIVE_ELSEWHERE}) map to
     * {@link CallState#ENDED}.
     *
     * @return the corresponding public lifecycle phase, never {@code null}
     */
    public CallState toPublic() {
        return switch (this) {
            case CALLING, PRE_CALLING, RECEIVED_CALL, RECEIVED_CALL_WITHOUT_OFFER, PREACCEPT_RECEIVED, BCALL_STARTING ->
                    CallState.RINGING;
            case ACCEPT_SENT, ACCEPT_RECEIVED, LINK -> CallState.CONNECTING;
            case CALL_ACTIVE, CONNECTED_LONELY -> CallState.ACTIVE;
            case REJOINING -> CallState.RECONNECTING;
            case NONE, ENDING, CALL_ACTIVE_ELSEWHERE -> CallState.ENDED;
        };
    }

    /**
     * Looks up the internal state for a native state value.
     *
     * <p>The lookup is keyed on the protocol value. A value outside the range {@code 0} to {@code 14}
     * yields an empty result, mirroring the engine's {@code UNKNOWN CALL STATE} handling.
     *
     * @param wireOrdinal the native state value
     * @return the matching state, or an empty result when the value is out of range
     */
    public static Optional<CallLifecycleState> ofWireOrdinal(int wireOrdinal) {
        if (wireOrdinal < 0 || wireOrdinal >= values().length) {
            return Optional.empty();
        }
        return Optional.of(values()[wireOrdinal]);
    }
}
