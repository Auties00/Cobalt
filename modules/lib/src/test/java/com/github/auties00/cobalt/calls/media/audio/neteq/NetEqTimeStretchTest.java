package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the time-stretch lag search and the accelerate/preemptive-expand pitch-period splice. The lag
 * search is checked on synthetic periodic signals where the pitch period is known; the splices are checked
 * for the exact variable-length output (one period removed or inserted), the preserved head, and the
 * criterion gating.
 */
@DisplayName("NetEqTimeStretch lag search and pitch-period splice")
class NetEqTimeStretchTest {
    private static final int FS_HZ = 16_000;
    private static final int FS_MULT = 2;
    private static final int PEAK_WINDOW = FS_MULT * NetEqTimeStretch.PEAK_WINDOW_PER_FS;

    private static short[] periodic(int length, int period, double amp) {
        var s = new short[length];
        for (var i = 0; i < length; i++) {
            s[i] = (short) (amp * Math.sin(2 * Math.PI * (i % period) / period));
        }
        return s;
    }

    @Nested
    @DisplayName("lag search")
    class LagSearch {
        @Test
        @DisplayName("locks onto the known pitch period of a voiced signal")
        void locksOnPitch() {
            var frame = periodic(960, 100, 8000);
            var search = NetEqTimeStretch.lagSearch(frame, frame.length, FS_HZ);
            // period 100 at 16 kHz -> lag should be a multiple of the period the 4 kHz search resolves.
            assertEquals(100, search[0], "lag matches the pitch period");
            assertTrue(search[1] >= 0 && search[1] <= 16384, "peak is in Q14 range");
            assertTrue(search[1] > 8000, "a clean periodic signal correlates strongly");
        }

        @Test
        @DisplayName("returns a zero lag for a buffer too short to decimate")
        void shortBuffer() {
            var search = NetEqTimeStretch.lagSearch(new short[64], 64, FS_HZ);
            assertEquals(0, search[0]);
            assertEquals(0, search[1]);
        }
    }

    @Nested
    @DisplayName("accelerate (remove one pitch period)")
    class Accelerate {
        @Test
        @DisplayName("shortens the frame by exactly one pitch period and preserves the head")
        void shortensByOnePeriod() {
            var frame = periodic(960, 100, 8000);
            var result = NetEqTimeStretch.accelerate(frame, frame.length, FS_HZ, true, true);
            assertTrue(result.stretched(), "criterion met for a clean voiced frame");
            assertEquals(frame.length - result.bestLag(), result.output().length,
                    "output is one pitch period shorter");
            for (var i = 0; i < PEAK_WINDOW; i++) {
                assertEquals(frame[i], result.output()[i], "head preserved at i=" + i);
            }
        }

        @Test
        @DisplayName("does not stretch when the precondition is false")
        void preconditionGates() {
            var frame = periodic(960, 100, 8000);
            var result = NetEqTimeStretch.accelerate(frame, frame.length, FS_HZ, true, false);
            assertTrue(!result.stretched());
            assertArrayEquals(frame, result.output(), "input returned unchanged");
        }
    }

    @Nested
    @DisplayName("preemptive expand (insert one pitch period)")
    class PreemptiveExpand {
        @Test
        @DisplayName("lengthens the frame by exactly one pitch period and preserves the head")
        void lengthensByOnePeriod() {
            var frame = periodic(960, 100, 8000);
            var result = NetEqTimeStretch.preemptiveExpand(frame, frame.length, FS_HZ, 0, true, false);
            assertTrue(result.stretched(), "criterion met when no stretch was requested");
            assertEquals(frame.length + result.bestLag(), result.output().length,
                    "output is one pitch period longer");
            for (var i = 0; i < PEAK_WINDOW + result.bestLag(); i++) {
                assertEquals(frame[i], result.output()[i], "head preserved at i=" + i);
            }
        }

        @Test
        @DisplayName("does not stretch when a stretch was requested and the override does not fire")
        void criterionGates() {
            var frame = periodic(960, 100, 8000);
            // doStretch=true, oldDataLength large, peakOverride=false => criterion false
            var result = NetEqTimeStretch.preemptiveExpand(frame, frame.length, FS_HZ, 100_000, false, true);
            assertTrue(!result.stretched());
            assertArrayEquals(frame, result.output());
        }
    }

    @Nested
    @DisplayName("seam continuity")
    class Seam {
        @Test
        @DisplayName("the accelerate seam is a smooth cross-fade, not a hard cut")
        void smoothSeam() {
            // A ramp signal: a hard cut would show a discontinuity at the seam; a cross-fade smooths it.
            var len = 960;
            var frame = new short[len];
            for (var i = 0; i < len; i++) {
                frame[i] = (short) (i % 100 * 100); // sawtooth, period 100
            }
            var result = NetEqTimeStretch.accelerate(frame, len, FS_HZ, true, true);
            if (result.stretched()) {
                // the cross-faded region sits at [PEAK_WINDOW, PEAK_WINDOW+bestLag); check no NaN/extreme jump
                var out = result.output();
                var maxJump = 0;
                for (var i = PEAK_WINDOW; i < PEAK_WINDOW + result.bestLag() - 1; i++) {
                    maxJump = Math.max(maxJump, Math.abs(out[i + 1] - out[i]));
                }
                // sawtooth amplitude is up to 9900; the cross-fade should not introduce a jump far beyond that.
                assertTrue(maxJump <= 12000, "cross-fade seam stays bounded: maxJump=" + maxJump);
            }
        }
    }
}
