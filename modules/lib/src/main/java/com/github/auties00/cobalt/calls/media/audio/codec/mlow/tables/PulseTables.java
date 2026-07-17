package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

/**
 * Algebraic-codebook pulse-coding tables for the MLow speech decoder and the loaders that expand them
 * into the runtime cumulative mass functions (CMFs) the pulse decoder consumes, the port of
 * {@code smpl_pulse_tables.c} and the table-construction half of {@code smpl_pulse_coding.c}.
 *
 * <p>MLow codes each subframe's excitation as a sparse set of unit pulses on an algebraic grid. The
 * decoder ({@code smpl_decode_pulses} and {@code smpl_decode_pulse_pos_signs}) reads three families of
 * entropy-coded quantities, each driven by a CMF this class builds:
 * <ul>
 * <li>Pulse count at low rate: the number of pulses in the frame is drawn from one of three static
 * delta cumulative mass functions (DCMFs) selected by the coded voicing class (background-noise,
 * unvoiced, voiced). Those three DCMFs are the only baked static pulse tables and are transcribed here
 * byte-for-byte from {@code smpl_pulse_tables.c}; {@link #loadNPulseCmfs()} runs each through
 * {@link CmfBuilder#dcmfToCmf(byte[])}.</li>
 * <li>Pulse-position run lengths: the gap from one pulse to the next is coded against a run-length CMF
 * whose shape depends on how many samples and pulses remain. These CMFs are not baked as tables; they
 * are generated at codec init by a fixed-point probability recurrence ({@code create_runlen_table}).
 * {@link #loadRunLenCmfs()} reproduces that recurrence with the exact silk fixed-point math so the
 * built CMFs are bit-identical to the native ones.</li>
 * <li>Subframe pulse splits: for multi-subframe frames the total pulse count is split across subframes
 * using a CMF derived from a Stirling-approximation binomial model ({@code create_split_CMFs} and
 * {@code smpl_prob_split_fast}). {@link #loadSplitCmfs()} reproduces it, again bit-exact.</li>
 * </ul>
 *
 * <p>Pulse signs are coded as raw uniform symbols and need no table, so no sign table appears here. The
 * non-low-rate pulse-count path is computed from a closed-form expression ({@code smpl_num_pulses_cmf})
 * rather than a table and likewise has no entry here. The {@code smpl_max_pulses_per_frame} array that
 * scales the maximum pulse count is a general codec table owned by {@code smpl_tables.c}, not a
 * pulse-coding table, and is intentionally not duplicated here.
 *
 * <p>This class targets only the 16 kHz, 60 ms, mono SMPL-mode decode path. The native run-length and
 * split generators are frequency-independent (they are functions of subframe length and pulse count),
 * so the same builders serve every supported subframe length in that configuration.
 *
 * @implNote This implementation reproduces {@code create_runlen_table}, {@code create_split_CMFs},
 * {@code smpl_pdf_to_CMF}, {@code smpl_stirling}, and {@code smpl_prob_split_fast} from
 * {@code smpl_pulse_coding.c}, together with the silk fixed-point primitives {@code silk_lin2log},
 * {@code silk_log2lin}, and {@code silk_sigm_Q15} those generators call. Every intermediate is carried
 * in the same width as the C reference ({@code long} for the {@code int64_t} Q31 accumulators,
 * {@code int} for the {@code int32_t} fixed-point terms) so the truncating divisions, the
 * {@code 0.99995} probability clamps, and the {@code silk_SMULWB}-style {@code >> 16} folds reproduce
 * the native values exactly. The pulse-count DCMFs share the {@link CmfBuilder} transform with every
 * other MLow DCMF; the run-length and split CMFs instead use the distinct {@code smpl_pdf_to_CMF}
 * scaling (mass times {@code maxval - N} over the sum, plus one), which is ported privately here.
 */
public final class PulseTables {
    /**
     * Maximum number of unit pulses MLow codes in a single subframe, the native
     * {@code SMPL_MAX_PULSES_PER_SF}.
     *
     * <p>This bounds the pulse-count dimension of the run-length CMF family (one CMF per pulse count
     * from one up to this value) and the subframe split model.
     */
    static final int MAX_PULSES_PER_SF = 40;

    /**
     * Maximum subframe length in samples at the supported rate, the native {@code SMPL_MAX_SF_LEN}
     * ({@code 16 * 10}).
     *
     * <p>The run-length CMFs are indexed by a quantized remaining-sample count that steps by
     * {@link #RUNLENGTH_STEP} up to this length.
     */
    static final int MAX_SF_LEN = 160;

