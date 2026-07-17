package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.wire.linked.call.CallPeerState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the membership lifecycle state the engine stores for a call participant.
 *
 * <p>Every participant carries a membership state that tracks where the participant sits
 * in the join to leave lifecycle of a call: freshly invited, negotiating transport,
 * actively exchanging media, or removed. The engine stores this as a small integer in the
 * participant record and consults it constantly, most often to test whether a participant
 * is {@link #CONNECTED} (the canonical media flowing check) or whether a slot is
 * {@link #INVALID} (empty or removed).
 *
 * <p>This membership state is distinct from the wire {@link CallPeerState} a peer
 * advertises through a {@code peer_state} payload: {@link CallPeerState} is a transient
 * status a peer broadcasts about itself (busy, on hold, reconnecting), whereas this enum
 * is the engine's own bookkeeping of a participant's position in the call. The two are
 * never interchanged.
 *
 * <p>Each constant binds the {@link #code() integer code} the engine stores. A handful of
 * codes carry load bearing numeric semantics used in the engine's bitmask tests rather
 * than in a named string table: {@code 0} marks an invalid (empty or removed) slot,
 * {@code 1} marks a connected participant, and {@code 5} together with {@code 0xb} form
 * the transitional pair the engine moves through during a peer to peer to relay handoff.
 * Codes at or below {@code 0xc} participate in the engine's active bitmasks; codes above
 * {@code 0xc} are treated as active without further classification.
 *
 * <p>When a participant is serialized back into an outbound membership stanza, the engine
 * projects this membership state onto a separate server user state vocabulary through a
 * lookup table; that projection is owned by the membership serializer and is not modeled
 * here. The projection vocabulary is a twelve entry table:
 * <ul>
 *   <li>{@code 0} = {@code invalid}</li>
 *   <li>{@code 1} = {@code connected}</li>
 *   <li>{@code 2} = {@code outgoing}</li>
 *   <li>{@code 3} = {@code receipt}</li>
 *   <li>{@code 4} = {@code rejected}</li>
 *   <li>{@code 5} = {@code terminated}</li>
 *   <li>{@code 6} = {@code timedout}</li>
 *   <li>{@code 7} = {@code creating}</li>
 *   <li>{@code 8} = {@code invisible}</li>
 *   <li>{@code 9} = {@code visible}</li>
 *   <li>{@code 10} = {@code cancel_offer}</li>
 *   <li>{@code 11} = {@code invited}</li>
 * </ul>
 *
 * @implNote This implementation names the constants; only code {@code 0}'s name is confirmed against the
 * engine binary, which logs it as the string {@code kParticipantStateInvalid}. Every other state is logged
 * numerically (the engine emits no name string for codes {@code 2}, {@code 5}, {@code 0xb}, or {@code 0xc}),
 * so the {@link #INVITED}, {@link #CONNECTING}, and {@link #LEFT} names are reconstructions from the
 * participant lifecycle rather than recovered strings and cannot be confirmed against WhatsApp. The numeric
 * codes are load bearing and correct; the names are Cobalt's.
 */
public enum CallParticipantState {
    /**
     * An empty or removed participant slot.
     *
     * <p>This is the state of a never filled slot and the state the engine writes when a
     * participant is removed from the call.
     */
    INVALID(0),

    /**
     * A participant that is connected, with transport up and media flowing.
     *
     * <p>This is the canonical "is this participant active" value the engine tests
     * throughout the media plane.
     */
    CONNECTED(1),

    /**
     * A participant that has been invited and is awaiting acceptance.
     *
     * <p>This is the state of a freshly allocated participant filled from an inbound
     * membership stanza before it has connected.
     */
    INVITED(2),

    /**
     * A participant that is negotiating transport, between invitation and connection.
     *
     * <p>The engine moves a participant through this state while bringing up the peer to
     * peer or relay transport; the {@code 5} code additionally appears as one half of the
     * transitional pair the engine uses during a peer to peer to relay handoff.
     */
    CONNECTING(5),

    /**
     * A participant that has left the call or whose device set has been removed.
     */
    LEFT(12);

    /**
     * Caches the constant array so the {@link #ofCode(int)} decode scan does not pay the
     * defensive clone cost of {@link #values()} on every membership state lookup.
     */
    private static final CallParticipantState[] VALUES = values();

    /**
     * Resolves an engine code to its membership state, backing {@link #ofCode(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves to its
     * state in constant time rather than by scanning {@link #VALUES}.
     */
    private static final Map<Integer, CallParticipantState> BY_CODE;

    static {
        var byCode = new HashMap<Integer, CallParticipantState>();
        for (var state : VALUES) {
            if (byCode.put(state.code, state) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_CODE = Map.copyOf(byCode);
    }

    /**
     * The integer code the engine stores for this membership state.
     */
    private final int code;

    /**
     * Constructs a membership state constant bound to its engine code.
     *
     * @param code the integer code the engine stores
     */
    CallParticipantState(int code) {
        this.code = code;
    }

    /**
     * Returns the integer code the engine stores for this membership state.
     *
     * @return the engine code
     */
    public int code() {
        return code;
    }

    /**
     * Returns whether this state denotes a participant whose media is flowing.
     *
     * <p>This is the engine's canonical connected test: {@code true} only for
     * {@link #CONNECTED}.
     *
     * @return {@code true} if this state is {@link #CONNECTED}
     */
    public boolean isConnected() {
        return this == CONNECTED;
    }

    /**
     * Returns whether this state denotes an allocated, non removed participant slot.
     *
     * <p>Every state other than {@link #INVALID} denotes a slot that currently holds a
     * participant.
     *
     * @return {@code true} if this state is not {@link #INVALID}
     */
    public boolean isAllocated() {
        return this != INVALID;
    }

    /**
     * Returns the membership state whose {@linkplain #code() code} equals the given value.
     *
     * <p>An unmapped code yields {@link Optional#empty()} rather than a sentinel, so a
     * caller can distinguish a recognized state from an unknown engine value.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
     * scanning {@link #VALUES}.
     * @param code the engine code to resolve
     * @return the matching membership state, or {@link Optional#empty()} if no state
     *         matches
     */
    public static Optional<CallParticipantState> ofCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
