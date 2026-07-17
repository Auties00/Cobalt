package com.github.auties00.cobalt.calls.media.audio.neteq;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * The fixed point signal processing leaf kernels the NetEq time stretch and concealment operations are
 * built from, a faithful Java transcription of WhatsApp's fixed point audio signal processing primitives.
 *
 * <p>Every method here is a pure, allocation light fixed point primitive that reproduces a single kernel
 * exactly: the integer cross correlation the lag search runs, the parabolic peak refinement that turns an
 * integer lag into a sub sample lag, the Q14 cross fade that splices two signal regions, the polyphase
 * decimation that downsamples the search signal to the internal four kilohertz analysis rate, the fixed
 * point Levinson Durbin recursion that fits the autoregressive model the expander extrapolates from, and
 * the integer floor square root the energy normalization uses. They carry no state; the operations that
 * compose them ({@link NetEqSyncBuffer} history, the time stretch and concealment kernels) own all buffers
 * and call these as stateless functions.
 *
 * <p>Q format conventions reproduced here: filter taps and the autoregressive coefficients are Q12; cross
 * fade and correlation peak weights are Q14 (unity {@code 16384}); the Levinson Durbin internal reflection
 * coefficients are Q15 with a hi/lo thirty two bit split; the decimation rounds with {@code 1 << 11} before
 * the Q12 down shift. Each kernel's exact shift amounts and rounding constants are documented on its method.
 */
public final class NetEqSignalProcessing {
    /**
     * The logger for {@link NetEqSignalProcessing}.
     */
    private static final System.Logger LOGGER = Log.get(NetEqSignalProcessing.class);

    /**
     * The internal analysis sample rate the lag search runs at, in hertz.
     *
     * <p>The time stretch and concealment lag searches decimate the input to this rate before cross
     * correlating, so the correlation runs over a quarter as many samples at sixteen kilohertz.
     */
    static final int ANALYSIS_RATE_HZ = 4_000;

    /**
     * The eight kilohertz to four kilohertz decimation FIR taps, Q12.
     *
     * <p>A symmetric three tap lowpass summing to unity in Q12; the input is decimated by two.
     */
    private static final short[] DECIMATE_8K = {1229, 1638, 1229};

    /**
     * The sixteen kilohertz to four kilohertz decimation FIR taps, Q12.
     *
     * <p>A symmetric five tap lowpass; the input is decimated by four. This is the bank the call audio
     * format uses, since call audio is sixteen kilohertz mono.
     */
    private static final short[] DECIMATE_16K = {614, 819, 1229, 819, 614};

    /**
     * The twenty four kilohertz to four kilohertz decimation FIR taps, Q12.
     *
     * <p>A symmetric seven tap lowpass; the input is decimated by six.
     */
    private static final short[] DECIMATE_24K = {306, 565, 760, 833, 760, 565, 306};

    /**
     * The thirty two kilohertz to four kilohertz decimation FIR taps, Q12.
     *
     * <p>A symmetric seven tap lowpass; the input is decimated by eight.
     */
    private static final short[] DECIMATE_32K = {584, 512, 625, 667, 625, 512, 584};

    /**
     * The forty eight kilohertz to four kilohertz decimation FIR taps, Q12.
     *
     * <p>A symmetric seven tap lowpass; the input is decimated by twelve.
     */
    private static final short[] DECIMATE_48K = {1019, 390, 427, 440, 427, 390, 1019};

    /**
     * The parabolic fit coefficient triples, sixteen records of {@code {c0, c1, c2}} flattened.
     *
     * <p>Indexed by the sub sample fractional lag position the parabolic fit walks to; each record supplies
     * the three window coefficients the refined peak value is blended from.
     */
    private static final short[] PARABOLA_COEFFICIENTS = {
            120, 32, 64, 140, 44, 75, 150, 50, 80, 160, 57, 85,
            180, 72, 96, 200, 89, 107, 210, 98, 112, 220, 108, 117,
            240, 128, 128, 260, 150, 139, 270, 162, 144, 280, 174, 149,
            300, 200, 160, 320, 228, 171, 330, 242, 176, 340, 257, 181,
            360, 288, 192
    };

    /**
     * Prevents instantiation of this stateless kernel holder.
     */
    private NetEqSignalProcessing() {
    }

    /**
     * Computes the fixed point cross correlation of an anchor sequence against successive lags of a sliding
     * sequence, the lag search energy curve.
     *
     * <p>For each of {@code numCc} output lags it accumulates the inner product of the first {@code length}
     * samples of {@code seq1} against the window of {@code seq2} starting {@code i * step} samples in, right
     * shifting each product by {@code shift} before the accumulation rather than shifting the final sum, so
     * the fixed point rounding matches the generic integer kernel exactly. The output is a plain signed
     * thirty two bit correlation per lag.
     *
     * @implNote This implementation applies the per product arithmetic right shift
     * {@code (seq1[k] * seq2[i*step+k]) >> shift} inside the accumulation loop, with no rounding constant.
     *
     * @param crossCorrelation the output array receiving one correlation per lag; at least {@code numCc} long
     * @param seq1             the anchor sequence
     * @param seq2             the sliding sequence
     * @param length           the number of taps accumulated per lag
     * @param numCc            the number of output lags
     * @param shift            the per product arithmetic right shift
     * @param step             the lag step in samples between successive correlation outputs
     */
    static void crossCorrelation(int[] crossCorrelation, short[] seq1, short[] seq2,
                                 int length, int numCc, int shift, int step) {
        for (var i = 0; i < numCc; i++) {
            var sum = 0;
            var base = i * step;
            for (var k = 0; k < length; k++) {
                sum += (seq1[k] * seq2[base + k]) >> shift;
            }
            crossCorrelation[i] = sum;
        }
    }

