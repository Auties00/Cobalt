package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.media.audio.neteq.LiveNetEq;
import com.github.auties00.cobalt.calls.media.audio.neteq.NetEqConfig;
import com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver;
import com.github.auties00.cobalt.calls.media.audio.pipeline.AudioEncoderSender;
import com.github.auties00.cobalt.calls.media.audio.pipeline.AudioLevelRtpExtension;
import com.github.auties00.cobalt.calls.media.audio.codec.EncodedAudioFrame;
import com.github.auties00.cobalt.calls.media.audio.pipeline.StreamPacketCache;
import com.github.auties00.cobalt.calls.media.sframe.SFrameKeyProvider;
import com.github.auties00.cobalt.calls.media.sframe.SFrameSecureFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial P10 media-plane loopback that runs with NO native codec, complementing the native-libopus
 * {@code MediaLoopbackTest}. It drives a payload through the real P9 send orchestrator
 * ({@link AudioEncoderSender}) and the real receive orchestrator ({@link AudioDecoderReceiver}) across an
 * in-process loopback, substituting a deterministic identity "codec" for the encode/decode seams so the
 * whole chain (capture block -> encode seam -> repacketize seam -> SFrame seal -> loopback -> SFrame open ->
 * LiveNetEq insert -> getAudio decode seam -> rendered frame) exercises unconditionally in CI without
 * libopus, libsrtp, or a socket.
 *
 * <p>The point is independent: where the native suite proves the real Opus path survives a round trip but
 * skips when {@code cobalt-native} is absent, this suite proves the calls send/receive ORCHESTRATORS and
 * the pure-Java SFrame transform are wired correctly regardless of platform. Two crypto paths are covered:
 * a one-to-one call with no SFrame transform (the bare codec payload, hop-by-hop SRTP omitted by the
 * loopback) and a group call where the encoder seals each payload with a real {@link SFrameSecureFrame}
 * chain and the receiver opens it under a matching chain key, proving the group end-to-end transform and the
 * frames-per-packet aggregation survive a media round trip.
 */
@DisplayName("calls media-plane seam loopback (pure Java, no native codec)")
class SeamLoopbackTest {
    private static final int FRAME_SAMPLES = AudioDecoderReceiver.FRAME_SAMPLES; // 320 / 20 ms at 16 kHz

    @Nested
    @DisplayName("one-to-one path (no SFrame; bare payload)")
    class OneToOne {
        @Test
        @DisplayName("each captured block round-trips through encode -> loopback -> decode at full geometry")
        void payloadSurvivesOneToOne() {
            var pipeline = new SeamPipeline(false, 1);
            var rendered = pipeline.run(List.of(payload("alpha"), payload("bravo"), payload("charlie")));
            assertEquals(3, pipeline.packetsSent(), "one packet per captured block at frames-per-packet 1");
            assertEquals(3, rendered.size());
            for (var frame : rendered) {
                assertEquals(FRAME_SAMPLES, frame.length, "every rendered frame keeps the 20 ms geometry");
            }
            assertFalse(pipeline.isGroupSecured(), "a one-to-one sender must not seal with SFrame");
            // The identity codec stamps the payload into the frame, so the decoded marker proves the exact
            // bytes reached the decoder seam unchanged across the bare loopback.
            assertArrayEquals(payload("alpha"), pipeline.recoveredPayloads().get(0),
                    "the bare payload must reach the decoder unchanged");
        }

        @Test
        @DisplayName("frames-per-packet aggregation combines several blocks into one packet")
        void framesPerPacketAggregates() {
            // frames-per-packet 3: six blocks pack into exactly two packets. This proves the sender's
            // aggregation buffer (the put_frame_imp port) ships one packet per full group.
            var pipeline = new SeamPipeline(false, 3);
            pipeline.run(List.of(payload("1"), payload("2"), payload("3"),
                    payload("4"), payload("5"), payload("6")));
            assertEquals(2, pipeline.packetsSent(), "six blocks at fpp=3 must ship two combined packets");
        }
    }

