package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Adversarial verification of {@link OpusAudioCodec} encode/decode/FEC/PLC against SPEC 12 (audio).
 *
 * <p>Every test skips when the {@code cobalt-native} libopus binary is not loadable (see
 * {@link NativeOpus}), so the suite is portable. A correct codec must round-trip a frame back to the same
 * length, classify a loud tone as voice-active and silence as discontinuous under DTX, conceal a lost
 * frame through PLC, and reconstruct a lost frame from the next packet's in-band FEC. Reconstruction
 * fidelity is asserted only at the structural level (non-null PCM of the expected length and finite
 * energy) because Opus is lossy; the spec-level contract is that the recovery paths run and produce a
 * frame, not bit-exact PCM.
 */
@DisplayName("OpusAudioCodec encode/decode/FEC/PLC round-trip")
class OpusAudioCodecRoundTripTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int FRAME_MS = 20;
    private static final int FRAME_SIZE = SAMPLE_RATE * FRAME_MS / 1000; // 960 samples per channel

    @BeforeAll
    static void requireNative() {
        assumeTrue(NativeOpus.available(),
                "cobalt-native libopus not loadable in this environment; skipping Opus round-trip tests");
    }

    /**
     * Builds a mono sine tone frame at the given amplitude, the canonical voice-active stimulus.
     */
    private static short[] tone(int frameSize, double freqHz, double amplitude, int startSample) {
        var pcm = new short[frameSize];
        for (var i = 0; i < frameSize; i++) {
            var t = (startSample + i) / (double) SAMPLE_RATE;
            pcm[i] = (short) (amplitude * Math.sin(2 * Math.PI * freqHz * t));
        }
        return pcm;
    }

    private static OpusCodecParams voiceParams() {
        return OpusCodecParams.forSampleRate(SAMPLE_RATE, 1, OpusApplication.VOIP);
    }

    @Nested
    @DisplayName("encode then decode")
    class RoundTrip {
        @ParameterizedTest(name = "{0} Hz tone")
        @ValueSource(ints = {200, 440, 1000, 3000})
        @DisplayName("a tone encodes to a non-empty packet and decodes back to a full-length frame")
        void toneRoundTrips(int freqHz) {
            try (var codec = new OpusAudioCodec(voiceParams())) {
                var pcm = tone(FRAME_SIZE, freqHz, 8000.0, 0);
                var encoded = codec.encode(pcm, FRAME_SIZE);
                assertNotNull(encoded);
                assertFalse(encoded.isEmpty(), "a loud tone must not encode to an empty (DTX) packet");
                assertTrue(encoded.voiceActive(), "a loud tone must classify as voice-active");

                var decoded = codec.decode(encoded.payload(), FRAME_SIZE, false);
                assertNotNull(decoded);
                assertEquals(FRAME_SIZE, decoded.length, "decoded mono frame must be FRAME_SIZE samples");
                assertTrue(hasEnergy(decoded), "decoded tone must carry signal energy");
            }
        }

        @Test
        @DisplayName("a continuous stream of tones round-trips frame by frame without error")
        void streamRoundTrips() {
            try (var codec = new OpusAudioCodec(voiceParams())) {
                for (var n = 0; n < 25; n++) {
                    var pcm = tone(FRAME_SIZE, 440, 9000.0, n * FRAME_SIZE);
                    var encoded = codec.encode(pcm, FRAME_SIZE);
                    var decoded = codec.decode(encoded.payload(), FRAME_SIZE, false);
                    assertEquals(FRAME_SIZE, decoded.length, "frame " + n + " must decode to full length");
                }
                var stats = codec.stats();
                assertEquals(25, stats.totalEncodedFrames());
                assertEquals(25, stats.totalDecodedFrames());
            }
        }
    }

    @Nested
    @DisplayName("discontinuous transmission")
    class Dtx {
        @Test
        @DisplayName("silence under DTX eventually produces a short/empty discontinuous frame")
        void silenceIsDiscontinuous() {
            try (var codec = new OpusAudioCodec(voiceParams())) {
                // Prime with a tone, then feed a long run of pure silence; DTX collapses to tiny frames.
                codec.encode(tone(FRAME_SIZE, 440, 9000.0, 0), FRAME_SIZE);
                var sawDiscontinuous = false;
                for (var n = 0; n < 30 && !sawDiscontinuous; n++) {
                    var encoded = codec.encode(new short[FRAME_SIZE], FRAME_SIZE);
                    sawDiscontinuous = encoded.discontinuous();
                }
                assertTrue(sawDiscontinuous,
                        "sustained silence under DTX must yield a discontinuous (sub-3-byte) frame");
                assertTrue(codec.lastFrameWasDiscontinuous());
            }
        }
    }

    @Nested
    @DisplayName("loss recovery")
    class LossRecovery {
        @Test
        @DisplayName("PLC conceals a lost frame and returns a full-length frame")
        void plcConceals() {
            try (var codec = new OpusAudioCodec(voiceParams())) {
                // Decode one good frame so the decoder has history for concealment.
                var good = codec.encode(tone(FRAME_SIZE, 440, 9000.0, 0), FRAME_SIZE);
                codec.decode(good.payload(), FRAME_SIZE, false);

                // recover(null, ...) is pure packet-loss concealment.
                var concealed = codec.recover(null, FRAME_SIZE);
                assertNotNull(concealed);
                assertEquals(FRAME_SIZE, concealed.length, "PLC must return a full-length frame");
                assertEquals(1, codec.stats().plcFrames(), "PLC must be counted");
            }
        }

        @Test
        @DisplayName("in-band FEC reconstructs a lost frame from the next packet")
        void fecReconstructs() {
            // FEC requires in-band FEC on AND a non-zero expected loss so the encoder spends bits on LBRR.
            var params = voiceParams().withInbandFec(true).withPacketLossPercent(30);
            try (var enc = new OpusAudioCodec(params); var dec = new OpusAudioCodec(params)) {
                // Encode two consecutive voiced frames; frame 2 carries an LBRR copy of frame 1.
                var f1 = enc.encode(tone(FRAME_SIZE, 440, 12000.0, 0), FRAME_SIZE);
                var f2 = enc.encode(tone(FRAME_SIZE, 440, 12000.0, FRAME_SIZE), FRAME_SIZE);
                assertFalse(f1.isEmpty());
                assertFalse(f2.isEmpty());

                // Frame 1 is "lost"; reconstruct it from frame 2's in-band FEC via recover(nextPayload).
                var recovered = dec.recover(f2.payload(), FRAME_SIZE);
                assertNotNull(recovered);
                assertEquals(FRAME_SIZE, recovered.length, "FEC recovery must return a full-length frame");
                assertEquals(1, dec.stats().fecFrames(), "FEC recovery must be counted");
            }
        }

        @Test
        @DisplayName("decode with the FEC flag set returns a full-length frame")
        void decodeFecFlag() {
            var params = voiceParams().withInbandFec(true).withPacketLossPercent(30);
            try (var enc = new OpusAudioCodec(params); var dec = new OpusAudioCodec(params)) {
                enc.encode(tone(FRAME_SIZE, 440, 12000.0, 0), FRAME_SIZE);
                var f2 = enc.encode(tone(FRAME_SIZE, 440, 12000.0, FRAME_SIZE), FRAME_SIZE);
                var fec = dec.decode(f2.payload(), FRAME_SIZE, true);
                assertEquals(FRAME_SIZE, fec.length);
            }
        }
    }

    @Nested
    @DisplayName("lifecycle and validation")
    class Lifecycle {
        @Test
        @DisplayName("encoding after close throws IllegalStateException")
        void encodeAfterCloseThrows() {
            var codec = new OpusAudioCodec(voiceParams());
            codec.close();
            assertThrows(IllegalStateException.class,
                    () -> codec.encode(new short[FRAME_SIZE], FRAME_SIZE));
        }

        @Test
        @DisplayName("close is idempotent")
        void closeIdempotent() {
            var codec = new OpusAudioCodec(voiceParams());
            codec.close();
            codec.close();
        }

        @Test
        @DisplayName("modify re-applies the mutable control set without disturbing the round trip")
        void modifyKeepsRoundTrip() {
            try (var codec = new OpusAudioCodec(voiceParams())) {
                codec.modify(voiceParams().withBitrate(24_000).withComplexity(5));
                var encoded = codec.encode(tone(FRAME_SIZE, 440, 9000.0, 0), FRAME_SIZE);
                var decoded = codec.decode(encoded.payload(), FRAME_SIZE, false);
                assertEquals(FRAME_SIZE, decoded.length);
            }
        }
    }

    private static boolean hasEnergy(short[] pcm) {
        long sum = 0;
        for (var s : pcm) {
            sum += (long) s * s;
        }
        return sum > 0;
    }
}
