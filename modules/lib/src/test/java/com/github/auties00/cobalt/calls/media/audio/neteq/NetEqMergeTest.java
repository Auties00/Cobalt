package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the merge cross-fade kernels and the energy-scaling weight against independent WAT-literal
 * references transcribed from the wa-voip WASM module ff-tScznZ8P (the single-source weight ramp $f9924, the
 * two-source weighted blend $f9923, and the Merge::Process SignalScaling block). Agreement across random
 * sweeps proves the production code reproduces the native arithmetic bit-for-bit.
 */
@DisplayName("NetEqMerge cross-fade and energy scaling")
class NetEqMergeTest {
    private static int energyShift(int value) {
        if (value == 0) {
            return 0;
        }
        return 32 - Integer.numberOfLeadingZeros(value ^ (value >> 31));
    }

    private static int maxAbs16(short[] in, int len) {
        var best = 0;
        for (var i = 0; i < len; i++) {
            int v = in[i];
            var abs = (v ^ (v >> 31)) - (v >> 31);
            best = Math.max(best, abs);
        }
        return Math.min(best, 32767);
    }

    private static int dot(short[] v, int len, int shift) {
        var sum = 0;
        for (var i = 0; i < len; i++) {
            sum += (v[i] * v[i]) >> shift;
        }
        return sum;
    }

    private static int exactSqrt(int v) {
        if (v <= 0) {
            return 0;
        }
        var r = (int) Math.sqrt(v);
        while ((long) (r + 1) * (r + 1) <= v) {
            r++;
        }
        while ((long) r * r > v) {
            r--;
        }
        return r;
    }

    // $f7470 SignalScaling, literal.
    private static int watSignalScaling(short[] expanded, short[] decoded, int decodedLength, int fsMult, int ew) {
        var l4 = Math.min(fsMult << 6, decodedLength);
        var divisor = Integer.MAX_VALUE / l4;
        var expShift = energyShift((maxAbs16(expanded, l4) * maxAbs16(expanded, l4)) / divisor);
        var expEnergy = dot(expanded, l4, expShift);
        var decShift = energyShift((maxAbs16(decoded, l4) * maxAbs16(decoded, l4)) / divisor);
        var decEnergy = dot(decoded, l4, decShift);
        var scale = 16384;
        var d1 = expShift - decShift;
        var decAligned = decEnergy >> (Integer.compareUnsigned(d1, expShift) <= 0 ? d1 : 0);
        var d2 = decShift - expShift;
        var expAligned = expEnergy >> (Integer.compareUnsigned(d2, decShift) <= 0 ? d2 : 0);
        if (decAligned > expAligned) {
            var n = decAligned == 0 ? 0 : Integer.numberOfLeadingZeros(decAligned ^ (decAligned >> 31)) - 1;
            var num = n > 2 ? expAligned >> (3 - n) : expAligned << (n - 3);
            var den = n > 16 ? decAligned >> (17 - n) : decAligned << (n - 17);
            scale = exactSqrt((num / den) << 14);
        }
        return Math.max(Math.min(scale, 16384), ew);
    }

    // $f9924 single-source weight ramp, literal.
    private static void watRampSingle(short[] out, short[] in, int len, int inc, int[] w) {
        var l5 = w[0] & 0xFFFF;
        var l7 = (l5 << 6) | 32;
        for (var i = 0; i < len; i++) {
            out[i] = (short) ((in[i] * (l5 & 0xFFFF) - (-8192)) >>> 14);
            l7 = Math.max(inc + l7, 0);
            var next = l7 >>> 6;
            l5 = Integer.compareUnsigned(next, 16384) >= 0 ? 16384 : next;
        }
        w[0] = l5;
    }

    // $f9923 two-source weighted blend, literal.
    private static void watRampBlend(short[] out, short[] a, short[] b, int len, int inc, int[] w) {
        var l6 = w[0] & 0xFFFF;
        var l7 = 16384 - l6;
        for (var i = 0; i < len; i++) {
            out[i] = (short) ((a[i] * (short) l6 + b[i] * (short) l7 - (-8192)) >>> 14);
            l7 += inc;
            l6 -= inc;
        }
        w[0] = l6;
    }