    /**
     * Quantization step, in samples, of the run-length CMF sample-count index, the native
     * {@code RUNLENGTH_STEP}.
     *
     * <p>Run-length CMFs are built for sample counts {@code RUNLENGTH_STEP, 2 * RUNLENGTH_STEP, ...} up
     * to {@link #MAX_SF_LEN}; the decoder rounds the remaining-sample count up to the next step to pick
     * a table.
     */
    static final int RUNLENGTH_STEP = 8;

    /**
     * Number of distinct run-length CMF tables, the native {@code NUM_RUNLEN_CMFS}
     * ({@code MAX_SF_LEN / RUNLENGTH_STEP}).
     */
    static final int NUM_RUNLEN_CMFS = MAX_SF_LEN / RUNLENGTH_STEP;

    /**
     * Per-subframe pulse-count DCMF for the background-noise voicing class, the native
     * {@code smpl_n_pulses_dcmf_bgn}.
     *
     * <p>Read as unsigned bytes. {@link #loadNPulseCmfs()} feeds this through
     * {@link CmfBuilder#dcmfToCmf(byte[])} to produce the {@code n + 1}-entry pulse-count CMF.
     */
    static final byte[] N_PULSES_DCMF_BGN = {
            (byte) 255, 56, 43, 35, 35, 37, 43, 50, 59, 61, 65, 63, 57, 48, 37, 27,
            19,
    };

    /**
     * Per-subframe pulse-count DCMF for the unvoiced voicing class, the native
     * {@code smpl_n_pulses_dcmf_uv}.
     *
     * <p>Read as unsigned bytes; expanded to a CMF by {@link #loadNPulseCmfs()}.
     */
    static final byte[] N_PULSES_DCMF_UV = {
            (byte) 255, 68, 84, 55, 66, 44, 46, 47, 48, 49, 50, 52, 53, 52, 53, 54,
            60, 52, 54, 54, 54, 53, 51, 48, 45, 40, 36, 30, 25, 16, 15, 14,
            20,
    };

    /**
     * Per-subframe pulse-count DCMF for the voiced voicing class, the native
     * {@code smpl_n_pulses_dcmf_v}.
     *
     * <p>Read as unsigned bytes; expanded to a CMF by {@link #loadNPulseCmfs()}.
     */
    static final byte[] N_PULSES_DCMF_V = {
            72, 86, 114, (byte) 153, (byte) 255, 68, 81, 93, 104, 112, 118, 122, 125, 126, (byte) 128, (byte) 129,
            (byte) 131, (byte) 131, (byte) 132, (byte) 130, (byte) 128, 123, 118, 111, 103, 92, 81, 70, 58, 40, 35, 31,
            32,
    };

    /**
     * Q31 representation of the constant one, the native {@code ONE_Q31} ({@code 1 << 31} held as a
     * {@code long}).
     */
    private static final long ONE_Q31 = 1L << 31;

    /**
     * Probability clamp ceiling in Q31, the native {@code 2147376274} (approximately {@code 0.99995}).
     *
     * <p>The run-length recurrence clamps each per-step survival probability to this value to keep the
     * model from assigning a symbol zero residual mass.
     */
    private static final long PROB_CLAMP_Q31 = 2147376274L;

    /**
     * Sigmoid bias in Q5 applied to the log-rate term in the run-length scale factor, the native
     * {@code SIGM_BIAS_Q5}.
     */
    private static final int SIGM_BIAS_Q5 = 146;

    /**
     * Upper bound of the run-length scale factor in Q15, the native {@code SCALE_MAX_Q15}.
     */
    private static final int SCALE_MAX_Q15 = 36000;

    /**
     * Lower bound of the run-length scale factor in Q15, the native {@code SCALE_MIN_Q15}.
     */
    private static final int SCALE_MIN_Q15 = 26000;

    /**
     * Stirling approximation constant {@code log2(e)} in Q15, the native {@code LOG2_EXP1_Q15}.
     */
    private static final int LOG2_EXP1_Q15 = 47274;

    /**
     * Stirling approximation constant {@code log2(2 * pi)} in Q14, the native {@code LOG2_2PI_Q14}.
     */
    private static final int LOG2_2PI_Q14 = 43442;

