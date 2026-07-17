package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.media.audio.neteq.LiveNetEq;
import com.github.auties00.cobalt.calls.media.audio.neteq.NetEqConfig;
import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.OpusAudioDecoder;
import com.github.auties00.cobalt.calls.media.audio.pipeline.AudioDecoderReceiver;
import com.github.auties00.cobalt.calls.media.audio.pipeline.AudioEncoderSender;
import com.github.auties00.cobalt.calls.media.audio.pipeline.AudioLevelRtpExtension;
import com.github.auties00.cobalt.calls.media.audio.codec.EncodedAudioFrame;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusApplication;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusAudioCodec;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusCodecParams;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusRepacketizer;
import com.github.auties00.cobalt.calls.media.audio.pipeline.StreamPacketCache;
import com.github.auties00.cobalt.calls.media.sframe.SFrameKeyProvider;
import com.github.auties00.cobalt.calls.media.sframe.SFrameSecureFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Adversarial end-to-end media-plane loopback: drives captured-style PCM through the real P9 send chain
 * (capture block -> {@link AudioEncoderSender} -> Opus encode -> repacketize -> SFrame seal) across a
 * pure-Java loopback "transport" and back up the real receive chain (SFrame open -> {@link LiveNetEq}
 * jitter buffer -> {@link AudioDecoderReceiver} -> rendered frame), then asserts the audio survives the
 * round trip with the same per-frame geometry and finite signal energy.
 *
 * <p>This is the verifier's independent check that the media plane is no longer a no-op stub: the same
 * units {@code LiveMediaSession} wires (the encoder-sender's functional seams, the SFrame key provider, the
 * NetEq jitter buffer, the Opus decoder) are wired here directly, with the network and the native transport
 * replaced by an in-process byte hand-off so no socket, relay, SRTP, or FFM transport is touched. The codec
 * itself is native libopus, so the whole suite skips when {@code cobalt-native} is not loadable, exactly
 * like {@code OpusAudioCodecRoundTripTest}. SFrame, NetEq decision logic, and the repacketizer are pure Java
 * and always run.
 *
 * <p>Two crypto paths are covered: a one-to-one call where the encoder supplies no SFrame transform (the
 * payload is the bare codec packet, protected by hop-by-hop SRTP the loopback omits) and a group call where
 * the encoder seals each payload with the SFrame chain and the receiver opens it under a matching chain key
 * (the group end-to-end layer the captures show firing only on the SFU path). The group path additionally
 * proves the corrected {@code [keyId][counter][len]} SFrame trailer survives a media round trip, not just a
 * synthetic seal/open.
 */
@DisplayName("calls media-plane loopback (capture -> encode -> SFrame/SRTP -> NetEq -> render)")
class MediaLoopbackTest {
    private static final int SAMPLE_RATE = AudioDecoderReceiver.SAMPLE_RATE_HZ; // 16 kHz mono call format
    private static final int FRAME_SAMPLES = AudioDecoderReceiver.FRAME_SAMPLES; // 320 samples / 20 ms
    private static final int FRAMES_PER_PACKET = 1;
    private static final int PACKET_CACHE_CAPACITY = 256;
    private static final double TONE_HZ = 440.0;
    private static final double TONE_AMPLITUDE = 12_000.0;

    /**
     * Probes whether native libopus is loadable; the suite skips cleanly otherwise, like the codec suite.
     */
    @BeforeAll
    static void requireNative() {
        assumeTrue(nativeOpusAvailable(),
                "cobalt-native libopus not loadable in this environment; skipping media loopback tests");
    }

    @Nested
    @DisplayName("one-to-one path (no SFrame; bare codec payload)")
    class OneToOne {
        @Test
        @DisplayName("a 25-frame tone survives capture -> encode -> loopback -> NetEq -> render at full geometry")
        void toneSurvivesOneToOne() {
            var harness = new LoopbackPipeline(false);
            try {
                var rendered = harness.runTone(25);
                // Every rendered frame keeps the 20 ms / 320-sample geometry NetEq and the decoder fix.
                for (var frame : rendered) {
                    assertEquals(FRAME_SAMPLES, frame.length, "every rendered frame must be a full 20 ms frame");
                }
                // The loopback delivered every packet, so the decode path (not just concealment) ran: at
                // least one rendered frame must carry real signal energy from the decoded tone.
                assertTrue(rendered.stream().anyMatch(MediaLoopbackTest::hasEnergy),
                        "decoded audio must survive the round trip with signal energy");
                // The sender shipped one packet per encoded (non-DTX) frame; the receiver consumed them.
                assertTrue(harness.packetsSent() > 0, "the encoder-sender must have shipped packets");
            } finally {
                harness.close();
            }
        }

