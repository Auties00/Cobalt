package com.github.auties00.cobalt.calls.media.audio.neteq;

import com.github.auties00.cobalt.calls.media.audio.neteq.NetEqOperation;

/**
 * Joins a freshly decoded packet onto the tail of an expansion with a cross fade, a faithful single channel
 * port of WhatsApp's WebRTC {@code Merge}.
 *
 * <p>When a packet finally arrives after one or more {@link NetEqOperation#EXPAND} concealment frames, the
 * synthesized concealment and the real decoded audio are at different phases and energies, so splicing them
 * directly would click. The merge lines the two signals up by their best correlation lag, scales the
 * expansion to the decoded energy, and cross fades from the expansion into the decoded audio over a pitch
 * period so the transition is inaudible. The result is a single contiguous frame: the head is the energy
 * matched, phase aligned expansion tail, the body is the cross fade, and the tail is the untouched decoded
 * audio.
 *
 * <p>This port is specialized to the single sixteen kilohertz mono channel the call audio format carries, so
 * the native per channel loop collapses to one pass over flat {@code short[]} arrays and the per channel
 * expand weight is one scalar. The expansion samples are the recent {@link NetEqSyncBuffer} history the prior
 * expansions wrote; the decoded samples are the codec output of the arriving packet.
 *
 * @implNote This implementation reuses the leaf kernels of {@link NetEqSignalProcessing}: the energy dot
 * products and the square root energy ratio for the Q14 scale, the four kilohertz decimation and the sixty
 * lag cross correlation with parabolic peak refinement for the lag search, and the two Q domain cross fade
 * kernels ({@link #rampSingle} single source weight ramp and {@link #rampBlend} two source weighted blend)
 * for the splice. The {@code 1260} sample expanded cap, the {@code min(fs_mult*64, input_length)} energy
 * length, the interpolation length clamp, and the
 * {@code increment = min(4194/fs_mult, (2^20 - weight*64)/old_length)} ramp slope are reproduced from the
 * native body.
 */
public final class NetEqMerge {
    /**
     * The maximum number of expanded samples the merge consumes.
     *
     * <p>An expansion longer than this is trimmed before the merge so the splice region and output stay
     * bounded.
     */
    static final int MAX_EXPANDED_LENGTH = 1260;

    /**
     * Prevents instantiation of this stateless operation holder.
     */
    private NetEqMerge() {
    }

    /**
     * Applies the single source weight ramp to one signal region, the first merge cross fade segment.
     *
     * <p>Walks {@code length} samples writing {@code out[i] = (in[i] * weight + 8192) >> 14} where
     * {@code weight} ramps each sample by {@code increment} in the Q20 domain, clamped into {@code [0, 16384)}.
     * The weight is read from and written back to {@code weightState[0]} so a following segment continues the
     * ramp. This fades a single signal up or down in level without a second source.
     *
     * @implNote This implementation stores each sample with the current Q14 weight
     * {@code (in[i] * (weight & 0xFFFF) + 8192) >>> 14} as a logical shift, then advances the running Q20
     * weight {@code q20 = max(increment + q20, 0)} and derives the next Q14 weight {@code min(q20 >>> 6, 16384)}
     * under unsigned comparison, so the multiply uses the weight before it advances on each sample. The
     * running Q20 weight is seeded {@code (weight << 6) | 32}, the {@code | 32} being the Q20 rounding bit.
     *
     * @param out         the output region
     * @param outPos      the offset into {@code out}
     * @param in          the single source region
     * @param inPos       the offset into {@code in}
     * @param length      the number of samples
     * @param increment   the per sample Q14 weight increment
     * @param weightState a one element array holding the Q14 weight, read and updated in place
     */
    static void rampSingle(short[] out, int outPos, short[] in, int inPos, int length, int increment,
                           int[] weightState) {
        var weight = weightState[0] & 0xFFFF;
        var q20 = (weight << 6) | 32;
        for (var i = 0; i < length; i++) {
            var blended = (in[inPos + i] * (weight & 0xFFFF) - (-8192)) >>> 14;
            out[outPos + i] = (short) blended;
            q20 = Math.max(increment + q20, 0);
            var next = q20 >>> 6;
            weight = Integer.compareUnsigned(next, 16384) >= 0 ? 16384 : next;
        }
        weightState[0] = weight;
    }

