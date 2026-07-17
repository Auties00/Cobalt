package com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Interpolates one line spectral frequency (LSF) vector per frame into per subframe LSF vectors and
 * stabilized linear prediction (LPC) filters for the MLow speech codec decode path.
 *
 * <p>MLow codes one LSF vector per 20 ms frame, but the short term synthesis filter is applied per
 * subframe. This class bridges that gap: it interpolates between the previous frame's reconstructed LSF
 * vector and the current frame's vector to produce one LSF vector per subframe, converts each to an LPC
 * filter, and forces each filter to be stable. The interpolation weight of each subframe is supplied by
 * the decoder as a fixed factor vector selected by the frame's interpolation index; this class consumes
 * that already resolved per subframe factor vector.
 *
 * <p>The interpolation carries state across frames. The previous frame's LSF vector is held on this
 * instance and updated after every frame. The first frame of a decode session is a reset: when the held
 * previous vector is all zeros (its last coefficient is {@code 0}), it is seeded from the current frame's
 * vector so the first frame interpolates against itself. Construct one interpolator per logical stream and
 * feed it every frame in order; {@link #reset()} returns it to the freshly constructed state.
 *
 * <p>For each subframe the interpolated vector {@code ilsf} is:
 * <ul>
 * <li>the current frame's LSF vector when the factor is exactly {@code 1.0f};</li>
 * <li>otherwise the convex blend {@code previousLsf * (1 - factor) + lsf * factor}, computed in that
 * statement order so the {@code float} rounding is deterministic.</li>
 * </ul>
 * When a subframe's factor equals the immediately preceding subframe's factor, the previous subframe's
 * already computed LPC filter is reused verbatim and {@code ilsf} is not recomputed; this class copies the
 * previous LPC row and leaves the carried {@code ilsf} unchanged. The held previous frame vector is then
 * updated to the last value of {@code ilsf}, which for a trailing run of repeated factors is the last
 * distinctly computed vector, not the nominal last subframe vector. That detail is load bearing for the
 * next frame's interpolation.
 *
 * <p>Each interpolated LSF vector is converted to an LPC filter through {@link NlsfBridge#nlsf2a(float[])}
 * and then stabilized. Stabilization first tests the filter with the reflection coefficient stability test,
 * and if it fails applies progressively stronger bandwidth expansion ({@code A[i] *= bwe^i}) with
 * {@code bwe = 1 - iter * 0.001f} until the test passes.
 *
 * <p>The integer NLSF2A conversion is bit exact; the float interpolation and the bandwidth expansion
 * stabilization round to single precision arithmetic, so the produced per subframe LPC filters match the
 * reference decoder to within IEEE-754 rounding (a relative epsilon of {@code 1e-4}).
 *
 * <p>Scope is the 16 kHz, 60 ms, mono low band decode path with an LPC order of {@value #LPC_ORDER}. The
 * encoder side interpolation index search and the high band reuse of this interpolation are out of scope.
 * It is stateful per stream and is not thread safe.
 *
 * @implNote This implementation keeps the float interpolation and the stabilization chain here and splits
 * the integer NLSF2A conversion out to {@link NlsfBridge}. The stability test uses the
 * {@link #MAX_RC_STABLE} threshold of {@code 0.9995f}, a leading coefficient short circuit, a double
 * precision Levinson down recursion, and an alternating {@code a0}/{@code a1} ping pong that halves the
 * work. The bandwidth expansion uses a running {@code float} accumulator multiplied by {@code bwe} each
 * tap rather than recomputing {@code bwe^i}, so the rounding is identical.
 */
public final class LpcInterpolator {
    /**
     * The logger for {@link LpcInterpolator}.
     */
    private static final System.Logger LOGGER = Log.get(LpcInterpolator.class);

    /**
     * Linear prediction order of the MLow short term filter; the LSF vector length and the number of LPC
     * predictor taps. Each LPC filter row is this many taps plus the leading unity coefficient.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Squared reflection coefficient stability threshold.
     *
     * <p>A filter is judged unstable when any reflection coefficient of its Levinson down recursion has a
     * square exceeding this value; {@code 0.9995f} corresponds to a reflection magnitude just under one, the
     * margin that keeps the synthesis filter comfortably inside the unit circle.
     */
    private static final float MAX_RC_STABLE = 0.9995f;

    /**
     * The previous frame's reconstructed LSF vector.
     *
     * <p>Read as the left endpoint of every subframe's interpolation and overwritten after each frame with
     * the last computed interpolated vector. Zero on construction and after {@link #reset()}, which the
     * first frame of a session detects as a reset.
     */
    private final float[] previousLsf;

    /**
     * Constructs a per subframe LSF interpolator with a zeroed previous frame vector.
     *
     * <p>The held previous frame LSF vector starts all zeros, so the first frame fed to
     * {@link #interpolate(float[], float[])} is treated as a reset and interpolates against itself.
     */
    public LpcInterpolator() {
        this.previousLsf = new float[LPC_ORDER];
    }

    /**
     * Returns this interpolator to its freshly constructed state.
     *
     * <p>Zeroes the held previous frame LSF vector so the next frame is treated as the reset frame. Call
     * this between independent decode sessions; do not call it between the frames of one continuous stream,
     * which must thread the previous frame vector.
     */
    public void reset() {
        Arrays.fill(previousLsf, 0.0f);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mlow lpc interpolator: reset");
    }

    /**
     * Returns the carried previous frame LSF vector so packet loss concealment can adapt it toward the
     * concealment or comfort noise spectrum on the first good frame after a loss.
     *
     * <p>The returned array is the live backing store: the concealment adaptation overwrites it in place, and
     * the next {@link #interpolate(float[], float[])} reads the adapted vector.
     *
     * @return the live previous frame LSF vector
     */
    public float[] previousLsf() {
        return previousLsf;
    }

    /**
     * The per subframe result of one frame's LSF interpolation and LPC stabilization.
     *
     * <p>{@code lpc[sf]} is the stabilized LPC filter of subframe {@code sf}, {@value #LPC_ORDER}{@code  + 1}
     * taps with a leading unity coefficient, consumed by the synthesis filter. {@code lsf[sf]} is the
     * interpolated line spectral frequency vector of subframe {@code sf}, {@value #LPC_ORDER} entries, which
     * feeds the noise generator and the comfort noise model. For a trailing run of subframes sharing one
     * interpolation factor, each repeated {@code lsf[sf]} holds the same carried vector as its predecessor.
     *
     * @param lpc the per subframe stabilized LPC filters, indexed {@code lpc[subframe][tap]}
     * @param lsf the per subframe interpolated LSF vectors, indexed {@code lsf[subframe][coefficient]}
     */
    public record InterpolatedFrame(float[][] lpc, float[][] lsf) {
    }

    /**
     * Interpolates one frame's LSF vector to per subframe LSF and stabilized LPC, advancing the carried
     * previous frame state.
     *
     * <p>If this is the reset frame (the held previous frame vector is all zeros) the previous frame vector
     * is first seeded from {@code lsf} so the frame interpolates against itself. Then, for each subframe in
     * order, the interpolation factor {@code interpol[sf]} selects the interpolated vector: the current
     * vector when the factor is {@code 1.0f}, the convex blend of the previous frame and current vectors
     * otherwise, or a verbatim reuse of the previous subframe's filter and vector when the factor repeats.
     * Each freshly interpolated vector is converted to an LPC filter through {@link NlsfBridge#nlsf2a(float[])}
     * and stabilized in place. After the loop the held previous frame vector is updated to the last
     * interpolated vector, ready for the next frame.
     *
     * @param lsf      the current frame's reconstructed LSF vector, {@value #LPC_ORDER} entries, as produced
     *                 by {@link LsfDequantizer#dequantize(int[], int, int, float[])}
     * @param interpol the per subframe interpolation factors; its length is the subframe count
     * @return the per subframe stabilized LPC filters and interpolated LSF vectors
     */
    public InterpolatedFrame interpolate(float[] lsf, float[] interpol) {
        if (previousLsf[LPC_ORDER - 1] == 0.0f) {
            System.arraycopy(lsf, 0, previousLsf, 0, LPC_ORDER);
        }
        var numSubfr = interpol.length;
        var a = new float[numSubfr][];
        var lsfs = new float[numSubfr][];
        var ilsf = new float[LPC_ORDER];
        var prevFactor = -1.0f;
        for (var j = 0; j < numSubfr; j++) {
            var factor = interpol[j];
            if (factor == prevFactor) {
                // A repeated factor reuses the prior subframe's filter and interpolated vector verbatim. Both
                // rows are only read downstream and are never mutated or pooled as scratch, so aliasing the
                // prior rows produces the same result as cloning them.
                a[j] = a[j - 1];
                lsfs[j] = lsfs[j - 1];
            } else {
                if (factor == 1.0f) {
                    System.arraycopy(lsf, 0, ilsf, 0, LPC_ORDER);
                } else {
                    var oneMinus = 1.0f - factor;
                    for (var i = 0; i < LPC_ORDER; i++) {
                        ilsf[i] = previousLsf[i] * oneMinus;
                    }
                    for (var i = 0; i < LPC_ORDER; i++) {
                        ilsf[i] += factor * lsf[i];
                    }
                }
                a[j] = nlsf2aStabilize(ilsf);
                lsfs[j] = ilsf.clone();
            }
            prevFactor = factor;
        }
        System.arraycopy(ilsf, 0, previousLsf, 0, LPC_ORDER);
        return new InterpolatedFrame(a, lsfs);
    }

    /**
     * Converts an interpolated LSF vector to a stabilized LPC filter.
     *
     * <p>Runs the integer fixed point NLSF2A conversion through {@link NlsfBridge#nlsf2a(float[])} and then
     * stabilizes the resulting filter in place with {@link #stabilize(float[])}.
     *
     * @param ilsf the interpolated LSF vector of one subframe, {@value #LPC_ORDER} entries
     * @return a freshly allocated stabilized LPC filter, {@value #LPC_ORDER}{@code  + 1} taps with a leading
     *         unity coefficient
     */
    public static float[] nlsf2aStabilize(float[] ilsf) {
        var a = NlsfBridge.nlsf2a(ilsf);
        stabilize(a);
        return a;
    }

    /**
     * Forces an LPC filter to be stable in place.
     *
     * <p>Tests the filter with {@link #isStable(float[])}; if it is already stable the filter is left
     * untouched. Otherwise progressively stronger bandwidth expansion is applied, the expansion factor
     * decreasing by {@code 0.001f} each iteration ({@code bwe = 1 - iter * 0.001f}), until the stability
     * test passes. The loop is guaranteed to terminate because the expansion drives every reflection
     * coefficient toward zero.
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
            LOGGER.log(Level.DEBUG, "mlow lpc interpolator: unstable filter, bandwidth-expanded iterations={0}",
                    iter);
        }
    }

    /**
     * Applies bandwidth expansion to an LPC filter in place.
     *
     * <p>Scales tap {@code i} (tap indexing starts at one, the leading unity coefficient untouched) by
     * {@code bwe^i} using a running accumulator: {@code A[1] *= bwe}, {@code A[2] *= bwe^2}, and so on. A
     * {@code bwe} of zero or less zeroes the entire predictor; the decode stabilize loop never supplies such
     * a value, but the branch is kept so the full function behavior is preserved.
     *
     * @param a   the LPC filter to expand in place, {@value #LPC_ORDER}{@code  + 1} taps
     * @param bwe the bandwidth expansion factor, in {@code (0, 1)} for the decode path
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
     * <p>Short circuits to unstable when the last predictor tap's square exceeds {@link #MAX_RC_STABLE},
     * then runs a double precision Levinson down recursion: at each order it forms the next lower order
     * predictor from the current one, divided by {@code 1 - tail^2}, and rejects the filter when the
     * division denominator is zero or when the new leading reflection coefficient's square exceeds the
     * threshold. The recursion alternates between two scratch buffers ({@code a0} and {@code a1}) so each
     * pass reuses the other's output without copying; the filter is stable when the recursion reaches order
     * zero without a rejection.
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