    @Nested
    @DisplayName("group path (real SFrame seal/open over the loopback)")
    class Group {
        @Test
        @DisplayName("a sealed payload opens and decodes back at full geometry with no open failures")
        void payloadSurvivesGroup() {
            var pipeline = new SeamPipeline(true, 1);
            var rendered = pipeline.run(List.of(payload("secret-1"), payload("secret-2")));
            assertEquals(2, rendered.size());
            for (var frame : rendered) {
                assertEquals(FRAME_SAMPLES, frame.length);
            }
            assertTrue(pipeline.isGroupSecured(), "a group sender must seal with SFrame");
            assertEquals(0, pipeline.openFailures(), "a clean loopback must open every sealed frame");
            assertArrayEquals(payload("secret-1"), pipeline.recoveredPayloads().get(0),
                    "the SFrame-opened payload must equal the sealed plaintext");
        }

        @Test
        @DisplayName("the sealed wire payload carries the SFrame tag and trailer over the bare payload")
        void sealedPayloadCarriesOverhead() {
            var bare = new SeamPipeline(false, 1);
            var secured = new SeamPipeline(true, 1);
            bare.run(List.of(payload("xx")));
            secured.run(List.of(payload("xx")));
            assertTrue(secured.lastWireLength() > bare.lastWireLength(),
                    "an SFrame-sealed payload must be longer than the bare payload by the tag and trailer");
        }
    }

    /**
     * Wires the real audio send and receive orchestrators around a synchronous in-process byte hand-off. The
     * codec encode/decode and the repacketizer are replaced by deterministic identity seams (no libopus), so
     * the suite runs everywhere; the SFrame transform on the group path is the real pure-Java
     * {@link SFrameSecureFrame} chain shared between the sender's seal and the receiver's open. The decode
     * seam stamps a fixed-length frame whose head carries the recovered payload, so a test can assert the
     * exact bytes survived the chosen confidentiality path.
     */
    private static final class SeamPipeline {
        private final boolean group;
        private final AudioEncoderSender sender;
        private final LiveNetEq netEq;
        private final AudioDecoderReceiver receiver;
        private final SFrameSecureFrame sealFrame;
        private final SFrameSecureFrame openFrame;
        private final ConcurrentLinkedQueue<byte[]> recoveredPayloads = new ConcurrentLinkedQueue<>();
        private final AtomicInteger packetsSent = new AtomicInteger();
        private final AtomicInteger openFailures = new AtomicInteger();
        private final AtomicInteger rtpSequence = new AtomicInteger();
        private volatile int lastWireLength;

        private SeamPipeline(boolean group, int framesPerPacket) {
            this.group = group;
            this.netEq = new LiveNetEq(NetEqConfig.defaults(), new IdentityDecoder());
            this.receiver = new AudioDecoderReceiver(netEq);
            if (group) {
                var chainKey = chainKey();
                this.sealFrame = new SFrameSecureFrame(providerWith(chainKey), 0L);
                this.openFrame = new SFrameSecureFrame(providerWith(chainKey), 0L);
            } else {
                this.sealFrame = null;
                this.openFrame = null;
            }
            AudioEncoderSender.SFrameTransform seal = group ? sealFrame::seal : null;
            this.sender = new AudioEncoderSender(
                    new IdentityEncoder(),
                    new ConcatPacker(),
                    seal,
                    this::onWirePacket,
                    () -> {
                    },
                    new StreamPacketCache(256),
                    framesPerPacket,
                    0);
        }

        /**
         * Drives the given payloads through the send chain as captured blocks, flushes any partial group, then
         * pulls one rendered frame per shipped packet up the receive chain.
         */
        private List<short[]> run(List<byte[]> payloads) {
            for (var payload : payloads) {
                sender.accept(asBlock(payload), payload.length);
            }
            sender.flush();
            var rendered = new ArrayList<short[]>();
            for (var n = 0; n < packetsSent.get(); n++) {
                var frame = new short[FRAME_SAMPLES];
                var copied = receiver.pull(frame, FRAME_SAMPLES);
                assertTrue(copied >= 0 && copied <= FRAME_SAMPLES);
                rendered.add(frame);
            }
            return rendered;
        }

