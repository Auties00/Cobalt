package com.github.auties00.cobalt.calls.engine.state;

import java.util.Optional;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;

/**
 * Enumerates the five substates of the group call link join handshake.
 *
 * <p>While a call sits in {@link CallLifecycleState#LINK} the engine tracks a separate substate that
 * advances through the query and join legs of joining a call through a call link. Each constant binds the
 * {@link #wireOrdinal()} the engine stores alongside the call state. The substate is meaningful only while
 * the call state is {@link CallLifecycleState#LINK}; outside that phase it stays {@link #NONE}. The five
 * constants form the ordered progression {@link #NONE}, {@link #LINK_QUERY_SENT}, {@link #LINK_QUERY_ACKED},
 * {@link #LINK_JOIN_SENT}, {@link #LINK_JOIN_ACKED}, and any transition between them drives the engine's
 * link state event.
 */
public enum CallLinkState {
    /**
     * Represents no link join activity, the initial and inactive substate.
     */
    NONE(0),

    /**
     * Represents a link query having been sent with its acknowledgement still awaited.
     */
    LINK_QUERY_SENT(1),

    /**
     * Represents a link query having been acknowledged.
     */
    LINK_QUERY_ACKED(2),

    /**
     * Represents a link join having been sent with its acknowledgement still awaited.
     */
    LINK_JOIN_SENT(3),

    /**
     * Represents a link join having been acknowledged, completing the handshake.
     */
    LINK_JOIN_ACKED(4);

    /**
     * Holds the numeric substate value the engine stores in the call context.
     */
    private final int wireOrdinal;

    /**
     * Constructs a constant bound to its numeric substate value.
     *
     * @param wireOrdinal the numeric substate value stored in the call context
     */
    CallLinkState(int wireOrdinal) {
        this.wireOrdinal = wireOrdinal;
    }

    /**
     * Returns the numeric substate value the engine stores in the call context.
     *
     * @return the substate value, in the range {@code 0} to {@code 4}
     */
    public int wireOrdinal() {
        return wireOrdinal;
    }

    /**
     * Looks up the link substate carrying a given numeric substate value.
     *
     * <p>The lookup is keyed on the numeric value. A value outside the range {@code 0} to {@code 4} yields
     * an empty result.
     *
     * @param wireOrdinal the numeric substate value
     * @return the matching substate, or an empty result when the value is out of range
     */
    public static Optional<CallLinkState> ofWireOrdinal(int wireOrdinal) {
        if (wireOrdinal < 0 || wireOrdinal >= values().length) {
            return Optional.empty();
        }
        return Optional.of(values()[wireOrdinal]);
    }
}
