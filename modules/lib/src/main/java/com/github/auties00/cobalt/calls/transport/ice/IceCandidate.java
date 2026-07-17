package com.github.auties00.cobalt.calls.transport.ice;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Represents one ICE candidate on the Web P2P interop path: a transport address paired with its type
 * and RFC 8445 priority.
 *
 * <p>A candidate is one of the local or remote endpoints ICE forms pairs from. It carries the
 * {@link InetSocketAddress transport address} it represents, the {@link Type candidate type} (host,
 * peer reflexive, server reflexive, or relayed), the wire {@link Protocol protocol} (UDP or TCP), and
 * the {@code u32} candidate priority RFC 8445 assigns. The priority is computed from the type
 * preference, the local preference, and the component id by {@link #computePriority(int, int, int)}
 * when gathering candidates.
 *
 * @implNote This implementation uses the RFC 8445 priority formula
 *           {@code (2^24)*typePref + (2^8)*localPref + (256 - componentId)}.
 */
public record IceCandidate(InetSocketAddress address, Type type, Protocol protocol, long priority) {
    /**
     * Validates the record components, rejecting a null address, type, or protocol.
     *
     * @throws NullPointerException if {@code address}, {@code type}, or {@code protocol} is {@code null}
     */
    public IceCandidate {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(protocol, "protocol cannot be null");
    }

    /**
     * Creates a candidate, computing its RFC 8445 priority from the type and local preferences.
     *
     * @param address         the transport address the candidate represents
     * @param type            the candidate type
     * @param protocol        the wire protocol
     * @param localPreference the local preference in {@code 0..65535}, higher preferred
     * @param componentId     the component id (RTP is {@code 1})
     * @return a candidate whose priority is {@link #computePriority(int, int, int)} of the inputs
     * @throws NullPointerException if {@code address}, {@code type}, or {@code protocol} is {@code null}
     */
    public static IceCandidate of(InetSocketAddress address,
                                  Type type,
                                  Protocol protocol,
                                  int localPreference,
                                  int componentId) {
        Objects.requireNonNull(type, "type cannot be null");
        var priority = computePriority(type.preference(), localPreference, componentId);
        return new IceCandidate(address, type, protocol, priority);
    }

    /**
     * Computes the RFC 8445 candidate priority from its three inputs.
     *
     * <p>The priority is {@code (2^24) * typePreference + (2^8) * localPreference + (256 - componentId)},
     * giving the type the dominant weight, then the local preference, then a small component id
     * tiebreaker.
     *
     * @param typePreference  the type preference in {@code 0..126}
     * @param localPreference the local preference in {@code 0..65535}
     * @param componentId     the component id (RTP is {@code 1})
     * @return the {@code u32} candidate priority
     */
    public static long computePriority(int typePreference, int localPreference, int componentId) {
        return ((long) typePreference << 24) + ((long) localPreference << 8) + (256L - componentId);
    }

    /**
     * Enumerates the ICE candidate types in descending default type preference order.
     *
     * @implNote This implementation uses the RFC 8445 recommended type preferences ({@code host} 126,
     *           {@code peer-reflexive} 110, {@code server-reflexive} 100, {@code relayed} 0).
     */
    public enum Type {
        /**
         * A host candidate, a local interface address; the most preferred.
         */
        HOST(126),

        /**
         * A peer reflexive candidate, learned from an inbound check from the peer.
         */
        PEER_REFLEXIVE(110),

        /**
         * A server reflexive candidate, the reflexive address learned from a STUN server.
         */
        SERVER_REFLEXIVE(100),

        /**
         * A relayed candidate, allocated on a TURN relay; the least preferred.
         */
        RELAYED(0);

        /**
         * Holds the default RFC 8445 type preference of this candidate type.
         */
        private final int preference;

        /**
         * Constructs a type bound to its default type preference.
         *
         * @param preference the default RFC 8445 type preference
         */
        Type(int preference) {
            this.preference = preference;
        }

        /**
         * Returns the default RFC 8445 type preference of this candidate type.
         *
         * @return the type preference in {@code 0..126}
         */
        public int preference() {
            return preference;
        }
    }

    /**
     * Enumerates the wire protocols an ICE candidate can use.
     */
    public enum Protocol {
        /**
         * The UDP transport, the default for ICE media.
         */
        UDP,

        /**
         * The TCP transport, used as a fallback when UDP is blocked.
         */
        TCP
    }
}