    @Nested
    @DisplayName("single-source weight ramp ($f9924)")
    class RampSingle {
        @Test
        @DisplayName("reproduces the native single-source ramp across random overlaps")
        void matchesReference() {
            var rnd = new Random(31);
            for (var t = 0; t < 50_000; t++) {
                var len = 1 + rnd.nextInt(120);
                var in = new short[len];
                for (var i = 0; i < len; i++) {
                    in[i] = (short) (rnd.nextInt(65536) - 32768);
                }
                var inc = rnd.nextInt(2000);
                var w0 = rnd.nextInt(16385);
                var e = new short[len];
                var a = new short[len];
                var ws1 = new int[]{w0};
                var ws2 = new int[]{w0};
                watRampSingle(e, in, len, inc, ws1);
                NetEqMerge.rampSingle(a, 0, in, 0, len, inc, ws2);
                assertArrayEquals(e, a, "t=" + t);
                assertEquals(ws1[0], ws2[0], "weight t=" + t);
            }
        }
    }

    @Nested
    @DisplayName("two-source weighted blend ($f9923)")
    class RampBlend {
        @Test
        @DisplayName("reproduces the native two-source blend across random overlaps")
        void matchesReference() {
            var rnd = new Random(37);
            for (var t = 0; t < 50_000; t++) {
                var len = 1 + rnd.nextInt(120);
                var a = new short[len];
                var b = new short[len];
                for (var i = 0; i < len; i++) {
                    a[i] = (short) (rnd.nextInt(65536) - 32768);
                    b[i] = (short) (rnd.nextInt(65536) - 32768);
                }
                var inc = rnd.nextInt(2000);
                var w0 = rnd.nextInt(16385);
                var e = new short[len];
                var ac = new short[len];
                var ws1 = new int[]{w0};
                var ws2 = new int[]{w0};
                watRampBlend(e, a, b, len, inc, ws1);
                NetEqMerge.rampBlend(ac, 0, a, 0, b, 0, len, inc, ws2);
                assertArrayEquals(e, ac, "t=" + t);
                assertEquals(ws1[0], ws2[0], "weight t=" + t);
            }
        }

        @Test
        @DisplayName("fades from the expansion toward the decoded audio")
        void fadesExpandedToDecoded() {
            var len = 64;
            var exp = new short[len];
            var dec = new short[len];
            Arrays.fill(exp, (short) 10000);
            Arrays.fill(dec, (short) -10000);
            var out = new short[len];
            var ws = new int[]{16384};
            NetEqMerge.rampBlend(out, 0, exp, 0, dec, 0, len, 16384 / 65, ws);
            assertTrue(out[0] > 0, "starts near the expansion");
            assertTrue(out[len - 1] < out[0], "ramps toward the decoded audio");
        }
    }

    @Nested
    @DisplayName("signal scaling energy match")
    class SignalScaling {
        @Test
        @DisplayName("reproduces the native energy-ratio weight across random energies")
        void matchesReference() {
            var rnd = new Random(41);
            for (var t = 0; t < 50_000; t++) {
                var fsMult = 1 + rnd.nextInt(4);
                var decLen = (fsMult << 6) + rnd.nextInt(200);
                var n = Math.max(fsMult << 6, decLen) + 8;
                var exp = new short[n];
                var dec = new short[n];
                var ampE = rnd.nextInt(20000);
                var ampD = rnd.nextInt(20000);
                for (var i = 0; i < n; i++) {
                    exp[i] = (short) (rnd.nextInt(2 * ampE + 1) - ampE);
                    dec[i] = (short) (rnd.nextInt(2 * ampD + 1) - ampD);
                }
                var ew = rnd.nextInt(16385);
                assertEquals(watSignalScaling(exp, dec, decLen, fsMult, ew),
                        NetEqMerge.signalScaling(exp, dec, decLen, fsMult, ew), "t=" + t);
            }
        }

        @Test
        @DisplayName("keeps unity weight when the expansion is no louder than the decoded audio")
        void unityWhenExpansionLouder() {
            var loud = new short[160];
            var quiet = new short[160];
            Arrays.fill(loud, (short) 16000);
            Arrays.fill(quiet, (short) 2000);
            assertEquals(16384, NetEqMerge.signalScaling(loud, quiet, 160, 2, 0),
                    "expansion louder keeps unity");
        }
    }
}