    /**
     * Slope look-up table for the sigmoid approximation, the native {@code sigm_LUT_slope_Q10}.
     */
    private static final int[] SIGM_LUT_SLOPE_Q10 = {237, 153, 73, 30, 12, 7};

    /**
     * Positive-branch look-up table for the sigmoid approximation, the native {@code sigm_LUT_pos_Q15}.
     */
    private static final int[] SIGM_LUT_POS_Q15 = {16384, 23955, 28861, 31213, 32178, 32548};

    /**
     * Negative-branch look-up table for the sigmoid approximation, the native {@code sigm_LUT_neg_Q15}.
     */
    private static final int[] SIGM_LUT_NEG_Q15 = {16384, 8812, 3906, 1554, 589, 219};

    /**
     * Prevents instantiation of this stateless table holder.
     */
    private PulseTables() {
        throw new AssertionError("no instances");
    }

    /**
     * Builds the three low-rate pulse-count CMFs, the {@code n_pulse_cmfs} array in
     * {@code smpl_create_pulse_tables}.
     *
     * <p>Returns a three-element array indexed by voicing class in the native order
     * {@code [background-noise, unvoiced, voiced]}, each a CMF produced from the matching DCMF by
     * {@link CmfBuilder#dcmfToCmf(byte[])}. Entry {@code i} has length {@code dcmf.length + 1}.
     *
     * @return a freshly allocated {@code int[3][]} of pulse-count CMFs, one per voicing class
     */
    static int[][] loadNPulseCmfs() {
        return new int[][] {
                CmfBuilder.dcmfToCmf(N_PULSES_DCMF_BGN),
                CmfBuilder.dcmfToCmf(N_PULSES_DCMF_UV),
                CmfBuilder.dcmfToCmf(N_PULSES_DCMF_V)
        };
    }

    /**
     * Builds the run-length CMF family, the {@code runlen_CMFs} produced by {@code create_runlen_table}.
     *
     * <p>The result is indexed {@code [tableIndex][pulseCount - 1]} and yields the CMF for coding the
     * gap to the next pulse. {@code tableIndex} runs over {@link #NUM_RUNLEN_CMFS} tables whose maximum
     * sample count is {@code (tableIndex + 1) * RUNLENGTH_STEP}; {@code pulseCount} runs from one to
     * {@link #MAX_PULSES_PER_SF}. Each CMF has {@code maxSamples + 1} entries.
     *
     * <p>For each table and pulse count the native code evaluates a per-step survival probability
     * recurrence in Q31 fixed point, shaped by a sigmoid-scaled log-rate term, then converts the
     * resulting probability density to a CMF with {@link #pdfToCmf(int[], int)}. This method reproduces
     * that recurrence verbatim; see the class implementation note for the fixed-point fidelity
     * contract.
     *
     * @return a freshly allocated {@code int[NUM_RUNLEN_CMFS][MAX_PULSES_PER_SF][]} of run-length CMFs
     */
    static int[][][] loadRunLenCmfs() {
        var tables = new int[NUM_RUNLEN_CMFS][MAX_PULSES_PER_SF][];
        for (var t = 0; t < NUM_RUNLEN_CMFS; t++) {
            var maxSamples = (t + 1) * RUNLENGTH_STEP;
            for (var nump = 1; nump <= MAX_PULSES_PER_SF; nump++) {
                var plongerQ31 = ONE_Q31;
                var pdf = new int[maxSamples];
                for (var nums = 1; nums <= maxSamples; nums++) {
                    var tmp = ONE_Q31 - (ONE_Q31 / (maxSamples - nums + 1));
                    var p1Q31 = tmp;
                    for (var i = 0; i < nump - 1; i++) {
                        p1Q31 = (p1Q31 * tmp) >> 31;
                    }
                    p1Q31 = ONE_Q31 - p1Q31;
                    p1Q31 = Math.min(p1Q31, PROB_CLAMP_Q31);
                    int logOutQ7;
                    if (nump > maxSamples) {
                        logOutQ7 = lin2log((nump << 10) / maxSamples) - 10 * 128;
                    } else {
                        logOutQ7 = -(lin2log((maxSamples << 10) / nump) - 10 * 128);
                    }
                    var scaleFacQ15 = SCALE_MAX_Q15
                                      - (((SCALE_MAX_Q15 - SCALE_MIN_Q15) * sigmQ15((logOutQ7 >> 2) + SIGM_BIAS_Q5)) >> 15);
                    p1Q31 = ONE_Q31 - log2lin(((scaleFacQ15 * (lin2log((int) (ONE_Q31 - p1Q31)) - 31 * 128)) >> 15) + 31 * 128);
                    p1Q31 = Math.min(p1Q31, PROB_CLAMP_Q31);
                    pdf[nums - 1] = (int) ((plongerQ31 * p1Q31) >> 31);
                    plongerQ31 = (plongerQ31 * (ONE_Q31 - p1Q31)) >> 31;
                }
                tables[t][nump - 1] = pdfToCmf(pdf, maxSamples);
            }
        }
        return tables;
    }

