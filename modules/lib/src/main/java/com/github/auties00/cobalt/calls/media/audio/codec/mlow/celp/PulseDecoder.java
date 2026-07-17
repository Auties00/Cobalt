package com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeDecoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.PulseTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Decodes the algebraic fixed codebook (FCB) excitation pulses for one frame of the MLow speech codec.
 *
 * <p>MLow codes each frame's excitation as a sparse set of unit pulses placed on an integer sample grid.
 * For a frame the decoder reconstructs, in this fixed order off the range coder stream:
 * <ul>
 * <li>the total pulse count for the frame, either against a static low rate CMF selected by voicing class
 * or, in the regular mode, against a closed form cumulative model decoded with the raw range coder
 * primitives;</li>
 * <li>the split of that total across subframes: for a four subframe frame a two level split (whole frame
 * into halves, then each half into its two subframes) and for a two subframe frame a single split, both
 * driven by the binomial split CMF family, with a one subframe frame taking the whole count;</li>
 * <li>the position of each pulse within its subframe, coded as run lengths against a run length CMF whose
 * shape depends on how many samples and pulses remain, with repeated positions accumulating into a
 * stacked pulse magnitude;</li>
 * <li>the sign of each pulse, packed into uniform symbols of up to {@link #MAX_SIGNS_PER_SYMBOL} bits.</li>
 * </ul>
 *
 * <p>The decoder is stateless across frames: every call to {@link #decode} operates purely on the supplied
 * range decoder state, the prebuilt {@link PulseTables.Tables}, and the per frame mode parameters, and
 * returns a fresh {@link Result}. The MLow decode pipeline shares one {@link PulseTables.Tables} instance
 * across the whole session.
 *
 * <p>This decoder targets only the 16 kHz, 60 ms, mono SMPL mode low band path. At 16 kHz the high band is
 * absent, so this is the sole excitation pulse path; the high band pulse decode is a separate, out of scope
 * concern. The decode logic is nevertheless written generically over frame length, subframe count, and
 * mode flags, because those are supplied per frame by the parameter decoder.
 *
 * @implNote This implementation keeps the small per mode maximum pulse table {@link #MAX_PULSES_PER_FRAME}
 * transcribed inline on this decode path rather than reached through a shared table holder. The run length
 * and split CMF lookups slice a contiguous window out of a prebuilt CMF rather than copying it: the offset
 * aware {@link MlowEntropyWrapper#decodeUpdate(MlowRangeDecoder, int[], int, int)} measures every span
 * relative to the first windowed entry.
 */
public final class PulseDecoder {
    /**
     * The logger for {@link PulseDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(PulseDecoder.class);

    /**
     * Maximum number of pulse signs packed into a single uniform range coder symbol.
     *
     * <p>Signs are emitted as uniform symbols over {@code 1 << signsInSymbol} values; each full symbol
     * carries this many one bit signs, with a final short symbol holding any remainder.
     */
    static final int MAX_SIGNS_PER_SYMBOL = 15;

    /**
     * Reference frame length, in samples, that the per mode maximum pulse count is scaled against.
     *
     * <p>The maximum pulse count multiplies the per mode table value by {@code framelen / 320}, so a 20 ms
     * frame ({@code framelen == 320}) uses the table value unscaled.
     */
    private static final int MAX_PULSES_REF_FRAMELEN = 320;

    /**
     * Per mode maximum pulse count, indexed {@code [lowRate][codedAsActiveVoice + voiced]}.
     *
     * <p>The first index selects the regular or low rate mode. The second index is the sum of the coded as
     * active voice flag and the voiced flag, selecting the background noise, unvoiced, or voiced column. The
     * value scales by {@code framelen / 320} to yield the frame's pulse count ceiling.
     */
    private static final int[][] MAX_PULSES_PER_FRAME = {
            {80, 160, 160},
            {16, 32, 32}
    };

    /**
     * Prevents instantiation of this stateless decoder utility.
     */
    private PulseDecoder() {
        throw new AssertionError("no instances");
    }

    /**
     * Decoded excitation pulse layout for one frame.
     *
     * <p>{@code positions} and {@code posPulses} hold one entry per distinct decoded pulse position, in
     * decode order, for the first {@code nPositions} entries; a position may carry a stacked magnitude
     * greater than one in absolute value, and the sign of {@code posPulses[i]} is the decoded pulse sign.
     * {@code sfPulses} holds the unsigned pulse count assigned to each subframe and {@code nPulses} is
     * their sum (the frame total). On a payload corruption guard both {@code nPulses} and every
     * {@code sfPulses} entry are zero and {@code nPositions} is zero.
     *
     * @param nPulses     the total number of pulses in the frame
     * @param nPositions  the number of distinct decoded pulse positions
     * @param positions   the absolute sample position of each pulse, for the first {@code nPositions}
     *                    entries
     * @param posPulses   the signed stacked magnitude at each position, for the first {@code nPositions}
     *                    entries
     * @param sfPulses    the unsigned pulse count per subframe
     */
    public record Result(int nPulses, int nPositions, short[] positions, short[] posPulses, short[] sfPulses) {
    }

    /**
     * Decodes the excitation pulse layout for one frame off the range coder stream.
     *
     * <p>Reads the frame pulse count, splits it across subframes, then decodes each pulse's position and
     * sign, advancing {@code decoder} past every consumed symbol. The per frame mode flags select the pulse
     * count model, the maximum pulse count, and the split structure.
     *
     * @param decoder              the range decoder to read from and advance
     * @param tables               the prebuilt pulse coding CMF families
     * @param framelen             the frame length in samples
     * @param nSubfr               the number of subframes in the frame; 1, 2, or 4
     * @param lowRate              {@code true} for the low rate pulse count model, {@code false} for the
     *                             closed form model
     * @param voiced               {@code true} if the frame is coded voiced
     * @param codedAsActiveVoice   {@code true} if the frame is coded as active voice
     * @return the decoded pulse layout for the frame
     */
    public static Result decode(
            MlowRangeDecoder decoder,
            PulseTables.Tables tables,
            int framelen,
            int nSubfr,
            boolean lowRate,
            boolean voiced,
            boolean codedAsActiveVoice) {
        var subfrlen = framelen / nSubfr;
        var voicedFlag = voiced ? 1 : 0;
        var codedFlag = codedAsActiveVoice ? 1 : 0;
        var maxPulses = MAX_PULSES_PER_FRAME[lowRate ? 1 : 0][codedFlag + voicedFlag] * framelen / MAX_PULSES_REF_FRAMELEN;
        var maxSubfrPulses = maxPulses / nSubfr;

        int nPulses;
        if (lowRate) {
            nPulses = MlowEntropyWrapper.decodeUpdate(decoder, tables.nPulseCmfs()[codedFlag + voicedFlag]);
        } else {
            nPulses = decodeNumPulsesClosedForm(decoder, maxPulses);
        }

        var positions = new short[framelen];
        var posPulses = new short[framelen];
        var sfPulses = new short[nSubfr];
        if (nPulses == 0) {
            return new Result(0, 0, positions, posPulses, sfPulses);
        }

        nPulses = splitPulses(decoder, tables, nSubfr, maxSubfrPulses, nPulses, sfPulses);
        if (nPulses == 0) {
            return new Result(0, 0, positions, posPulses, sfPulses);
        }

        var nPositions = decodePulsePosSigns(decoder, tables, subfrlen, nSubfr, positions, posPulses, sfPulses);
        return new Result(nPulses, nPositions, positions, posPulses, sfPulses);
    }

    /**
     * Decodes the frame pulse count from the closed form cumulative model.
     *
     * <p>Computes the total mass {@code numPulsesCmf(maxPulses + 1, maxPulses + 1)}, reads a cumulative
     * value over it with {@link MlowRangeDecoder#decode(long)}, then scans pulse counts {@code 0..maxPulses}
     * for the half open span {@code [cmf(s), cmf(s + 1))} that contains it and advances the decoder past
     * that span with {@link MlowRangeDecoder#update(long, long, long)}.
     *
     * @param decoder   the range decoder to read from and advance
     * @param maxPulses the frame pulse count ceiling
     * @return the decoded frame pulse count, in {@code [0, maxPulses]}
     */
    private static int decodeNumPulsesClosedForm(MlowRangeDecoder decoder, int maxPulses) {
        var cmfMax = numPulsesCmf(maxPulses + 1, maxPulses + 1);
        var cmfLow = decoder.decode(cmfMax);
        long cmf0 = 0;
        for (var s = 0; s <= maxPulses; s++) {
            var cmf1 = numPulsesCmf(maxPulses + 1, s + 1);
            if (Long.compareUnsigned(cmfLow, cmf0) >= 0 && Long.compareUnsigned(cmfLow, cmf1) < 0) {
                decoder.update(cmf0, cmf1, cmfMax);
                return s;
            }
            cmf0 = cmf1;
        }
        return 0;
    }

    /**
     * Evaluates the closed form pulse count cumulative model.
     *
     * <p>Returns {@code 0} for {@code nPulses == 0}; otherwise
     * {@code maxPulses * (nPulses + 1) - (((nPulses - 2) * (nPulses - 3)) >> 1)}, the cumulative frequency
     * up to and including {@code nPulses} on the regular mode pulse count scale. Callers pass
     * {@code maxPulses + 1} as the first argument.
     *
     * @param maxPulses the first model argument, the frame pulse count ceiling plus one
     * @param nPulses   the pulse count to evaluate the cumulative frequency at
     * @return the cumulative frequency as an unsigned value
     */
    private static long numPulsesCmf(int maxPulses, int nPulses) {
        if (nPulses == 0) {
            return 0;
        }
        return ((long) maxPulses * (nPulses + 1) - (((nPulses - 2) * (nPulses - 3)) >> 1)) & 0xFFFFFFFFL;
    }

    /**
     * Splits the frame pulse count across subframes.
     *
     * <p>For a four subframe frame the count is split twice: first into the two halves of the frame against
     * the frame level split CMF, then each nonempty half into its two subframes against the half level split
     * CMF. A two subframe frame uses a single split and a one subframe frame assigns the whole count to its
     * only subframe. When a half level split underflows (the corruption sentinel {@code -1}), the whole
     * frame is treated as having no pulses: {@code sfPulses} is cleared and zero is returned.
     *
     * @param decoder        the range decoder to read from and advance
     * @param tables         the prebuilt pulse coding CMF families
     * @param nSubfr         the number of subframes; 1, 2, or 4
     * @param maxSubfrPulses the per subframe pulse count ceiling
     * @param nPulses        the frame pulse count to split
     * @param sfPulses       the per subframe pulse count output, written in place
     * @return the frame pulse count, unchanged on success or {@code 0} on a corruption guard
     */
    private static int splitPulses(
            MlowRangeDecoder decoder,
            PulseTables.Tables tables,
            int nSubfr,
            int maxSubfrPulses,
            int nPulses,
            short[] sfPulses) {
        var maxPulsesPerSf = tables.maxPulsesPerSf();
        if (nSubfr == 4) {
            var minSplit = Math.max(nPulses - maxPulsesPerSf * 2, 0);
            var maxSplit = nPulses - minSplit;
            var minSplit2 = Math.max(nPulses - maxSubfrPulses * 2, 0);
            var maxSplit2 = nPulses - minSplit;
            if (minSplit2 < minSplit || maxSplit2 > maxSplit) {
                return nPulses;
            }
            int nPulsesFirsthalf;
            if (maxSplit2 > minSplit2) {
                var cmf = tables.splitCmfs()[nPulses - 1];
                var cmfLen = maxSplit2 - minSplit2 + 2;
                var base = minSplit2 - minSplit;
                nPulsesFirsthalf = MlowEntropyWrapper.decodeUpdate(decoder, cmf, base, cmfLen) + minSplit2;
            } else {
                nPulsesFirsthalf = minSplit2;
            }
            if (nPulsesFirsthalf > 0) {
                sfPulses[0] = (short) decodeSplit2Subfrs(decoder, tables, nPulsesFirsthalf, maxSubfrPulses);
                sfPulses[1] = (short) (nPulsesFirsthalf - sfPulses[0]);
            }
            if (nPulsesFirsthalf < nPulses) {
                sfPulses[2] = (short) decodeSplit2Subfrs(decoder, tables, nPulses - nPulsesFirsthalf, maxSubfrPulses);
                sfPulses[3] = (short) (nPulses - nPulsesFirsthalf - sfPulses[2]);
            }
            if (sfPulses[0] == -1 || sfPulses[2] == -1) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "mlow pulse decode: corrupt fcb split, treating frame as pulse-free");
                }
                Arrays.fill(sfPulses, (short) 0);
                return 0;
            }
        } else if (nSubfr == 2) {
            sfPulses[0] = (short) decodeSplit2Subfrs(decoder, tables, nPulses, maxSubfrPulses);
            sfPulses[1] = (short) (nPulses - sfPulses[0]);
            if (sfPulses[0] == -1) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "mlow pulse decode: corrupt fcb split, treating frame as pulse-free");
                }
                Arrays.fill(sfPulses, (short) 0);
                return 0;
            }
        } else {
            sfPulses[0] = (short) nPulses;
        }
        return nPulses;
    }

    /**
     * Splits a pulse count between two subframes.
     *
     * <p>Returns {@code minSplit} when the split range is degenerate (a single feasible value) and
     * {@code -1} when the range is empty (the corruption sentinel). Otherwise decodes the first subframe's
     * count against the split CMF window {@code [minSplit, maxSplit]} of {@code splitCmfs[nPulses - 1]} and
     * offsets the decoded index by {@code minSplit}.
     *
     * @param decoder        the range decoder to read from and advance
     * @param tables         the prebuilt pulse coding CMF families
     * @param nPulses        the count to split between the two subframes
     * @param maxSubfrPulses the per subframe pulse count ceiling
     * @return the first subframe's pulse count, or {@code -1} on the corruption sentinel
     */
    private static int decodeSplit2Subfrs(
            MlowRangeDecoder decoder,
            PulseTables.Tables tables,
            int nPulses,
            int maxSubfrPulses) {
        var minSplit = Math.max(nPulses - maxSubfrPulses, 0);
        var maxSplit = nPulses - minSplit;
        if (maxSplit < minSplit) {
            return -1;
        }
        if (maxSplit == minSplit) {
            return minSplit;
        }
        var cmfLen = maxSplit - minSplit + 2;
        var cmf = tables.splitCmfs()[nPulses - 1];
        return MlowEntropyWrapper.decodeUpdate(decoder, cmf, minSplit, cmfLen) + minSplit;
    }

    /**
     * Decodes every pulse's position and sign.
     *
     * <p>Walks the subframes in order; within a subframe each pulse's gap to the previous pulse (or to the
     * subframe start for the first) is decoded as a run length against the run length CMF selected by the
     * remaining sample and remaining pulse counts. A decoded gap of zero on any pulse other than the first
     * stacks onto the current position's magnitude rather than opening a new position. After all positions
     * are read, signs are decoded as uniform symbols of up to {@link #MAX_SIGNS_PER_SYMBOL} bits and applied
     * to the stacked magnitudes, mapping a sign bit of one to a positive pulse and zero to a negative pulse.
     *
     * @param decoder   the range decoder to read from and advance
     * @param tables    the prebuilt pulse coding CMF families
     * @param subfrlen  the subframe length in samples
     * @param nSubfr    the number of subframes
     * @param positions the absolute pulse positions, written in place
     * @param posPulses the signed stacked magnitudes per position, written in place
     * @param sfPulses  the per subframe pulse counts
     * @return the number of distinct decoded pulse positions
     */
    private static int decodePulsePosSigns(
            MlowRangeDecoder decoder,
            PulseTables.Tables tables,
            int subfrlen,
            int nSubfr,
            short[] positions,
            short[] posPulses,
            short[] sfPulses) {
        var runLengthStep = tables.runLengthStep();
        var nPositions = -1;
        for (var i = 0; i < nSubfr; i++) {
            int pulsesLeft = sfPulses[i];
            var nSamplesLeft = subfrlen;
            var pos = subfrlen * i;
            for (var j = 0; j < sfPulses[i]; j++) {
                pulsesLeft--;
                var cmfInd = (nSamplesLeft + runLengthStep - 1) / runLengthStep - 1;
                var cmfFull = tables.runLenCmfs()[cmfInd][pulsesLeft];
                var maxSamples = tables.runLenMaxSamples(cmfInd);
                var start = maxSamples - nSamplesLeft;
                var ix = MlowEntropyWrapper.decodeUpdate(decoder, cmfFull, start, nSamplesLeft + 1);
                if (j == 0 || ix > 0) {
                    pos += ix;
                    positions[++nPositions] = (short) pos;
                    posPulses[nPositions] = 1;
                    nSamplesLeft -= ix;
                } else {
                    posPulses[nPositions] += 1;
                }
            }
        }
        nPositions++;

        var signsDecoded = 0;
        while (signsDecoded < nPositions) {
            var signsInSym = Math.min(nPositions - signsDecoded, MAX_SIGNS_PER_SYMBOL);
            var sym = MlowEntropyWrapper.decodeUniform(decoder, 1 << signsInSym);
            sym <<= (MAX_SIGNS_PER_SYMBOL + 1) - signsInSym;
            for (var i = 0; i < signsInSym; i++) {
                var sgn = (sym & 0x8000) >> (MAX_SIGNS_PER_SYMBOL - 1);
                sym <<= 1;
                posPulses[signsDecoded++] *= (short) (sgn - 1);
            }
        }
        return nPositions;
    }
}
