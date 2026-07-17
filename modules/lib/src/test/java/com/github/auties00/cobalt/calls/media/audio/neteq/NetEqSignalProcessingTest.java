package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates the NetEq fixed-point leaf kernels against independent WAT-literal references transcribed
 * instruction-for-instruction from the wa-voip WASM module ff-tScznZ8P. Each reference here is a second,
 * structurally different transcription of the same native function, so an agreement across many random
 * inputs proves the production kernel reproduces the native arithmetic bit-for-bit. The square root is
 * additionally pinned to the exact integer floor-sqrt, the documented WebRtcSpl_SqrtFloor contract.
 */
@DisplayName("NetEqSignalProcessing fixed-point leaf kernels")
class NetEqSignalProcessingTest {
    private static final short[] PCOEF = {
            120, 32, 64, 140, 44, 75, 150, 50, 80, 160, 57, 85,
            180, 72, 96, 200, 89, 107, 210, 98, 112, 220, 108, 117,
            240, 128, 128, 260, 150, 139, 270, 162, 144, 280, 174, 149,
            300, 200, 160, 320, 228, 171, 330, 242, 176, 340, 257, 181,
            360, 288, 192
    };

    private static int exactFloorSqrt(int v) {
        if (v <= 0) {
            return 0;
        }
        int r = (int) Math.sqrt(v);
        while ((long) (r + 1) * (r + 1) <= v) {
            r++;
        }
        while ((long) r * r > v) {
            r--;
        }
        return r;
    }

    // $f8886 divide helper, literal.
    private static int watDiv(int p0, int p1, int p2) {
        if (p1 == 0) {
            return -1;
        }
        int l3 = 0 - (p1 << 1);
        int q = (short) (536870911 / p1);
        int acc = q * l3 - (((q * p2) >> 14) & ~1) + 2147483647;
        int r = (((acc >>> 1) & 32767) * q >> 15) + ((acc >> 16) * q);
        int rHi = (r << 1) >> 16;
        int numHi = p0 >> 16;
        return (rHi * numHi
                + ((r & 32767) * numHi >> 15)
                + (rHi * ((p0 >>> 1) & 32767) >> 15)) << 3;
    }

    // $f8892 cross-correlation, literal.
    private static void watXcorr(int[] out, short[] s1, short[] s2, int len, int numCc, int shift, int step) {
        int base = 0;
        for (int i = 0; i < numCc; i++) {
            int acc = 0;
            for (int k = 0; k < len; k++) {
                acc += (s2[base + k] * s1[k]) >> shift;
            }
            out[i] = acc;
            base += step;
        }
    }

    // $f8893 downsample, literal: inOff = warm-up advance, p3 = OUTPUT length (native range bound).
    private static int watDownsampleFast(short[] out, short[] in, int inOff, int inLen, int outLen,
                                         short[] coeff, int tap, int factor, int delay) {
        if (outLen == 0 || tap == 0) {
            return -1;
        }
        int end = (outLen - 1) * factor + 1 + delay;
        if (inLen < end) {
            return -1;
        }
        if (end <= delay) {
            return 0;
        }
        int outPos = 0;
        for (int i = delay; i < end; i += factor) {
            int acc = 2048;
            for (int j = 0; j < tap; j++) {
                acc += in[inOff + i - j] * coeff[j];
            }
            int v = acc >> 12;
            int t = (v <= -32768) ? -32768 : v;
            t = (t >= 32767) ? 32767 : t;
            out[outPos++] = (short) t;
        }
        return outPos;
    }

