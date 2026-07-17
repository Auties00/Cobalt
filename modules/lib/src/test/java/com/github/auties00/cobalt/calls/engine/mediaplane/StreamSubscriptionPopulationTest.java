package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.transport.subscription.StreamLayout;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionExt;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionsSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptionsSpec;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.participant.CallSecureSsrcGenerator;

/**
 * Verifies that the runtime population of the fused {@code 0x4024} {@link StreamSubscriptions} matrix in
 * {@code LiveMediaSession.LiveMediaPlane#buildStreamSubscriptions} reproduces the live group-call capture:
 * the participant-less self block first, then one positionally-indexed block of audio plus simulcast-video
 * entries per connected peer, with each entry's SSRC the deterministic per-device value the roster tracks.
 *
 * <p>The gold case feeds the exact per-(participant, stream) SSRCs the connected three-way group-call capture
 * decodes to (re/calls2-spec/captures/webrtc-datachannel-transport-2026-06-21.md) and asserts the built matrix
 * encodes to the captured ninety-five-byte attribute value byte-for-byte, so the population's participant
 * indexing (self omitted, peers one-based by roster order) and stream indexing (audio absent, video
 * {@code 1}/{@code 2}) are tied to the capture rather than to the implementation.
 */
@DisplayName("calls fused 0x4024 stream-subscription population")
class StreamSubscriptionPopulationTest {
    private static final HexFormat HEX = HexFormat.of();

    // The captured 0x4024 attribute value: nine repeated field-1 entries {participant?, stream?, ssrc}, from
    // the connected 3-way group call (self + 2 peers), as decoded in TransportWireFormatTest.
    private static final byte[] CAPTURED_SUBSCRIPTION_VALUE = HEX.parseHex(
            "0a0618c3a2a8930f"
                    + "0a08100118bbd0fce00e"
                    + "0a08100218b9e4b9be01"
                    + "0a08080118b1f2c8c00a"
                    + "0a0a0801100118cbdc86bc01"
                    + "0a0a0801100218e597cef80b"
                    + "0a08080218d2c8bfbb05"
                    + "0a09080210011899d1f31b"
                    + "0a0a0802100218daa898a40c");

    // The self stream SSRCs the capture's first (participant-less) block decodes to: audio main, then the two
    // simulcast video primary SSRCs. These are the local send layout's SSRCs.
    private static final int SELF_AUDIO = 0xf26a1143;
    private static final int SELF_VIDEO0 = 0xec1f283b;
    private static final int SELF_VIDEO1 = 0x17ce7239;

    // The two connected peers' stream SSRCs the capture's participant=1 and participant=2 blocks decode to.
    private static final CallMembership.PeerStreamSsrcs PEER_1 =
            new CallMembership.PeerStreamSsrcs(0xa8123931, 0x1781ae4b, 0xbf138be5);
    private static final CallMembership.PeerStreamSsrcs PEER_2 =
            new CallMembership.PeerStreamSsrcs(0x576fe452, 0x037ce899, 0xc486145a);

    private static StreamLayout videoSelfLayout() {
        return new StreamLayout(SELF_AUDIO, SELF_VIDEO0, SELF_VIDEO1, StreamLayout.ABSENT_SSRC,
                StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC, false);
    }

    private static StreamSubscriptions build(StreamLayout self, List<CallMembership.PeerStreamSsrcs> peers,
                                             boolean video) {
        return LiveMediaSession.LiveMediaPlane.buildStreamSubscriptions(self, peers, video);
    }

    @Nested
    @DisplayName("captured 3-way video group call")
    class CapturedGroupCall {
        @Test
        @DisplayName("population of self + two peers encodes to the captured 0x4024 value byte-for-byte")
        void matchesCapture() {
            var built = build(videoSelfLayout(), List.of(PEER_1, PEER_2), true);
            var encoded = StreamSubscriptionsSpec.encode(built);
            assertEquals(95, encoded.length);
            assertArrayEquals(CAPTURED_SUBSCRIPTION_VALUE, encoded,
                    "the populated matrix must reproduce the captured 0x4024 value byte-for-byte");
        }

        @Test
        @DisplayName("the self block omits the participant field and carries the own WARP audio SSRC first")
        void selfBlockShape() {
            var entries = build(videoSelfLayout(), List.of(PEER_1, PEER_2), true).entries();
            var selfAudio = entries.getFirst();
            assertTrue(selfAudio.participant().isEmpty(), "the self audio entry carries no participant index");
            assertTrue(selfAudio.stream().isEmpty(), "the self audio entry carries no stream index");
            assertEquals(Integer.toUnsignedLong(SELF_AUDIO), selfAudio.ssrc().orElseThrow());
        }

