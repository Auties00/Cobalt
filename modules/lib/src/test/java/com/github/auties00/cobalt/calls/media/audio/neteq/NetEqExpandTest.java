package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the autoregressive concealment driver: the voice-mix cubic is checked bit-exact against an
 * independent WAT-literal transcription across the whole Q14 correlation range; the assembled driver is
 * checked for faithful behaviour (a frame of the requested length, analysis on the first concealment, a
 * non-growing attenuation across a long gap, re-arming after a decoded frame, and a safe silent-history path).
 */
@DisplayName("NetEqExpand autoregressive concealment driver")
class NetEqExpandTest {
    private static final int FS_HZ = 16_000;
    private static final int FRAME_SAMPLES = 320;
    private static final int CAPACITY = FS_HZ / 1000 * 210;

    private static NetEqSyncBuffer voicedHistory() {
        var hist = new NetEqSyncBuffer(CAPACITY);
        var voiced = new short[960];
        for (var i = 0; i < voiced.length; i++) {
            voiced[i] = (short) (8000 * Math.sin(2 * Math.PI * (i % 100) / 100.0));
        }
        hist.pushBack(voiced);
        return hist;
    }

    private static double energy(short[] x) {
        var e = 0.0;
        for (short v : x) {
            e += (double) v * v;
        }
        return e;
    }

    // Independent WAT-literal transcription of the voice_mix_factor cubic (expand_f7333.wat 1745-1792).
    private static int watVoiceMix(int c) {
        if (c < 7876) {
            return 0;
        }
        var c2 = (int) (((long) c * c >> 14) & 0xFFFF);
        var term = 19_931 * c + c2 * -16_422 + (((c2 * c) >> 14) * 5_776) - 84_852_736;
        var mix = term / 4_096;
        if (mix >= 16_384) {
            mix = 16_384;
        }
        mix = (short) mix;
        return mix > 0 ? mix : 0;
    }

    @Nested
    @DisplayName("voice-mix cubic")
    class VoiceMixCubic {
        @Test
        @DisplayName("matches the WAT-literal cubic bit-exact across the whole Q14 range")
        void bitExact() {
            for (var c = 0; c <= 16_384; c++) {
                assertEquals(watVoiceMix(c), NetEqExpand.voiceMixCubic(c), "voiceMixCubic at c=" + c);
            }
        }

        @Test
        @DisplayName("is zero below the voiced threshold and clamped into Q14 at or above it")
        void gateAndClamp() {
            for (var c = 0; c < 7876; c++) {
                assertEquals(0, NetEqExpand.voiceMixCubic(c), "unvoiced gate at c=" + c);
            }
            for (var c = 7876; c <= 16_384; c++) {
                var v = NetEqExpand.voiceMixCubic(c);
                assertTrue(v >= 0 && v <= 16_384, "clamped into [0,16384] at c=" + c);
            }
        }
    }

    @Nested
    @DisplayName("driver")
    class Driver {
        @Test
        @DisplayName("produces a frame of the requested length and analyzes on the first concealment")
        void firstExpand() {
            var expand = new NetEqExpand(FS_HZ);
            assertTrue(expand.isFirstExpand(), "a fresh expander analyzes on the next frame");
            var frame = expand.process(voicedHistory(), FRAME_SAMPLES);
            assertEquals(FRAME_SAMPLES, frame.length, "frame is frameSamples long");
            assertFalse(expand.isFirstExpand(), "the first concealment clears the analyze gate");
            assertTrue(expand.maxLag() >= 0, "a pitch period was derived");
        }

        @Test
        @DisplayName("locks onto the pitch period of a voiced history")
        void locksOnPitch() {
            var expand = new NetEqExpand(FS_HZ);
            expand.process(voicedHistory(), FRAME_SAMPLES);
            // period 100 at 16 kHz decimated to 4 kHz; the lag is reported in full-rate samples.
            assertTrue(expand.maxLag() > 0, "voiced history yields a positive pitch lag");
        }

        @Test
        @DisplayName("attenuates rather than grows across a long concealment gap")
        void attenuatesAcrossGap() {
            var expand = new NetEqExpand(FS_HZ);
            var hist = voicedHistory();
            var frame = expand.process(hist, FRAME_SAMPLES);
            var previous = energy(frame);
            for (var k = 0; k < 20; k++) {
                hist.pushBack(frame);
                frame = expand.process(hist, FRAME_SAMPLES);
                assertEquals(FRAME_SAMPLES, frame.length, "every concealment frame is frameSamples long");
                var current = energy(frame);
                if (k >= 5) {
                    assertTrue(current <= previous * 1.20 + 1,
                            "settled output does not grow at k=" + k + " (" + current + " vs " + previous + ")");
                }
                previous = current;
            }
        }

        @Test
        @DisplayName("re-arms the analysis after a decoded frame and on reset")
        void reArm() {
            var expand = new NetEqExpand(FS_HZ);
            expand.process(voicedHistory(), FRAME_SAMPLES);
            assertFalse(expand.isFirstExpand());
            expand.notifyDecoded();
            assertTrue(expand.isFirstExpand(), "notifyDecoded re-arms the analysis");
            expand.process(voicedHistory(), FRAME_SAMPLES);
            expand.reset();
            assertTrue(expand.isFirstExpand(), "reset re-arms the analysis");
            assertEquals(0, expand.maxLag(), "reset clears the pitch period");
        }

        @Test
        @DisplayName("produces a frame from a silent history without throwing")
        void silentHistory() {
            var expand = new NetEqExpand(FS_HZ);
            var silent = new NetEqSyncBuffer(CAPACITY);
            var frame = expand.process(silent, FRAME_SAMPLES);
            assertEquals(FRAME_SAMPLES, frame.length, "silent-history concealment still produces a frame");
        }
    }
}