    // $f9925 selector, literal.
    private static int watDownsampleSelect(short[] out, short[] in, int inLen, int outLen, int fs, boolean warmup) {
        short[] cf;
        int factor;
        int warmIdx;
        switch (fs) {
            case 8_000 -> { cf = new short[]{1229, 1638, 1229}; factor = 2; warmIdx = 2; }
            case 16_000 -> { cf = new short[]{614, 819, 1229, 819, 614}; factor = 4; warmIdx = 3; }
            case 24_000 -> { cf = new short[]{306, 565, 760, 833, 760, 565, 306}; factor = 6; warmIdx = 4; }
            case 32_000 -> { cf = new short[]{584, 512, 625, 667, 625, 512, 584}; factor = 8; warmIdx = 4; }
            case 48_000 -> { cf = new short[]{1019, 390, 427, 440, 427, 390, 1019}; factor = 12; warmIdx = 4; }
            default -> {
                return -1;
            }
        }
        int tap = cf.length;
        int advance = tap - 1;
        int delay = warmup ? warmIdx : 0;
        return watDownsampleFast(out, in, advance, inLen - advance, outLen, cf, tap, factor, delay);
    }

    // $f9899 Q14 cross-fade, literal contiguous form.
    private static void watCrossFade(short[] out, short[] oldd, short[] newd, int overlap) {
        int step = 16384 / (overlap + 1);
        int w = 16384;
        for (int i = 0; i < overlap; i++) {
            w -= step;
            int blended = (w * oldd[i] + (16384 - w) * newd[i] - (-8192)) >> 14;
            out[i] = (short) blended;
        }
    }

    private static short[] stepTab(int p1) {
        return switch (p1) {
            case 1 -> new short[]{0, 8, 16};
            case 2 -> new short[]{0, 4, 8, 12, 16};
            case 4 -> new short[]{0, 1, 3, 4, 5, 7, 8, 9, 11, 12, 13, 15, 16};
            default -> new short[]{0, 2, 4, 6, 8, 10, 12, 14, 16};
        };
    }

    private static int pc(int e) {
        return PCOEF[(e & 0xFFFF) * 3];
    }

    // $f9922 parabolic fit, literal.
    private static void watParab(short[] data, int b0, int p1, int[] pi, short[] pv) {
        short[] steps = stepTab(p1);
        int top = pc(steps[p1]);
        int next = pc(steps[p1 - 1]);
        int coeffStep = top - next;
        int s0 = data[b0];
        int s1 = data[b0 + 1];
        int s2 = data[b0 + 2];
        int b = (-3 * s0) + (s1 << 2) - s2;
        int numerator = b * 120;
        int mid = (top + next) / 2;
        int a = s0 - (s1 << 1) + s2;
        int negA = 0 - a;
        int storedIndex = pi[0];
        int counter = s1;
        boolean leftTail = false;
        boolean foundRight = false;
        int fraction = 1;
        if (numerator < mid * negA) {
            int coeff = mid;
            if (p1 == 1) {
                leftTail = true;
            } else {
                while (true) {
                    coeff -= coeffStep;
                    if (numerator > (short) coeff * negA) {
                        leftTail = true;
                        break;
                    }
                    fraction++;
                    if (fraction == p1) {
                        leftTail = true;
                        break;
                    }
                }
            }
        } else {
            int firstStep = (short) coeffStep;
            if ((mid + firstStep) * negA < numerator) {
                counter = 1;
                if (p1 == 1) {
                    pv[0] = (short) counter;
                    pi[0] = (p1 * storedIndex) << 1;
                    return;
                }
                int coeff = mid + (firstStep << 1);
                while (true) {
                    if (numerator < (short) coeff * negA) {
                        foundRight = true;
                        break;
                    }
                    coeff += coeffStep;
                    counter++;
                    if (counter == p1) {
                        break;
                    }
                }
            }
        }
        if (leftTail) {
            int rec = (steps[p1 - fraction] & 0xFFFF) * 3;
            pv[0] = (short) ((b * PCOEF[rec + 2] + a * PCOEF[rec + 1] + (s0 << 8)) / 256);
            pi[0] = (p1 << 1) * storedIndex - fraction;
        } else if (foundRight) {
            int rec = (steps[p1 + counter] & 0xFFFF) * 3;
            pv[0] = (short) ((b * PCOEF[rec + 2] + a * PCOEF[rec + 1] + (s0 << 8)) / 256);
            pi[0] = (p1 << 1) * storedIndex + counter;
        } else {
            pv[0] = (short) counter;
            pi[0] = (p1 * storedIndex) << 1;
        }
    }