    /**
     * Cross fades two signal regions with a descending weight, the second merge cross fade segment.
     *
     * <p>Walks {@code length} samples writing
     * {@code out[i] = (expanded[i] * weight + decoded[i] * (16384 - weight) + 8192) >> 14}, where
     * {@code weight} starts at {@code weightState[0]} and decrements by {@code increment} each sample while the
     * complement weight ascends by the same, so the output fades from the expansion into the decoded audio.
     * The final weight is written back to {@code weightState[0]}.
     *
     * @implNote This implementation sign extends both weights to sixteen bits before the multiply, stores
     * {@code (expanded[i] * sext16(weight) + decoded[i] * sext16(16384 - weight) + 8192) >>> 14} as a logical
     * shift, and updates the weights {@code complement += increment; weight -= increment} with no clamp,
     * matching the native unclamped two source ramp.
     *
     * @param out         the output region
     * @param outPos      the offset into {@code out}
     * @param expanded    the fading out expansion region
     * @param expandedPos the offset into {@code expanded}
     * @param decoded     the fading in decoded region
     * @param decodedPos  the offset into {@code decoded}
     * @param length      the number of samples
     * @param increment   the per sample Q14 weight decrement
     * @param weightState a one element array holding the starting Q14 weight, updated in place
     */
    static void rampBlend(short[] out, int outPos, short[] expanded, int expandedPos, short[] decoded,
                          int decodedPos, int length, int increment, int[] weightState) {
        var weight = weightState[0] & 0xFFFF;
        var complement = 16384 - weight;
        for (var i = 0; i < length; i++) {
            var blended = (expanded[expandedPos + i] * (short) weight
                           + decoded[decodedPos + i] * (short) complement - (-8192)) >>> 14;
            out[outPos + i] = (short) blended;
            complement += increment;
            weight -= increment;
        }
        weightState[0] = weight;
    }