    /**
     * Decimates a signal to four kilohertz through the per rate polyphase FIR bank, the lag search front end.
     *
     * <p>Selects the decimation FIR for {@code sampleRateHz} (one of eight, sixteen, twenty four, thirty
     * two, or forty eight kilohertz), advances the input past the filter warm up, and produces up to
     * {@code outLength} output samples, one per {@code factor} input samples, by accumulating the FIR with a
     * {@code 1 << 11} rounding constant and a Q12 down shift, saturating each output to signed sixteen bits.
     * Returns the number of output samples written, or {@code -1} for an unsupported rate or too short an
     * input.
     *
     * @implNote This implementation picks per rate the tap count (the FIR length, which also fixes the warm
     * up advance), the decimation factor, and the warm up index: sixteen kilohertz uses the five tap bank
     * with factor four. The input pointer is advanced by {@code tapCount - 1} and the available length
     * reduced by the same, then {@link #downsampleFast(short[], short[], int, int, int, short[], int, int, int)}
     * runs with the requested {@code outLength} as its range bound, the per rate warm up index as the start
     * delay (selected by {@code warmup}), and the tap count as the inner FIR length. The inner accumulation
     * rounds with {@code 2048}, shifts the Q12 taps down with {@code >> 12}, and clamps to
     * {@code [-32768, 32767]}.
     *
     * @param out          the output buffer receiving the decimated samples; at least {@code outLength} long
     * @param in           the input signal
     * @param inLength     the number of input samples available
     * @param outLength    the number of decimated samples to produce, the analysis window length
     * @param sampleRateHz the input sample rate in hertz
     * @param warmup       whether to start at the per rate warm up index rather than at zero
     * @return the number of decimated samples written, or {@code -1} on an unsupported rate or short input
     */
    static int downsampleTo4kHz(short[] out, short[] in, int inLength, int outLength,
                                int sampleRateHz, boolean warmup) {
        short[] coefficients;
        int factor;
        int warmupIndex;
        switch (sampleRateHz) {
            case 8_000 -> { coefficients = DECIMATE_8K; factor = 2; warmupIndex = 2; }
            case 16_000 -> { coefficients = DECIMATE_16K; factor = 4; warmupIndex = 3; }
            case 24_000 -> { coefficients = DECIMATE_24K; factor = 6; warmupIndex = 4; }
            case 32_000 -> { coefficients = DECIMATE_32K; factor = 8; warmupIndex = 4; }
            case 48_000 -> { coefficients = DECIMATE_48K; factor = 12; warmupIndex = 4; }
            default -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "unsupported decimation sample rate {0} hz", sampleRateHz);
                }
                return -1;
            }
        }
        var tapCount = coefficients.length;
        var advance = tapCount - 1;
        var delay = warmup ? warmupIndex : 0;
        return downsampleFast(out, in, advance, inLength - advance, outLength,
                coefficients, tapCount, factor, delay);
    }

    /**
     * Runs the polyphase FIR decimation with explicit coefficients over a requested output length, the core
     * of the downsample.
     *
     * <p>Computes the last input index the FIR reaches from the requested {@code outLength} and returns
     * {@code -1} when the input is shorter than that or when {@code outLength} or the tap count is zero,
     * otherwise accumulates the FIR for every {@code factor}th input sample from {@code delay} onward,
     * rounding with {@code 1 << 11}, shifting the Q12 result down with {@code >> 12}, and saturating to
     * signed sixteen bits.
     *
     * @implNote This implementation computes the range bound as
     * {@code endIdx = (outLength - 1) * factor + 1 + delay}; the guard {@code inLength < endIdx} returns
     * {@code -1}; the inner product walks {@code in[inOffset + i - j] * coeff[j]} for {@code j} in
     * {@code [0, tapCount)} with {@code i} advancing by {@code factor}.
     *
     * @param out       the output buffer
     * @param in        the input signal
     * @param inOffset  the offset into {@code in} the FIR reads from, the warm up advance
     * @param inLength  the number of input samples available from {@code inOffset}
     * @param outLength the number of output samples to produce, the range bound
     * @param coeff     the FIR taps, Q12
     * @param tapCount  the number of taps
     * @param factor    the decimation factor
     * @param delay     the initial input index, the filter warm up skip
     * @return the number of output samples written, or {@code -1} on a short input or zero length
     */
    static int downsampleFast(short[] out, short[] in, int inOffset, int inLength, int outLength,
                              short[] coeff, int tapCount, int factor, int delay) {
        if (outLength == 0 || tapCount == 0) {
            return -1;
        }
        var endIdx = (outLength - 1) * factor + 1 + delay;
        if (inLength < endIdx) {
            return -1;
        }
        if (endIdx <= delay) {
            return 0;
        }
        var outPos = 0;
        for (var i = delay; i < endIdx; i += factor) {
            var acc = 2048;
            for (var j = 0; j < tapCount; j++) {
                acc += in[inOffset + i - j] * coeff[j];
            }
            var v = acc >> 12;
            if (v < -32768) {
                v = -32768;
            } else if (v > 32767) {
                v = 32767;
            }
            out[outPos++] = (short) v;
        }
        return outPos;
    }

    /**
     * Computes the integer floor of the square root of a non negative value, the energy normalization
     * primitive.
     *
     * <p>Returns {@code floor(sqrt(value))} for {@code value} in {@code [0, 2^31)} using a sixteen stage non
     * restoring binary square root, so the result matches the engine exactly. The result is undefined for a
     * negative input.
     *
     * @implNote This implementation runs the iterative non restoring square root, which produces the
     * identical {@code floor(sqrt)} across the whole non negative range as the fully unrolled sixteen stage
     * bit chain.
     *
     * @param value the non negative value to take the square root of
     * @return the integer floor of the square root
     */
    static int sqrtFloor(int value) {
        if (value <= 0) {
            return 0;
        }
        var root = 0;
        var bit = 1 << 30;
        while (bit > value) {
            bit >>= 2;
        }
        var rem = value;
        while (bit != 0) {
            if (rem >= root + bit) {
                rem -= root + bit;
                root = (root >> 1) + bit;
            } else {
                root >>= 1;
            }
            bit >>= 2;
        }
        return root;
    }

    /**
     * Divides a non negative thirty two bit numerator by a sixteen bit denominator supplied as a hi/lo
     * split, the reflection coefficient divide.
     *
     * <p>Computes the reciprocal of the denominator with a single Newton refinement and multiplies it by
     * the numerator using a hi/lo Q15 split, returning the quotient scaled by the routine's fixed final left
     * shift. Returns {@code -1} when the denominator high word is zero.
     *
     * @implNote This implementation seeds the reciprocal estimate as {@code 0x1FFFFFFF / denHi}, refines it
     * with {@code q*(-2*denHi) - ((q*denLo>>14) & ~1) + 0x7FFFFFFF}, then multiplies by the numerator high
     * and low words with the closing {@code << 3}.
     *
     * @param numerator the non negative numerator
     * @param denHi     the denominator high word
     * @param denLo     the denominator low word
     * @return the fixed point quotient, or {@code -1} when {@code denHi} is zero
     */
    static int divW32W16(int numerator, int denHi, int denLo) {
        if (denHi == 0) {
            return -1;
        }
        var negTwoHi = 0 - (denHi << 1);
        var q = (int) (short) (0x1FFFFFFF / denHi);
        var acc = q * negTwoHi - (((q * denLo) >> 14) & ~1) + 0x7FFFFFFF;
        var r = (((acc >>> 1) & 0x7FFF) * q >> 15) + ((acc >> 16) * q);
        var rHi = (r << 1) >> 16;
        var numHi = numerator >> 16;
        return (rHi * numHi
                + ((r & 0x7FFF) * numHi >> 15)
                + (rHi * ((numerator >>> 1) & 0x7FFF) >> 15)) << 3;
    }

    /**
     * Detects the dominant peak of a correlation curve and refines it to a sub sample lag, the lag picker.
     *
     * <p>Scans {@code data} for its maximum, then for an interior peak refines the index and value with a
     * three point parabolic fit, for an endpoint peak either fits the parabola on the descending side or
     * averages the two endpoint samples, and writes the chosen index into {@code peakIndex[0]} and value
     * into {@code peakValue[0]}. Only the single peak form the time stretch lag search uses is supported;
     * the index is reported in the half sample units scaled by {@code fsMult}.
     *
     * @implNote This implementation handles the single peak case: the argmax seed is {@code -32768}; an
     * interior peak (index not at either end) refines through
     * {@link #parabolicFit(short[], int, int, int[], int, short[])}; the right endpoint descending case also
     * refines, otherwise the value is {@code (next + best) >> 1} and the index
     * {@code ((best << 1) | 1) * fsMult}; the left endpoint reports {@code best * 2 * fsMult}.
     *
     * @param data       the correlation curve to search; not modified for the single peak case
     * @param dataLength the number of valid samples in {@code data}
     * @param fsMult      the sample rate multiplier scaling the reported index
     * @param peakIndex   the one element output array receiving the refined index
     * @param peakValue   the one element output array receiving the refined value
     */
    static void peakDetection(short[] data, int dataLength, int fsMult,
                              int[] peakIndex, short[] peakValue) {
        var byteScale = fsMult << 1;
        var bound = dataLength - 1;
        var bestVal = -32768;
        var bestIdx = 0;
        for (var j = 0; j < bound; j++) {
            int v = data[j];
            if (v > bestVal) {
                bestVal = v;
                bestIdx = j;
            }
        }
        peakIndex[0] = bestIdx;
        var endpoint = dataLength - 2;
        if (bestIdx != 0 && bestIdx != endpoint) {
            parabolicFit(data, bestIdx - 1, fsMult, peakIndex, 0, peakValue);
        } else if (bestIdx == endpoint) {
            int next = data[bestIdx + 1];
            if (next < bestVal) {
                parabolicFit(data, bestIdx - 1, fsMult, peakIndex, 0, peakValue);
            } else {
                peakValue[0] = (short) ((next + bestVal) >> 1);
                peakIndex[0] = ((bestIdx << 1) | 1) * fsMult;
            }
        } else {
            peakValue[0] = (short) bestVal;
            peakIndex[0] = bestIdx * byteScale;
        }
    }

    /**
     * Refines an integer correlation peak to a sub sample lag with a three point parabolic fit.
     *
     * <p>Fits a parabola through the three samples around the integer peak, walks the per {@code fsMult}
     * fractional lag step table to bracket the sub sample fraction, and writes the interpolated value into
     * {@code peakValue[indexSlot]} and the refined index into {@code peakIndex[indexSlot]}. The integer peak
     * index already stored in {@code peakIndex[indexSlot]} is read and scaled into the refined index. Three
     * result branches are possible: a left vertex branch (subtractive fraction), a found bracket right
     * branch (additive fraction), and the unbracketed branch that stores the loop counter as the value and
     * the scaled integer index.
     *
     * @implNote This implementation forms the parabola terms {@code b = -3*s0 + 4*s1 - s2} and
     * {@code a = s0 - 2*s1 + s2}; the numerator is {@code 120 * b}; {@code negA = -a}; the bracket
     * coefficient starts at {@code mid = (c0(top)+c0(next))/2} and steps by
     * {@code coeffStep = c0(top) - c0(next)} where {@code c0(e)} is the first coefficient of the
     * {@link #PARABOLA_COEFFICIENTS} record selected by step table entry {@code e}. Each bracket compare
     * sign extends the stepped coefficient to sixteen bits ({@code (short) coeff}) before the multiply. The
     * interpolated value is {@code (b*c2 + a*c1 + (s0 << 8)) / 256}. The refined index is
     * {@code (fsMult << 1) * storedIndex} minus the fraction on the left branch and plus the fraction on the
     * found bracket branch; the unbracketed branch stores {@code (fsMult * storedIndex) << 1} and stores the
     * live loop counter as the value, including the case where the inner bracket loop never runs.
     *
     * @param data      the correlation curve
     * @param baseIndex the index of the first of the three samples around the peak ({@code best - 1})
     * @param fsMult     the sample rate multiplier selecting the step table
     * @param peakIndex the output index array, read for the stored integer index and overwritten
     * @param indexSlot the slot in {@code peakIndex} and {@code peakValue} to read and write
     * @param peakValue the output value array
     */
    static void parabolicFit(short[] data, int baseIndex, int fsMult,
                             int[] peakIndex, int indexSlot, short[] peakValue) {
        var steps = stepTable(fsMult);
        var top = parabolaCoeff(steps[fsMult]);
        var next = parabolaCoeff(steps[fsMult - 1]);
        var coeffStep = top - next;

        int s0 = data[baseIndex];
        int s1 = data[baseIndex + 1];
        int s2 = data[baseIndex + 2];
        var b = (-3 * s0) + (s1 << 2) - s2;
        var numerator = b * 120;
        var mid = (top + next) / 2;
        var a = s0 - (s1 << 1) + s2;
        var negA = 0 - a;

        var storedIndex = peakIndex[indexSlot];
        var counter = s1;

        var leftTail = false;
        var foundRight = false;
        var fraction = 1;

        if (numerator < mid * negA) {
            var coeff = mid;
            if (fsMult == 1) {
                leftTail = true;
            } else {
                while (true) {
                    coeff -= coeffStep;
                    if (numerator > (short) coeff * negA) {
                        leftTail = true;
                        break;
                    }
                    fraction++;
                    if (fraction == fsMult) {
                        leftTail = true;
                        break;
                    }
                }
            }
        } else {
            int firstStep = (short) coeffStep;
            if ((mid + firstStep) * negA < numerator) {
                counter = 1;
                if (fsMult == 1) {
                    peakValue[indexSlot] = (short) counter;
                    peakIndex[indexSlot] = (fsMult * storedIndex) << 1;
                    return;
                }
                var coeff = mid + (firstStep << 1);
                while (true) {
                    if (numerator < (short) coeff * negA) {
                        foundRight = true;
                        break;
                    }
                    coeff += coeffStep;
                    counter++;
                    if (counter == fsMult) {
                        break;
                    }
                }
            }
        }

        if (leftTail) {
            writeRefinedValue(steps[fsMult - fraction], b, a, s0, peakValue, indexSlot);
            peakIndex[indexSlot] = (fsMult << 1) * storedIndex - fraction;
        } else if (foundRight) {
            writeRefinedValue(steps[fsMult + counter], b, a, s0, peakValue, indexSlot);
            peakIndex[indexSlot] = (fsMult << 1) * storedIndex + counter;
        } else {
            peakValue[indexSlot] = (short) counter;
            peakIndex[indexSlot] = (fsMult * storedIndex) << 1;
        }
    }

    /**
     * Writes the interpolated parabolic peak value from a step table record, the shared value tail of the
     * fit.
     *
     * <p>Blends the {@code c1} and {@code c2} window coefficients of the selected record with the parabola
     * terms and the base sample into the refined value.
     *
     * @implNote This implementation computes the value shared by both result tails as
     * {@code (b*c2 + a*c1 + (s0 << 8)) / 256}, with {@code c1} at column one and {@code c2} at column two of
     * the {@link #PARABOLA_COEFFICIENTS} record the step table entry selects.
     *
     * @param stepEntry the step table entry selecting the coefficient record
     * @param b         the parabola first order term
     * @param a         the parabola second difference term
     * @param s0        the first window sample
     * @param peakValue the output value array
     * @param indexSlot the output slot
     */
    private static void writeRefinedValue(int stepEntry, int b, int a, int s0,
                                          short[] peakValue, int indexSlot) {
        var record = (stepEntry & 0xFFFF) * 3;
        int c1 = PARABOLA_COEFFICIENTS[record + 1];
        int c2 = PARABOLA_COEFFICIENTS[record + 2];
        var value = (b * c2 + a * c1 + (s0 << 8)) / 256;
        peakValue[indexSlot] = (short) value;
    }

    /**
     * Returns the parabola window coefficient for a fractional lag step table entry.
     *
     * <p>Looks up the first coefficient {@code c0} of the {@link #PARABOLA_COEFFICIENTS} record the table
     * entry selects, reduced to a record index here since the table is stored as packed triples.
     *
     * @param tableEntry the fractional lag step table entry, the unsigned record selector
     * @return the {@code c0} window coefficient of the selected record
     */
    private static int parabolaCoeff(int tableEntry) {
        return PARABOLA_COEFFICIENTS[(tableEntry & 0xFFFF) * 3];
    }

    /**
     * Returns the Q4 fractional lag step table for a sample rate multiplier.
     *
     * <p>Each table holds the sub sample fractional offsets, {@code 16} representing one whole sample, with
     * one entry per {@code 1 / fsMult} position plus the closing {@code 16}.
     *
     * @implNote This implementation builds the step tables {@code {0,8,16}} for {@code fsMult} one,
     * {@code {0,4,8,12,16}} for two, {@code {0,2,...,16}} for three and the default, and
     * {@code {0,1,3,4,5,7,8,9,11,12,13,15,16}} for four.
     *
     * @param fsMult the sample rate multiplier
     * @return the fractional lag step table
     */
    private static short[] stepTable(int fsMult) {
        return switch (fsMult) {
            case 1 -> new short[]{0, 8, 16};
            case 2 -> new short[]{0, 4, 8, 12, 16};
            case 4 -> new short[]{0, 1, 3, 4, 5, 7, 8, 9, 11, 12, 13, 15, 16};
            default -> new short[]{0, 2, 4, 6, 8, 10, 12, 14, 16};
        };
    }

    /**
     * Returns the signed extreme of a sixteen bit vector, the value whose magnitude is largest, the energy
     * scaling primitive.
     *
     * <p>Scans the {@code length} samples of {@code in} from {@code offset}, tracking the running minimum
     * and maximum, then returns whichever of the two has the larger magnitude, signed. A vector whose
     * maximum exactly equals its (unsigned truncated) minimum returns the maximum.
     *
     * @implNote This implementation tracks {@code max} (sign extended) and {@code min} (sign extended)
     * across the vector, then returns {@code (max == (min & 0xFFFF)) ? max : (sext16(max) < -min ? max : min)}
     * sign extended, the tie break that prefers the maximum when the two truncate equal and otherwise picks
     * the larger magnitude.
     *
     * @param in     the sixteen bit input vector
     * @param offset the offset into {@code in} to start at
     * @param length the number of samples to scan
     * @return the signed extreme value, sign extended to a sixteen bit range
     */
    static int minMax(short[] in, int offset, int length) {
        var min = 32767;
        var max = -32768;
        for (var i = 0; i < length; i++) {
            int v = in[offset + i];
            min = Math.min((short) min, v);
            max = Math.max((short) max, v);
        }
        var maxU = max & 0xFFFF;
        int minS = (short) min;
        var inner = ((short) maxU < (0 - minS)) ? maxU : minS;
        var result = (maxU == (minS & 0xFFFF)) ? maxU : inner;
        return (short) result;
    }

    /**
     * Returns the largest absolute value of a sixteen bit vector, clamped to the sixteen bit maximum, the
     * downsample max abs.
     *
     * <p>Scans the {@code length} samples of {@code in} from {@code offset} for the largest absolute value,
     * returning it clamped to {@code 32767}.
     *
     * @implNote This implementation keeps the running maximum of {@code |v|} (computed as
     * {@code (v ^ (v >> 31)) - (v >> 31)}) clamped to {@code 32767}.
     *
     * @param in     the sixteen bit input vector
     * @param offset the offset into {@code in} to start at
     * @param length the number of samples to scan
     * @return the largest absolute value, in {@code [0, 32767]}
     */
    static int maxAbs16(short[] in, int offset, int length) {
        var best = 0;
        for (var i = 0; i < length; i++) {
            int v = in[offset + i];
            var abs = (v ^ (v >> 31)) - (v >> 31);
            if (abs > best) {
                best = abs;
            }
        }
        return Math.min(best, 32767);
    }

    /**
     * Returns the largest absolute value of a thirty two bit vector, clamped to the thirty two bit maximum,
     * the correlation max abs.
     *
     * <p>Scans the {@code length} entries of {@code in} from {@code offset} for the largest absolute value
     * under unsigned comparison, treating {@code Integer.MIN_VALUE} as {@code Integer.MAX_VALUE}, and
     * returns it clamped to {@code Integer.MAX_VALUE}.
     *
     * @implNote This implementation keeps the running unsigned maximum of {@code |v|}, maps
     * {@code Integer.MIN_VALUE} to {@code Integer.MAX_VALUE}, and clamps the result to
     * {@code Integer.MAX_VALUE} under unsigned comparison.
     *
     * @param in     the thirty two bit input vector
     * @param offset the offset into {@code in} to start at
     * @param length the number of entries to scan
     * @return the largest absolute value, clamped to {@code Integer.MAX_VALUE}
     */
    static int maxAbs32(int[] in, int offset, int length) {
        var best = 0;
        for (var i = 0; i < length; i++) {
            var v = in[offset + i];
            var abs = v == Integer.MIN_VALUE ? Integer.MAX_VALUE : (v ^ (v >> 31)) - (v >> 31);
            if (Integer.compareUnsigned(abs, best) > 0) {
                best = abs;
            }
        }
        return Integer.compareUnsigned(best, Integer.MAX_VALUE) >= 0 ? Integer.MAX_VALUE : best;
    }

    /**
     * Shifts a thirty two bit vector into a sixteen bit vector by a signed amount with saturation, the
     * correlation normalization.
     *
     * <p>For a non negative {@code shift} each input entry is arithmetic right shifted by {@code shift}; for
     * a negative {@code shift} each is left shifted by {@code -shift}; every result is saturated to signed
     * sixteen bits before being stored.
     *
     * @implNote This implementation stores {@code sat16(in[i] >> shift)} on the non negative branch and
     * {@code sat16(in[i] << -shift)} on the negative branch, applying the lower clamp to {@code -32768}
     * before the upper clamp to {@code 32767}.
     *
     * @param out    the sixteen bit output vector
     * @param outPos the offset into {@code out} to start writing
     * @param in     the thirty two bit input vector
     * @param inPos  the offset into {@code in} to start reading
     * @param length the number of entries to shift
     * @param shift  the signed shift amount, right when non negative, left when negative
     */
    static void vectorBitShift(short[] out, int outPos, int[] in, int inPos, int length, int shift) {
        for (var i = 0; i < length; i++) {
            var v = shift >= 0 ? in[inPos + i] >> shift : in[inPos + i] << (0 - shift);
            var t = v <= -32768 ? -32768 : v;
            t = t >= 32767 ? 32767 : t;
            out[outPos + i] = (short) t;
        }
    }

    /**
     * Computes the normalized cross correlation of two sixteen bit vectors over a lag window, the lag search
     * correlation wrapper.
     *
     * <p>Derives the per product right shift from the dynamic range of the two input vectors so the
     * correlation does not overflow, then runs
     * {@link #crossCorrelation(int[], short[], short[], int, int, int, int)} with that shift and returns the
     * shift it chose. The number of output lags is {@code numCc} and the lag step is {@code step}; the
     * accumulation length is {@code length}.
     *
     * @implNote This implementation derives the two extremes from {@link #minMax(short[], int, int)}: the
     * anchor over {@code length}, the sliding sequence over {@code |(numCc-1)*step| + length} from offset
     * {@code (numCc-1)*step & sign}. Their product's magnitude times {@code length} as an unsigned sixty four
     * bit value is right shifted by thirty one and wrapped; the right shift is
     * {@code wrapped == 0 ? 0 : 32 - clz(wrapped)}, which is the shift passed to
     * {@link #crossCorrelationShifted(int[], short[], int, short[], int, int, int, int, int)}.
     *
     * @param crossCorrelation the output correlation array; at least {@code numCc} long
     * @param seq1             the anchor sequence base
     * @param seq1Pos          the offset of the anchor sequence
     * @param seq2             the sliding sequence base
     * @param seq2Pos          the offset of the sliding sequence
     * @param length           the accumulation length per lag
     * @param numCc            the number of output lags
     * @param step             the lag step in samples
     * @return the per product right shift chosen
     */
    static int crossCorrelationScaled(int[] crossCorrelation, short[] seq1, int seq1Pos, short[] seq2,
                                      int seq2Pos, int length, int numCc, int step) {
        var extA = minMax(seq1, seq1Pos, length);
        var product = (numCc - 1) * step;
        var sign = product >> 31;
        var absProduct = (product ^ sign) - sign;
        var extB = minMax(seq2, seq2Pos + (product & sign), absProduct + length);
        var ab = extA * extB;
        var absAb = (ab ^ (ab >> 31)) - (ab >> 31);
        var prod = ((long) absAb & 0xFFFFFFFFL) * ((long) length & 0xFFFFFFFFL);
        var wrapped = (int) (prod >>> 31);
        var shift = wrapped == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(wrapped);
        crossCorrelationShifted(crossCorrelation, seq1, seq1Pos, seq2, seq2Pos, length, numCc, shift, step);
        return shift;
    }

    /**
     * Runs the cross correlation with explicit base offsets, the offset taking form of the lag search
     * correlation.
     *
     * <p>Identical to {@link #crossCorrelation(int[], short[], short[], int, int, int, int)} but reading the
     * two sequences from explicit offsets, so a sub window of a larger buffer can be correlated without a
     * copy.
     *
     * @param crossCorrelation the output correlation array
     * @param seq1             the anchor sequence base
     * @param seq1Pos          the offset of the anchor sequence
     * @param seq2             the sliding sequence base
     * @param seq2Pos          the offset of the sliding sequence
     * @param length           the accumulation length per lag
     * @param numCc            the number of output lags
     * @param shift            the per product arithmetic right shift
     * @param step             the lag step in samples
     */
    static void crossCorrelationShifted(int[] crossCorrelation, short[] seq1, int seq1Pos, short[] seq2,
                                        int seq2Pos, int length, int numCc, int shift, int step) {
        for (var i = 0; i < numCc; i++) {
            var sum = 0;
            var base = seq2Pos + i * step;
            for (var k = 0; k < length; k++) {
                sum += (seq1[seq1Pos + k] * seq2[base + k]) >> shift;
            }
            crossCorrelation[i] = sum;
        }
    }

    /**
     * Filters a sixteen bit vector through an all pole autoregressive synthesis filter, the concealment
     * excitation synthesis.
     *
     * <p>Runs the recursive filter {@code out[i] = sat((sum_j coeff[j] * out[i-j]) + 1<<11) >> 12} over
     * {@code length} samples, where the history {@code out[i-j]} reaches back into the samples already
     * written to {@code out}, the standard autoregressive recursion the expander excites with a random or
     * periodic source.
     *
     * @implNote This implementation accumulates {@code out[i-j] * coeff[j]} for {@code j} in
     * {@code [0, order)}, clamps the accumulator to {@code [-134217728, 134215679]} (the Q12 headroom), then
     * rounds with {@code 2048} and shifts down with {@code >> 12}. The history index {@code i - j} reads
     * previously written outputs, so {@code out} must be pre seeded with the filter state.
     *
     * @param out    the output and history vector; pre seeded with the filter state below {@code outPos}
     * @param outPos the offset of the first sample to produce
     * @param coeff  the autoregressive coefficients, Q12, {@code coeff[0]} the gain on the current sample
     * @param order  the number of coefficients
     * @param length the number of output samples to produce
     */
    static void filterAr(short[] out, int outPos, short[] coeff, int order, int length) {
        for (var i = 0; i < length; i++) {
            var acc = 0;
            for (var j = 0; j < order; j++) {
                acc += out[outPos + i - j] * coeff[j];
            }
            var clamped = acc <= -134217728 ? -134217728 : acc;
            clamped = clamped >= 134215679 ? 134215679 : clamped;
            out[outPos + i] = (short) ((clamped + 2048) >> 12);
        }
    }

    /**
     * Filters an input vector through an input driven all pole autoregressive filter, the concealment
     * residual synthesis.
     *
     * <p>Runs the recursive filter {@code out[i] = sat(in[i] * coeff[0] - sum_{j=1}^{order-1} out[i j] *
     * coeff[j]) >> 12} over {@code length} samples, the input excited form the expander drives its unvoiced
     * and background noise residual through. The history {@code out[i-j]} reaches back into the samples
     * already written, so {@code out} must be pre seeded with the filter state below {@code outPos}, exactly
     * as {@link #filterAr(short[], int, short[], int, int)} but with the current sample taken from the input
     * rather than fed back.
     *
     * @implNote This implementation runs the accumulation in sixty four bits:
     * {@code acc = (long) in[i] * coeff[0] - sum_{j=1}^{order-1} (long) out[i j] * coeff[j]}, clamps to
     * {@code [-134217728, 134215679]} (the Q12 headroom), then rounds with {@code 2048} and shifts down with
     * an unsigned {@code >>> 12}. The inner feedback loop walks {@code j} from {@code order - 1} down to
     * {@code 1}.
     *
     * @param in     the input excitation vector
     * @param inPos  the offset of the first input sample
     * @param out    the output and history vector; pre seeded with the filter state below {@code outPos}
     * @param outPos the offset of the first sample to produce
     * @param coeff  the autoregressive coefficients, Q12, {@code coeff[0]} the gain on the current input sample
     * @param order  the number of coefficients
     * @param length the number of output samples to produce
     */
    static void filterArInput(short[] in, int inPos, short[] out, int outPos, short[] coeff, int order,
                              int length) {
        long coeff0 = coeff[0];
        for (var i = 0; i < length; i++) {
            long acc = 0;
            for (var j = order - 1; j != 0; j--) {
                acc += (long) out[outPos + i - j] * coeff[j];
            }
            var v = (long) in[inPos + i] * coeff0 - acc;
            var clamped = v <= -134217728L ? -134217728L : v;
            clamped = clamped >= 134215679L ? 134215679L : clamped;
            out[outPos + i] = (short) ((clamped + 2048L) >>> 12);
        }
    }

    /**
     * Scales a sixteen bit vector by a constant with rounding and a right shift, the gain staging primitive.
     *
     * <p>Computes {@code out[i] = (in[i] * scale + add) >> shift} over {@code length} samples, stored as
     * sixteen bit values (the low sixteen bits, without saturation).
     *
     * @implNote This implementation computes the per sample {@code (in[i] * scale + add) >> shift}, with
     * {@code shift} masked to sixteen bits ({@code shift & 0xFFFF}).
     *
     * @param out    the output vector
     * @param outPos the offset into {@code out}
     * @param in     the input vector
     * @param inPos  the offset into {@code in}
     * @param scale  the multiplier
     * @param add    the rounding addend applied before the shift
     * @param shift  the right shift, masked to sixteen bits
     * @param length the number of samples
     */
    static void scaleVector(short[] out, int outPos, short[] in, int inPos, int scale, int add, int shift,
                            int length) {
        var s = shift & 0xFFFF;
        for (var i = 0; i < length; i++) {
            out[outPos + i] = (short) ((in[inPos + i] * scale + add) >> s);
        }
    }

    /**
     * Blends two sixteen bit vectors with per vector weights and a rounded right shift, the voiced/unvoiced
     * mix.
     *
     * <p>Computes {@code out[i] = (a[i] * weightA + b[i] * weightB + ((1 << shift) >> 1)) >> shift} over
     * {@code length} samples. Returns {@code 0} on success or {@code -1} when an argument is degenerate (a
     * null vector, a negative shift, or a zero length).
     *
     * @implNote This implementation forms the rounding addend {@code (1 << shift) >> 1}, applies it between
     * the two weighted products, then arithmetic right shifts the sum by {@code shift} and stores it as
     * sixteen bits.
     *
     * @param out     the output vector
     * @param outPos  the offset into {@code out}
     * @param a       the first input vector
     * @param aPos    the offset into {@code a}
     * @param weightA the weight on {@code a}
     * @param b       the second input vector
     * @param bPos    the offset into {@code b}
     * @param weightB the weight on {@code b}
     * @param shift   the non negative right shift
     * @param length  the number of samples
     * @return {@code 0} on success, {@code -1} on a degenerate argument
     */
    static int weightedAdd(short[] out, int outPos, short[] a, int aPos, int weightA, short[] b, int bPos,
                           int weightB, int shift, int length) {
        if (out == null || a == null || b == null || shift < 0 || length == 0) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "weighted add received degenerate arguments, shift={0} length={1}",
                        shift, length);
            }
            return -1;
        }
        var round = (1 << shift) >> 1;
        for (var i = 0; i < length; i++) {
            var v = (a[aPos + i] * weightA + round + b[bPos + i] * weightB) >> shift;
            out[outPos + i] = (short) v;
        }
        return 0;
    }

    /**
     * Fits an autoregressive model to a normalized autocorrelation through the fixed point Levinson Durbin
     * recursion, the concealment LPC analysis.
     *
     * <p>Runs the integer Levinson Durbin recursion over the {@code order + 1} autocorrelation values in
     * {@code r}, writing the {@code order + 1} Q12 autoregressive coefficients into {@code a} (with
     * {@code a[0]} forced to {@code 4096}, unity in Q12) and the per step reflection coefficients into
     * {@code k}. Returns {@code 1} when the model is stable and {@code 0} when a reflection coefficient
     * exceeds the stability ceiling, in which case {@code a} holds only the partial result up to the abort.
     *
     * <p>The recursion keeps the autocorrelation, the working coefficients, and the running prediction error
     * in a hi/lo thirty two bit split so the fixed point multiplies do not overflow: the autocorrelation is
     * pre normalized by the leading zero count of {@code r[0]}, each coefficient is carried as a high word
     * and a Q15 low word, and the reflection coefficient divide reuses {@link #divW32W16(int, int, int)}. The
     * output coefficients are recombined into Q12 by {@code ((aHi << 2) + (aLo << 17) + 32768) >> 16}.
     *
     * @implNote This implementation carries six sixteen bit working arrays: the autocorrelation high and low
     * words, the working coefficient high and low words, and the previous iteration coefficient high and low
     * words that the copy step carries forward. The normalization is
     * {@code norm = r[0] != 0 ? (clz(r[0] ^ (r[0] >> 31)) - 1) & 0xFFFF : 0}; each {@code r[i] << norm} is
     * split {@code hi = x >>> 16}, {@code lo = (x & 0xFFFE) >>> 1}. The first reflection coefficient is
     * {@code k0 = (r[1] << norm > 0) ? -divW32W16(|r1|, rHi[0], rLo[0]) : divW32W16(...)}; the coefficient
     * words seeded from it are {@code aLo[1] = k0 >> 20} and {@code aHi[1] = ((k0 >>> 4) - ((k0 >> 20) << 16))
     * >>> 1}. The instability guard is {@code |km >> 16| > 32750 -> return 0}; the running error update forms
     * {@code INT32_MAX ^ |(km*km + (km*kmLo >> 14)) << 1|} and renormalizes through a leading zero shift. The
     * final Q12 recombination is {@code ((aHi << 2) + (aLo << 17) + 32768) >> 16} and {@code a[0]} is forced
     * to {@code 4096}.
     *
     * @param r     the autocorrelation, {@code order + 1} thirty two bit values, {@code r[0]} the energy
     * @param a     the output autoregressive coefficients, Q12, {@code order + 1} long; {@code a[0]} is forced
     *              to {@code 4096}
     * @param k     the output reflection coefficients, one per order step; at least {@code order} long
     * @param order the autoregressive order
     * @return {@code 1} when the fitted model is stable, {@code 0} when a reflection coefficient is unstable
     */
    static int levinsonDurbin(int[] r, short[] a, short[] k, int order) {
        var rHi = new int[order + 1];
        var rLo = new int[order + 1];
        var aHi = new int[order + 1];
        var aLo = new int[order + 1];
        var aHiPrev = new int[order + 1];
        var aLoPrev = new int[order + 1];

        var r0 = r[0];
        var norm = r0 != 0 ? (Integer.numberOfLeadingZeros(r0 ^ (r0 >> 31)) - 1) & 0xFFFF : 0;
        for (var i = 0; i <= order; i++) {
            var x = r[i] << norm;
            rHi[i] = (short) (x >>> 16);
            rLo[i] = (short) ((x & 0xFFFE) >>> 1);
        }

        var r1 = r[1] << norm;
        var signR1 = r1 >> 31;
        var absR1 = (r1 ^ signR1) - signR1;
        var denom = divW32W16(absR1, rHi[0], rLo[0]);
        var k0 = r1 > 0 ? -denom : denom;
        k[0] = (short) (k0 >>> 16);
        aLoPrev[1] = (short) (k0 >> 20);
        aHiPrev[1] = (short) (((k0 >>> 4) - ((k0 >> 20) << 16)) >>> 1);

        var result = 1;
        if (order >= 2) {
            var iterationGuard = order <= 1 ? 1 : order;

            var rLo0 = rLo[0];
            var rHi0 = rHi[0];
            var kHi0 = k0 >> 16;
            var eTermPre = (((kHi0 * ((k0 >>> 1) & 0x7FFF)) >> 14) + kHi0 * kHi0) << 1;
            var eAbs = (eTermPre ^ (eTermPre >> 31)) - (eTermPre >> 31);
            var eComp = 0x7FFFFFFF ^ eAbs;
            var eCompHi = eComp >>> 16;
            var eCompLo = (eComp >>> 1) & 0x7FFF;
            var eVal = (((rLo0 * eCompHi) >> 15) + eCompHi * rHi0 + ((eCompLo * rHi0) >> 15)) << 1;
            var eShift = eVal == 0 ? 0 : Integer.numberOfLeadingZeros(eVal ^ (eVal >> 31)) - 1;
            var eRun = eVal << eShift;

            var m = 2;
            var iteration = 0;
            while (true) {
                var eHi = eRun >> 16;
                var eLo = (eRun >>> 1) & 0x7FFF;

                var acc = 0;
                for (var p = 1; p != m; p++) {
                    var rHiP = rHi[p];
                    var back = m - p;
                    var aLoBack = aLoPrev[back];
                    var contrib = ((rHiP * aHiPrev[back]) >> 15)
                                  + rHiP * aLoBack
                                  + ((rLo[p] * aLoBack) >> 15);
                    acc += contrib << 1;
                }

                var numPre = ((rHi[m] & 0xFFFF) << 16) + (acc << 4) + (rLo[m] << 1);
                var absNum = (numPre ^ (numPre >> 31)) - (numPre >> 31);
                var divResult = divW32W16(absNum, (short) eHi, eLo);
                var kmSigned = numPre > 0 ? -divResult : divResult;
                var kmShifted = kmSigned << eShift;
                var sat = kmSigned > 0 ? 0x7FFFFFFF : 0x80000000;
                var normKm = kmSigned == 0 ? 0 : Integer.numberOfLeadingZeros(kmSigned ^ (kmSigned >> 31)) - 1;
                var branch = (short) eShift <= normKm ? kmShifted : sat;
                var km = kmSigned != 0 ? branch : kmShifted;
                k[m - 1] = (short) (km >>> 16);

                var kmHiCheck = km >> 16;
                if (((kmHiCheck ^ (kmHiCheck >> 31)) - (kmHiCheck >> 31)) > 32750) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "levinson-durbin unstable at order {0} of {1}", m, order);
                    }
                    return 0;
                }

                var kmHi = km >> 16;
                var kmLo = (km >>> 1) & 0x7FFF;
                for (var p = 1; p != m; p++) {
                    var back = m - p;
                    var aLoBack = aLoPrev[back];
                    var newAHi = aHiPrev[p] + kmHi * aLoBack
                                 + ((kmLo * aLoBack) >> 15)
                                 + ((kmHi * aHiPrev[back]) >> 15);
                    aHi[p] = (short) (newAHi & 0x7FFF);
                    aLo[p] = (short) ((aLoPrev[p] & 0xFFFF) + (newAHi >>> 15));
                }
                aLo[m] = (short) (km >> 20);
                aHi[m] = (short) (((km >>> 4) - ((km >> 20) << 16)) >>> 1);

                var copyElems = iteration + 2;
                System.arraycopy(aLo, 1, aLoPrev, 1, copyElems);
                System.arraycopy(aHi, 1, aHiPrev, 1, copyElems);

                var kTerm = (kmHi * kmHi + ((kmHi * kmLo) >> 14)) << 1;
                var kAbs = (kTerm ^ (kTerm >> 31)) - (kTerm >> 31);
                var eCompNew = 0x7FFFFFFF ^ kAbs;
                var eCompNewHi = eCompNew >>> 16;
                var eNew = ((eCompNewHi * eLo) >>> 15)
                           + eCompNewHi * eHi
                           + (((eCompNew >>> 1) & 0x7FFF) * eHi >> 15);
                var renorm = eNew == 0 ? 0 : Integer.numberOfLeadingZeros((eNew << 1) ^ (eNew >> 30)) - 1;
                eShift = eShift + renorm;
                eRun = (eNew << 1) << renorm;

                iteration++;
                if (m == iterationGuard) {
                    break;
                }
                m++;
            }
        }

        a[0] = 4096;
        if (order + 1 >= 2) {
            for (var i = 1; i <= order; i++) {
                var v = ((aHiPrev[i] << 2) + ((aLoPrev[i] & 0xFFFF) << 17) + 32768) >>> 16;
                a[i] = (short) v;
            }
        }
        return result;
    }

    /**
     * Cross fades the tail of an old region into the head of a new region in Q14, the splice that joins two
     * signal segments smoothly.
     *
     * <p>Walks {@code overlap} samples writing {@code (weight * old + (16384 - weight) * new + 8192) >> 14}
     * into {@code out}, where {@code weight} starts at {@code 16384}, is decremented by
     * {@code 16384 / (overlap + 1)} before each sample, so the fade runs from almost all old to almost all
     * new across the overlap region. The {@code 8192} addend is the round to nearest constant for the Q14
     * down shift.
     *
     * @implNote This implementation flattens the cross fade to plain arrays since {@link NetEqSyncBuffer}
     * hands the operations contiguous regions. The rounding addend is {@code + 8192}; the weight decrement
     * happens before the first sample so sample zero already carries {@code 16384 - step}.
     *
     * @param out     the output buffer receiving the cross faded samples
     * @param outPos  the offset in {@code out} to start writing
     * @param oldData the fading out region
     * @param oldPos  the offset of the fading out region
     * @param newData the fading in region
     * @param newPos  the offset of the fading in region
     * @param overlap the number of samples to cross fade
     */
    static void crossFade(short[] out, int outPos, short[] oldData, int oldPos,
                          short[] newData, int newPos, int overlap) {
        var step = 16384 / (overlap + 1);
        var weight = 16384;
        for (var i = 0; i < overlap; i++) {
            weight -= step;
            int oldSample = oldData[oldPos + i];
            int newSample = newData[newPos + i];
            var blended = (weight * oldSample + (16384 - weight) * newSample + 8192) >> 14;
            out[outPos + i] = (short) blended;
        }
    }
}