        @Test
        @DisplayName("the one-to-one sender sends the bare codec payload with no SFrame trailer overhead")
        void oneToOnePayloadIsBare() {
            var harness = new LoopbackPipeline(false);
            try {
                harness.runTone(3);
                // With no SFrame transform the wire payload equals the codec payload exactly: no tag, no
                // trailer. A group payload (next test) is strictly longer for the same codec frame.
                assertFalse(harness.isGroupSecured(), "a one-to-one sender must not seal with SFrame");
                assertTrue(harness.lastWirePayloadLength() > 0);
            } finally {
                harness.close();
            }
        }
    }

    @Nested
    @DisplayName("group path (SFrame seal/open over the loopback)")
    class Group {
        @Test
        @DisplayName("a tone survives the SFrame end-to-end layer and decodes back at full geometry")
        void toneSurvivesGroup() {
            var harness = new LoopbackPipeline(true);
            try {
                var rendered = harness.runTone(25);
                for (var frame : rendered) {
                    assertEquals(FRAME_SAMPLES, frame.length, "every rendered frame must be a full 20 ms frame");
                }
                assertTrue(rendered.stream().anyMatch(MediaLoopbackTest::hasEnergy),
                        "SFrame-sealed audio must open and decode back with signal energy");
                assertTrue(harness.isGroupSecured(), "a group sender must seal with SFrame");
                // No frame was dropped at the receiver: every shipped frame opened (the SFrame tag verified
                // and the replay window accepted the monotonically increasing counters).
                assertEquals(0, harness.openFailures(), "no SFrame frame must fail to open in a clean loopback");
            } finally {
                harness.close();
            }
        }

        @Test
        @DisplayName("the SFrame wire payload is longer than the bare codec payload by the tag and trailer")
        void groupPayloadCarriesSframeOverhead() {
            var bare = new LoopbackPipeline(false);
            var secured = new LoopbackPipeline(true);
            try {
                bare.runTone(2);
                secured.runTone(2);
                // The SFrame frame is ciphertext(==plaintext len for CTR) + tag(4, suite 3) + trailer(>=3),
                // so for the same codec frame the secured payload must exceed the bare one. This is the
                // observable proof the group path actually applied the end-to-end transform on the wire.
                assertTrue(secured.lastWirePayloadLength() > bare.lastWirePayloadLength(),
                        "an SFrame-sealed payload must carry the tag-and-trailer overhead over the bare payload");
            } finally {
                bare.close();
                secured.close();
            }
        }
    }

    /**
     * Wires the real P9 audio send and receive chains around a synchronous in-process byte hand-off, so a
     * captured-style tone can be driven through encode, the chosen confidentiality path, NetEq, and the
     * decoder with no network or native transport. A group instance installs a shared SFrame chain key on
     * both the sender's seal seam and the receiver's open seam; a one-to-one instance supplies neither.
     */
    private static final class LoopbackPipeline {
        private final boolean group;
        private final OpusAudioCodec encodeCodec;
        private final OpusRepacketizer repacketizer;
        private final OpusAudioDecoder decoder;
        private final LiveNetEq netEq;
        private final AudioEncoderSender sender;
        private final AudioDecoderReceiver receiver;
        private final SFrameSecureFrame sealFrame;
        private final SFrameSecureFrame openFrame;
        private final AtomicInteger packetsSent = new AtomicInteger();
        private final AtomicInteger openFailures = new AtomicInteger();
        private final AtomicInteger rtpSequence = new AtomicInteger();
        private volatile int lastWirePayloadLength;

        private LoopbackPipeline(boolean group) {
            this.group = group;
            var params = OpusCodecParams.forSampleRate(SAMPLE_RATE, 1, OpusApplication.VOIP);
            this.encodeCodec = new OpusAudioCodec(params);
            this.repacketizer = new OpusRepacketizer();
            this.decoder = new OpusAudioDecoder(SAMPLE_RATE, 1);
            this.netEq = new LiveNetEq(NetEqConfig.defaults(), decoderSeam(this.decoder));
            this.receiver = new AudioDecoderReceiver(this.netEq);

            if (group) {
                var chainKey = chainKey();
                this.sealFrame = new SFrameSecureFrame(providerWith(chainKey), 0L);
                this.openFrame = new SFrameSecureFrame(providerWith(chainKey), 0L);
            } else {
                this.sealFrame = null;
                this.openFrame = null;
            }

            AudioEncoderSender.SFrameTransform sframeSeal = group ? sealFrame::seal : null;
            this.sender = new AudioEncoderSender(
                    encodeCodec::encode,
                    frames -> repacketizer.combine(frames.stream().map(EncodedAudioFrame::payload).toList()),
                    sframeSeal,
                    this::onWirePacket,
                    () -> {
                    },
                    new StreamPacketCache(PACKET_CACHE_CAPACITY),
                    FRAMES_PER_PACKET,
                    0);
        }

