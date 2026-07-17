package com.github.auties00.cobalt.calls.media.audio.codec.mlow.postfilter;

/**
 * The pitch indexed low pass comb window table for the MLow harmonic postfilter.
 *
 * <p>The harmonic postfilter mixes a pitch delayed copy of the synthesis back into itself through a short
 * symmetric low pass FIR whose cutoff tracks the pitch period. Rather than recompute that FIR for every lag,
 * one filter per distinct quantized pitch lag is precomputed once at startup and indexed by
 * {@link #lagToFiltIx(int)}. This holder carries that table: {@value #NUM_LP_FILT} rows of
 * {@value #FILTER_LEN} symmetric taps, built from a fixed cosine analysis window scaled by the running
 * harmonic index and a per lag cutoff. The table is immutable and shared by every postfilter instance.
 *
 * @implNote This implementation builds a {@value #NUM_LP_FILT} by {@value #FILTER_LEN} table: the analysis
 * window {@code FiltWin[i] = cos(omega) / (i + 1)} with {@code omega} stepping by
 * {@code (pi/2) / (FB_DELAY + 1)}, then for each distinct filter index a sinc like low pass of cutoff
 * {@code omegaC = min(omega0 * NHARM_CUTOFF, CUTOFF_HZ / 16000 * pi)} where {@code omega0 = 2*pi/lag},
 * normalized to unit DC gain. The table arithmetic is strict single precision with no fused multiply add
 * contraction; the cosine and sine are evaluated as {@code (float) Math.cos} and {@code (float) Math.sin} on
 * the promoted double argument followed by a narrowing store, which reproduces a single precision result to
 * within the float rounding of the last bit.
 */
final class HarmonicPostfilterTables {
    /**
     * The comb window feedback delay in samples; each low pass filter spans {@value #FILTER_LEN} symmetric
     * taps centered on the comb tap.
     */
    static final int FB_DELAY = 8;

    /**
     * The symmetric low pass tap count, {@code 2 * FB_DELAY + 1}.
     */
    static final int FILTER_LEN = 2 * FB_DELAY + 1;

    /**
     * The minimum pitch lag in samples, {@code 2 ms} sampled at {@code 16 kHz}.
     */
    static final int MIN_PITCH_LAG = 32;

    /**
     * The maximum pitch lag in samples, {@code 20 ms} sampled at {@code 16 kHz}.
     */
    static final int MAX_PITCH_LAG = 320;

    /**
     * The lag to index resolution numerator; a higher value yields more distinct precomputed filters.
     */
    private static final int LP_FILT_RES = 2500;

    /**
     * The number of distinct precomputed low pass filters,
     * {@code (LP_FILT_RES / 80) - LP_FILT_RES / MAX_PITCH_LAG + 1}.
     */
    static final int NUM_LP_FILT = (LP_FILT_RES / 80) - LP_FILT_RES / MAX_PITCH_LAG + 1;

    /**
     * The harmonic count cutoff; the low pass passes at most this many pitch harmonics.
     */
    private static final float NHARM_CUTOFF = 6.3f;

    /**
     * The absolute cutoff frequency in hertz.
     */
    private static final float CUTOFF_HZ = 4000.0f;

    /**
     * Half pi at single precision truncation, used for the analysis window step.
     */
    private static final float HALF_PI = 3.1415926535897f * 0.5f;

    /**
     * Pi at single precision truncation, used for {@code omega0} and the absolute cutoff.
     */
    private static final float PI = 3.1415926535897f;

    /**
     * The shared immutable instance.
     */
    static final HarmonicPostfilterTables INSTANCE = new HarmonicPostfilterTables();

    /**
     * The precomputed low pass filters indexed by {@link #lagToFiltIx(int)}, {@value #NUM_LP_FILT} rows of
     * {@value #FILTER_LEN} symmetric taps.
     */
    private final float[][] lpFilters;

    /**
     * Builds the pitch indexed low pass comb window table.
     *
     * <p>Constructs the cosine analysis window, then sweeps every pitch lag in
     * {@code [MIN_PITCH_LAG, MAX_PITCH_LAG]}, creating one low pass filter per distinct
     * {@link #lagToFiltIx(int) filter index} the first time it appears.
     */
    private HarmonicPostfilterTables() {
        this.lpFilters = new float[NUM_LP_FILT][FILTER_LEN];
        var filtWin = new float[FB_DELAY];
        var dOmega = HALF_PI / (FB_DELAY + 1.0f);
        var omega = dOmega;
        for (var i = 0; i < FB_DELAY; i++) {
            filtWin[i] = (float) Math.cos(omega) / (i + 1);
            omega += dOmega;
        }
        var ixPrev = -1;
        for (var lag = MIN_PITCH_LAG; lag <= MAX_PITCH_LAG; lag++) {
            var ix = lagToFiltIx(lag);
            if (ix != ixPrev) {
                var omega0 = 2.0f * PI / lag;
                createLpFilter(omega0, filtWin, lpFilters[ix]);
                ixPrev = ix;
            }
        }
    }

    /**
     * Maps a pitch lag to its precomputed filter index.
     *
     * <p>Computes {@code LP_FILT_RES / max(lag + 30, 80) - LP_FILT_RES / MAX_PITCH_LAG} with integer
     * division, the row index into {@link #lpFilters}.
     *
     * @param lag the pitch lag in samples; must be nonzero
     * @return the row index in {@code [0, NUM_LP_FILT)}
     */
    static int lagToFiltIx(int lag) {
        return LP_FILT_RES / Math.max(lag + 30, 80) - LP_FILT_RES / MAX_PITCH_LAG;
    }

    /**
     * Returns the precomputed low pass filter for the given filter index.
     *
     * @param ix the filter index from {@link #lagToFiltIx(int)}
     * @return the {@value #FILTER_LEN} tap symmetric low pass filter; not to be mutated by the caller
     */
    float[] filter(int ix) {
        return lpFilters[ix];
    }

    /**
     * Builds one normalized low pass comb window for a given pitch fundamental.
     *
     * <p>Forms a symmetric sinc like low pass of cutoff
     * {@code omegaC = min(omega0 * NHARM_CUTOFF, CUTOFF_HZ / 16000 * pi)}, windowed by {@code filtWin}, with
     * the DC term at the center tap, then normalizes the whole filter to unit sum (unit DC gain).
     *
     * @param omega0  the pitch fundamental angular frequency {@code 2*pi/lag}
     * @param filtWin the cosine analysis window of length {@value #FB_DELAY}
     * @param bLP     the output filter row of length {@value #FILTER_LEN}; filled in place
     */
    private static void createLpFilter(float omega0, float[] filtWin, float[] bLP) {
        var omegaC = Math.min(omega0 * NHARM_CUTOFF, CUTOFF_HZ / 16000.0f * PI);
        var sumB = 0.0f;
        var omegaCSum = omegaC;
        for (var i = 0; i < FB_DELAY; i++) {
            var b = filtWin[i] * (float) Math.sin(omegaCSum);
            omegaCSum += omegaC;
            bLP[FB_DELAY + i + 1] = b;
            bLP[FB_DELAY - i - 1] = b;
            sumB += 2.0f * b;
        }
        bLP[FB_DELAY] = omegaC;
        sumB += omegaC;
        var inv = 1.0f / sumB;
        for (var i = 0; i < FILTER_LEN; i++) {
            bLP[i] *= inv;
        }
    }
}