    // $f8888 Levinson-Durbin, WAT-literal stack-machine transcription over the native 288-byte scratch frame
    // modeled as a flat little-endian byte array (i16 load/store exactly as wasm linear memory).
    private static final class Frame {
        final byte[] b = new byte[288];

        int load16s(int off) {
            return (short) ((b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8));
        }

        int load16u(int off) {
            return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
        }

        void store16(int off, int v) {
            b[off] = (byte) v;
            b[off + 1] = (byte) (v >>> 8);
        }
    }

    private static int watLevinson(int[] r, short[] a, short[] k, int order) {
        var mem = new Frame();
        final int RHI = 240, RLO = 192, AHI = 0, ALO = 48, AHIP = 96, ALOP = 144;
        int p3 = order;
        int t = r[0];
        int l4 = t != 0 ? (Integer.numberOfLeadingZeros(t ^ (t >> 31)) - 1) & 65535 : 0;
        int l18 = p3 + 1;
        int l6 = 0;
        while (true) {
            int l7 = l6 << 1;
            int x = r[l6] << l4;
            mem.store16(RHI + l7, x >>> 16);
            mem.store16(RLO + l7, (x & 65534) >>> 1);
            boolean cont = p3 != l6;
            l6++;
            if (!cont) {
                break;
            }
        }
        int p0 = r[1] << l4;
        int sR1 = p0 >> 31;
        int absR1 = (p0 ^ sR1) - sR1;
        int rHi0 = mem.load16s(RHI);
        int rLo0 = mem.load16s(RLO);
        int denom = watDiv(absR1, rHi0, rLo0);
        int k0 = p0 > 0 ? -denom : denom;
        p0 = k0;
        k[0] = (short) (p0 >>> 16);
        int l8 = p0 >> 20;
        mem.store16(ALOP + 2, l8);
        mem.store16(AHIP + 2, ((p0 >>> 4) - (l8 << 16)) >>> 1);
        int l9 = 1;
        int l15 = 0;
        int l13 = 0, l8acc = 0, l24, l11;
        boolean aborted = false;
        if (Integer.compareUnsigned(p3, 2) >= 0) {
            int l19 = Integer.compareUnsigned(p3, 1) <= 0 ? 1 : p3;
            int kHi0 = p0 >> 16;
            int term = (((kHi0 * ((p0 >>> 1) & 32767)) >> 14) + kHi0 * kHi0) << 1;
            int ts = term >> 31;
            int ecomp = 2147483647 ^ ((term ^ ts) - ts);
            int ecompHi = ecomp >>> 16;
            int ecompLo = (ecomp >>> 1) & 32767;
            int valE = (((rLo0 * ecompHi) >> 15) + ecompHi * rHi0 + ((ecompLo * rHi0) >> 15)) << 1;
            int vs = valE >> 31;
            l8acc = valE == 0 ? 0 : Integer.numberOfLeadingZeros(valE ^ vs) - 1;
            l13 = valE << l8acc;
            l6 = 2;
            while (true) {
                int copyLen = (((l15 + 3 <= 2) ? 2 : (l15 + 3)) << 1) - 2;
                int eHi = l13 >>> 16;
                l4 = 0;
                for (p0 = 1; p0 != l6; p0++) {
                    int idx = p0 << 1;
                    int rHiP = mem.load16s(RHI + idx);
                    int back = (l6 - p0) << 1;
                    int aHiBack = mem.load16s(AHIP + back);
                    int aLoBack = mem.load16s(ALOP + back);
                    int rLoP = mem.load16s(RLO + idx);
                    l4 += (((rHiP * aHiBack) >> 15) + rHiP * aLoBack + ((rLoP * aLoBack) >> 15)) << 1;
                }
                int l10 = l6 << 1;
                int numPre = (mem.load16u(RHI + l10) << 16) + (l4 << 4) + (mem.load16s(RLO + l10) << 1);
                int sN = numPre >> 31;
                int absNum = (numPre ^ sN) - sN;
                l24 = (l13 >>> 1) & 32767;
                int divr = watDiv(absNum, (short) eHi, l24);
                p0 = numPre > 0 ? -divr : divr;
                int kmShifted = p0 << l8acc;
                int sat = p0 > 0 ? 2147483647 : -2147483648;
                int sP = p0 >> 31;
                int normKm = p0 == 0 ? 0 : Integer.numberOfLeadingZeros(p0 ^ sP) - 1;
                int branch = ((short) l8acc <= normKm) ? kmShifted : sat;
                l11 = p0 != 0 ? branch : kmShifted;
                k[l6 - 1] = (short) (l11 >>> 16);
                int kc = l11 >> 16;
                int kcS = kc >> 31;
                if (Integer.compareUnsigned((kc ^ kcS) - kcS, 32750) > 0) {
                    aborted = true;
                    l9 = 0;
                    break;
                }
                l4 = l11 >> 16;
                int l14 = (l11 >>> 1) & 32767;
                for (p0 = 1; p0 != l6; p0++) {
                    int idx = p0 << 1;
                    int aHiPrevP = mem.load16s(AHIP + idx);
                    int back = (l6 - p0) << 1;
                    int aLoBack = mem.load16s(ALOP + back);
                    int newAHi = aHiPrevP + l4 * aLoBack + ((l14 * aLoBack) >> 15)
                            + ((l4 * mem.load16s(AHIP + back)) >> 15);
                    mem.store16(AHI + idx, newAHi & 32767);
                    mem.store16(ALO + idx, mem.load16u(ALOP + idx) + (newAHi >>> 15));
                }
                int pm = l11 >> 20;
                mem.store16(ALO + l10, pm);
                mem.store16(AHI + l10, ((l11 >>> 4) - (pm << 16)) >>> 1);
                System.arraycopy(mem.b, ALO + 2, mem.b, ALOP + 2, copyLen);
                System.arraycopy(mem.b, AHI + 2, mem.b, AHIP + 2, copyLen);
                int oldEShift = l8acc;
                int kmHi = l4;
                int kterm = (kmHi * kmHi + ((kmHi * l14) >> 14)) << 1;
                int kts = kterm >> 31;
                int ecn = 2147483647 ^ ((kterm ^ kts) - kts);
                int ecnHi = ecn >>> 16;
                int eHiOld = l13 >> 16;
                int eNew = ((ecnHi * l24) >>> 15) + ecnHi * eHiOld + (((ecn >>> 1) & 32767) * eHiOld >> 15);
                int renorm = eNew == 0 ? 0 : Integer.numberOfLeadingZeros((eNew << 1) ^ (eNew >> 30)) - 1;
                l8acc = oldEShift + renorm;
                l13 = (eNew << 1) << renorm;
                l15++;
                boolean cont = l6 != l19;
                l6++;
                if (!cont) {
                    break;
                }
            }
        }
        if (aborted) {
            return l9;
        }
        a[0] = (short) 4096;
        l9 = 1;
        if (Integer.compareUnsigned(l18, 2) >= 0) {
            for (int i = 1; ; i++) {
                int off = i << 1;
                int hi = mem.load16s(AHIP + off);
                int lo = mem.load16u(ALOP + off);
                a[i] = (short) (((hi << 2) + (lo << 17) + 32768) >>> 16);
                boolean cont = p3 != i;
                if (!cont) {
                    break;
                }
            }
        }
        return l9;
    }