    /**
     * Computes the Q14 weight the merge scales the expansion by, the energy match.
     *
     * <p>Compares the energy of the expansion against the energy of the decoded audio over the leading
     * {@code min(fs_mult * 64, decodedLength)} samples; when the expansion is no louder than the decoded audio
     * the weight is unity ({@code 16384}), otherwise it is the floored square root of
     * {@code (decodedEnergy / expandedEnergy) << 14} clamped to unity and to the supplied per channel expand
     * weight floor.
     *
     * <p>The floored square root is {@link NetEqSignalProcessing#sqrtFloor(int)}, and each energy is measured
     * with {@link NetEqSignalProcessing#maxAbs16(short[], int, int)} for its headroom.
     *
     * @implNote This implementation reproduces the energy comparison exactly. The energy length is
     * {@code min(fs_mult << 6, decodedLength)}; the overflow divisor is {@code INT32_MAX / energyLength}; each
     * energy is a dot product scaled down by the normalized headroom of {@code (maxAbs^2) / overflowDivisor}.
     * The two energies are aligned to a common shift through unsigned compared selects
     * ({@code expandedEnergy >> sel} and {@code decodedEnergy >> sel}); a decoded energy no greater than the
     * expanded energy keeps the weight at {@code 16384}. Otherwise {@code norm} is the leading sign bit count
     * of the aligned decoded energy, the numerator is built from the aligned expanded energy
     * ({@code norm > 2 ? expandedAligned >> (3 - norm) : expandedAligned << (norm - 3)}), the denominator from
     * the aligned decoded energy ({@code norm > 16 ? decodedAligned >> (17 - norm) :
     * decodedAligned << (norm - 17)}), and the Q14 scale is the floored square root of
     * {@code (numerator / denominator) << 14}. The result is clamped to {@code 16384} then floored at
     * {@code expandWeight}.
     *
     * @param expanded      the expansion samples
     * @param decoded       the decoded samples
     * @param decodedLength the number of decoded samples available
     * @param fsMult        the sample rate multiplier
     * @param expandWeight  the per channel expand weight floor, Q14
     * @return the Q14 scale weight, in {@code [expandWeight, 16384]}
     */
    static int signalScaling(short[] expanded, short[] decoded, int decodedLength, int fsMult,
                             int expandWeight) {
        var energyLength = Math.min(fsMult << 6, decodedLength);
        var expandedMaxAbs = NetEqSignalProcessing.maxAbs16(expanded, 0, energyLength);
        var decodedMaxAbs = NetEqSignalProcessing.maxAbs16(decoded, 0, energyLength);
        var overflowDivisor = Integer.MAX_VALUE / energyLength;

        var expandedShift = energyShift((expandedMaxAbs * expandedMaxAbs) / overflowDivisor);
        var expandedEnergy = dotProductWithScale(expanded, expanded, energyLength, expandedShift);
        var decodedShift = energyShift((decodedMaxAbs * decodedMaxAbs) / overflowDivisor);
        var decodedEnergy = dotProductWithScale(decoded, decoded, energyLength, decodedShift);

        var scale = 16384;
        var diffDec = expandedShift - decodedShift;
        var selDec = Integer.compareUnsigned(diffDec, expandedShift) <= 0 ? diffDec : 0;
        var decodedAligned = decodedEnergy >> selDec;
        var diffExp = decodedShift - expandedShift;
        var selExp = Integer.compareUnsigned(diffExp, decodedShift) <= 0 ? diffExp : 0;
        var expandedAligned = expandedEnergy >> selExp;

        if (decodedAligned > expandedAligned) {
            var norm = decodedAligned == 0
                    ? 0 : Integer.numberOfLeadingZeros(decodedAligned ^ (decodedAligned >> 31)) - 1;
            var numerator = norm > 2 ? expandedAligned >> (3 - norm) : expandedAligned << (norm - 3);
            var denominator = norm > 16 ? decodedAligned >> (17 - norm) : decodedAligned << (norm - 17);
            scale = NetEqSignalProcessing.sqrtFloor((numerator / denominator) << 14);
        }

        var clamped = Math.min(scale, 16384);
        return Math.max(clamped, expandWeight);
    }

    /**
     * Returns the per energy right shift the merge dot product scales by, the energy overflow guard.
     *
     * <p>Computes {@code value == 0 ? 0 : 32 - clz(value ^ (value >> 31))}, the shift that keeps the energy
     * accumulation inside thirty two bits given the per sample magnitude headroom.
     *
     * @implNote This implementation derives the shift from the constant {@code 32}, which is also the dot
     * product scaling argument: the shift is {@code 32 - clz(value ^ (value >> 31))}, selected to {@code 0}
     * when {@code value} is zero.
     *
     * @param value the squared max abs over the overflow divisor
     * @return the per product right shift
     */
    private static int energyShift(int value) {
        if (value == 0) {
            return 0;
        }
        return 32 - Integer.numberOfLeadingZeros(value ^ (value >> 31));
    }

    /**
     * Computes a self correlation energy with a per element right shift, the merge energy dot product.
     *
     * <p>Accumulates {@code sum(in[i] * in[i] >> shift)} over {@code length} samples, the energy the
     * {@link #signalScaling(short[], short[], int, int, int)} ratio compares.
     *
     * @implNote This implementation applies the same per product arithmetic right shift as
     * {@link NetEqSignalProcessing#crossCorrelation(int[], short[], short[], int, int, int, int)}, with the
     * two vectors identical.
     *
     * @param a      the first vector
     * @param b      the second vector
     * @param length the number of samples
     * @param shift  the per product right shift
     * @return the scaled energy
     */
    private static int dotProductWithScale(short[] a, short[] b, int length, int shift) {
        var sum = 0;
        for (var i = 0; i < length; i++) {
            sum += (a[i] * b[i]) >> shift;
        }
        return sum;
    }

}
