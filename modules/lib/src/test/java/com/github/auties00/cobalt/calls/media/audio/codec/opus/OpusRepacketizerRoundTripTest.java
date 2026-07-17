package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Adversarial verification of {@link OpusRepacketizer} frames-per-packet aggregation against SPEC 12
 * (FPP).
 *
 * <p>Skips when the {@code cobalt-native} libopus binary is not loadable. Real Opus frames are produced
 * by an {@link OpusAudioCodec} (the native repacketizer validates that aggregated frames share a TOC
 * configuration, so synthetic bytes would not combine), then combined and split. The invariant is a
 * clean round trip: splitting an N-frame combined packet yields exactly N frames, each byte-identical to
 * the corresponding input frame, for every legal frames-per-packet count {@code 1..6}. Frame-count
 * bounds are also pinned.
 */
@DisplayName("OpusRepacketizer combine/split round-trip")
class OpusRepacketizerRoundTripTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int FRAME_SIZE = SAMPLE_RATE * 20 / 1000; // 960 samples, 20 ms

    @BeforeAll
    static void requireNative() {
        assumeTrue(NativeOpus.available(),
                "cobalt-native libopus not loadable in this environment; skipping repacketizer tests");
    }

    /**
     * Encodes {@code count} consecutive 20 ms voiced frames so they share a TOC and can be aggregated.
     */
    private static List<byte[]> encodeFrames(int count) {
        var params = OpusCodecParams.forSampleRate(SAMPLE_RATE, 1, OpusApplication.VOIP)
                .withInbandFec(false);
        var frames = new ArrayList<byte[]>(count);
        try (var codec = new OpusAudioCodec(params)) {
            for (var n = 0; n < count; n++) {
                var pcm = new short[FRAME_SIZE];
                for (var i = 0; i < FRAME_SIZE; i++) {
                    var t = (n * FRAME_SIZE + i) / (double) SAMPLE_RATE;
                    pcm[i] = (short) (10000.0 * Math.sin(2 * Math.PI * 440 * t));
                }
                var encoded = codec.encode(pcm, FRAME_SIZE);
                frames.add(encoded.payload());
            }
        }
        return frames;
    }

    @ParameterizedTest(name = "{0} frames per packet")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("combine then split round-trips every frame byte-for-byte")
    void combineSplitRoundTrip(int framesPerPacket) {
        var frames = encodeFrames(framesPerPacket);
        try (var repacketizer = new OpusRepacketizer()) {
            var combined = repacketizer.combine(frames);
            assertTrue(combined.length > 0, "combined packet must be non-empty");

            var split = repacketizer.split(combined);
            assertEquals(framesPerPacket, split.size(),
                    "split must recover the same number of frames that were combined");
            for (var i = 0; i < framesPerPacket; i++) {
                assertArrayEquals(frames.get(i), split.get(i),
                        "frame " + i + " must survive combine/split unchanged");
            }
        }
    }

    @Test
    @DisplayName("a single-frame combine is the frame itself and splits back to one frame")
    void singleFrameIsIdentity() {
        var frames = encodeFrames(1);
        try (var repacketizer = new OpusRepacketizer()) {
            var combined = repacketizer.combine(frames);
            var split = repacketizer.split(combined);
            assertEquals(1, split.size());
            assertArrayEquals(frames.get(0), split.get(0));
        }
    }

    @Test
    @DisplayName("an aggregated packet stays within Opus framing overhead of the separate frames")
    void aggregationStaysWithinFramingOverhead() {
        var frames = encodeFrames(3);
        var separateTotal = frames.stream().mapToInt(f -> f.length).sum();
        try (var repacketizer = new OpusRepacketizer()) {
            var combined = repacketizer.combine(frames);
            // Frames-per-packet aggregation replaces N transport packets with one Opus packet; the win is
            // N-1 sets of RTP/SRTP/transport headers, not Opus payload bytes. The combined code-3 packet
            // carries one TOC plus a frame-count byte and per-frame length bytes, so on the Opus layer it
            // sits within a few framing bytes of the separate sum rather than duplicating a full TOC per
            // frame.
            assertTrue(combined.length <= separateTotal + frames.size(),
                    "combined " + combined.length + " must stay within framing overhead of separate sum "
                            + separateTotal);
        }
    }

    @Test
    @DisplayName("an empty frame list is rejected")
    void emptyRejected() {
        try (var repacketizer = new OpusRepacketizer()) {
            assertThrows(IllegalArgumentException.class, () -> repacketizer.combine(List.of()));
        }
    }

    @Test
    @DisplayName("more than six frames is rejected")
    void tooManyRejected() {
        var frames = encodeFrames(7);
        try (var repacketizer = new OpusRepacketizer()) {
            assertThrows(IllegalArgumentException.class, () -> repacketizer.combine(frames));
        }
    }

    @Test
    @DisplayName("combine after close throws IllegalStateException")
    void combineAfterCloseThrows() {
        var frames = encodeFrames(2);
        var repacketizer = new OpusRepacketizer();
        repacketizer.close();
        assertThrows(IllegalStateException.class, () -> repacketizer.combine(frames));
    }
}