    // $f8885 input-driven AR filter, WAT-literal (64-bit accumulation, descending taps).
    private static void watFilterArInput(short[] in, int inPos, short[] out, int outPos, short[] coeff,
                                         int order, int len) {
        long l6 = coeff[0];
        for (int l7 = 0; l7 < len; l7++) {
            long l5 = 0;
            for (int p3 = order - 1; p3 != 0; p3--) {
                l5 += (long) out[outPos + l7 - p3] * coeff[p3];
            }
            long v = (long) in[inPos + l7] * l6 - l5;
            long c = v <= -134217728L ? -134217728L : v;
            c = c >= 134215679L ? 134215679L : c;
            out[outPos + l7] = (short) ((c + 2048L) >>> 12);
        }
    }

    // $f9921 peak detection (single peak), literal.
    private static void watPeak(short[] data, int len, int fsMult, int[] pi, short[] pv) {
        int byteScale = fsMult << 1;
        int bound = len - 1;
        int best = -32768;
        int bestIdx = 0;
        for (int j = 0; j < bound; j++) {
            int v = data[j];
            if (v > best) {
                best = v;
                bestIdx = j;
            }
        }
        pi[0] = bestIdx;
        int endpoint = len - 2;
        if (bestIdx != 0 && bestIdx != endpoint) {
            watParab(data, bestIdx - 1, fsMult, pi, pv);
        } else if (bestIdx == endpoint) {
            int next = data[bestIdx + 1];
            if (next < best) {
                watParab(data, bestIdx - 1, fsMult, pi, pv);
            } else {
                pv[0] = (short) ((next + best) >> 1);
                pi[0] = ((bestIdx << 1) | 1) * fsMult;
            }
        } else {
            pv[0] = (short) best;
            pi[0] = bestIdx * byteScale;
        }
    }