        @Test
        @DisplayName("each peer block is tagged with its one-based roster position, not a relay PID")
        void peerBlocksArePositional() {
            var entries = build(videoSelfLayout(), List.of(PEER_1, PEER_2), true).entries();
            // 3 self entries, then 3 per peer: peer 1 at index 3, peer 2 at index 6.
            var peer1Audio = entries.get(3);
            var peer2Audio = entries.get(6);
            assertEquals(OptionalInt.of(1), peer1Audio.participant());
            assertEquals(Integer.toUnsignedLong(PEER_1.audioSsrc()), peer1Audio.ssrc().orElseThrow());
            assertEquals(OptionalInt.of(2), peer2Audio.participant());
            assertEquals(Integer.toUnsignedLong(PEER_2.audioSsrc()), peer2Audio.ssrc().orElseThrow());
        }

        @Test
        @DisplayName("video entries carry stream index 1 and 2 with the per-stream simulcast SSRCs")
        void videoStreamIndices() {
            var entries = build(videoSelfLayout(), List.of(PEER_1, PEER_2), true).entries();
            var peer1Video0 = entries.get(4);
            var peer1Video1 = entries.get(5);
            assertEquals(OptionalInt.of(1), peer1Video0.participant());
            assertEquals(OptionalInt.of(1), peer1Video0.stream());
            assertEquals(Integer.toUnsignedLong(PEER_1.videoStream0Ssrc()), peer1Video0.ssrc().orElseThrow());
            assertEquals(OptionalInt.of(2), peer1Video1.stream());
            assertEquals(Integer.toUnsignedLong(PEER_1.videoStream1Ssrc()), peer1Video1.ssrc().orElseThrow());
        }
    }

    @Nested
    @DisplayName("degenerate and audio-only layouts")
    class Edges {
        @Test
        @DisplayName("an audio-only call emits only audio entries, no stream-indexed video entries")
        void audioOnlyOmitsVideo() {
            var audioSelf = StreamLayout.audioOnly(SELF_AUDIO);
            // Peers still carry derived video SSRCs, but the audio-only (video=false) call omits all video.
            var built = build(audioSelf, List.of(PEER_1, PEER_2), false);
            var entries = built.entries();
            assertEquals(3, entries.size(), "one audio entry for self and one per peer");
            for (var entry : entries) {
                assertTrue(entry.stream().isEmpty(), "no video stream entries on an audio-only call");
            }
            assertEquals(Integer.toUnsignedLong(SELF_AUDIO), entries.get(0).ssrc().orElseThrow());
            assertEquals(OptionalInt.of(1), entries.get(1).participant());
            assertEquals(OptionalInt.of(2), entries.get(2).participant());
        }

        @Test
        @DisplayName("no connected peers yields the self block alone")
        void selfOnly() {
            var built = build(videoSelfLayout(), List.of(), true);
            var entries = built.entries();
            assertEquals(3, entries.size(), "self audio plus two self video entries");
            for (var entry : entries) {
                assertTrue(entry.participant().isEmpty(), "the self block carries no participant index");
            }
        }

        @Test
        @DisplayName("a peer missing a simulcast video SSRC omits only that video entry")
        void peerMissingVideoStream() {
            var partialPeer = new CallMembership.PeerStreamSsrcs(0xa8123931, 0x1781ae4b, 0);
            var built = build(StreamLayout.audioOnly(SELF_AUDIO), List.of(partialPeer), true);
            var entries = built.entries();
            // self audio (1) + peer audio + peer video stream 0 only.
            assertEquals(3, entries.size());
            assertEquals(OptionalInt.of(1), entries.get(2).participant());
            assertEquals(OptionalInt.of(1), entries.get(2).stream());
            assertEquals(Integer.toUnsignedLong(0x1781ae4b), entries.get(2).ssrc().orElseThrow());
        }
    }

