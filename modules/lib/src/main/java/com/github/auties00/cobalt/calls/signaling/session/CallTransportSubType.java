package com.github.auties00.cobalt.calls.signaling.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the inner sub types carried by a Transport signaling message.
 *
 * <p>A Transport message (the type {@code 6} entry in the call signaling taxonomy) is a
 * container: every Transport message carries an inner sub type byte that selects which
 * transport payload it conveys. The defined sub types and their wire values are:
 *
 * <ul>
 * <li>{@link #REMOTE_CANDIDATE} ({@code 0}): a single remote ICE candidate, sent with no
 * explicit sub type byte and read back as the zero sub type.</li>
 * <li>{@link #CANDIDATE_LIST} ({@code 1}): the full local candidate list.</li>
 * <li>{@link #TRANSPORT_PROTOCOL} ({@code 4}): the negotiated transport protocol and
 * network medium.</li>
 * <li>{@link #RELAY_LATENCY} ({@code 9}): the relay latency probe.</li>
 * <li>{@link #PEER_HEALTH} ({@code 11}): the peer network health status.</li>
 * <li>{@link #ICE_DTLS} ({@code 13}): the web peer to peer ICE/DTLS handshake
 * parameters.</li>
 * </ul>
 *
 * <p>Each constant carries the {@link #wireValue() wire value} the engine stamps into the
 * Transport message. The value {@code 14} ({@code 0xe}) is a reserved sentinel that is
 * never a real sub type: the engine's type only Transport sender refuses to send a message
 * whose sub type is {@code 0} or {@code 14}, so {@code 14} marks an absent or invalid sub
 * type rather than a payload kind and has no constant here.
 */
public enum CallTransportSubType {
    /**
     * Conveys a single remote ICE candidate.
     *
     * <p>This sub type is the implicit default: a Transport message carrying a remote
     * candidate is sent with no explicit sub type byte, which the engine reads back as the
     * zero sub type.
     */
    REMOTE_CANDIDATE(0),

    /**
     * Conveys the full list of local ICE candidates gathered by this device.
     *
     * <p>The engine sends this once the peer to peer transport is ready and the device is
     * not held in a waiting room, bundling every local candidate plus server reflexive port
     * information into one Transport message.
     */
    CANDIDATE_LIST(1),

    /**
     * Conveys the negotiated transport protocol and network medium.
     *
     * <p>The payload tells the peer which transport protocol this device selected and over
     * which network medium it is operating.
     */
    TRANSPORT_PROTOCOL(4),

    /**
     * Conveys a relay latency probe used to elect the lowest latency relay.
     */
    RELAY_LATENCY(9),

    /**
     * Conveys the peer network health status.
     *
     * <p>The payload reports the measured network health status to the peer, for example
     * when a relay is unbound or no relay has been elected.
     */
    PEER_HEALTH(11),

    /**
     * Conveys the web peer to peer ICE/DTLS handshake parameters.
     *
     * <p>The payload carries the peer to peer ufrag, password, and DTLS fingerprint used to
     * establish a direct browser to browser media path.
     */
    ICE_DTLS(13);

    /**
     * The reserved sentinel sub type value the engine refuses to send.
     *
     * <p>The type only Transport sender rejects a sub type of {@code 0} or this value, so
     * {@code 0xe} denotes an absent or invalid sub type rather than a payload kind.
     */
    private static final int RESERVED_SENTINEL = 0xe;

    /**
     * Caches the constant array so the per message {@link #ofWireValue(int)} decode scan
     * does not pay the defensive clone cost of {@link #values()} on every Transport message
     * parsed.
     */
    private static final CallTransportSubType[] VALUES = values();

    /**
     * Resolves an engine wire value to its sub type, backing {@link #ofWireValue(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #wireValue}, so a wire value
     * resolves to its sub type in constant time rather than by scanning {@link #VALUES}.
     */
    private static final Map<Integer, CallTransportSubType> BY_WIRE_VALUE;

    static {
        var byWireValue = new HashMap<Integer, CallTransportSubType>();
        for (var subType : VALUES) {
            if (byWireValue.put(subType.wireValue, subType) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_WIRE_VALUE = Map.copyOf(byWireValue);
    }

    /**
     * The integer value the engine stamps into the Transport message for this sub type.
     */
    private final int wireValue;

    /**
     * Constructs a sub type constant bound to its engine wire value.
     *
     * @param wireValue the integer value the engine uses for this sub type
     */
    CallTransportSubType(int wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the integer value the engine stamps into the Transport message for this sub
     * type.
     *
     * @return the engine wire value for this sub type
     */
    public int wireValue() {
        return wireValue;
    }

    /**
     * Returns the sub type whose {@linkplain #wireValue() wire value} equals the given
     * value.
     *
     * <p>The result is empty for any value that does not correspond to a defined sub type,
     * including the reserved {@code 0xe} sentinel the engine never sends.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_WIRE_VALUE} map rather than
     * scanning {@link #VALUES}; the reserved {@code 0xe} sentinel is still short circuited to
     * {@link Optional#empty()} first.
     * @param wireValue the engine wire value to resolve
     * @return the matching sub type, or {@link Optional#empty()} if no sub type matches
     */
    public static Optional<CallTransportSubType> ofWireValue(int wireValue) {
        if (wireValue == RESERVED_SENTINEL) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_WIRE_VALUE.get(wireValue));
    }
}