    /**
     * Builds the subframe pulse-split CMF family, the {@code split_CMFs} produced by
     * {@code create_split_CMFs}.
     *
     * <p>The result is indexed {@code [numPulses - 1]} for {@code numPulses} from one to
     * {@code 4 * MAX_PULSES_PER_SF - 1}. The CMF for {@code numPulses} models how the total splits
     * between two halves, ranging over split values {@code [minSplit, maxSplit]} where
     * {@code minSplit = max(numPulses - 2 * MAX_PULSES_PER_SF, 0)} and
     * {@code maxSplit = numPulses - minSplit}; its length is {@code maxSplit - minSplit + 2}.
     *
     * <p>Each density entry is a Stirling-approximation binomial probability
     * ({@link #probSplitFast(int, int)}); the density is converted to a CMF by
     * {@link #pdfToCmf(int[], int)}. This reproduces {@code create_split_CMFs} verbatim.
     *
     * @return a freshly allocated {@code int[4 * MAX_PULSES_PER_SF - 1][]} of split CMFs
     */
    static int[][] loadSplitCmfs() {
        var count = 4 * MAX_PULSES_PER_SF - 1;
        var cmfs = new int[count][];
        for (var numPulses = 1; numPulses < 4 * MAX_PULSES_PER_SF; numPulses++) {
            var minSplit = Math.max(numPulses - MAX_PULSES_PER_SF * 2, 0);
            var maxSplit = numPulses - minSplit;
            var n = maxSplit - minSplit + 1;
            var pdf = new int[n];
            for (var k = minSplit; k <= maxSplit; k++) {
                pdf[k - minSplit] = probSplitFast(k, numPulses);
            }
            cmfs[numPulses - 1] = pdfToCmf(pdf, n);
        }
        return cmfs;
    }

    /**
     * Immutable bundle of the three pulse-coding CMF families and the geometric constants the pulse
     * decoder needs to index them, the runtime equivalent of the native {@code PulseCodingTables} struct.
     *
     * <p>The decoder selects a run-length CMF as {@code runLenCmfs[tableIndex][pulseCount - 1]} where
     * {@code tableIndex} is derived from the remaining-sample count and {@link #runLenMaxSamples(int)}
     * gives that table's native {@code max_samples}; it selects a split CMF as
     * {@code splitCmfs[numPulses - 1]} and a low-rate pulse-count CMF as {@code nPulseCmfs[voicingClass]}.
     * All three arrays are the verbatim outputs of {@link #loadRunLenCmfs()}, {@link #loadSplitCmfs()},
     * and {@link #loadNPulseCmfs()}; this holder only groups them so a single {@link #build()} call
     * materializes the whole pulse-coding table set once.
     *
     * @param nPulseCmfs    the three low-rate pulse-count CMFs indexed by voicing class
     *                      {@code [background-noise, unvoiced, voiced]}
     * @param runLenCmfs    the run-length CMF family indexed {@code [tableIndex][pulseCount - 1]}
     * @param splitCmfs     the subframe pulse-split CMF family indexed {@code [numPulses - 1]}
     * @param maxPulsesPerSf the native {@code SMPL_MAX_PULSES_PER_SF}, the per-subframe pulse-count bound
     * @param runLengthStep  the native {@code RUNLENGTH_STEP}, the run-length sample-count quantization
     */
    public record Tables(
            int[][] nPulseCmfs,
            int[][][] runLenCmfs,
            int[][] splitCmfs,
            int maxPulsesPerSf,
            int runLengthStep) {
        /**
         * Returns the native {@code max_samples} of the run-length table at the given index.
         *
         * <p>Table {@code tableIndex} covers remaining-sample counts up to
         * {@code (tableIndex + 1) * runLengthStep}; that product is the table's {@code max_samples}, the
         * length of each of its CMFs minus one.
         *
         * @param tableIndex the run-length table index, in {@code [0, runLenCmfs.length)}
         * @return the maximum sample count, in samples, of that table
         */
        public int runLenMaxSamples(int tableIndex) {
            return (tableIndex + 1) * runLengthStep;
        }
    }

