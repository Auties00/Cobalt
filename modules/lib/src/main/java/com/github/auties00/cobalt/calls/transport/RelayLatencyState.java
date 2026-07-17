package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.calls.signaling.relay.RelayEndpoint;
import com.github.auties00.cobalt.calls.signaling.relay.RelayLatencyEntry;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the round trip latencies to each relay that the two ends of a call exchange and runs the peer aware
 * relay election over them.
 *
 * <p>A relay call only carries media when both ends bind the same relay, yet each end is offered its own
 * relay set with its own {@code c2r_rtt} estimates, so picking the lowest local latency lets the two ends
 * bind different relays and carry nothing. This state reproduces the relay latency exchange the engine runs
 * to converge them: each end probes its relays, reports its measured latencies in a {@code <relaylatency>}
 * message, and feeds the latencies it receives from the other end into {@link RelayElection#findBestRelay}
 * keyed by the relay's {@linkplain RelayEndpoint#relayName() name}, which is the relay identity shared across
 * both ends' offers. A relay both ends reported is reachable by both parties and wins over a relay only the
 * local end can reach.
 *
 * <p>Local latencies are seeded from the {@code c2r_rtt} the server measured on the offer ack, then refined
 * in place by {@link #recordProbeLatency(String, int)} as the live probe round measures each relay; remote
 * latencies arrive through {@link #recordPeerLatencies(List)} as the peer's {@code <relaylatency>} messages
 * are received. The election is recomputed on demand by {@link #electBestRelayName(RelayElection.Mode)}; it
 * yields a relay name only once a relay both ends reported exists, so before the peer reports anything the
 * caller falls back to its local pick. This state is not thread safe: the single call transport thread that
 * drives the probe round and consumes inbound signaling also calls every method here.
 *
 * @implNote This implementation keys its per relay latency table by {@link RelayEndpoint#relayName()}, the
 *           {@code relay_name} identity carried on each {@link RelayLatencyEntry}, because that is the shared
 *           identity both ends' offers list. The single peer matrix it builds reduces to the same outcome for
 *           the one to one and small group cases: each relay carries the local end's connection and the one
 *           peer's, and the relay both reported is elected.
 */
public final class RelayLatencyState {
    /**
     * The logger for {@link RelayLatencyState}.
     */
    private static final System.Logger LOGGER = Log.get(RelayLatencyState.class);

    /**
     * The transport protocol index reported for a relay seeded from signaling rather than a live probe.
     *
     * <p>The exchange keys convergence on the relay name, not the transport protocol, so both ends are
     * modeled as connected over the primary protocol; the protocol aware tie break only refines a choice
     * among relays both ends already reach.
     */
    private static final int PRIMARY_PROTOCOL = 0;

    /**
     * The local end's measured round trip latency to each relay, keyed by relay name in offer order.
     *
     * <p>Seeded from the {@code c2r_rtt} on the offer ack and refined by
     * {@link #recordProbeLatency(String, int)}; the iteration order is the relay order the offer ack listed,
     * so the election walks relays in that order. Never {@code null}.
     */
    private final Map<String, Integer> localLatencies;

    /**
     * The peer's reported round trip latency to each relay, keyed by relay name.
     *
     * <p>Populated by {@link #recordPeerLatencies(List)} from the peer's {@code <relaylatency>} reports; a
     * relay name present here is one the peer can reach. Never {@code null}.
     */
    private final Map<String, Integer> peerLatencies;

    /**
     * Constructs the latency state from the local end's offered relay endpoints.
     *
     * <p>Each endpoint that carries a {@linkplain RelayEndpoint#relayName() relay name} seeds a local
     * latency from its {@code c2r_rtt}, or its {@code xrtt_ms} estimate when {@code c2r_rtt} is absent;
     * endpoints sharing a relay name (the primary and alternate address families) keep the lowest seed.
     * Endpoints without a relay name are skipped because the exchange cannot key them to the peer's reports.
     *
     * @param ownEndpoints the local end's offered relay endpoints, in offer order; never {@code null}
     * @throws NullPointerException if {@code ownEndpoints} is {@code null} or holds a {@code null} element
     */
    public RelayLatencyState(List<RelayEndpoint> ownEndpoints) {
        Objects.requireNonNull(ownEndpoints, "ownEndpoints cannot be null");
        this.localLatencies = new LinkedHashMap<>();
        this.peerLatencies = new LinkedHashMap<>();
        for (var endpoint : ownEndpoints) {
            Objects.requireNonNull(endpoint, "endpoint cannot be null");
            var relayName = endpoint.relayName();
            if (relayName == null) {
                continue;
            }
            var seed = endpoint.c2rRttValue().orElse(endpoint.xrttMs());
            localLatencies.merge(relayName, seed, Math::min);
        }
    }

    /**
     * Records the live probe round trip latency measured to one relay, replacing its seeded estimate.
     *
     * <p>Called as the probe round times each relay's response; a relay name the local end did not offer is
     * ignored. The refined latency feeds the next {@link #electBestRelayName(RelayElection.Mode) election}
     * and the next {@link #toLatencyEntries() report}.
     *
     * @param relayName    the name of the probed relay; never {@code null}
     * @param latencyMillis the measured round trip latency, in milliseconds
     * @throws NullPointerException if {@code relayName} is {@code null}
     */
    public void recordProbeLatency(String relayName, int latencyMillis) {
        Objects.requireNonNull(relayName, "relayName cannot be null");
        if (localLatencies.containsKey(relayName)) {
            localLatencies.put(relayName, latencyMillis);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "calls relay probe latency recorded: relay={0}, latency={1}ms",
                        relayName, latencyMillis);
            }
        }
    }

    /**
     * Records the peer's reported round trip latencies, marking the relays the peer can reach.
     *
     * <p>Each entry that carries a {@linkplain RelayLatencyEntry#relayName() relay name} marks that relay
     * reachable by the peer at the reported latency; a later report for the same relay replaces the earlier
     * value, matching the engine's transaction keyed overwrite. Entries without a relay name are skipped.
     *
     * @param entries the peer's {@code <relaylatency>} entries; never {@code null}
     * @throws NullPointerException if {@code entries} is {@code null} or holds a {@code null} element
     */
    public void recordPeerLatencies(List<RelayLatencyEntry> entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        var recorded = 0;
        for (var entry : entries) {
            Objects.requireNonNull(entry, "entry cannot be null");
            var relayName = entry.relayName();
            if (relayName != null && entry.isReachable()) {
                peerLatencies.put(relayName, RelayLatencyEntry.unpackLatencyMillis(entry.latency()));
                recorded++;
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "calls relay peer latencies recorded: {0} of {1} entries reachable",
                    recorded, entries.size());
        }
    }

    /**
     * Builds the local end's {@code <relaylatency>} entries from its current per relay latencies.
     *
     * <p>One entry is produced per relay the local end offered, carrying the relay name and the current
     * latency (seeded from {@code c2r_rtt} or refined by the probe round). The result is suitable for a
     * {@link com.github.auties00.cobalt.calls.signaling.relay.RelayLatencyStanza} sent to the peer.
     *
     * @return the local latency entries, one per offered relay, in offer order
     */
    public List<RelayLatencyEntry> toLatencyEntries() {
        var entries = new ArrayList<RelayLatencyEntry>(localLatencies.size());
        for (var entry : localLatencies.entrySet()) {
            entries.add(new RelayLatencyEntry(RelayLatencyEntry.packLatency(entry.getValue()), false,
                    -1, -1, false, entry.getKey(), null, null));
        }
        return entries;
    }

    /**
     * Elects the relay both ends can reach with the lowest combined latency, returning its name.
     *
     * <p>Builds one {@link RelayElection.Candidate} per relay the local end offered, with the local end's
     * connection always present and the single peer's connection present only for a relay the peer reported,
     * then runs {@link RelayElection#findBestRelay}. The result is a relay both ends reach; an empty result
     * means no relay both ends reported exists yet, so the caller keeps its local pick until the peer's
     * report arrives.
     *
     * @param mode the election mode flags; never {@code null}
     * @return the elected relay name, or an empty result when no relay both ends reach is known yet
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public Optional<String> electBestRelayName(RelayElection.Mode mode) {
        Objects.requireNonNull(mode, "mode cannot be null");
        var relayNames = new ArrayList<String>(localLatencies.size());
        var candidates = new ArrayList<RelayElection.Candidate>(localLatencies.size());
        var relayId = 0;
        for (var entry : localLatencies.entrySet()) {
            var relayName = entry.getKey();
            var self = new RelayElection.Link(true, PRIMARY_PROTOCOL, entry.getValue());
            var peerLatency = peerLatencies.get(relayName);
            var peer = peerLatency == null
                    ? RelayElection.Link.UNREACHABLE
                    : new RelayElection.Link(true, PRIMARY_PROTOCOL, peerLatency);
            relayNames.add(relayName);
            candidates.add(new RelayElection.Candidate(relayId++, self, List.of(peer)));
        }
        var elected = RelayElection.findBestRelay(candidates, RelayElection.SELF_PARTY, mode)
                .map(result -> relayNames.get(result.index()));
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "calls relay latency election result: relay={0}",
                    elected.isPresent() ? elected.get() : "NONE");
        }
        return elected;
    }
}
