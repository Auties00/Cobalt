package com.github.auties00.cobalt.calls.transport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.signal.BweHistory;
import com.github.auties00.cobalt.calls.transport.ice.IceAgent;
import com.github.auties00.cobalt.calls.transport.ice.IceCandidate;
import com.github.auties00.cobalt.calls.transport.ice.IceCandidatePair;
import com.github.auties00.cobalt.calls.transport.stun.StunMessage;

/**
 * Verifies the pure-logic transport helpers the relay and Web-P2P paths share: the RFC 8445 ICE
 * candidate and pair priority formulas and checklist ordering, and the per-call bandwidth-estimate
 * history seeding and fast-probe gate. The ICE priority expectations are computed from the RFC formula
 * by hand so a shift or term error is caught.
 */
class IceAndBweHistoryTest {
    @Nested
    @DisplayName("ICE candidate and pair priority")
    class Priority {
        @Test
        @DisplayName("candidate priority follows the RFC 8445 (2^24 typePref + 2^8 localPref + 256 - component) formula")
        void candidatePriority() {
            var expected = (126L << 24) + (65535L << 8) + (256L - 1);
            assertEquals(expected, IceCandidate.computePriority(126, 65535, 1));
        }

        @Test
        @DisplayName("a host candidate outranks a server-reflexive candidate of equal local preference")
        void hostOutranksSrflx() {
            var host = IceCandidate.of(new InetSocketAddress("10.0.0.1", 5000),
                    IceCandidate.Type.HOST, IceCandidate.Protocol.UDP, 65535, 1);
            var srflx = IceCandidate.of(new InetSocketAddress("203.0.113.1", 5000),
                    IceCandidate.Type.SERVER_REFLEXIVE, IceCandidate.Protocol.UDP, 65535, 1);
            assertTrue(host.priority() > srflx.priority());
        }

        @Test
        @DisplayName("pair priority weights the lower of the two candidate priorities by 2^32")
        void pairPriority() {
            var g = 100L;
            var d = 200L;
            var expected = (Math.min(g, d) << 32) + (2 * Math.max(g, d)) + 0;
            assertEquals(expected, IceCandidatePair.computePairPriority(g, d, true));
        }

        @Test
        @DisplayName("two candidates of different protocols are incompatible")
        void protocolMismatchIncompatible() {
            var udp = IceCandidate.of(new InetSocketAddress("10.0.0.1", 5000),
                    IceCandidate.Type.HOST, IceCandidate.Protocol.UDP, 100, 1);
            var tcp = IceCandidate.of(new InetSocketAddress("10.0.0.2", 5000),
                    IceCandidate.Type.HOST, IceCandidate.Protocol.TCP, 100, 1);
            assertFalse(IceCandidatePair.isCompatible(udp, tcp));
        }
    }

    @Nested
    @DisplayName("ICE agent checklist")
    class Checklist {
        @Test
        @DisplayName("forms pairs from every compatible combination and sorts them descending by priority")
        void formsAndSorts() {
            var agent = new IceAgent("local", "lpwd".getBytes(), "remote", "rpwd".getBytes(), true);
            agent.addLocalCandidate(IceCandidate.of(new InetSocketAddress("10.0.0.1", 5000),
                    IceCandidate.Type.HOST, IceCandidate.Protocol.UDP, 65535, 1));
            agent.addLocalCandidate(IceCandidate.of(new InetSocketAddress("203.0.113.1", 5000),
                    IceCandidate.Type.SERVER_REFLEXIVE, IceCandidate.Protocol.UDP, 65535, 1));
            agent.appendRemoteCandidates(List.of(IceCandidate.of(new InetSocketAddress("10.0.0.9", 6000),
                    IceCandidate.Type.HOST, IceCandidate.Protocol.UDP, 65535, 1)));
            var checklist = agent.checklist();
            assertEquals(2, checklist.size());
            assertTrue(checklist.get(0).priority() >= checklist.get(1).priority());
        }

        @Test
        @DisplayName("rejects more remote candidates than the bound allows")
        void rejectsTooManyRemote() {
            var agent = new IceAgent("local", "lpwd".getBytes(), "remote", "rpwd".getBytes(), false);
            var many = new java.util.ArrayList<IceCandidate>();
            for (var index = 0; index <= IceAgent.MAX_REMOTE_CANDIDATES; index++) {
                many.add(IceCandidate.of(new InetSocketAddress("10.0.0." + (index % 250), 6000),
                        IceCandidate.Type.HOST, IceCandidate.Protocol.UDP, 100, 1));
            }
            assertThrows(IllegalArgumentException.class, () -> agent.appendRemoteCandidates(many));
        }

        @Test
        @DisplayName("a controlled agent cannot build a nominating binding request")
        void controlledCannotNominate() {
            var agent = new IceAgent("local", "lpwd".getBytes(), "remote", "rpwd".getBytes(), false);
            var pair = new IceCandidatePair(
                    IceCandidate.of(new InetSocketAddress("10.0.0.1", 5000), IceCandidate.Type.HOST, IceCandidate.Protocol.UDP, 100, 1),
                    IceCandidate.of(new InetSocketAddress("10.0.0.9", 6000), IceCandidate.Type.HOST, IceCandidate.Protocol.UDP, 100, 1),
                    false);
            assertThrows(IllegalStateException.class,
                    () -> agent.buildBindingRequest(pair, new byte[StunMessage.TRANSACTION_ID_LENGTH], true));
        }
    }

    @Nested
    @DisplayName("BWE history")
    class History {
        @Test
        @DisplayName("seeds from the mean of recorded samples")
        void seedsFromMean() {
            var history = new BweHistory();
            history.record(1000);
            history.record(2000);
            history.record(3000);
            assertEquals(2000, history.seedEstimateKbps());
        }

        @Test
        @DisplayName("ignores a non-positive sample")
        void ignoresNonPositive() {
            var history = new BweHistory();
            history.record(1000);
            history.record(0);
            history.record(-5);
            assertEquals(1, history.size());
            assertEquals(1000, history.seedEstimateKbps());
        }

        @Test
        @DisplayName("activates fast-probe mode when empty or when the latest sample is at or below the threshold")
        void fastProbeGate() {
            var history = new BweHistory();
            assertTrue(history.shouldActivateSfuFastProbeMode());
            history.record(BweHistory.FAST_PROBE_THRESHOLD_KBPS);
            assertTrue(history.shouldActivateSfuFastProbeMode());
            history.record(BweHistory.FAST_PROBE_THRESHOLD_KBPS + 5000);
            assertFalse(history.shouldActivateSfuFastProbeMode());
        }

        @Test
        @DisplayName("overwrites the oldest sample once the ring is full")
        void overwritesWhenFull() {
            var history = new BweHistory();
            for (var index = 0; index < BweHistory.CAPACITY + 4; index++) {
                history.record(1000);
            }
            assertEquals(BweHistory.CAPACITY, history.size());
        }
    }
}