    /**
     * Builds the complete pulse-coding table set the pulse decoder consumes, the runtime equivalent of
     * {@code smpl_create_pulse_tables}.
     *
     * <p>Materializes the three low-rate pulse-count CMFs, the run-length CMF family, and the subframe
     * split CMF family in one call and bundles them with the geometric constants the decoder indexes
     * them by. The native code caches a single shared instance; callers here are expected to build once
     * and hold the returned {@link Tables}.
     *
     * @return a freshly built {@link Tables} holding every pulse-coding CMF family
     */
    public static Tables build() {
        return new Tables(loadNPulseCmfs(), loadRunLenCmfs(), loadSplitCmfs(), MAX_PULSES_PER_SF, RUNLENGTH_STEP);
    }

    /**
     * Converts a probability density to a cumulative mass function, {@code smpl_pdf_to_CMF} with the
     * default {@code maxval} of {@code 32767}.
     *
     * <p>This is the pulse-coding sibling of {@link CmfBuilder#dcmfToCmf(byte[])} and uses a different
     * scaling: with grand total {@code sump} over the {@code n} density entries, output slot
     * {@code i + 1} is {@code cmf[i] + (pdf[i] * (32767 - n)) / sump + 1}, in 64-bit intermediate
     * arithmetic with C truncating division. The returned array has {@code n + 1} entries starting at
     * zero and is strictly increasing.
     *
     * @param pdf the probability density, {@code n} non-negative entries with positive sum
     * @param n   the number of density entries
     * @return a freshly allocated CMF of length {@code n + 1}
     */
    private static int[] pdfToCmf(int[] pdf, int n) {
        var maxval = 32767;
        long sump = 0;
        for (var i = 0; i < n; i++) {
            sump += pdf[i];
        }
        var cmf = new int[n + 1];
        cmf[0] = 0;
        for (var i = 0; i < n; i++) {
            var p = (int) (((long) pdf[i] * (long) (maxval - n)) / sump) + 1;
            cmf[i + 1] = cmf[i] + p;
        }
        return cmf;
    }

    /**
     * Evaluates the Stirling log-factorial approximation in Q15, {@code smpl_stirling}.
     *
     * <p>Returns {@code 0} for {@code n == 0}; otherwise computes
     * {@code (2n + 1) * (lin2log(n) << 7) - log2(e) * n + log2(2 * pi) + log2(e) / (12 * n)} in Q15
     * fixed point. This is the building block of {@link #probSplitFast(int, int)}.
     *
     * @param n the argument, non-negative
     * @return the Q15 Stirling approximation of {@code log2(n!)} scaled per the native formula
     */
    private static int stirling(int n) {
        if (n == 0) {
            return 0;
        }
        var retQ15 = ((n << 1) + 1) * (lin2log(n) << 7);
        retQ15 -= LOG2_EXP1_Q15 * n;
        retQ15 += LOG2_2PI_Q14;
        retQ15 += LOG2_EXP1_Q15 / (12 * n);
        return retQ15;
    }

    /**
     * Evaluates the binomial split probability
     * {@code 2^(stirling(N) - stirling(k) - stirling(N - k) - N)}, {@code smpl_prob_split_fast}.
     *
     * <p>Computes the Q15 log-probability from {@link #stirling(int)}, returns {@code 1 << 30} when it
     * is zero (probability one), and otherwise converts it back to a linear scale through
     * {@link #log2lin(int)} and returns {@code (1 << 30) / linear}, matching the native reciprocal
     * form.
     *
     * @param k the split value
     * @param n the total pulse count
     * @return the relative split probability, a positive density value
     */
    private static int probSplitFast(int k, int n) {
        var tmpQ15 = stirling(n) - stirling(k) - stirling(n - k) - n * (1 << 15);
        if (tmpQ15 == 0) {
            return 1 << 30;
        }
        tmpQ15 = -tmpQ15;
        var ret = log2lin(tmpQ15 >> 8);
        return (1 << 30) / ret;
    }

