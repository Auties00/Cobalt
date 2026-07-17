package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Elects the relay a call binds its media leg to, choosing the relay reachable by the most call parties
 * rather than the one with the lowest local latency.
 *
 * <p>A call is offered several relays; before the media leg comes up every party probes each relay and
 * exchanges its per relay round trip latencies with the others (the {@code <relaylatency>} reports), so each
 * party learns which relays every other party can reach. This helper is the election rule over that exchanged
 * view: given, per relay, the connection of the electing party itself and of each peer, it returns the relay
 * that the most parties can reach, because a one to one call only carries media when both ends bind the
 * <em>same</em> relay. A relay reachable by only one party is never elected; among relays reachable by the
 * same number of parties the lowest summed latency wins.
 *
 * <p>This rule is stateless. A caller builds the {@link Candidate} list from the parsed local probe and the
 * remote {@code <relaylatency>} reports, matching a remote relay to a local one by its
 * {@link com.github.auties00.cobalt.calls.signaling.relay.RelayLatencyEntry#relayName() relay name}, and calls
 * {@link #findBestRelay(List, int, Mode)} once per probe round; the global election passes
 * {@link #SELF_PARTY} as the party index.
 *
 * @implNote This implementation walks the relay list and, per relay, counts the parties (the electing self
 *           when connected plus each connected reachable peer) that reach it and sums their latencies, then
 *           keeps the relay reachable by the most parties, breaking a tie on party count by the lowest summed
 *           latency. In the cross population screen share mode the tie break first prefers the relay reached
 *           by more parties over the primary transport protocol. A relay reachable by at most
 *           {@link #MIN_REACHABLE_PARTIES} parties is rejected. When {@link Mode#preferIncumbent()} is set the
 *           running best latency is lowered by {@link Mode#incumbentBiasMillis()} before a challenger is
 *           compared, applying a relay switch hysteresis. The per party reachability and latency arrive as the
 *           {@link Candidate} input, which the caller builds from
 *           {@link com.github.auties00.cobalt.calls.signaling.relay.RelayLatencyEntry}.
 */
public final class RelayElection {
    /**
     * The logger for {@link RelayElection}.
     */
    private static final System.Logger LOGGER = Log.get(RelayElection.class);

    /**
     * The party index that elects on behalf of the local endpoint itself rather than a specific peer.
     *
     * <p>Passed as the {@code party} argument to {@link #findBestRelay(List, int, Mode)} for the global
     * election that chooses the relay the local media leg binds; a nonnegative value instead computes the
     * best relay for the peer at that index.
     */
    public static final int SELF_PARTY = -1;

    /**
     * The index of the transport protocol that is not connected, reported by {@link Link#connectedProtocol()}.
     */
    public static final int NO_PROTOCOL = -1;

    /**
     * The index of the primary transport protocol, the one the cross population tie break favors.
     */
    private static final int PRIMARY_PROTOCOL = 0;

    /**
     * The minimum party count a relay must reach to be eligible to win the election.
     *
     * <p>A relay reachable by at most this many parties is skipped, so a relay only the electing party can
     * reach never wins; on a one to one call this forces the election onto a relay both ends share.
     */
    private static final int MIN_REACHABLE_PARTIES = 1;

    /**
     * Prevents instantiation of this stateless election holder.
     */
    private RelayElection() {
        throw new AssertionError("RelayElection is not instantiable");
    }

    /**
     * Elects the best relay from the candidate list, returning the elected relay and its alternate.
     *
     * <p>Skips a relay the {@code party} cannot reach, then for each remaining relay counts the parties that
     * reach it (the electing self when it is connected plus every connected, reachable peer) and sums their
     * latencies. Keeps the relay reachable by the most parties; on a tie in party count keeps the lowest
     * summed latency, after the cross population protocol tie break when {@link Mode#crossPopulation()} is
     * set. A relay reachable by a single party is never elected. Returns an empty result when no relay is
     * reachable by more than one party, so the caller falls back to the relay delivered inline.
     *
     * @param relays the relay candidates, in relay order; never {@code null} and holding no {@code null}
     *               element
     * @param party  {@link #SELF_PARTY} to elect for the local endpoint, or a peer index to compute that
     *               peer's best relay
     * @param mode   the election mode flags read from the call's transport state; never {@code null}
     * @return the elected relay and its alternate, or an empty result when no relay is reachable by more than
     *         one party
     * @throws NullPointerException      if {@code relays}, {@code mode}, or any candidate is {@code null}
     * @throws IndexOutOfBoundsException if {@code party} is a peer index a candidate's peer list does not hold
     */
    public static Optional<Result> findBestRelay(List<Candidate> relays, int party, Mode mode) {
        Objects.requireNonNull(relays, "relays cannot be null");
        Objects.requireNonNull(mode, "mode cannot be null");
        var bestRelayIndex = -1;
        var alternateRelayIndex = -1;
        var bestPartyCount = 0;
        var bestPrimaryCount = 0;
        var bestLatencySum = 0;
        var bestMaxLatency = 0;
        for (var relayIndex = 0; relayIndex < relays.size(); relayIndex++) {
            var relay = Objects.requireNonNull(relays.get(relayIndex), "candidate cannot be null");
            var self = relay.self();
            var entryLink = party == SELF_PARTY ? self : relay.peers().get(party);
            if (!entryLink.isConnected()) {
                continue;
            }
            var entryMaxLatency = entryLink.latencyMillis();
            var primaryCount = 0;
            var partyCount = 0;
            var latencySum = 0;
            for (var peer : relay.peers()) {
                if (peer.reachable() && peer.isConnected()) {
                    if (peer.connectedProtocol() == PRIMARY_PROTOCOL) {
                        primaryCount++;
                    }
                    latencySum += peer.latencyMillis();
                    partyCount++;
                }
            }
            if (self.isConnected()) {
                if (self.connectedProtocol() == PRIMARY_PROTOCOL) {
                    primaryCount++;
                }
                latencySum += self.latencyMillis();
                partyCount++;
            }
            if (partyCount <= MIN_REACHABLE_PARTIES || partyCount < bestPartyCount) {
                continue;
            }
            if (partyCount > bestPartyCount) {
                bestRelayIndex = relayIndex;
                alternateRelayIndex = -1;
                bestPartyCount = partyCount;
                bestPrimaryCount = primaryCount;
                bestLatencySum = latencySum;
                bestMaxLatency = entryMaxLatency;
                continue;
            }
            if (mode.crossPopulation()) {
                if (primaryCount < bestPrimaryCount) {
                    continue;
                }
                if (primaryCount > bestPrimaryCount) {
                    bestRelayIndex = relayIndex;
                    bestPrimaryCount = primaryCount;
                    bestLatencySum = latencySum;
                    bestMaxLatency = entryMaxLatency;
                    continue;
                }
            }
            // Matches find_best_relay (wa_transport_relay_election.cc, voip WASM func 5419): the max latency
            // is folded as a running minimum over every candidate tied at the top party count and applied
            // before the latency election below, so the reported value can come from a relay that did not
            // win. This is WA behavior, not a defect; every find_best_relay caller reads only the elected and
            // alternate relay indices and discards this value, so it is diagnostic only.
            if (entryMaxLatency < bestMaxLatency) {
                bestMaxLatency = entryMaxLatency;
            }
            var threshold = mode.preferIncumbent() ? bestLatencySum - mode.incumbentBiasMillis() : bestLatencySum;
            if (latencySum < threshold) {
                bestLatencySum = latencySum;
                bestRelayIndex = relayIndex;
                bestMaxLatency = entryMaxLatency;
            }
            if (bestRelayIndex != -1 && bestRelayIndex != relayIndex && alternateRelayIndex == -1) {
                alternateRelayIndex = relayIndex;
            }
        }
        if (bestRelayIndex < 0) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "calls relay election found no relay reachable by more than one party "
                        + "for party={0} among {1} candidate(s)", party, relays.size());
            }
            return Optional.empty();
        }
        var alternateRelayId = alternateRelayIndex < 0 ? -1 : relays.get(alternateRelayIndex).relayId();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "calls relay election elected relayId={0} (partyCount={1}, latencySum={2}ms) for party={3}, "
                            + "alternate relayId={4}",
                    relays.get(bestRelayIndex).relayId(), bestPartyCount, bestLatencySum, party, alternateRelayId);
        }
        return Optional.of(new Result(bestRelayIndex, relays.get(bestRelayIndex).relayId(),
                alternateRelayIndex, alternateRelayId, bestLatencySum, bestMaxLatency));
    }

    /**
     * One party's connection to one relay over its connected transport protocol.
     *
     * <p>A party reaches a relay when it is {@link #reachable() marked reachable} for that relay and has a
     * {@link #connectedProtocol() connected protocol}; the {@link #latencyMillis() latency} is then that
     * party's measured round trip latency to the relay. The electing self is treated as reachable whenever it
     * is connected, so its {@code reachable} component is not consulted.
     *
     * @param reachable         whether this party is marked reachable for the relay, consulted only for peers
     * @param connectedProtocol the index of the connected transport protocol, or {@link #NO_PROTOCOL} when the
     *                          party has not connected to the relay
     * @param latencyMillis     the round trip latency to the relay over the connected protocol, in
     *                          milliseconds
     */
    public record Link(boolean reachable, int connectedProtocol, int latencyMillis) {
        /**
         * A link for a party that has not connected to the relay.
         */
        public static final Link UNREACHABLE = new Link(false, NO_PROTOCOL, 0);

        /**
         * Returns whether this party has a connected transport protocol to the relay.
         *
         * @return {@code true} when {@link #connectedProtocol()} is not {@link #NO_PROTOCOL}
         */
        public boolean isConnected() {
            return connectedProtocol != NO_PROTOCOL;
        }
    }

    /**
     * One relay candidate in an election round, pairing a relay identifier with the per party connections to
     * it.
     *
     * <p>The {@code relayId} names the relay so the caller can map the elected index back to the relay it
     * binds. The {@code self} link is the electing endpoint's own connection to the relay; the {@code peers}
     * list holds one {@link Link} per peer, indexed by party index, each an {@link Link#UNREACHABLE}
     * placeholder for a peer that has not reported reaching this relay.
     *
     * @param relayId the relay identifier
     * @param self    the local endpoint's connection to this relay
     * @param peers   the per peer connections to this relay, indexed by party index; never {@code null}
     */
    public record Candidate(int relayId, Link self, List<Link> peers) {
        /**
         * Canonicalizes the record, rejecting a {@code null} self or peer list and copying the peer list.
         *
         * @throws NullPointerException if {@code self} or {@code peers} is {@code null} or holds a
         *                              {@code null} element
         */
        public Candidate {
            Objects.requireNonNull(self, "self cannot be null");
            peers = List.copyOf(peers);
        }
    }

    /**
     * The election mode flags read from the call's transport state.
     *
     * <p>The {@code crossPopulation} flag selects the screen share tie break that prefers the relay more
     * parties reach over the primary transport protocol before comparing latency. The {@code preferIncumbent}
     * flag and {@code incumbentBiasMillis} apply the relay switch hysteresis: when set, the running best
     * latency is lowered by the bias before a challenger's latency is compared, so a marginally faster relay
     * does not trigger a switch.
     *
     * @param crossPopulation     whether the call is in the cross population screen share mode
     * @param preferIncumbent     whether to apply the relay switch hysteresis bias
     * @param incumbentBiasMillis the latency bias, in milliseconds, subtracted from the running best when
     *                            {@code preferIncumbent} is set
     */
    public record Mode(boolean crossPopulation, boolean preferIncumbent, int incumbentBiasMillis) {
        /**
         * The default mode for a one to one call: no cross population tie break and no switch hysteresis.
         */
        public static final Mode DEFAULT = new Mode(false, false, 0);
    }

    /**
     * The result of an election: the elected relay, an alternate relay, and a diagnostic latency.
     *
     * @param index            the index of the elected candidate in the candidate list
     * @param relayId          the identifier of the elected relay
     * @param alternateIndex   the index of the alternate candidate, or {@code -1} when none was recorded
     * @param alternateRelayId the identifier of the alternate relay, or {@code -1} when none was recorded
     * @param latencySumMillis the summed party latency of the elected relay, in milliseconds
     * @param maxLatencyMillis the running minimum of the electing party's per-relay latency across the
     *                         candidates tied at the top party count, in milliseconds; because it is folded
     *                         before the latency election it need not equal the elected relay's own latency,
     *                         and it is diagnostic only
     */
    public record Result(int index,
                         int relayId,
                         int alternateIndex,
                         int alternateRelayId,
                         int latencySumMillis,
                         int maxLatencyMillis) {
    }
}