        /**
         * The transport send seam. On the group path it opens the SFrame frame back to the codec payload (as
         * the inbound demux does after hop-by-hop decrypt), then inserts the recovered payload into the
         * receiver under a synthetic RTP sequence.
         */
        private void onWirePacket(byte[] payload, long extendedSequence, AudioLevelRtpExtension level) {
            assertNotNull(level, "the sender must always derive an audio-level extension");
            packetsSent.incrementAndGet();
            lastWireLength = payload.length;
            byte[] codecPayload;
            if (group) {
                codecPayload = openFrame.open(payload);
                if (codecPayload == null) {
                    openFailures.incrementAndGet();
                    return;
                }
            } else {
                codecPayload = payload;
            }
            var sequence = rtpSequence.getAndIncrement() & 0xFFFF;
            receiver.receivePacket(sequence, (long) sequence * FRAME_SAMPLES, codecPayload);
        }

        private List<byte[]> recoveredPayloads() {
            return new ArrayList<>(recoveredPayloads);
        }

        private int packetsSent() {
            return packetsSent.get();
        }

        private int openFailures() {
            return openFailures.get();
        }

        private boolean isGroupSecured() {
            return sender.isGroupSecured();
        }

        private int lastWireLength() {
            return lastWireLength;
        }

        /**
         * The codec decode seam: recovers the payload the identity encoder stamped, records it, and returns a
         * fixed-geometry frame so the receiver's frame-length contract is met.
         */
        private final class IdentityDecoder implements AudioDecoderReceiver.FrameDecoder {
            @Override
            public AudioDecoderReceiver.DecodedFrame decode(byte[] payload, int frameSamples, boolean fec) {
                recoveredPayloads.add(payload.clone());
                return new AudioDecoderReceiver.DecodedFrame(new short[frameSamples], false);
            }

            @Override
            public short[] conceal(int frameSamples) {
                return new short[frameSamples];
            }
        }
    }

    /**
     * An identity encoder: it stamps the captured block's leading bytes back into the encoded payload so the
     * decode seam can recover and verify them, marking every frame voice-active and non-discontinuous so the
     * sender buffers and ships it.
     */
    private static final class IdentityEncoder implements AudioEncoderSender.FrameEncoder {
        @Override
        public EncodedAudioFrame encode(short[] pcm, int length) {
            var payload = new byte[length];
            for (var i = 0; i < length; i++) {
                payload[i] = (byte) pcm[i];
            }
            return new EncodedAudioFrame(payload, true, false, false, 0);
        }
    }

    /**
     * A repacketizer seam that concatenates the frames-per-packet group's payloads, the pure-Java stand-in
     * for the Opus repacketizer the codec unit supplies.
     */
    private static final class ConcatPacker implements AudioEncoderSender.FramePacker {
        @Override
        public byte[] pack(List<EncodedAudioFrame> frames) {
            var total = frames.stream().mapToInt(f -> f.payload().length).sum();
            var combined = new byte[total];
            var offset = 0;
            for (var frame : frames) {
                System.arraycopy(frame.payload(), 0, combined, offset, frame.payload().length);
                offset += frame.payload().length;
            }
            return combined;
        }
    }

    /**
     * Encodes a short ASCII marker into a captured block by stamping its bytes into the leading samples, so
     * the identity codec can recover the exact marker after the round trip.
     */
    private static short[] asBlock(byte[] payload) {
        var pcm = new short[FRAME_SAMPLES];
        for (var i = 0; i < payload.length; i++) {
            pcm[i] = payload[i];
        }
        return pcm;
    }

    private static byte[] payload(String marker) {
        return marker.getBytes(StandardCharsets.US_ASCII);
    }

    private static SFrameKeyProvider providerWith(byte[] chainKey) {
        var provider = new SFrameKeyProvider();
        provider.setChainKey(chainKey, 1L);
        return provider;
    }

    private static byte[] chainKey() {
        var key = new byte[SFrameKeyProvider.CHAIN_KEY_LENGTH];
        for (var i = 0; i < key.length; i++) {
            key[i] = (byte) (i * 5 + 1);
        }
        return key;
    }
}