    /**
     * Approximates {@code 128 * log2(inLin)}, the silk primitive {@code silk_lin2log}.
     *
     * <p>Uses the piece-wise parabolic approximation of the silk reference: the leading-zero count and
     * the seven fractional bits of {@code inLin} feed
     * {@code (frac + ((frac * (128 - frac) * 179) >> 16)) + ((31 - lz) << 7)}. Reproduced in {@code int}
     * arithmetic with a 64-bit fold for the {@code silk_SMLAWB} term.
     *
     * @param inLin the input on a linear scale, treated as a 32-bit value
     * @return the approximate {@code 128 * log2(inLin)}
     */
    private static int lin2log(int inLin) {
        var lz = inLin == 0 ? 32 : Integer.numberOfLeadingZeros(inLin);
        var fracQ7 = ror32(inLin, 24 - lz) & 0x7f;
        var smlawb = (int) (fracQ7 + (((long) (fracQ7 * (128 - fracQ7)) * (short) 179) >> 16));
        return smlawb + ((31 - lz) << 7);
    }

    /**
     * Approximates {@code 2^(inLogQ7 / 128)}, the silk primitive {@code silk_log2lin}.
     *
     * <p>Returns {@code 0} for negative input and {@code Integer.MAX_VALUE} for input at or above
     * {@code 3967}. Otherwise it forms {@code 1 << (inLogQ7 >> 7)} and refines it with the silk
     * piece-wise parabolic correction, choosing the {@code >> 7} branch below {@code 2048} and the
     * multiply-accumulate branch at or above it, with the
     * {@code silk_SMLAWB(frac, frac * (128 - frac), -174)} fold computed in 64-bit.
     *
     * @param inLogQ7 the input on a Q7 log scale
     * @return the approximate linear value
     */
    private static int log2lin(int inLogQ7) {
        if (inLogQ7 < 0) {
            return 0;
        } else if (inLogQ7 >= 3967) {
            return Integer.MAX_VALUE;
        }
        var out = 1 << (inLogQ7 >> 7);
        var fracQ7 = inLogQ7 & 0x7F;
        var smulbb = (short) fracQ7 * (short) (128 - fracQ7);
        var smlawb = (int) (fracQ7 + (((long) smulbb * (short) -174) >> 16));
        if (inLogQ7 < 2048) {
            out = out + ((int) ((long) out * smlawb) >> 7);
        } else {
            out = out + (out >> 7) * smlawb;
        }
        return out;
    }

    /**
     * Approximates the logistic sigmoid in Q15, the silk primitive {@code silk_sigm_Q15}.
     *
     * <p>Clips to {@code 0} or {@code 32767} outside {@code [-6 * 32, 6 * 32)} in Q5 and otherwise
     * linearly interpolates the positive or negative branch look-up table by the low five bits of the
     * input, mirroring the silk reference exactly.
     *
     * @param inQ5 the input in Q5
     * @return the sigmoid value in Q15, in {@code [0, 32767]}
     */
    private static int sigmQ15(int inQ5) {
        int ind;
        if (inQ5 < 0) {
            inQ5 = -inQ5;
            if (inQ5 >= 6 * 32) {
                return 0;
            }
            ind = inQ5 >> 5;
            return SIGM_LUT_NEG_Q15[ind] - (short) SIGM_LUT_SLOPE_Q10[ind] * (short) (inQ5 & 0x1F);
        } else {
            if (inQ5 >= 6 * 32) {
                return 32767;
            }
            ind = inQ5 >> 5;
            return SIGM_LUT_POS_Q15[ind] + (short) SIGM_LUT_SLOPE_Q10[ind] * (short) (inQ5 & 0x1F);
        }
    }

    /**
     * Rotates a 32-bit value right by {@code rot} bits, the silk primitive {@code silk_ROR32}.
     *
     * <p>Returns the input unchanged for {@code rot == 0}; for negative {@code rot} it rotates left by
     * {@code -rot}, matching the unsigned-shift definition of the reference. Used by
     * {@link #lin2log(int)} to extract the fractional bits after the leading one.
     *
     * @param a32 the value to rotate
     * @param rot the rotation amount; positive rotates right, negative rotates left
     * @return the rotated value
     */
    private static int ror32(int a32, int rot) {
        if (rot == 0) {
            return a32;
        } else if (rot < 0) {
            var m = -rot;
            return (a32 << m) | (a32 >>> (32 - m));
        } else {
            return (a32 << (32 - rot)) | (a32 >>> rot);
        }
    }
}