    @Nested
    @DisplayName("WebRtcSpl_SqrtFloor ($f8908)")
    class SqrtFloor {
        @Test
        @DisplayName("matches the exact integer floor-sqrt across the non-negative range")
        void exact() {
            var rnd = new Random(7);
            assertEquals(0, NetEqSignalProcessing.sqrtFloor(0));
            assertEquals(1, NetEqSignalProcessing.sqrtFloor(1));
            assertEquals(46340, NetEqSignalProcessing.sqrtFloor(Integer.MAX_VALUE));
            for (var t = 0; t < 300_000; t++) {
                var v = rnd.nextInt() & 0x7FFFFFFF;
                assertEquals(exactFloorSqrt(v), NetEqSignalProcessing.sqrtFloor(v), "v=" + v);
            }
        }
    }

    @Nested
    @DisplayName("WebRtcSpl divide helper ($f8886)")
    class DivW32W16 {
        @Test
        @DisplayName("reproduces the native reflection-coefficient divide")
        void matchesReference() {
            var rnd = new Random(11);
            for (var t = 0; t < 300_000; t++) {
                var num = rnd.nextInt() & 0x7FFFFFFF;
                var denHi = rnd.nextInt(65536) - 32768;
                var denLo = rnd.nextInt(32768);
                assertEquals(watDiv(num, denHi, denLo),
                        NetEqSignalProcessing.divW32W16(num, denHi, denLo),
                        "num=" + num + " hi=" + denHi + " lo=" + denLo);
            }
        }
    }

    @Nested
    @DisplayName("WebRtcSpl_CrossCorrelation ($f8892)")
    class CrossCorrelation {
        @Test
        @DisplayName("reproduces the per-product-shifted correlation across random shapes")
        void matchesReference() {
            var rnd = new Random(13);
            for (var t = 0; t < 5_000; t++) {
                var len = 1 + rnd.nextInt(40);
                var numCc = 1 + rnd.nextInt(20);
                var step = 1 + rnd.nextInt(3);
                var shift = rnd.nextInt(8);
                var s1 = new short[len + 8];
                var s2 = new short[len + numCc * step + 8];
                for (var i = 0; i < s1.length; i++) {
                    s1[i] = (short) (rnd.nextInt(65536) - 32768);
                }
                for (var i = 0; i < s2.length; i++) {
                    s2[i] = (short) (rnd.nextInt(65536) - 32768);
                }
                var expected = new int[numCc];
                var actual = new int[numCc];
                watXcorr(expected, s1, s2, len, numCc, shift, step);
                NetEqSignalProcessing.crossCorrelation(actual, s1, s2, len, numCc, shift, step);
                assertArrayEquals(expected, actual, "t=" + t);
            }
        }
    }