    /**
     * Verifies the self {@code 0x4025} {@link SenderSubscriptions} build in
     * {@code LiveMediaSession.LiveMediaPlane#buildSelfSenderSubscriptions}: the four ordered SSRC-to-PID
     * assignment sources (video stream 0, video stream 1, audio, app-data), the packed unsigned SSRCs in
     * {@code [primary, FEC, NACK]} order, the {@code (selfPid, ENHANCEMENT)} descriptor on video stream 0 only
     * when sending video, and the {@code (selfPid, BASE)} descriptors on audio and app-data with the BASE layer
     * dropped as the proto3 default.
     *
     * <p>The fixed inputs are the byte-verified live caller (call-id and device JID from
     * {@code CallSecureSsrcGenerator}, whose audio/video SSRC triples are pinned against a live call); the
     * expected attribute value is reconstructed here independently from the protobuf wire grammar and the
     * external-ground-truth SSRC constants, never copied from the production encoder, so a nesting, ordering,
     * packed-varint, or descriptor-placement regression is caught. The live 0x4025 caller capture was 86 bytes
     * for its own session; the exact length is per-call (the deterministic SSRC and self-PID varint widths are
     * session-specific), so this vector asserts its own independently-reconstructed length rather than a magic 86.
     */
    @Nested
    @DisplayName("self 0x4025 sender-subscription build")
    class SenderSubscriptionsBuild {
        // The byte-verified live caller (CallSecureSsrcGenerator javadoc): call-id and "<lid>:<device>@lid"
        // device JID whose audio and simulcast-video SSRC triples are pinned against a real call.
        private static final String CALL_ID = "007498E578A915C0F9814AC2CB48D28F";
        private static final Jid SELF_JID = Jid.of("258252122116273:94@lid");
        private static final int SELF_PID = 1;

        // The SSRC triples the live caller derives for this call-id/JID, as external ground truth (audio and
        // video are byte-verified in CallSecureSsrcGenerator; app-data recomputed by the same HKDF schedule).
        private static final int[] VIDEO0 = {0xA4AF8E8F, 0x2D89D462, 0x935DF37C};
        private static final int[] VIDEO1 = {0xCC5B7B35, 0xC8FE9CF6, 0xB49EB3A4};
        private static final int[] AUDIO = {0xE438BF63, 0xD3CB93DC, 0x6DE76824};
        private static final int[] APP_DATA = {0x33626DC5};

        @Test
        @DisplayName("sending video encodes the four sources to the independently reconstructed 0x4025 value")
        void matchesIndependentReconstruction() {
            var built = LiveMediaSession.LiveMediaPlane.buildSelfSenderSubscriptions(CALL_ID, SELF_JID, SELF_PID, true);
            var encoded = SenderSubscriptionsSpec.encode(built);
            var expected = concat(
                    senderSource(VIDEO0, SELF_PID, true),
                    senderSource(VIDEO1, null, false),
                    senderSource(AUDIO, SELF_PID, false),
                    senderSource(APP_DATA, SELF_PID, false));
            assertArrayEquals(expected, encoded,
                    "the 0x4025 value must match the independent four-source reconstruction byte-for-byte");
        }

        @Test
        @DisplayName("the four sources nest, order, and carry their descriptors exactly")
        void sourceStructure() {
            var built = LiveMediaSession.LiveMediaPlane.buildSelfSenderSubscriptions(CALL_ID, SELF_JID, SELF_PID, true);
            var sources = built.subscriptions();
            assertEquals(4, sources.size(), "video0, video1, audio, app-data");

            var video0 = sources.get(0).ssrcLayers().orElseThrow();
            assertEquals(List.of(toUnsigned(VIDEO0[0]), toUnsigned(VIDEO0[1]), toUnsigned(VIDEO0[2])),
                    video0.ssrcs(), "video stream 0 carries [primary, FEC, NACK] in order");
            assertEquals(1, video0.pids().size());
            assertEquals(OptionalInt.of(SELF_PID), video0.pids().getFirst().pid());
            assertEquals(SenderSubscriptionExt.TemporalLayer.ENHANCEMENT,
                    video0.pids().getFirst().layerId().orElseThrow(), "video stream 0 is the ENHANCEMENT layer");

            var video1 = sources.get(1).ssrcLayers().orElseThrow();
            assertEquals(3, video1.ssrcs().size());
            assertTrue(video1.pids().isEmpty(), "video stream 1 carries no descriptor");

            var audio = sources.get(2).ssrcLayers().orElseThrow();
            assertEquals(List.of(toUnsigned(AUDIO[0]), toUnsigned(AUDIO[1]), toUnsigned(AUDIO[2])), audio.ssrcs());
            assertEquals(1, audio.pids().size());
            assertEquals(OptionalInt.of(SELF_PID), audio.pids().getFirst().pid());
            assertTrue(audio.pids().getFirst().layerId().isEmpty(), "audio is the dropped BASE default layer");

            var appData = sources.get(3).ssrcLayers().orElseThrow();
            assertEquals(List.of(toUnsigned(APP_DATA[0])), appData.ssrcs(), "app-data carries a single SSRC");
            assertEquals(1, appData.pids().size());
            assertEquals(OptionalInt.of(SELF_PID), appData.pids().getFirst().pid());
            assertTrue(appData.pids().getFirst().layerId().isEmpty(), "app-data is the dropped BASE default layer");
        }