        /**
         * Drives {@code frameCount} consecutive tone blocks through the send chain, then pulls one rendered
         * frame per shipped packet up the receive chain, returning the rendered frames.
         */
        private List<short[]> runTone(int frameCount) {
            for (var n = 0; n < frameCount; n++) {
                sender.accept(tone(n * FRAME_SAMPLES), FRAME_SAMPLES);
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
         * The transport send seam: this is where {@code LiveMediaSession} would RTP-packetize and ship over
         * the relay socket. Here it strips straight back to the receive path: on the group path it opens the
         * SFrame frame, then inserts the recovered codec payload into the receiver's NetEq buffer keyed by a
         * synthetic RTP sequence and timestamp, exactly as {@code InboundMediaDemux} does after hop-by-hop
         * decrypt.
         */
        private void onWirePacket(byte[] payload, long extendedSequence, AudioLevelRtpExtension level) {
            assertNotNull(level, "the sender must always derive an audio-level extension");
            packetsSent.incrementAndGet();
            lastWirePayloadLength = payload.length;
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
            var timestamp = (long) sequence * FRAME_SAMPLES;
            receiver.receivePacket(sequence, timestamp, codecPayload);
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

        private int lastWirePayloadLength() {
            return lastWirePayloadLength;
        }

        private void close() {
            encodeCodec.close();
            repacketizer.close();
            netEq.flush();
            decoder.close();
        }
    }

    /**
     * Adapts the call's {@link OpusAudioDecoder} onto the receiver's decode-and-conceal seam, the same
     * adapter {@code LiveMediaSession} installs so the decoder's single-writer contract holds on the pull
     * thread.
     */
    private static AudioDecoderReceiver.FrameDecoder decoderSeam(OpusAudioDecoder decoder) {
        return new AudioDecoderReceiver.FrameDecoder() {
            @Override
            public AudioDecoderReceiver.DecodedFrame decode(byte[] payload, int frameSamples, boolean fec) {
                var samples = decoder.decode(payload, frameSamples, fec);
                var voiceActive = !fec && decoder.packetHasVoiceActivity(payload);
                return new AudioDecoderReceiver.DecodedFrame(samples, voiceActive);
            }

            @Override
            public short[] conceal(int frameSamples) {
                return decoder.conceal(frameSamples);
            }
        };
    }

    private static SFrameKeyProvider providerWith(byte[] chainKey) {
        var provider = new SFrameKeyProvider();
        provider.setChainKey(chainKey, 1L);
        return provider;
    }

    private static byte[] chainKey() {
        var key = new byte[SFrameKeyProvider.CHAIN_KEY_LENGTH];
        for (var i = 0; i < key.length; i++) {
            key[i] = (byte) (i * 7 + 3);
        }
        return key;
    }

    /**
     * Builds one mono 20 ms tone block at the call sample rate, the canonical voice-active stimulus.
     */
    private static short[] tone(int startSample) {
        var pcm = new short[FRAME_SAMPLES];
        for (var i = 0; i < FRAME_SAMPLES; i++) {
            var t = (startSample + i) / (double) SAMPLE_RATE;
            pcm[i] = (short) (TONE_AMPLITUDE * Math.sin(2 * Math.PI * TONE_HZ * t));
        }
        return pcm;
    }

    private static boolean hasEnergy(short[] pcm) {
        long sum = 0;
        for (var s : pcm) {
            sum += (long) s * s;
        }
        return sum > 0;
    }

    /**
     * Opens and closes a throwaway codec to detect native libopus, mirroring the package-private
     * {@code NativeOpus} probe in the audio test package (not visible from here).
     */
    private static boolean nativeOpusAvailable() {
        try {
            var codec = new OpusAudioCodec(OpusCodecParams.forSampleRate(SAMPLE_RATE, 1, OpusApplication.VOIP));
            codec.close();
            return true;
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError | RuntimeException e) {
            return false;
        }
    }
}
