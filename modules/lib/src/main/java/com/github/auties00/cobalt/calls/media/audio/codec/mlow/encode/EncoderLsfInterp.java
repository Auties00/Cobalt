package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.NlsfBridge;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Encodes one line spectral frequency (LSF) vector per frame into per subframe interpolated LSF vectors and
 * stabilized linear prediction (LPC) filters for the MLow speech codec.
 *
 * <p>MLow quantizes one LSF vector per 20 ms frame but runs the analysis by synthesis (AbS) search and the
 * short term synthesis filter per subframe. This class bridges that gap on the encode side: it blends the
 * previous frame's reconstructed LSF vector with the current frame's quantized LSF vector, subframe by
 * subframe, using the interpolation factor row the frame's interpolation index selects, converts each
 * interpolated vector to a stabilized LPC filter, and threads the per frame previous LSF carry the next frame
 * interpolates against. The decode side runs the same interpolation through
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.LpcInterpolator}; this class is its
 * encode side twin, sharing the integer NLSF to LPC bridge {@link NlsfBridge} and reproducing the float blend
 * and the bandwidth expansion stabilization with the same arithmetic order.
 *
 * <p>The encoder differs from the decoder in one structural way: it interpolates each frame against two
 * candidate factor rows and keeps the one whose per subframe residual energy is smaller. Both candidates start
 * from the same pristine previous LSF carry, so the first interpolation must not advance that carry before the
 * second one reads it. This class reproduces that by computing each candidate from the held carry without
 * mutating it ({@link #interpolate(float[], int)}) and committing the winning candidate's carry separately
 * ({@link #commit(Candidate)}). The residual energy comparison that picks the winner is the AbS loop's
 * responsibility and is not part of this class; the caller decides which {@link Candidate} to commit.
 *
 * <p>The carry threaded to the next frame is the interpolated last subframe LSF vector of the committed
 * candidate. When the final interpolation factor is less than {@code 1.0} the carry is the blended last
 * subframe vector, not the quantized vector. On the high rate path every factor row ends in {@code 1.0}
 * ({@link #INTERPOL_4} both rows, {@link #INTERPOL_2} row {@code 0}), so the carry equals the quantized vector
 * and {@link EncodeFrontEnd} may store the quantized vector directly; on the low rate alternative index path
 * ({@link #INTERPOL_2} row {@code 1} ends in {@code 0.95}) the carry is the blended vector and must come from
 * here. Computing the carry through this class is correct for both paths.
 *
 * <p>The first frame of a stream is a reset: the held carry starts all zeros, which
 * {@link #interpolate(float[], int)} detects (its last coefficient is {@code 0}) and seeds from the current
 * quantized vector so the frame interpolates against itself. Construct one interpolator per logical stream,
 * feed it every frame in order, commit exactly one candidate per frame, and call {@link #reset()} between
 * independent streams.
 *
 * <p>The integer NLSF to LPC conversion inside the interpolation is bit exact (it runs the integer fixed point
 * conversion through {@link NlsfBridge}); the float blend and the bandwidth expansion stabilization round
 * identically to native single precision arithmetic. The blend scales the previous vector and then adds the
 * scaled quantized vector ({@code ilsf[i] = prev[i] * (1 - factor)} then {@code ilsf[i] += factor * qlsf[i]});
 * the blend has no cross element reduction and the build targets no fused multiply add, so the per element
 * order is the only load bearing detail and is matched here.
 *
 * <p>Scope is the SMPL 16 kHz / 60 ms / mono low band encode path with prediction order {@value #LPC_ORDER}.
 * The high band reuse of the interpolation and the comfort noise (DTX) interpolation rows are out of scope;
 * this class is the low band active voice encode path only. It is stateful per stream and is not thread safe.
 *
 * @implNote This implementation co locates its own copy of the interpolation rather than reusing the decode
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.LpcInterpolator}, because the encoder's
 * two candidates from one pristine carry control flow does not map onto that class's single threaded carry: the
 * winning index alternates per frame, so no single persistent interpolator can hold the carry. The NLSF to LPC
 * conversion is delegated to the shared {@link NlsfBridge#nlsf2a(float[])}; the stability test and the
 * bandwidth expansion stabilization are reproduced here (the same chain
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.LpcInterpolator} runs), keeping this class
 * self contained. The repeated factor fast path (reuse the prior subframe's filter verbatim when the factor
 * repeats) is reproduced, so a trailing run of equal factors carries the last distinctly computed vector, not
 * the nominal last subframe vector.
 */
public final class EncoderLsfInterp {
    /**
     * The logger for {@link EncoderLsfInterp}.
     */
    private static final System.Logger LOGGER = Log.get(EncoderLsfInterp.class);

    /**
     * Linear prediction order of the MLow short term filter: the LSF vector length and the number of predictor
     * taps following the leading unity coefficient.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Subframe count of the locked high rate scope, the subframe count for a 16 kHz / 60 ms / mono high rate
     * frame.
     *
     * <p>The index driven convenience {@link #interpolate(float[], int)} selects a four entry factor row with
     * this count; callers driving a different subframe count supply their own factor row to
     * {@link #interpolate(float[], float[], float[])}.
     */
    private static final int HIGH_RATE_SUBFRAMES = 4;

    /**
     * Squared reflection coefficient stability threshold.
     *
     * <p>A filter is judged unstable when any reflection coefficient of its Levinson down recursion has a square
     * exceeding this value; {@code 0.9995f} corresponds to a reflection magnitude just under one, the margin the
     * codec uses to keep the synthesis filter comfortably inside the unit circle.
     */
    private static final float MAX_RC_STABLE = 0.9995f;

    /**
     * Per subframe interpolation factors for a single subframe frame.
     *
     * <p>A 10 ms low band frame has one subframe and no interpolation index, so the single factor {@code 0.95f}
     * is always used. Held as a one row table so the row selection logic is uniform across subframe counts.
     */
    private static final float[][] INTERPOL_1 = {{0.95f}};

    /**
     * Per subframe interpolation factors for a two subframe frame.
     *
     * <p>Indexed by the interpolation index: row {@code 0} is {@code {0.75f, 1.0f}} and row {@code 1} is
     * {@code {0.4f, 0.95f}}. The two subframe layout is the low rate 60 ms path. Row {@code 1} ends in
     * {@code 0.95f}, so its committed carry is the blended last subframe vector, not the quantized vector.
     */
    private static final float[][] INTERPOL_2 = {
            {0.75f, 1.0f},
            {0.4f, 0.95f}
    };

    /**
     * Per subframe interpolation factors for a four subframe frame.
     *
     * <p>Indexed by the interpolation index: row {@code 0} is {@code {0.55f, 0.88f, 1.0f, 1.0f}} and row
     * {@code 1} is {@code {0.3f, 0.65f, 0.95f, 1.0f}}. The four subframe layout is the high rate 60 ms path, the
     * common case of the SMPL scope. Both rows end in {@code 1.0f}, so the committed carry equals the quantized
     * vector regardless of the chosen index; the trailing repeated {@code 1.0f} of row {@code 0} triggers the
     * filter reuse fast path.
     */
    private static final float[][] INTERPOL_4 = {
            {0.55f, 0.88f, 1.0f, 1.0f},
            {0.3f, 0.65f, 0.95f, 1.0f}
    };

    /**
     * The previous frame's reconstructed LSF carry.
     *
     * <p>Read as the pristine left endpoint of both candidate interpolations and overwritten only by
     * {@link #commit(Candidate)} with the winning candidate's carry. Zero on construction and after
     * {@link #reset()}, which the first frame of a stream detects as a reset.
     */
    private final float[] prevLsf;

    /**
     * One frame's interpolation candidate: the per subframe interpolated LSF vectors and stabilized LPC filters,
     * plus the carry the next frame interpolates against if this candidate is committed.
     *
     * <p>{@code lsf[sf]} is the interpolated LSF vector of subframe {@code sf}, {@value #LPC_ORDER} entries, the
     * vector that feeds the noise generator. {@code lpc[sf]} is the stabilized monic LPC filter of subframe
     * {@code sf}, {@value #LPC_ORDER}{@code  + 1} taps with a leading unity coefficient, the filter the analysis
     * by synthesis loop synthesizes with. {@code prevLsf} is the interpolated last subframe vector this candidate
     * would carry to the next frame. For a trailing run of subframes sharing one factor, each repeated
     * {@code lsf[sf]} holds the same carried vector as its predecessor, matching the reuse fast path.
     *
     * @param lsf     the per subframe interpolated LSF vectors, indexed {@code lsf[subframe][coefficient]}
     * @param lpc     the per subframe stabilized LPC filters, indexed {@code lpc[subframe][tap]}
     * @param prevLsf the carry to the next frame, the interpolated last subframe LSF vector
     */
    public record Candidate(float[][] lsf, float[][] lpc, float[] prevLsf) {
    }

    /**
     * Constructs an encode side interpolator with a zeroed previous LSF carry.
     *
     * <p>The held carry starts all zeros, so the first frame fed to {@link #interpolate(float[], int)} is treated
     * as a reset and interpolates against itself.
     */
    public EncoderLsfInterp() {
        this.prevLsf = new float[LPC_ORDER];
    }

    /**
     * Returns this interpolator to its freshly constructed state.
     *
     * <p>Zeroes the held previous LSF carry so the next frame is treated as the reset frame. Call this between
     * independent streams; do not call it between the frames of one continuous stream, which must thread the
     * carry.
     */
    public void reset() {
        Arrays.fill(prevLsf, 0.0f);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "lsf interpolator reset");
        }
    }

    /**
     * Computes one interpolation candidate for the current frame from the held pristine carry, without advancing
     * it.
     *
     * <p>The held carry is read but not modified, so the default index and the alternative index both interpolate
     * against the same pristine previous LSF vector. If the held carry is all zeros (its last coefficient is
     * {@code 0}) this is the reset frame and the carry the candidate reports is seeded from {@code qlsf} so the
     * frame interpolates against itself.
     *
     * <p>For each subframe in order the factor {@code interpol[sf]} of the selected row drives the blend: the
     * quantized vector when the factor is exactly {@code 1.0f}, the convex blend
     * {@code prev * (1 - factor) + qlsf * factor} otherwise, or a verbatim reuse of the previous subframe's
     * filter and vector when the factor repeats the previous subframe's. Each freshly blended vector is
     * converted to a stabilized LPC filter. The candidate's carry is the last computed interpolated vector.
     *
     * <p>This convenience selects the {@value #HIGH_RATE_SUBFRAMES} entry high rate factor row by index; a caller
     * driving a different subframe count uses {@link #interpolate(float[], float[], float[])} with an explicit
     * row.
     *
     * @param qlsf           the current frame's quantized LSF vector, {@value #LPC_ORDER} entries, as produced
     *                       by {@link LsfQuantizer.QuantizedLsf#lsf()}
     * @param lsfInterpolIdx the interpolation factor row index; {@code 0} for the default row and {@code 1} for
     *                       the alternative search row
     * @return the candidate's per subframe interpolated LSF vectors, stabilized LPC filters, and carry
     */
    public Candidate interpolate(float[] qlsf, int lsfInterpolIdx) {
        return interpolate(qlsf, HIGH_RATE_SUBFRAMES, lsfInterpolIdx);
    }

    /**
     * Computes one interpolation candidate for the current frame from the held pristine carry for a given
     * subframe count, without advancing it.
     *
     * <p>Selects the factor row by {@code numSubframes} ({@link #INTERPOL_4} for four subframes,
     * {@link #INTERPOL_2} for two, {@link #INTERPOL_1} for one) and index {@code lsfInterpolIdx}, then runs the
     * same reset resolved blend as {@link #interpolate(float[], int)}: the low rate path drives
     * {@value #LPC_ORDER} entry vectors through the two subframe {@link #INTERPOL_2} row whose alternative index
     * ends in {@code 0.95}, so the committed carry is the blended last subframe vector, not the quantized vector.
     *
     * @param qlsf           the current frame's quantized LSF vector, {@value #LPC_ORDER} entries
     * @param numSubframes   the subframe count of the frame; 1, 2, or 4
     * @param lsfInterpolIdx the interpolation factor row index; {@code 0} for the default row and {@code 1} for
     *                       the alternative search row
     * @return the candidate's per subframe interpolated LSF vectors, stabilized LPC filters, and carry
     */
    public Candidate interpolate(float[] qlsf, int numSubframes, int lsfInterpolIdx) {
        var interpol = interpolRow(numSubframes, lsfInterpolIdx);
        var pristine = new float[LPC_ORDER];
        System.arraycopy(prevLsf, 0, pristine, 0, LPC_ORDER);
        if (pristine[LPC_ORDER - 1] == 0.0f) {
            System.arraycopy(qlsf, 0, pristine, 0, LPC_ORDER);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "lsf interpolate: reset frame, carry seeded from current lsf");
            }
        }
        return interpolate(qlsf, pristine, interpol);
    }

    /**
     * Computes one interpolation candidate from an explicit pristine carry and an explicit factor row.
     *
     * <p>The reset seeding is the caller's responsibility ({@link #interpolate(float[], int)} performs it before
     * calling this); {@code pristine} is therefore the already reset resolved left endpoint of the blend. This
     * entry point is the one a residual energy AbS search drives directly when it supplies its own carry and
     * factor row.
     *
     * @param qlsf     the current frame's quantized LSF vector, {@value #LPC_ORDER} entries
     * @param pristine the reset resolved previous frame LSF vector, {@value #LPC_ORDER} entries, read but not
     *                 modified
     * @param interpol the per subframe interpolation factor row, its length the subframe count
     * @return the candidate's per subframe interpolated LSF vectors, stabilized LPC filters, and carry
     */
    public Candidate interpolate(float[] qlsf, float[] pristine, float[] interpol) {
        var numSubfr = interpol.length;
        var lpc = new float[numSubfr][];
        var lsfs = new float[numSubfr][];
        var ilsf = new float[LPC_ORDER];
        var prevFactor = -1.0f;
        for (var j = 0; j < numSubfr; j++) {
            var factor = interpol[j];
            if (factor == prevFactor) {
                // Repeated factor reuses the prior subframe's filter and interpolated vector verbatim. The LPC
                // rows are read only downstream (computeReslpc and encodeSubframe only read the coefficients),
                // the LSF rows are read by no consumer, and neither is pooled as scratch, so aliasing the prior
                // rows is bit identical to cloning them.
                lpc[j] = lpc[j - 1];
                lsfs[j] = lsfs[j - 1];
            } else {
                if (factor == 1.0f) {
                    System.arraycopy(qlsf, 0, ilsf, 0, LPC_ORDER);
                } else {
                    var oneMinus = 1.0f - factor;
                    for (var i = 0; i < LPC_ORDER; i++) {
                        ilsf[i] = pristine[i] * oneMinus;
                    }
                    for (var i = 0; i < LPC_ORDER; i++) {
                        ilsf[i] += factor * qlsf[i];
                    }
                }
                lpc[j] = nlsf2aStabilize(ilsf);
                lsfs[j] = ilsf.clone();
            }
            prevFactor = factor;
        }
        var carry = ilsf.clone();
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "lsf candidate interpolated: subframes={0}", numSubfr);
        }
        return new Candidate(lsfs, lpc, carry);
    }

    /**
     * Returns the held previous LSF carry without advancing it.
     *
     * <p>The conditional LSF quantizer conditions on the same pristine previous LSF vector the interpolator
     * carries. This exposes the live backing array for that read; callers must not mutate it. The carry is zero
     * before the first frame of a stream, which the quantizer's conditional path never reads (the first frame is
     * always non conditional).
     *
     * @return the live previous LSF carry, {@value #LPC_ORDER} entries
     */
    public float[] peekCarry() {
        return prevLsf;
    }

    /**
     * Commits a candidate as the winning interpolation for the current frame, advancing the carry to the next
     * frame.
     *
     * <p>Overwrites the held previous LSF carry with the candidate's carry. Call this exactly once per frame
     * after the residual energy comparison selects the winner.
     *
     * @param winner the candidate selected by the analysis by synthesis residual energy comparison
     */
    public void commit(Candidate winner) {
        System.arraycopy(winner.prevLsf(), 0, prevLsf, 0, LPC_ORDER);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "lsf interpolation carry committed");
        }
    }

    /**
     * Selects the per subframe interpolation factor row for an active voice encode frame.
     *
     * <p>Returns row {@code lsfInterpolIdx} of the factor table chosen by {@code numSubframes}. The single
     * comfort noise (DTX) rows are out of scope for this encode path and are not selectable here.
     *
     * @param numSubframes   the subframe count of the frame; 1, 2, or 4
     * @param lsfInterpolIdx the interpolation index
     * @return the per subframe interpolation factor row of length {@code numSubframes}
     * @throws IllegalArgumentException if {@code numSubframes} is not 1, 2, or 4
     */
    private static float[] interpolRow(int numSubframes, int lsfInterpolIdx) {
        return switch (numSubframes) {
            case 1 -> INTERPOL_1[lsfInterpolIdx];
            case 2 -> INTERPOL_2[lsfInterpolIdx];
            case 4 -> INTERPOL_4[lsfInterpolIdx];
            default -> throw new IllegalArgumentException("unsupported subframe count " + numSubframes);
        };
    }

    /**
     * Converts an interpolated LSF vector to a stabilized LPC filter.
     *
     * <p>Runs the integer fixed point NLSF to LPC conversion through {@link NlsfBridge#nlsf2a(float[])} and then
     * stabilizes the resulting filter in place.
     *
     * @param ilsf the interpolated LSF vector of one subframe, {@value #LPC_ORDER} entries
     * @return a freshly allocated stabilized LPC filter, {@value #LPC_ORDER}{@code  + 1} taps with a leading
     *         unity coefficient
     */
    private static float[] nlsf2aStabilize(float[] ilsf) {
        var a = NlsfBridge.nlsf2a(ilsf);
        stabilize(a);
        return a;
    }

    /**
     * Forces an LPC filter to be stable in place.
     *
     * <p>Tests the filter with {@link #isStable(float[])}; if it is already stable the filter is left untouched.
     * Otherwise progressively stronger bandwidth expansion is applied, the expansion factor decreasing by
     * {@code 0.001f} each iteration ({@code bwe = 1 - iter * 0.001f}), until the stability test passes. The loop
     * terminates because the expansion drives every reflection coefficient toward zero.
     *
     * @param a the LPC filter to stabilize in place, {@value #LPC_ORDER}{@code  + 1} taps
     */
    private static void stabilize(float[] a) {
        if (isStable(a)) {
            return;
        }
        var iter = 0;
        do {
            iter++;
            bweExpand(a, 1.0f - iter * 0.001f);
        } while (!isStable(a));
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "lpc filter unstable, bandwidth expansion applied: iterations={0}", iter);
        }
    }

    /**
     * Applies bandwidth expansion to an LPC filter in place.
     *
     * <p>Scales tap {@code i} (one based, the leading unity coefficient untouched) by {@code bwe^i} using a
     * running accumulator: {@code A[1] *= bwe}, {@code A[2] *= bwe^2}, and so on. A non positive {@code bwe}
     * zeroes the entire predictor; the stabilize loop never supplies such a value, but the branch is retained.
     *
     * @param a   the LPC filter to expand in place, {@value #LPC_ORDER}{@code  + 1} taps
     * @param bwe the bandwidth expansion factor, in {@code (0, 1)} for the stabilize path
     */
    private static void bweExpand(float[] a, float bwe) {
        if (bwe <= 0.0f) {
            for (var i = 1; i < LPC_ORDER + 1; i++) {
                a[i] = 0.0f;
            }
            return;
        }
        var c = bwe;
        for (var i = 1; i < LPC_ORDER + 1; i++) {
            a[i] *= c;
            c *= bwe;
        }
    }

    /**
     * Tests whether an LPC synthesis filter is stable.
     *
     * <p>Short circuits to unstable when the last predictor tap's square exceeds {@link #MAX_RC_STABLE}, then
     * runs a double precision Levinson down recursion: at each order it forms the next lower order predictor from
     * the current one, divided by {@code 1 - tail^2}, and rejects the filter when the division denominator is
     * zero or when the new leading reflection coefficient's square exceeds the threshold. The recursion
     * alternates between two scratch buffers ({@code a0} and {@code a1}) so each pass reuses the other's output,
     * avoiding a copy; the filter is stable when the recursion reaches order zero without a rejection.
     *
     * @param a the LPC filter to test, {@value #LPC_ORDER}{@code  + 1} taps with a leading unity coefficient
     * @return {@code true} when the filter is stable, {@code false} otherwise
     */
    private static boolean isStable(float[] a) {
        if (a[LPC_ORDER] * a[LPC_ORDER] > MAX_RC_STABLE) {
            return false;
        }
        var a0 = new double[LPC_ORDER];
        var a1 = new double[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            a0[i] = a[i + 1];
        }
        var m = LPC_ORDER - 1;
        while (true) {
            var den = 1.0 - a0[m] * a0[m];
            if (den == 0.0) {
                return false;
            }
            var invDen = 1.0 / den;
            for (var k = 0; k < m; k++) {
                a1[k] = (a0[k] - a0[m] * a0[m - k - 1]) * invDen;
            }
            if (a1[m - 1] * a1[m - 1] > MAX_RC_STABLE) {
                return false;
            }
            if (--m == 0) {
                return true;
            }
            den = 1.0 - a1[m] * a1[m];
            if (den == 0.0) {
                return false;
            }
            invDen = 1.0 / den;
            for (var k = 0; k < m; k++) {
                a0[k] = (a1[k] - a1[m] * a1[m - k - 1]) * invDen;
            }
            if (a0[m - 1] * a0[m - 1] > MAX_RC_STABLE) {
                return false;
            }
            if (--m == 0) {
                return true;
            }
        }
    }
}