        @Test
        @DisplayName("the encoded value round-trips and re-encodes to itself")
        void roundTrips() {
            var built = LiveMediaSession.LiveMediaPlane.buildSelfSenderSubscriptions(CALL_ID, SELF_JID, SELF_PID, true);
            var encoded = SenderSubscriptionsSpec.encode(built);
            var reEncoded = SenderSubscriptionsSpec.encode(SenderSubscriptionsSpec.decode(encoded));
            assertArrayEquals(encoded, reEncoded);
        }

        @Test
        @DisplayName("an audio-only call drops the video-stream-0 descriptor but keeps all four SSRC sources")
        void audioOnlyDropsVideoDescriptor() {
            var built = LiveMediaSession.LiveMediaPlane.buildSelfSenderSubscriptions(CALL_ID, SELF_JID, SELF_PID, false);
            var sources = built.subscriptions();
            assertEquals(4, sources.size(), "all four SSRC sources are present regardless of video");
            var video0 = sources.get(0).ssrcLayers().orElseThrow();
            assertEquals(3, video0.ssrcs().size(), "video stream 0 still declares its SSRC triple");
            assertTrue(video0.pids().isEmpty(), "no video-stream-0 descriptor when not sending video");
            assertEquals(1, sources.get(2).ssrcLayers().orElseThrow().pids().size(), "audio keeps its BASE descriptor");
            assertEquals(1, sources.get(3).ssrcLayers().orElseThrow().pids().size(), "app-data keeps its BASE descriptor");
        }

        @Test
        @DisplayName("a null self device JID yields an empty subscription list")
        void noIdentityYieldsEmpty() {
            var built = LiveMediaSession.LiveMediaPlane.buildSelfSenderSubscriptions(CALL_ID, null, SELF_PID, true);
            assertTrue(built.subscriptions().isEmpty());
            assertEquals(0, SenderSubscriptionsSpec.encode(built).length);
        }

        private static long toUnsigned(int ssrc) {
            return Integer.toUnsignedLong(ssrc);
        }

        // Independent protobuf assembly of one SenderSubscriptions entry (the triple 0a/0a/0a nesting): a
        // SSrcsToPidAssignments holding a packed unsigned-varint ssrcs field (field 1) and, when pid is present,
        // a PidTemporalLayer descriptor (field 2) carrying 08 <pid> plus 10 01 only for the ENHANCEMENT layer.
        private static byte[] senderSource(int[] ssrcs, Integer pid, boolean enhancement) {
            var ssrcBody = new ByteArrayOutputStream();
            for (var ssrc : ssrcs) {
                ssrcBody.writeBytes(varint(Integer.toUnsignedLong(ssrc)));
            }
            var assignments = new ByteArrayOutputStream();
            assignments.writeBytes(lengthDelimited(1, ssrcBody.toByteArray()));
            if (pid != null) {
                var descriptor = new ByteArrayOutputStream();
                descriptor.write(0x08);
                descriptor.writeBytes(varint(pid));
                if (enhancement) {
                    descriptor.write(0x10);
                    descriptor.write(0x01);
                }
                assignments.writeBytes(lengthDelimited(2, descriptor.toByteArray()));
            }
            var ext = lengthDelimited(1, assignments.toByteArray());
            return lengthDelimited(1, ext);
        }

        private static byte[] lengthDelimited(int fieldNumber, byte[] body) {
            var out = new ByteArrayOutputStream();
            out.write((fieldNumber << 3) | 2);
            out.writeBytes(varint(body.length));
            out.writeBytes(body);
            return out.toByteArray();
        }

        private static byte[] varint(long value) {
            var out = new ByteArrayOutputStream();
            var remaining = value;
            while ((remaining & ~0x7FL) != 0) {
                out.write((int) ((remaining & 0x7F) | 0x80));
                remaining >>>= 7;
            }
            out.write((int) (remaining & 0x7F));
            return out.toByteArray();
        }

        private static byte[] concat(byte[]... parts) {
            var out = new ByteArrayOutputStream();
            for (var part : parts) {
                out.writeBytes(part);
            }
            return out.toByteArray();
        }
    }
}
