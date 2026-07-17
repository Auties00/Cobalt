package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Performs the fixed codebook (FCB) algebraic pulse search for one analysis by synthesis (AbS) subframe of the
 * MLow speech codec.
 *
 * <p>The fixed codebook is sparse: each subframe's excitation is a handful of unit pulses with positions and
 * signs. The search places pulses one at a time to greedily maximize the ratio of the squared perceptually
 * weighted correlation with the target to the weighted self energy, the criterion {@code Q = num^2 / den}. Two
 * strategies share this class:
 * <ul>
 * <li><b>Delayed decision tournament</b> ({@link #searchDeldec}). Keeps a beam of survivor pulse trains; at
 * each pulse stage it extends every survivor by every candidate position, deduplicates by a rolling 64 bit
 * signature so two trains that reach the same pulse set are not both kept, and re selects the top survivors.
 * This is the path the high rate active voice encoder always takes, where the survivor budget exceeds one per
 * pulse.</li>
 * <li><b>Single survivor greedy</b> ({@link #search}). Adds one pulse per stage at the {@code Q} argmax with
 * no beam, the fallback the encoder uses only when the survivor budget collapses to one.</li>
 * </ul>
 * Both jointly track two rate points, a primary point and a forward error correction point, stopping each at
 * the pulse count where adding another pulse no longer beats the per pulse weighted energy threshold.
 *
 * <p>{@link Result} carries, per rate point, the signed pulse list (encoded as {@code +-(position + 1)}), the
 * pulse count, the weighted energy at the optimum, the open loop gain estimate, and the weighted self energy;
 * {@link GainQuantizer} consumes the last two. The target {@code d} is the fixed codebook target: the closed
 * loop target from {@link AcbSearch} for a voiced subframe, or the raw target for an unvoiced subframe.
 *
 * <p>The pulse signatures are the per encoder random 64 bit tags the delayed decision deduplication keys on,
 * seeded once from a pseudorandom stream; the AbS loop owns them and threads the same array in for every
 * subframe so the deduplication is reproducible. The flipped weighting column and its zeroth lag are the
 * perceptually weighted impulse response autocorrelation the AbS loop builds.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono low band path. The pitch sharpening branch of the delayed decision
 * search runs for the low rate path but is inactive on the high rate path ({@code pitchSharp == 0}), where the
 * simpler denominator update is taken. This type is stateless across subframes apart from two reused scratch
 * survivor pools allocated per instance; construct one per encode stream, not shared across threads, and call
 * the search once per subframe.
 *
 * @implNote This implementation reproduces the survivor tournament float arithmetic exactly. {@code Q} is
 * formed as {@code num * num / den} (not {@code num / den * num}); {@link #getMaxi} and {@link #getMaxiK} are
 * transcribed with their pairwise tree reduction, the strict greater than comparison, the leaf flag toggle,
 * and the {@code -FLT_MAX} removal followed by re propagation, so a candidate index never flips relative to
 * the vectorized reduction they mirror. The denominator update reads the flipped column over the non zero
 * band. The delayed decision survivor pools are sized to {@value #MAX_NUMSURV} and its square. All
 * accumulations are single precision.
 */
public final class FcbSearch {
    /**
     * The logger for {@link FcbSearch}.
     */
    private static final System.Logger LOGGER = Log.get(FcbSearch.class);

    /**
     * The forward error correction rate point index.
     */
    private static final int IDX_FEC = 0;

    /**
     * The primary rate point index.
     */
    private static final int IDX_MAIN = 1;

    /**
     * The number of jointly tracked rate points.
     */
    private static final int MAX_RATES = 2;

    /**
     * The maximum low band subframe length in samples; the flipped weighting column is centered at this index.
     */
    private static final int MAX_SF_LEN = 160;

    /**
     * The maximum number of pulses searched per subframe.
     */
    private static final int MAX_PULSES_PER_SF = 40;

    /**
     * The survivor pool width, the maximum number of survivors kept per pulse stage.
     */
    private static final int MAX_NUMSURV = 8;

    /**
     * The largest tournament input length handled by {@link #getMaxiK}.
     */
    private static final int MAX_SORT_LEN = 187;

    /**
     * The largest finite single precision value, used as the removal sentinel in the sorted tournament.
     */
    private static final float FLT_MAX = Float.MAX_VALUE;

    /**
     * One delayed decision survivor pulse train.
     *
     * <p>Holds the running weighted energy, the pulse count placed so far, the last placed position and sign,
     * the rolling deduplication signature, and the index of its backing {@link FcbState} in the read survivor
     * buffer.
     */
    private static final class Fcb {
        /**
         * The running weighted energy of this survivor.
         */
        float wnrg;

        /**
         * The number of pulses placed so far.
         */
        int nPulses;

        /**
         * The pending pulse position to commit next.
         */
        int posNew;

        /**
         * The sign of the pending pulse, either {@code +1} or {@code -1}.
         */
        float signNew;

        /**
         * The rolling deduplication signature accumulated over the placed pulses.
         */
        long sgntr;

        /**
         * The index of this survivor's backing {@link FcbState} in the read buffer.
         */
        int fcbStateIdx;

        /**
         * Copies every field from another survivor into this one.
         *
         * @param o the source survivor
         */
        void copyFrom(Fcb o) {
            wnrg = o.wnrg;
            nPulses = o.nPulses;
            posNew = o.posNew;
            signNew = o.signNew;
            sgntr = o.sgntr;
            fcbStateIdx = o.fcbStateIdx;
        }
    }

    /**
     * The per survivor running numerator, denominator, and pulse history.
     */
    private static final class FcbState {
        /**
         * The placed pulse positions, one entry per pulse.
         */
        final int[] pulsePositions = new int[MAX_SF_LEN];

        /**
         * The signs of the placed pulses, either {@code +1} or {@code -1}.
         */
        final float[] pulseSigns = new float[MAX_SF_LEN];

        /**
         * The running per position numerator of the search criterion.
         */
        final float[] num = new float[MAX_SF_LEN];

        /**
         * The running per position denominator of the search criterion.
         */
        final float[] den = new float[MAX_SF_LEN];

        /**
         * Copies the pulse history and the running numerator and denominator from another state into this one.
         *
         * @param o the source state
         */
        void copyFrom(FcbState o) {
            System.arraycopy(o.pulsePositions, 0, pulsePositions, 0, MAX_SF_LEN);
            System.arraycopy(o.pulseSigns, 0, pulseSigns, 0, MAX_SF_LEN);
            System.arraycopy(o.num, 0, num, 0, MAX_SF_LEN);
            System.arraycopy(o.den, 0, den, 0, MAX_SF_LEN);
        }
    }

    /**
     * The fixed codebook search result for one subframe, indexed by rate point.
     *
     * @param pulses         the signed pulse list per rate point, each entry {@code +-(position + 1)}; only
     *                       the first {@link #nPulses()} entries of each row are valid
     * @param nPulses        the pulse count per rate point
     * @param wnrg           the weighted energy at the search optimum per rate point
     * @param gainFromSearch the open loop fixed codebook gain estimate per rate point
     * @param fcbWnrg        the weighted self energy at the optimum per rate point
     */
    public record Result(short[][] pulses, int[] nPulses, float[] wnrg, float[] gainFromSearch, float[] fcbWnrg) {
    }

    /**
     * The reused read side survivor state buffer, one of the two double buffered pools swapped after each
     * pulse stage.
     */
    private final FcbState[] statesA = newStates();

    /**
     * The reused write side survivor state buffer, one of the two double buffered pools swapped after each
     * pulse stage.
     */
    private final FcbState[] statesB = newStates();

    /**
     * The reused survivor pool holding the current stage's kept survivors.
     */
    private final Fcb[] fcbs = newFcbs(MAX_NUMSURV);

    /**
     * The reused candidate pool holding every extended survivor before the stage's top survivors are selected.
     */
    private final Fcb[] fcbCandidates = newFcbs(MAX_NUMSURV * MAX_NUMSURV);

    /**
     * The reused candidate signature deduplication set for the current pulse stage.
     */
    private final long[] uniqueSgntr = new long[MAX_NUMSURV * MAX_NUMSURV];

    /**
     * Reused per subframe scratch for the greedy search pulse positions, sized {@value #MAX_PULSES_PER_SF}.
     *
     * <p>Written entry by entry as pulses are placed and read only at indices already written this subframe,
     * and not retained past the subframe, so this single owner thread buffer is reused instead of reallocated.
     */
    private final int[] positionsScratch = new int[MAX_PULSES_PER_SF];

    /**
     * Allocates a survivor state buffer of {@link #MAX_NUMSURV} entries.
     *
     * @return a freshly allocated survivor state buffer
     */
    private static FcbState[] newStates() {
        var s = new FcbState[MAX_NUMSURV];
        for (var i = 0; i < s.length; i++) {
            s[i] = new FcbState();
        }
        return s;
    }

    /**
     * Allocates a survivor pool of the given size.
     *
     * @param size the pool size
     * @return a freshly allocated survivor pool
     */
    private static Fcb[] newFcbs(int size) {
        var f = new Fcb[size];
        for (var i = 0; i < f.length; i++) {
            f[i] = new Fcb();
        }
        return f;
    }

    /**
     * Runs the single survivor greedy fixed codebook search.
     *
     * <p>Places one pulse per stage at the running {@code Q = num^2 / den} argmax, jointly tracking the primary
     * and forward error correction rate points, each stopping where adding a pulse no longer beats its per
     * pulse threshold. Used only when the survivor budget is one per pulse.
     *
     * @param d            the fixed codebook target, {@code fcbSubfrlen} entries
     * @param wnrgPerPulse the per pulse weighted energy threshold per rate point
     * @param fcbPulsesMax the maximum pulse count per rate point
     * @param phi          the perceptually weighted impulse response autocorrelation; {@code phi[0]} is the
     *                     zeroth lag
     * @param phiFlip      the flipped autocorrelation column, centered at index {@value #MAX_SF_LEN}
     * @param lResp        the perceptual response length
     * @param fcbSubfrlen  the subframe length in samples
     * @return the per rate point pulse lists, counts, energies, and gain estimates
     */
    public Result search(float[] d, float[] wnrgPerPulse, int[] fcbPulsesMax, float[] phi, float[] phiFlip,
                         int lResp, int fcbSubfrlen) {
        var pulses = new short[MAX_RATES][MAX_PULSES_PER_SF];
        var nPulses = new int[MAX_RATES];
        var wnrg = new float[MAX_RATES];
        var gainFromSearch = new float[MAX_RATES];
        var fcbWnrg = new float[MAX_RATES];

        var positions = positionsScratch;
        var dAbs = new float[fcbSubfrlen];
        var dSign = new float[fcbSubfrlen];
        var num = new float[fcbSubfrlen];
        var den = new float[fcbSubfrlen];
        calcDAbsAndSign(d, fcbSubfrlen, dAbs, dSign);
        for (var i = 0; i < fcbSubfrlen; i++) {
            den[i] = phi[0] + 1e-16f;
        }
        System.arraycopy(dAbs, 0, num, 0, fcbSubfrlen);
        positions[0] = getMaxi(num, fcbSubfrlen);
        var nrgThr = new float[MAX_RATES];
        var ratio = num[positions[0]] / den[positions[0]];
        var wnrg0 = num[positions[0]] * ratio;
        if (checkIfBetter(wnrg0, nrgThr, IDX_MAIN, wnrgPerPulse[IDX_MAIN])) {
            nPulses[IDX_MAIN] = 1;
            wnrg[IDX_MAIN] = wnrg[IDX_FEC] = wnrg0;
            gainFromSearch[IDX_MAIN] = gainFromSearch[IDX_FEC] = ratio;
            fcbWnrg[IDX_MAIN] = fcbWnrg[IDX_FEC] = den[positions[0]];
            if (fcbPulsesMax[IDX_FEC] > 0) {
                nPulses[IDX_FEC] = nPulses[IDX_MAIN];
                wnrg[IDX_FEC] = wnrg[IDX_MAIN];
                gainFromSearch[IDX_FEC] = gainFromSearch[IDX_MAIN];
                fcbWnrg[IDX_FEC] = fcbWnrg[IDX_MAIN];
            }
        }

        var q = new float[fcbSubfrlen];
        for (var pulseNr = 1; pulseNr < fcbPulsesMax[IDX_MAIN]; pulseNr++) {
            var position = positions[pulseNr - 1];
            var sgn = dSign[position];
            for (var i = 0; i < fcbSubfrlen; i++) {
                num[i] += dAbs[position];
            }
            var nzr = new int[2];
            var phiCol = getPhiCol(position, lResp, fcbSubfrlen, nzr);
            var dDen = 0.0f;
            for (var i = 0; i < pulseNr - 1; i++) {
                dDen += phiFlip[phiCol + positions[i]] * dSign[positions[i]];
            }
            dDen *= 2.0f * sgn;
            dDen += phiFlip[phiCol + position];
            for (var i = 0; i < fcbSubfrlen; i++) {
                den[i] += dDen;
            }
            var twoSign = 2.0f * sgn;
            for (var i = nzr[0]; i < nzr[1]; i++) {
                den[i] += bandTerm(dSign[i], phiFlip[phiCol + i], twoSign);
            }
            celpQ(num, den, fcbSubfrlen, q);
            positions[pulseNr] = getMaxi(q, fcbSubfrlen);
            if (checkIfBetter(q[positions[pulseNr]], nrgThr, IDX_MAIN, wnrgPerPulse[IDX_MAIN])) {
                nPulses[IDX_MAIN] = pulseNr + 1;
                wnrg[IDX_MAIN] = q[positions[pulseNr]];
                gainFromSearch[IDX_MAIN] = num[positions[pulseNr]] / den[positions[pulseNr]];
                fcbWnrg[IDX_MAIN] = den[positions[pulseNr]];
            }
            if (fcbPulsesMax[IDX_FEC] >= pulseNr
                    && checkIfBetter(q[positions[pulseNr]], nrgThr, IDX_FEC, wnrgPerPulse[IDX_FEC])) {
                nPulses[IDX_FEC] = pulseNr + 1;
                wnrg[IDX_FEC] = q[positions[pulseNr]];
                gainFromSearch[IDX_FEC] = num[positions[pulseNr]] / den[positions[pulseNr]];
                fcbWnrg[IDX_FEC] = den[positions[pulseNr]];
            }
        }
        for (var r = IDX_FEC; r <= IDX_MAIN; r++) {
            if (nrgThr[r] > 0.0f) {
                for (var i = 0; i < nPulses[r]; i++) {
                    var position = positions[i];
                    pulses[r][i] = (short) (dSign[position] > 0 ? (1 + position) : -(1 + position));
                }
            } else {
                wnrg[r] = 0.0f;
                gainFromSearch[r] = 0.0f;
                fcbWnrg[r] = 0.0f;
                nPulses[r] = 0;
            }
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "fcb greedy search: subfrlen={0} pulsesMain={1} pulsesFec={2}",
                    fcbSubfrlen, nPulses[IDX_MAIN], nPulses[IDX_FEC]);
        }
        return new Result(pulses, nPulses, wnrg, gainFromSearch, fcbWnrg);
    }

    /**
     * Runs the delayed decision survivor tournament fixed codebook search.
     *
     * <p>Maintains a beam of survivor pulse trains; each pulse stage extends every survivor by every candidate
     * position, deduplicates extensions by their rolling signature, and keeps the top survivors. The primary
     * and forward error correction rate points are tracked jointly, each committing the best survivor at the
     * pulse count where adding another pulse no longer beats its per pulse threshold. This is the high rate
     * active voice path.
     *
     * @param d            the fixed codebook target, {@code fcbSubfrlen} entries
     * @param pitchSharp   the pitch sharpening coefficient, {@code 0} on the high rate path
     * @param lag          the integer pitch lag, used only when {@code pitchSharp} is non zero
     * @param wnrgPerPulse the per pulse weighted energy threshold per rate point
     * @param fcbPulsesMax the maximum pulse count per rate point
     * @param surv         the survivor budget per pulse stage; at least {@code fcbPulsesMax[IDX_MAIN]} entries
     * @param phi          the perceptually weighted impulse response autocorrelation
     * @param phiFlip      the flipped autocorrelation column, centered at index {@value #MAX_SF_LEN}
     * @param lResp        the perceptual response length
     * @param fcbSubfrlen  the subframe length in samples
     * @param sgntrs       the per encoder random pulse signatures; at least {@code fcbSubfrlen} entries
     * @return the per rate point pulse lists, counts, energies, and gain estimates
     */
    public Result searchDeldec(float[] d, float pitchSharp, int lag, float[] wnrgPerPulse, int[] fcbPulsesMax,
                               short[] surv, float[] phi, float[] phiFlip, int lResp, int fcbSubfrlen,
                               long[] sgntrs) {
        var pulses = new short[MAX_RATES][MAX_PULSES_PER_SF];
        var nPulses = new int[MAX_RATES];
        var wnrg = new float[MAX_RATES];
        var gainFromSearch = new float[MAX_RATES];
        var fcbWnrg = new float[MAX_RATES];

        var dAbs = new float[fcbSubfrlen];
        var dSign = new float[fcbSubfrlen];
        if (pitchSharp != 0.0f && lag > 0 && lag < fcbSubfrlen) {
            var dNew = new float[fcbSubfrlen];
            System.arraycopy(d, 0, dNew, 0, fcbSubfrlen);
            for (var j = 0; j < fcbSubfrlen; j++) {
                var g = pitchSharp;
                for (var i = lag + j; i < fcbSubfrlen; i += lag) {
                    dNew[j] += g * d[i];
                    g *= pitchSharp;
                }
            }
            calcDAbsAndSign(dNew, fcbSubfrlen, dAbs, dSign);
        } else {
            calcDAbsAndSign(d, fcbSubfrlen, dAbs, dSign);
            pitchSharp = 0.0f;
        }

        // Double buffered survivor state: read from statesA, write into statesB, swap after each stage.
        var readBuf = statesA;
        var writeBuf = statesB;
        var bestFcb = new Fcb[]{new Fcb(), new Fcb()};
        var bestFcbState = new FcbState[]{new FcbState(), new FcbState()};
        var nrgThr = new float[MAX_RATES];

        var fcbState = writeBuf[0];
        System.arraycopy(dAbs, 0, fcbState.num, 0, fcbSubfrlen);
        if (pitchSharp == 0.0f) {
            for (var i = 0; i < fcbSubfrlen; i++) {
                fcbState.den[i] = phi[0] + 1e-16f;
            }
        } else {
            var nzr = new int[2];
            var off = fcbSubfrlen - 1;
            for (var i = fcbSubfrlen - 1; i >= 0; i -= lag) {
                var res = 1e-16f;
                var g1 = 1.0f;
                for (var j = i; j < fcbSubfrlen; j += lag) {
                    var phiCol = getPhiCol(j, lResp, fcbSubfrlen, nzr);
                    var g2 = 1.0f;
                    for (var k = i; k < fcbSubfrlen; k += lag) {
                        res += g1 * g2 * phiFlip[phiCol + k];
                        g2 *= pitchSharp;
                    }
                    g1 *= pitchSharp;
                }
                var len = Math.min(lag, off + 1);
                for (var j = 0; j < len; j++) {
                    fcbState.den[off - j] = res;
                }
                off -= len;
            }
        }
        // After the swap, readBuf == statesB (holds the seed state), writeBuf == statesA.
        var tmpSwap = readBuf;
        readBuf = writeBuf;
        writeBuf = tmpSwap;

        var q = new float[Math.max(fcbSubfrlen, MAX_NUMSURV * MAX_NUMSURV)];
        if (pitchSharp == 0.0f) {
            System.arraycopy(fcbState.num, 0, q, 0, fcbSubfrlen);
        } else {
            celpQ(fcbState.num, fcbState.den, fcbSubfrlen, q);
        }

        var sortIx = new int[MAX_NUMSURV];
        var fcbsSize = 0;
        getMaxiK(q, sortIx, fcbSubfrlen, surv[0]);
        for (var i = 0; i < surv[0]; i++) {
            var pos = sortIx[i];
            var f = fcbs[fcbsSize++];
            f.sgntr = sgntrs[pos];
            f.posNew = pos;
            f.signNew = dSign[pos];
            f.wnrg = (fcbState.num[pos] * fcbState.num[pos]) / fcbState.den[pos];
            f.nPulses = 0;
            f.fcbStateIdx = 0;
        }

        checkIfBetterDeldec(readBuf, fcbs[0], bestFcb[IDX_MAIN], bestFcbState[IDX_MAIN], nrgThr, IDX_MAIN, wnrgPerPulse[IDX_MAIN]);
        if (fcbPulsesMax[IDX_FEC] > 0) {
            checkIfBetterDeldec(readBuf, fcbs[0], bestFcb[IDX_FEC], bestFcbState[IDX_FEC], nrgThr, IDX_FEC, wnrgPerPulse[IDX_FEC]);
        }

        if (fcbPulsesMax[IDX_MAIN] > 1) {
            for (var pulseNr = 2; pulseNr < fcbPulsesMax[IDX_MAIN]; pulseNr++) {
                var candidatesSize = 0;
                var uniqueSize = 0;
                var idx = 0;
                for (var i = 0; i < fcbsSize; i++) {
                    var sizes = addPulse(fcbs[i], readBuf, writeBuf, dAbs, dSign, surv[pulseNr - 1], idx,
                            lag, pitchSharp, phiFlip, sgntrs, lResp, fcbSubfrlen, candidatesSize, uniqueSize);
                    candidatesSize = sizes[0];
                    uniqueSize = sizes[1];
                    idx++;
                }
                var s2 = readBuf;
                readBuf = writeBuf;
                writeBuf = s2;
                for (var i = 0; i < candidatesSize; i++) {
                    q[i] = fcbCandidates[i].wnrg;
                }
                getMaxiK(q, sortIx, candidatesSize, surv[pulseNr - 1]);
                fcbsSize = 0;
                for (var i = 0; i < surv[pulseNr - 1]; i++) {
                    fcbs[fcbsSize++].copyFrom(fcbCandidates[sortIx[i]]);
                }
                checkIfBetterDeldec(readBuf, fcbs[0], bestFcb[IDX_MAIN], bestFcbState[IDX_MAIN], nrgThr, IDX_MAIN, wnrgPerPulse[IDX_MAIN]);
                if (fcbPulsesMax[IDX_FEC] >= pulseNr) {
                    checkIfBetterDeldec(readBuf, fcbs[0], bestFcb[IDX_FEC], bestFcbState[IDX_FEC], nrgThr, IDX_FEC, wnrgPerPulse[IDX_FEC]);
                }
            }
            // last pulse
            var candidatesSize = 0;
            var uniqueSize = 0;
            for (var i = 0; i < fcbsSize; i++) {
                var sizes = addPulse(fcbs[i], readBuf, writeBuf, dAbs, dSign, 1, i, lag, pitchSharp,
                        phiFlip, sgntrs, lResp, fcbSubfrlen, candidatesSize, uniqueSize);
                candidatesSize = sizes[0];
                uniqueSize = sizes[1];
            }
            var s2 = readBuf;
            readBuf = writeBuf;
            writeBuf = s2;
            var bestIdx = 0;
            var maxWnrg = fcbCandidates[0].wnrg;
            for (var i = 1; i < candidatesSize; i++) {
                if (fcbCandidates[i].wnrg > maxWnrg) {
                    maxWnrg = fcbCandidates[i].wnrg;
                    bestIdx = i;
                }
            }
            checkIfBetterDeldec(readBuf, fcbCandidates[bestIdx], bestFcb[IDX_MAIN], bestFcbState[IDX_MAIN], nrgThr, IDX_MAIN, wnrgPerPulse[IDX_MAIN]);
        }

        for (var r = IDX_FEC; r <= IDX_MAIN; r++) {
            for (var i = 0; i < bestFcb[r].nPulses; i++) {
                pulses[r][i] = (short) (bestFcbState[r].pulseSigns[i] > 0
                        ? (1 + bestFcbState[r].pulsePositions[i])
                        : -(1 + bestFcbState[r].pulsePositions[i]));
            }
            pulses[r][bestFcb[r].nPulses] = (short) (bestFcb[r].signNew > 0
                    ? 1 + bestFcb[r].posNew
                    : -(1 + bestFcb[r].posNew));
            if (bestFcb[r].wnrg > 0.0f) {
                wnrg[r] = bestFcb[r].wnrg;
                gainFromSearch[r] = bestFcbState[r].num[bestFcb[r].posNew] / bestFcbState[r].den[bestFcb[r].posNew];
                fcbWnrg[r] = bestFcbState[r].den[bestFcb[r].posNew];
                nPulses[r] = bestFcb[r].nPulses + 1;
            } else {
                wnrg[r] = 0.0f;
                gainFromSearch[r] = 0.0f;
                fcbWnrg[r] = 0.0f;
                nPulses[r] = 0;
            }
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "fcb deldec search: subfrlen={0} pitchSharp={1} pulsesMain={2} pulsesFec={3}",
                    fcbSubfrlen, pitchSharp, nPulses[IDX_MAIN], nPulses[IDX_FEC]);
        }
        return new Result(pulses, nPulses, wnrg, gainFromSearch, fcbWnrg);
    }

    /**
     * Extends one survivor by one pulse over all candidate positions.
     *
     * <p>Updates the running numerator and denominator for a new pulse at the survivor's pending position,
     * computes the per position {@code Q}, and for each of the top {@code numsurv} positions whose rolling
     * signature is not already present appends a deduplicated candidate. The pitch sharpening branch
     * ({@code pitchSharp != 0}) recomputes the denominator with the comb delayed pulse train; the high rate
     * path takes the simple branch.
     *
     * @param fcb            the survivor to extend, mutated to its new pending position and pulse count
     * @param readBuf        the read side survivor state buffer
     * @param writeBuf       the write side survivor state buffer
     * @param dAbs           the target absolute values
     * @param dSign          the target signs
     * @param numsurv        the number of top positions to spawn candidates from
     * @param idx            the write buffer slot for this survivor's new state
     * @param lag            the integer pitch lag, used only on the pitch sharpening branch
     * @param pitchSharp     the pitch sharpening coefficient, {@code 0} for the simple branch
     * @param phiFlip        the flipped autocorrelation column
     * @param sgntrs         the per encoder random pulse signatures
     * @param lResp          the perceptual response length
     * @param fcbSubfrlen    the subframe length in samples
     * @param candidatesSize the current candidate count
     * @param uniqueSize     the current deduplication set size
     * @return a two element array holding the updated candidate count and deduplication set size
     */
    private int[] addPulse(Fcb fcb, FcbState[] readBuf, FcbState[] writeBuf, float[] dAbs, float[] dSign,
                           int numsurv, int idx, int lag, float pitchSharp, float[] phiFlip, long[] sgntrs,
                           int lResp, int fcbSubfrlen, int candidatesSize, int uniqueSize) {
        var stateW = writeBuf[idx];
        var stateR = readBuf[fcb.fcbStateIdx];

        for (var i = 0; i < fcbSubfrlen; i++) {
            stateW.num[i] = stateR.num[i] + dAbs[fcb.posNew];
        }
        System.arraycopy(stateR.den, 0, stateW.den, 0, fcbSubfrlen);
        if (pitchSharp == 0.0f) {
            var nzr = new int[2];
            var phiCol = getPhiCol(fcb.posNew, lResp, fcbSubfrlen, nzr);
            var dDen = 0.0f;
            for (var i = 0; i < fcb.nPulses; i++) {
                dDen += phiFlip[phiCol + stateR.pulsePositions[i]] * stateR.pulseSigns[i];
            }
            dDen *= 2.0f * fcb.signNew;
            dDen += phiFlip[phiCol + fcb.posNew];
            for (var i = 0; i < fcbSubfrlen; i++) {
                stateW.den[i] += dDen;
            }
            var twoSign = 2.0f * fcb.signNew;
            for (var i = nzr[0]; i < nzr[1]; i++) {
                stateW.den[i] += bandTerm(dSign[i], phiFlip[phiCol + i], twoSign);
            }
        } else {
            var nzr = new int[2];
            var g1 = 1.0f;
            var dDen = 0.0f;
            for (var pos = fcb.posNew; pos < fcbSubfrlen; pos += lag) {
                var phiCol = getPhiCol(pos, lResp, fcbSubfrlen, nzr);
                for (var i = 0; i < fcb.nPulses; i++) {
                    var g2 = g1;
                    for (var posPrev = stateR.pulsePositions[i]; posPrev < fcbSubfrlen; posPrev += lag) {
                        dDen += g2 * phiFlip[phiCol + posPrev] * stateR.pulseSigns[i];
                        g2 *= pitchSharp;
                    }
                }
                g1 *= pitchSharp;
            }
            dDen *= 2.0f * fcb.signNew;
            g1 = 1.0f;
            for (var pos1 = fcb.posNew; pos1 < fcbSubfrlen; pos1 += lag) {
                var phiCol = getPhiCol(pos1, lResp, fcbSubfrlen, nzr);
                var g2 = g1;
                for (var pos2 = fcb.posNew; pos2 < fcbSubfrlen; pos2 += lag) {
                    dDen += g2 * phiFlip[phiCol + pos2];
                    g2 *= pitchSharp;
                }
                g1 *= pitchSharp;
            }
            for (var i = 0; i < fcbSubfrlen; i++) {
                stateW.den[i] += dDen;
            }
            var ddDen = new float[fcbSubfrlen];
            g1 = 1.0f;
            for (var pos = fcb.posNew; pos < fcbSubfrlen; pos += lag) {
                var phiCol = getPhiCol(pos, lResp, fcbSubfrlen, nzr);
                var g2 = g1;
                for (var k = 0; k < fcbSubfrlen; k += lag) {
                    var startI = Math.max(0, nzr[0] - k);
                    var endI = Math.min(fcbSubfrlen - k, nzr[1] - k);
                    for (var i = startI; i < endI; i++) {
                        ddDen[i] += g2 * phiFlip[phiCol + i + k];
                    }
                    g2 *= pitchSharp;
                }
                g1 *= pitchSharp;
            }
            for (var i = 0; i < fcbSubfrlen; i++) {
                stateW.den[i] += 2.0f * fcb.signNew * dSign[i] * ddDen[i];
            }
        }
        System.arraycopy(stateR.pulsePositions, 0, stateW.pulsePositions, 0, fcb.nPulses);
        stateW.pulsePositions[fcb.nPulses] = fcb.posNew;
        System.arraycopy(stateR.pulseSigns, 0, stateW.pulseSigns, 0, fcb.nPulses);
        stateW.pulseSigns[fcb.nPulses] = fcb.signNew;

        fcb.nPulses++;
        fcb.fcbStateIdx = idx;

        var qLocal = new float[fcbSubfrlen];
        celpQ(stateW.num, stateW.den, fcbSubfrlen, qLocal);
        var sortIx = new int[MAX_NUMSURV];
        getMaxiK(qLocal, sortIx, fcbSubfrlen, numsurv);

        var fcbSgntr = fcb.sgntr;
        for (var i = 0; i < numsurv; i++) {
            fcb.sgntr = fcbSgntr + sgntrs[sortIx[i]];
            if (isUnique(uniqueSize, fcb.sgntr)) {
                fcb.posNew = sortIx[i];
                fcb.signNew = dSign[sortIx[i]];
                fcb.wnrg = qLocal[sortIx[i]];
                fcbCandidates[candidatesSize++].copyFrom(fcb);
                uniqueSgntr[uniqueSize++] = fcb.sgntr;
            }
        }
        return new int[]{candidatesSize, uniqueSize};
    }

    /**
     * Commits the lead survivor as the best for a rate point when it beats the per pulse threshold.
     *
     * <p>Advances the running threshold by the per pulse weighted energy; when the lead survivor's weighted
     * energy exceeds it, raises the threshold to that energy and snapshots the survivor and its state.
     *
     * @param readBuf      the read side survivor state buffer holding the survivor's state
     * @param fcb          the lead survivor
     * @param bestFcb      the destination best survivor snapshot
     * @param bestFcbState the destination best state snapshot
     * @param nrgThr       the per rate point running thresholds
     * @param r            the rate point
     * @param wnrgPerPulse the per pulse weighted energy increment
     */
    private static void checkIfBetterDeldec(FcbState[] readBuf, Fcb fcb, Fcb bestFcb, FcbState bestFcbState,
                                            float[] nrgThr, int r, float wnrgPerPulse) {
        nrgThr[r] += wnrgPerPulse;
        if (fcb.wnrg > nrgThr[r]) {
            nrgThr[r] = fcb.wnrg;
            bestFcb.copyFrom(fcb);
            bestFcbState.copyFrom(readBuf[fcb.fcbStateIdx]);
        }
    }

    /**
     * Tests whether a weighted energy beats the running per pulse threshold.
     *
     * <p>Advances the running threshold by the per pulse weighted energy; when the candidate exceeds it, raises
     * the threshold to the candidate and reports success.
     *
     * @param wnrg         the candidate weighted energy
     * @param nrgThr       the per rate point running thresholds
     * @param r            the rate point
     * @param wnrgPerPulse the per pulse weighted energy increment
     * @return {@code true} when the candidate beats the advanced threshold
     */
    private static boolean checkIfBetter(float wnrg, float[] nrgThr, int r, float wnrgPerPulse) {
        nrgThr[r] += wnrgPerPulse;
        if (wnrg > nrgThr[r]) {
            nrgThr[r] = wnrg;
            return true;
        }
        return false;
    }

    /**
     * Tests whether a rolling signature is absent from the deduplication set.
     *
     * @param uniqueSize the current deduplication set size
     * @param sgntr      the candidate signature
     * @return {@code true} when the signature is not yet present
     */
    private boolean isUnique(int uniqueSize, long sgntr) {
        for (var i = 0; i < uniqueSize; i++) {
            if (uniqueSgntr[i] == sgntr) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the flipped column base offset and non zero band for a pulse position.
     *
     * <p>The flipped weighting column for position {@code col} starts at the offset {@code MAX_SF_LEN - col};
     * the denominator update only touches the band {@code [max(col - lResp + 1, 0), min(col + lResp,
     * fcbSubfrlen))}.
     *
     * @param col          the pulse position
     * @param lResp        the perceptual response length
     * @param fcbSubfrlen  the subframe length in samples
     * @param nonZeroRange the destination two element band {@code [inclusive, exclusive)}
     * @return the flipped column base offset into the flipped autocorrelation column
     */
    private static int getPhiCol(int col, int lResp, int fcbSubfrlen, int[] nonZeroRange) {
        nonZeroRange[0] = Math.max(col - lResp + 1, 0);
        nonZeroRange[1] = Math.min(col + lResp, fcbSubfrlen);
        return MAX_SF_LEN - col;
    }

    /**
     * Computes one cross energy band term of the denominator update, {@code 2.0f * signNew * dSign[i] *
     * phi[i]}.
     *
     * <p>Multiplies the target sign, the flipped autocorrelation entry, and twice the new pulse sign with the
     * association {@code (dSign * phi) * twoSign}.
     *
     * @param dSign   the target sign at the band index
     * @param phi     the flipped autocorrelation entry at the band index
     * @param twoSign twice the new pulse sign
     * @return the band term to add to the running denominator at this index
     * @implNote This implementation groups the single precision product as {@code (dSign * phi) * twoSign}
     * rather than {@code (twoSign * dSign) * phi}. The two orders can differ by one unit in the last place in
     * the running {@code den}, which the {@code Q = num^2 / den} argmax can resolve differently when two
     * candidate positions are near tied. The factor {@code twoSign} equals {@code sign + sign} bit for bit, so
     * the doubled sign needs no special handling here.
     */
    private static float bandTerm(float dSign, float phi, float twoSign) {
        return (dSign * phi) * twoSign;
    }

    /**
     * Computes the per position search criterion {@code Q = num^2 / den}.
     *
     * @param num the numerator array
     * @param den the denominator array
     * @param l   the length
     * @param q   the destination criterion array
     */
    private static void celpQ(float[] num, float[] den, int l, float[] q) {
        for (var i = 0; i < l; i++) {
            q[i] = (num[i] * num[i]) / den[i];
        }
    }

    /**
     * Splits a target into per sample absolute values and signs.
     *
     * <p>A zero or negative sample takes sign {@code -1}; a strictly positive sample takes sign {@code +1},
     * following the {@code d[i] > 0} test.
     *
     * @param d     the target
     * @param l     the length
     * @param dAbs  the destination absolute values
     * @param dSign the destination signs
     */
    private static void calcDAbsAndSign(float[] d, int l, float[] dAbs, float[] dSign) {
        for (var i = 0; i < l; i++) {
            if (d[i] > 0.0f) {
                dAbs[i] = d[i];
                dSign[i] = 1.0f;
            } else {
                dAbs[i] = -d[i];
                dSign[i] = -1.0f;
            }
        }
    }

    /**
     * Returns the index of the maximum element.
     *
     * <p>Performs a pairwise tree reduction: a first max fold of the two array halves, repeated halving while
     * the length is even, a strict greater than scan of the reduced leaf vector, then a descent back down the
     * tree resolving which child carried each maximum, with a strict less than tie break.
     *
     * @param x    the input array
     * @param xLen the length
     * @return the index of the maximum element
     */
    private static int getMaxi(float[] x, int xLen) {
        var buf = new float[MAX_SF_LEN];
        var numHalves = 0;
        var len = (xLen + 1) >> 1;
        for (var i = 0; i < xLen - len; i++) {
            buf[i] = Math.max(x[i], x[i + len]);
        }
        buf[xLen - len] = x[xLen - len];
        var bufPtr = 0;
        while ((len & 1) == 0) {
            bufPtr += len;
            len >>= 1;
            for (var i = 0; i < len; i++) {
                buf[bufPtr + i] = Math.max(buf[bufPtr - 2 * len + i], buf[bufPtr - len + i]);
            }
            numHalves++;
        }
        var idx = 0;
        var maxtmp = buf[bufPtr];
        for (var nn = 1; nn < len; nn++) {
            var xtmp = buf[bufPtr + nn];
            if (xtmp > maxtmp) {
                maxtmp = xtmp;
                idx = nn;
            }
        }
        for (var nn = 0; nn < numHalves; nn++) {
            bufPtr -= 2 * len;
            if (buf[bufPtr + idx] < buf[bufPtr + idx + len]) {
                idx += len;
            }
            len <<= 1;
        }
        if (idx + len < xLen && x[idx] < x[idx + len]) {
            idx += len;
        }
        return idx;
    }

    /**
     * Returns the sorted indices of the {@code k} highest values.
     *
     * <p>Builds the same pairwise tree reduction as {@link #getMaxi} once, then repeats: scan the leaf vector
     * for its strict argmax, descend the tree recording the path, resolve the final index using the per leaf
     * flag toggle and the {@code >=} tie break, then remove the winner by writing {@code -FLT_MAX} into its leaf
     * and re propagating the maxima back up the recorded path so the next iteration finds the next largest. The
     * leaf flag distinguishes the first from the second visit to a paired leaf.
     *
     * @param x    the input array
     * @param idx  the destination index array, at least {@code k} entries
     * @param xLen the length
     * @param k    the number of indices to return
     */
    private static void getMaxiK(float[] x, int[] idx, int xLen, int k) {
        var buf = new float[MAX_SORT_LEN];
        var flags = new byte[MAX_SORT_LEN / 2];
        var is = new int[7];
        var numHalves = 0;
        var len = (xLen + 1) >> 1;
        for (var i = 0; i < xLen - len; i++) {
            buf[i] = Math.max(x[i], x[i + len]);
        }
        buf[xLen - len] = x[xLen - len];
        var bufPtr = 0;
        while ((len & 1) == 0) {
            bufPtr += len;
            len >>= 1;
            for (var i = 0; i < len; i++) {
                buf[bufPtr + i] = Math.max(buf[bufPtr - 2 * len + i], buf[bufPtr - len + i]);
            }
            numHalves++;
        }
        for (var kk = 0; kk < k; kk++) {
            var i = 0;
            var maxtmp = buf[bufPtr];
            for (var nn = 1; nn < len; nn++) {
                var xtmp = buf[bufPtr + nn];
                if (xtmp > maxtmp) {
                    maxtmp = xtmp;
                    i = nn;
                }
            }
            for (var nn = 0; nn < numHalves; nn++) {
                is[nn] = i;
                bufPtr -= 2 * len;
                if (buf[bufPtr + i] < buf[bufPtr + i + len]) {
                    i += len;
                }
                len <<= 1;
            }
            var xtmp = -FLT_MAX;
            var iFinal = i;
            if (i + len < xLen) {
                if (flags[i]++ == 0) {
                    if (x[i] < x[i + len]) {
                        xtmp = x[i];
                        iFinal += len;
                    } else {
                        xtmp = x[i + len];
                    }
                } else {
                    if (x[i] >= x[i + len]) {
                        iFinal += len;
                    }
                }
            }
            idx[kk] = iFinal;
            if (kk == k - 1) {
                return;
            }
            buf[bufPtr + i] = xtmp;
            for (var nn = numHalves - 1; nn >= 0; nn--) {
                i = is[nn];
                len >>= 1;
                buf[bufPtr + i + 2 * len] = Math.max(buf[bufPtr + i], buf[bufPtr + i + len]);
                bufPtr += 2 * len;
            }
        }
    }
}