    @Nested
    @DisplayName("WebRtcSpl_DownsampleFast + per-rate bank ($f8893 / $f9925)")
    class Downsample {
        @Test
        @DisplayName("rejects an unsupported rate and too short an input")
        void rejectsBadRateAndShortInput() {
            var out = new short[64];
            assertEquals(-1, NetEqSignalProcessing.downsampleTo4kHz(out, new short[4], 4, 110, 16_001, true));
            assertEquals(-1, NetEqSignalProcessing.downsampleTo4kHz(out, new short[4], 4, 110, 16_000, true));
        }

        @Test
        @DisplayName("reproduces the native decimation across every rate, output length, and warm-up flag")
        void matchesReferenceAllRates() {
            int[] rates = {8_000, 16_000, 24_000, 32_000, 48_000};
            var rnd = new Random(17);
            for (var rate : rates) {
                for (var warm : new boolean[]{true, false}) {
                    for (var t = 0; t < 2_000; t++) {
                        var inLen = 50 + rnd.nextInt(800);
                        var outLen = 1 + rnd.nextInt(120);
                        var in = new short[inLen];
                        for (var i = 0; i < inLen; i++) {
                            in[i] = (short) (rnd.nextInt(65536) - 32768);
                        }
                        var expected = new short[outLen + 4];
                        var actual = new short[outLen + 4];
                        var er = watDownsampleSelect(expected, in, inLen, outLen, rate, warm);
                        var ar = NetEqSignalProcessing.downsampleTo4kHz(actual, in, inLen, outLen, rate, warm);
                        assertEquals(er, ar, "rate=" + rate + " warm=" + warm + " t=" + t);
                        for (var i = 0; i < Math.max(0, er); i++) {
                            assertEquals(expected[i], actual[i], "rate=" + rate + " t=" + t + " i=" + i);
                        }
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Q14 cross-fade ($f9899)")
    class CrossFade {
        @Test
        @DisplayName("reproduces the weighted overlap-add across random overlaps")
        void matchesReference() {
            var rnd = new Random(19);
            for (var t = 0; t < 20_000; t++) {
                var overlap = 1 + rnd.nextInt(120);
                var oldd = new short[overlap];
                var newd = new short[overlap];
                for (var i = 0; i < overlap; i++) {
                    oldd[i] = (short) (rnd.nextInt(65536) - 32768);
                    newd[i] = (short) (rnd.nextInt(65536) - 32768);
                }
                var expected = new short[overlap];
                var actual = new short[overlap];
                watCrossFade(expected, oldd, newd, overlap);
                NetEqSignalProcessing.crossFade(actual, 0, oldd, 0, newd, 0, overlap);
                assertArrayEquals(expected, actual, "t=" + t);
            }
        }

        @Test
        @DisplayName("fades from old toward new across the overlap")
        void fadesOldToNew() {
            var overlap = 80;
            var oldd = new short[overlap];
            var newd = new short[overlap];
            java.util.Arrays.fill(oldd, (short) 10_000);
            java.util.Arrays.fill(newd, (short) -10_000);
            var out = new short[overlap];
            NetEqSignalProcessing.crossFade(out, 0, oldd, 0, newd, 0, overlap);
            // first blended sample is mostly old (positive), last is mostly new (negative)
            org.junit.jupiter.api.Assertions.assertTrue(out[0] > 0, "fade should start near old");
            org.junit.jupiter.api.Assertions.assertTrue(out[overlap - 1] < 0, "fade should end near new");
        }
    }

    @Nested
    @DisplayName("WebRtcSpl_LevinsonDurbin ($f8888)")
    class LevinsonDurbin {
        @Test
        @DisplayName("reproduces the native LPC and reflection coefficients and the instability abort")
        void matchesReference() {
            var rnd = new Random(29);
            for (var order : new int[]{6, 7, 8, 10}) {
                for (var t = 0; t < 100_000; t++) {
                    var r = new int[order + 1];
                    var energy = 1 + rnd.nextInt(1 << 28);
                    r[0] = energy;
                    for (var i = 1; i <= order; i++) {
                        r[i] = (rnd.nextInt(2 * energy + 1) - energy) / (i + 1);
                    }
                    var aRef = new short[order + 1];
                    var kRef = new short[order];
                    var aAct = new short[order + 1];
                    var kAct = new short[order];
                    var rRef = watLevinson(r, aRef, kRef, order);
                    var rAct = NetEqSignalProcessing.levinsonDurbin(r, aAct, kAct, order);
                    assertEquals(rRef, rAct, "order=" + order + " t=" + t + " return");
                    assertArrayEquals(aRef, aAct, "order=" + order + " t=" + t + " A");
                    assertArrayEquals(kRef, kAct, "order=" + order + " t=" + t + " K");
                }
            }
        }
    }

    @Nested
    @DisplayName("input-driven AR filter ($f8885)")
    class FilterArInput {
        @Test
        @DisplayName("reproduces the native sixty-four-bit input-excited AR synthesis")
        void matchesReference() {
            var rnd = new Random(31);
            for (var t = 0; t < 100_000; t++) {
                var order = 2 + rnd.nextInt(7);
                var len = 1 + rnd.nextInt(40);
                var total = order + len;
                var in = new short[total];
                var ref = new short[total];
                var act = new short[total];
                for (var i = 0; i < total; i++) {
                    var v = (short) (rnd.nextInt(16000) - 8000);
                    in[i] = (short) (rnd.nextInt(16000) - 8000);
                    ref[i] = v;
                    act[i] = v;
                }
                var coeff = new short[order];
                for (var i = 0; i < order; i++) {
                    coeff[i] = (short) (rnd.nextInt(8192) - 4096);
                }
                watFilterArInput(in, order, ref, order, coeff, order, len);
                NetEqSignalProcessing.filterArInput(in, order, act, order, coeff, order, len);
                assertArrayEquals(ref, act, "t=" + t);
            }
        }
    }

    @Nested
    @DisplayName("DspHelper::PeakDetection + ParabolicFit ($f9921 / $f9922)")
    class PeakDetection {
        @Test
        @DisplayName("reproduces the native peak index and value across random correlation curves")
        void matchesReference() {
            var rnd = new Random(23);
            int[] fsMults = {1, 2, 3, 4};
            for (var fm : fsMults) {
                for (var t = 0; t < 60_000; t++) {
                    var len = 4 + rnd.nextInt(60);
                    var data = new short[len];
                    var shape = rnd.nextInt(3);
                    if (shape == 0) {
                        var peak = 1 + rnd.nextInt(len - 2);
                        for (var i = 0; i < len; i++) {
                            var d = Math.abs(i - peak);
                            data[i] = (short) Math.max(-32768,
                                    20000 - d * (500 + rnd.nextInt(2000)) + rnd.nextInt(400) - 200);
                        }
                    } else if (shape == 1) {
                        for (var i = 0; i < len; i++) {
                            data[i] = (short) (rnd.nextInt(65536) - 32768);
                        }
                    } else {
                        var c = (short) rnd.nextInt(2000);
                        java.util.Arrays.fill(data, c);
                    }
                    var refIdx = new int[1];
                    var actIdx = new int[1];
                    var refVal = new short[1];
                    var actVal = new short[1];
                    watPeak(data.clone(), len, fm, refIdx, refVal);
                    NetEqSignalProcessing.peakDetection(data.clone(), len, fm, actIdx, actVal);
                    assertEquals(refIdx[0], actIdx[0], "fm=" + fm + " t=" + t + " idx");
                    assertEquals(refVal[0], actVal[0], "fm=" + fm + " t=" + t + " val");
                }
            }
        }
    }
}
