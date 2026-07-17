package com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter;

/**
 * Provides the decode path linear filter primitives for the MLow speech codec.
 *
 * <p>MLow synthesis is a chain of short fixed structure FIR (moving average, MA) and IIR (autoregressive,
 * AR) sections plus their cascades (ARMA) and a second order allpass. The decoder applies them in a fixed
 * order per subframe and per frame: an optional unvoiced pulse shaping ARMA, a first order tilt postfilter
 * MA, the 16th order AR short term synthesis filter, a generator side second order MA noise shaper, and a
 * frame level second order ARMA high pass. This class collects every such primitive the synthesis path
 * invokes, each method reproducing the exact single precision operation order of the scalar reference
 * implementation.
 *
 * <p>Two state conventions appear. The explicit state filters
 * ({@link #ma1(float[], int, int, float[], float[], int, float[], int)},
 * {@link #ma2(float[], int, int, float[], float[], int, float[], int)},
 * {@link #ar1(float[], int, int, float[], float[], int, float[], int)},
 * {@link #ar2(float[], int, int, float[], float[], int, float[], int)},
 * {@link #arma1(float[], int, int, float[], float[], float[], int)},
 * {@link #arma2(float[], int, int, float[], float[], float[], int)},
 * {@link #allpass2(float[], int, int, float[], float[], int, float[], int)}) carry a small caller owned state
 * vector that holds the filter memory between calls; the caller threads the same state array across the
 * subframes or frames of one continuous decode. The in buffer history filters
 * ({@link #ma3(float[], int, int, float[], float[], int)},
 * {@link #ma9(float[], int, int, float[], float[], int)},
 * {@link #ma16Monic(float[], int, int, float[], float[], int)},
 * {@link #ma16Sym(float[], int, int, float[], float[], int)},
 * {@link #ma(float[], int, int, float[], int, float[], int)},
 * {@link #ar4(float[], int, int, float[], float[], int)},
 * {@link #ar16(float[], int, int, float[], float[], int)}) instead read their memory from the samples that
 * precede the input window in the same backing array, exactly as {@code x[n - i]} / {@code y[n - i]}
 * indexing does; the caller lays out a contiguous buffer with the previous order samples kept in front of
 * the current window. {@link #interpol(float[], int, float[], int, int, float[])} is a stateless symmetric
 * fractional delay FIR over a windowed input.
 *
 * <p>The filters operate on raw {@code float} arrays with explicit offsets so the in buffer history layout
 * is expressible without copies. None of them allocate except {@link #arma1} and {@link #arma2}, which
 * stage through a temporary buffer only when input and output alias; the decode call sites never alias, so
 * the in place path is taken. The arithmetic is single precision throughout and follows the reference
 * operation order term by term, matching that reference to a tight relative envelope; the impulse response
 * unit tests confirm a relative epsilon near {@code 1e-5}.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono low band decode path with an LPC order of 16 and the postfilter
 * disabled. The high band allpass filterbank and the 32 to 48 kHz upsampler are out of scope (they only run
 * above 16 kHz) and are not ported. This type is stateless: all filter memory lives in caller owned arrays,
 * so the methods are thread safe, but a given state array must not be shared across concurrent decodes.
 *
 * @implNote This implementation reproduces the software pipelined AR bodies (the five wide {@link #ar1}, the
 * four wide {@link #ar2}, and the two wide {@link #ar4}) term for term rather than as a plain recurrence,
 * because the pipelined form changes the float accumulation order and therefore the result. The MA kernels
 * inline the per element {@code coef[0] == 1.0f} monic fast path that decides whether the leading tap is a
 * copy or a scale.
 */
public final class Filters {
    /**
     * Bounds the temporary buffer that {@link #arma1} and {@link #arma2} allocate when input and output
     * alias, at {@code 16 * 20} (320) samples.
     *
     * <p>The ARMA filters stage through a scratch buffer of this size when input and output alias; the
     * decode call sites never alias, so the bound is only asserted, never reached.
     */
    private static final int MAX_FILTER_INPUT_SAMPLES = 16 * 20;

    /**
     * Prevents instantiation of this stateless filter primitive holder.
     *
     * <p>All filters are static and operate entirely on caller owned arrays, so there is no instance state
     * to construct.
     *
     * @throws AssertionError always
     */
    private Filters() {
        throw new AssertionError();
    }

    /**
     * Applies a first order moving average (FIR) filter that need not be monic.
     *
     * <p>Computes {@code y[n] = coef[0] * x[n] + coef[1] * x[n - 1]} for the {@code N} sample window, taking
     * the sample before the window from the single element {@code state} vector and updating that state to
     * the last input sample. When {@code coef[0]} is exactly {@code 1.0f} the leading tap is a copy rather
     * than a multiply. Input and output windows must not alias.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be positive
     * @param coef     the two filter coefficients {@code {coef0, coef1}}
     * @param state    the single element filter memory vector, read for {@code x[-1]} and updated to
     *                 {@code x[N - 1]}
     * @param stateOff the offset of the state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    public static void ma1(float[] x, int xOff, int n, float[] coef, float[] state, int stateOff, float[] y, int yOff) {
        if (coef[0] == 1.0f) {
            for (var i = 0; i < n - 1; i++) {
                y[yOff + 1 + i] = x[xOff + 1 + i] + coef[1] * x[xOff + i];
            }
        } else {
            for (var i = 0; i < n; i++) {
                y[yOff + i] = x[xOff + i] * coef[0];
            }
            for (var i = 0; i < n - 1; i++) {
                y[yOff + 1 + i] += coef[1] * x[xOff + i];
            }
        }
        y[yOff] = coef[0] * x[xOff] + coef[1] * state[stateOff];
        state[stateOff] = x[xOff + n - 1];
    }

    /**
     * Applies a second order moving average (FIR) filter that need not be monic.
     *
     * <p>Computes {@code y[n] = coef[0] * x[n] + coef[1] * x[n - 1] + coef[2] * x[n - 2]} for the
     * {@code N} sample window, taking the two samples before the window from the two element {@code state}
     * vector ({@code state[0]} is {@code x[-1]}, {@code state[1]} is {@code x[-2]}) and updating that state
     * to the last two input samples. When {@code coef[0]} is exactly {@code 1.0f} the leading tap is a copy.
     * The {@code coef[2]} contribution is added in a separate sweep over {@code y[2 .. N - 1]}, then the
     * first two outputs are patched with the state terms. Input and output windows must not alias.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be greater than one
     * @param coef     the three filter coefficients {@code {coef0, coef1, coef2}}
     * @param state    the two element filter memory vector, read for {@code x[-1]}/{@code x[-2]} and updated
     *                 to {@code x[N - 1]}/{@code x[N - 2]}
     * @param stateOff the offset of the first state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    public static void ma2(float[] x, int xOff, int n, float[] coef, float[] state, int stateOff, float[] y, int yOff) {
        if (coef[0] == 1.0f) {
            for (var i = 0; i < n - 1; i++) {
                y[yOff + 1 + i] = x[xOff + 1 + i] + coef[1] * x[xOff + i];
            }
        } else {
            for (var i = 0; i < n; i++) {
                y[yOff + i] = x[xOff + i] * coef[0];
            }
            for (var i = 0; i < n - 1; i++) {
                y[yOff + 1 + i] += coef[1] * x[xOff + i];
            }
        }
        for (var i = 0; i < n - 2; i++) {
            y[yOff + 2 + i] += coef[2] * x[xOff + i];
        }
        y[yOff] = coef[0] * x[xOff] + coef[1] * state[stateOff] + coef[2] * state[stateOff + 1];
        y[yOff + 1] += coef[2] * state[stateOff];
        state[stateOff] = x[xOff + n - 1];
        state[stateOff + 1] = x[xOff + n - 2];
    }

    /**
     * Applies a third order moving average (FIR) filter whose memory sits in the three samples before the
     * window.
     *
     * <p>Computes {@code y[n] = sum(coef[i] * x[n - i], i = 0 .. 3)} reading the three history samples from
     * {@code x[xOff - 3 .. xOff - 1]} in the same backing array. The caller lays out the buffer so the prior
     * order samples precede the current window.
     *
     * @param x    the input buffer with three history samples before {@code xOff}
     * @param xOff the offset of the first filtered sample in {@code x}; at least three
     * @param n    the number of samples to filter
     * @param coef the four filter coefficients
     * @param y    the output buffer
     * @param yOff the offset of the first output sample in {@code y}
     */
    public static void ma3(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        for (var sample = 0; sample < n; sample++) {
            float res = 0;
            for (var i = 0; i < 4; i++) {
                res += coef[i] * x[xOff + sample - i];
            }
            y[yOff + sample] = res;
        }
    }

    /**
     * Applies a ninth order moving average (FIR) filter whose memory sits in the nine samples before the
     * window.
     *
     * <p>Computes {@code y[n] = sum(coef[i] * x[n - i], i = 0 .. 9)} reading the nine history samples from
     * {@code x[xOff - 9 .. xOff - 1]} in the same backing array.
     *
     * @param x    the input buffer with nine history samples before {@code xOff}
     * @param xOff the offset of the first filtered sample in {@code x}; at least nine
     * @param n    the number of samples to filter
     * @param coef the ten filter coefficients
     * @param y    the output buffer
     * @param yOff the offset of the first output sample in {@code y}
     */
    public static void ma9(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        for (var sample = 0; sample < n; sample++) {
            float res = 0;
            for (var i = 0; i < 10; i++) {
                res += coef[i] * x[xOff + sample - i];
            }
            y[yOff + sample] = res;
        }
    }

    /**
     * Applies a 16th order monic moving average (FIR) filter whose memory sits in the 16 samples before the
     * window.
     *
     * <p>Computes {@code y[n] = x[n] + sum(coef[i] * x[n - i], i = 1 .. 16)} with the leading tap taken as a
     * copy ({@code coef[0]} is the monic {@code 1.0f} and the accumulator is seeded with {@code x[n]}),
     * reading the 16 history samples from {@code x[xOff - 16 .. xOff - 1]} in the same backing array.
     *
     * @param x    the input buffer with 16 history samples before {@code xOff}
     * @param xOff the offset of the first filtered sample in {@code x}; at least 16
     * @param n    the number of samples to filter
     * @param coef the 17 filter coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param y    the output buffer
     * @param yOff the offset of the first output sample in {@code y}
     */
    public static void ma16Monic(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        for (var sample = 0; sample < n; sample++) {
            var res = x[xOff + sample];
            for (var i = 1; i < 17; i++) {
                res += coef[i] * x[xOff + sample - i];
            }
            y[yOff + sample] = res;
        }
    }

    /**
     * Applies a 16th order symmetric moving average (FIR) filter whose memory sits in the 16 samples before
     * the window.
     *
     * <p>Computes the center tap {@code coef[8] * x[n - 8]} then folds the eight symmetric tap pairs
     * {@code coef[i] * (x[n - i] + x[n - 16 + i])} for {@code i = 0 .. 7}, reading the 16 history samples from
     * {@code x[xOff - 16 .. xOff - 1]} in the same backing array. The accumulation order is the center tap
     * first, then the pairs in ascending {@code i}.
     *
     * @param x    the input buffer with 16 history samples before {@code xOff}
     * @param xOff the offset of the first filtered sample in {@code x}; at least 16
     * @param n    the number of samples to filter
     * @param coef the 17 filter coefficients, used symmetrically
     * @param y    the output buffer
     * @param yOff the offset of the first output sample in {@code y}
     */
    public static void ma16Sym(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        for (var sample = 0; sample < n; sample++) {
            var res = x[xOff + sample - 8] * coef[8];
            for (var i = 0; i < 8; i++) {
                res += coef[i] * (x[xOff + sample - i] + x[xOff + sample - 16 + i]);
            }
            y[yOff + sample] = res;
        }
    }

    /**
     * Applies a general moving average (FIR) filter of arbitrary order whose memory sits in the
     * {@code coefLen - 1} samples before the window.
     *
     * <p>Computes {@code y[n] = sum(coef[i] * x[n - i], i = 0 .. coefLen - 1)} reading the history samples
     * from {@code x[xOff - (coefLen - 1) .. xOff - 1]} in the same backing array. The leading tap is applied
     * first (a copy when {@code coef[0] == 1.0f}, otherwise a scale), then each remaining tap is accumulated
     * by a full sweep over the window in ascending {@code i}. Input and output windows must not alias.
     *
     * @param x       the input buffer with {@code coefLen - 1} history samples before {@code xOff}
     * @param xOff    the offset of the first filtered sample in {@code x}; at least {@code coefLen - 1}
     * @param n       the number of samples to filter
     * @param coef    the filter coefficients; length greater than one
     * @param coefLen the coefficient count
     * @param y       the output buffer
     * @param yOff    the offset of the first output sample in {@code y}
     */
    public static void ma(float[] x, int xOff, int n, float[] coef, int coefLen, float[] y, int yOff) {
        int i;
        if (coef[0] == 1.0f) {
            for (var sample = 0; sample < n; sample++) {
                y[yOff + sample] = x[xOff + sample] + coef[1] * x[xOff + sample - 1];
            }
            i = 2;
        } else {
            for (var sample = 0; sample < n; sample++) {
                y[yOff + sample] = x[xOff + sample] * coef[0];
            }
            i = 1;
        }
        for (; i < coefLen; i++) {
            var c = coef[i];
            for (var sample = 0; sample < n; sample++) {
                y[yOff + sample] += c * x[xOff + sample - i];
            }
        }
    }

    /**
     * Applies a monic first order autoregressive (IIR) filter.
     *
     * <p>Computes the recursion {@code y[n] = x[n] - coef[1] * y[n - 1]} with {@code coef[0]} the monic
     * {@code 1.0f}, taking {@code y[-1]} from the single element {@code state} vector and updating that state
     * to the last output sample. The recursion is unrolled five samples at a time using the closed form
     * powers of {@code ar1 = -coef[1]}; this five wide body and its remainder loop are reproduced term for
     * term because the unrolled form changes the float accumulation order. Stability requires
     * {@code |coef[1]| < 0.9999f}.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter
     * @param coef     the two filter coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param state    the single element filter memory vector, read for {@code y[-1]} and updated to
     *                 {@code y[N - 1]}
     * @param stateOff the offset of the state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    public static void ar1(float[] x, int xOff, int n, float[] coef, float[] state, int stateOff, float[] y, int yOff) {
        final var ar1 = -coef[1];
        final var ar1_2 = ar1 * ar1;
        final var ar1_3 = ar1 * ar1_2;
        final var ar1_4 = ar1 * ar1_3;
        final var ar1_5 = ar1 * ar1_4;
        var ytmp = state[stateOff];
        var sample = 0;
        for (; sample < n - 4; sample += 5) {
            var xtmp0 = x[xOff + sample];
            var xtmp1 = x[xOff + sample + 1];
            var xtmp2 = x[xOff + sample + 2];
            var xtmp3 = x[xOff + sample + 3];
            var xtmp4 = x[xOff + sample + 4];
            y[yOff + sample + 4] = xtmp4 + ar1 * xtmp3 + ar1_2 * xtmp2 + ar1_3 * xtmp1 + ar1_4 * xtmp0 + ar1_5 * ytmp;
            y[yOff + sample] = xtmp0 + ar1 * ytmp;
            y[yOff + sample + 1] = xtmp1 + ar1 * xtmp0 + ar1_2 * ytmp;
            y[yOff + sample + 2] = xtmp2 + ar1 * xtmp1 + ar1_2 * xtmp0 + ar1_3 * ytmp;
            y[yOff + sample + 3] = xtmp3 + ar1 * xtmp2 + ar1_2 * xtmp1 + ar1_3 * xtmp0 + ar1_4 * ytmp;
            ytmp = y[yOff + sample + 4];
        }
        for (; sample < n; sample++) {
            ytmp = x[xOff + sample] + ytmp * ar1;
            y[yOff + sample] = ytmp;
        }
        state[stateOff] = ytmp;
    }

    /**
     * Applies a monic second order autoregressive (IIR) filter.
     *
     * <p>Computes the recursion {@code y[n] = x[n] - coef[1] * y[n - 1] - coef[2] * y[n - 2]} with
     * {@code coef[0]} the monic {@code 1.0f}, taking {@code y[-1]} from {@code state[0]} and {@code y[-2]}
     * from {@code state[1]} and updating both to the last two output samples. The recursion is unrolled four
     * samples at a time using the impulse response coefficients {@code imp1 .. imp4} and the second pole
     * coefficients {@code ymp1 .. ymp4}; this four wide body and its remainder loop are reproduced exactly,
     * including the precise per output accumulation order, because that order changes the float result.
     * Stability requires {@code |coef[2]| < 0.9999f}.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be greater than one
     * @param coef     the three filter coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param state    the two element filter memory vector, {@code state[0]} is {@code y[-1]} and
     *                 {@code state[1]} is {@code y[-2]}, updated to the last two outputs
     * @param stateOff the offset of the first state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    public static void ar2(float[] x, int xOff, int n, float[] coef, float[] state, int stateOff, float[] y, int yOff) {
        var ytmp0 = state[stateOff + 1];
        var ytmp1 = state[stateOff];
        final var ar1 = -coef[1];
        final var ar2 = -coef[2];
        final var ar1_2 = ar1 * ar1;
        final var ar1_3 = ar1 * ar1_2;
        final var ar1_4 = ar1 * ar1_3;
        final var imp1 = ar1;
        final var imp2 = ar1_2 + ar2;
        final var imp3 = ar1_3 + 2 * ar1 * ar2;
        final var imp4 = ar1_4 + ar2 * ar2 + 3 * ar1_2 * ar2;
        final var ymp1 = ar2;
        final var ymp2 = ar2 * imp1;
        final var ymp3 = ar2 * imp2;
        final var ymp4 = ar2 * imp3;
        var sample = 0;
        for (; sample < n - 3; sample += 4) {
            var xtmp0 = x[xOff + sample];
            var xtmp1 = x[xOff + sample + 1];
            var xtmp2 = x[xOff + sample + 2];
            y[yOff + sample + 2] = xtmp2 + imp1 * xtmp1 + imp2 * xtmp0 + imp3 * ytmp1 + ymp3 * ytmp0;
            var xtmp3 = x[xOff + sample + 3];
            y[yOff + sample + 3] = xtmp3 + imp1 * xtmp2 + imp2 * xtmp1 + imp3 * xtmp0 + imp4 * ytmp1 + ymp4 * ytmp0;
            y[yOff + sample] = xtmp0 + imp1 * ytmp1 + ymp1 * ytmp0;
            y[yOff + sample + 1] = xtmp1 + imp1 * xtmp0 + imp2 * ytmp1 + ymp2 * ytmp0;
            ytmp0 = y[yOff + sample + 2];
            ytmp1 = y[yOff + sample + 3];
        }
        for (; sample < n; sample++) {
            y[yOff + sample] = x[xOff + sample] + ar1 * ytmp1 + ar2 * ytmp0;
            ytmp0 = ytmp1;
            ytmp1 = y[yOff + sample];
        }
        state[stateOff + 1] = ytmp0;
        state[stateOff] = ytmp1;
    }

    /**
     * Applies a monic fourth order autoregressive (IIR) filter whose memory sits in the four samples before
     * the output window.
     *
     * <p>Computes the recursion {@code y[n] = x[n] - sum(coef[4 - i] * y[n - 4 + i], i = 0 .. 3)} with
     * {@code coef[0]} the monic {@code 1.0f}, reading the four history outputs from {@code y[yOff - 4 .. yOff - 1]}
     * in the same backing array. A derived second coefficient set {@code coef2} is computed and the recursion
     * is unrolled two samples at a time so that the second of each pair can be expressed against inputs and
     * the already known history; this two wide body and its remainder loop are reproduced term for term.
     *
     * @param x    the input buffer
     * @param xOff the offset of the first input sample in {@code x}
     * @param n    the number of samples to filter
     * @param coef the five filter coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param y    the output buffer with four history outputs before {@code yOff}
     * @param yOff the offset of the first output sample in {@code y}; at least four
     */
    public static void ar4(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        var coef2 = new float[4];
        coef2[0] = coef[2] - coef[1] * coef[1];
        coef2[1] = coef[3] - coef[1] * coef[2];
        coef2[2] = coef[4] - coef[1] * coef[3];
        coef2[3] = -coef[1] * coef[4];
        var res0 = y[yOff - 2];
        var res1 = y[yOff - 1];
        var sample = 0;
        for (; sample < n - 1; sample += 2) {
            var tmp0 = x[xOff + sample] - coef[4] * y[yOff + sample - 4] - coef[3] * y[yOff + sample - 3] - coef[2] * res0 - coef[1] * res1;
            res1 = x[xOff + sample + 1] - coef[1] * x[xOff + sample] - coef2[3] * y[yOff + sample - 4] - coef2[2] * y[yOff + sample - 3] - coef2[1] * res0 - coef2[0] * res1;
            res0 = tmp0;
            y[yOff + sample] = res0;
            y[yOff + sample + 1] = res1;
        }
        for (; sample < n; sample++) {
            var res = x[xOff + sample];
            for (var i = 0; i < 4; i++) {
                res -= coef[4 - i] * y[yOff + sample - 4 + i];
            }
            y[yOff + sample] = res;
        }
    }

    /**
     * Applies the monic 16th order autoregressive (IIR) short term synthesis filter whose memory sits in the
     * 16 samples before the output window.
     *
     * <p>Computes the recursion {@code y[n] = x[n] - sum(coef[16 - i] * y[n - 16 + i], i = 0 .. 15)} with
     * {@code coef[0]} the monic {@code 1.0f}, reading the 16 history outputs from
     * {@code y[yOff - 16 .. yOff - 1]} in the same backing array. This is the core LPC synthesis filter of
     * the MLow decoder: it is applied once per subframe with that subframe's stabilized LPC coefficients, the
     * history being the tail of the previous subframe's synthesized output that the decoder threads through
     * its LPC synthesis memory. The recursion sums the 16 history taps from the oldest
     * ({@code coef[16] * y[n - 16]}) to the newest ({@code coef[1] * y[n - 1]}) and subtracts the running sum
     * from {@code x[n]}.
     *
     * @param x    the input buffer (the LPC residual, that is the excitation)
     * @param xOff the offset of the first input sample in {@code x}
     * @param n    the subframe length in samples
     * @param coef the 17 LPC coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param y    the output buffer with 16 history outputs before {@code yOff}
     * @param yOff the offset of the first output sample in {@code y}; at least 16
     */
    public static void ar16(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        for (var sample = 0; sample < n; sample++) {
            var res = x[xOff + sample];
            for (var i = 0; i < 16; i++) {
                res -= coef[16 - i] * y[yOff + sample - 16 + i];
            }
            y[yOff + sample] = res;
        }
    }

    /**
     * Applies a first order ARMA (pole zero) filter, the cascade of {@link #ma1} into {@link #ar1}.
     *
     * <p>Runs the first order MA numerator (which need not be monic) followed by the monic first order AR
     * denominator, threading a two element state vector: {@code state[stateOff]} is the MA memory and
     * {@code state[stateOff + 1]} is the AR memory (the MA stage uses element zero and the AR stage element
     * one). The MLow decoder uses this for the unvoiced pulse shaping filter and for the unvoiced noise high
     * pass shaping inside the noise generator. The decode call sites pass distinct input and output buffers,
     * so the in place path runs directly into {@code y}; only when input and output alias does the filter
     * stage through a temporary buffer.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be positive
     * @param coefMa   the two MA (numerator) coefficients
     * @param coefAr   the two AR (denominator) coefficients; {@code coefAr[0]} is the monic {@code 1.0f}
     * @param state    the two element filter memory vector, element zero the MA memory and element one the AR
     *                 memory
     * @param stateOff the offset of the first state element in {@code state}
     */
    public static void arma1(float[] x, int xOff, int n, float[] coefMa, float[] coefAr, float[] state, int stateOff) {
        arma1(x, xOff, n, coefMa, coefAr, state, stateOff, x, xOff);
    }

    /**
     * Applies a first order ARMA (pole zero) filter into a separate output buffer.
     *
     * <p>Identical contract to {@link #arma1(float[], int, int, float[], float[], float[], int)} but writing
     * to {@code y} at {@code yOff}. When {@code y}/{@code yOff} equals {@code x}/{@code xOff} the temporary
     * buffer path runs (the MA stage writes a scratch buffer, the AR stage writes back into {@code y}); when
     * they differ the MA stage writes {@code y} directly and the AR stage filters in place. The intermediate
     * buffer must hold at least {@code n} samples in the aliasing case, bounded by
     * {@link #MAX_FILTER_INPUT_SAMPLES}.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be positive
     * @param coefMa   the two MA (numerator) coefficients
     * @param coefAr   the two AR (denominator) coefficients; {@code coefAr[0]} is the monic {@code 1.0f}
     * @param state    the two element filter memory vector, element zero the MA memory and element one the AR
     *                 memory
     * @param stateOff the offset of the first state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    public static void arma1(float[] x, int xOff, int n, float[] coefMa, float[] coefAr, float[] state, int stateOff, float[] y, int yOff) {
        float[] yPtr;
        int yPtrOff;
        if (x == y && xOff == yOff) {
            yPtr = new float[MAX_FILTER_INPUT_SAMPLES];
            yPtrOff = 0;
        } else {
            yPtr = y;
            yPtrOff = yOff;
        }
        ma1(x, xOff, n, coefMa, state, stateOff, yPtr, yPtrOff);
        ar1(yPtr, yPtrOff, n, coefAr, state, stateOff + 1, y, yOff);
    }

    /**
     * Applies a second order ARMA (pole zero) filter, the cascade of {@link #ma2} into {@link #ar2}.
     *
     * <p>Runs the second order MA numerator (which need not be monic) followed by the monic second order AR
     * denominator, threading a four element state vector: {@code state[stateOff .. stateOff + 1]} is the MA
     * memory and {@code state[stateOff + 2 .. stateOff + 3]} is the AR memory. The MLow decoder uses this for
     * the frame level high pass postfilter applied to the synthesized signal. The decode call site filters in
     * place ({@code y == x}), so the temporary buffer path is exercised.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be greater than one
     * @param coefMa   the three MA (numerator) coefficients
     * @param coefAr   the three AR (denominator) coefficients; {@code coefAr[0]} is the monic {@code 1.0f}
     * @param state    the four element filter memory vector, the first two the MA memory and the last two the
     *                 AR memory
     * @param stateOff the offset of the first state element in {@code state}
     */
    public static void arma2(float[] x, int xOff, int n, float[] coefMa, float[] coefAr, float[] state, int stateOff) {
        arma2(x, xOff, n, coefMa, coefAr, state, stateOff, x, xOff);
    }

    /**
     * Applies a second order ARMA (pole zero) filter into a separate output buffer.
     *
     * <p>Identical contract to {@link #arma2(float[], int, int, float[], float[], float[], int)} but writing
     * to {@code y} at {@code yOff}. When {@code y}/{@code yOff} equals {@code x}/{@code xOff} the temporary
     * buffer path runs; when they differ the MA stage writes {@code y} directly and the AR stage filters in
     * place.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be greater than one
     * @param coefMa   the three MA (numerator) coefficients
     * @param coefAr   the three AR (denominator) coefficients; {@code coefAr[0]} is the monic {@code 1.0f}
     * @param state    the four element filter memory vector, the first two the MA memory and the last two the
     *                 AR memory
     * @param stateOff the offset of the first state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    public static void arma2(float[] x, int xOff, int n, float[] coefMa, float[] coefAr, float[] state, int stateOff, float[] y, int yOff) {
        float[] yPtr;
        int yPtrOff;
        if (x == y && xOff == yOff) {
            yPtr = new float[MAX_FILTER_INPUT_SAMPLES];
            yPtrOff = 0;
        } else {
            yPtr = y;
            yPtrOff = yOff;
        }
        ma2(x, xOff, n, coefMa, state, stateOff, yPtr, yPtrOff);
        ar2(yPtr, yPtrOff, n, coefAr, state, stateOff + 2, y, yOff);
    }

    /**
     * Applies a monic second order allpass filter.
     *
     * <p>An allpass section of denominator {@code {1, coef[1], coef[2]}} has the reversed numerator
     * {@code {coef[2], coef[1], 1}}; this filter forms that MA numerator and cascades a second order MA into
     * a second order AR, threading a four element state vector exactly as
     * {@link #arma2(float[], int, int, float[], float[], float[], int, float[], int)} does (the first two
     * elements the MA memory, the last two the AR memory). It requires an even sample count and filters in
     * place, running the MA into {@code y} then the AR over {@code y}.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be even
     * @param coef     the three denominator coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param state    the four element filter memory vector, the first two the MA memory and the last two the
     *                 AR memory
     * @param stateOff the offset of the first state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    public static void allpass2(float[] x, int xOff, int n, float[] coef, float[] state, int stateOff, float[] y, int yOff) {
        var coefMa = new float[]{coef[2], coef[1], 1.0f};
        ma2(x, xOff, n, coefMa, state, stateOff, y, yOff);
        ar2(y, yOff, n, coef, state, stateOff + 2, y, yOff);
    }

    /**
     * Applies the stateless symmetric fractional delay interpolation FIR.
     *
     * <p>For each output sample folds the eight symmetric tap pairs
     * {@code (x[n + i] + x[n + 15 - i]) * kernel[i]} for {@code i = 0 .. 7} over the 16 tap window starting at
     * {@code x[n]}, producing the half sample delayed interpolation the CELP long term prediction basis uses
     * when the pitch lag has a subsample fractional part. The input window must extend {@code N + 15} samples
     * from {@code xOff}; the function reads no state.
     *
     * @param x      the input buffer; readable over {@code [xOff, xOff + N + 15)}
     * @param xOff   the offset of the first window sample in {@code x}
     * @param y      the output buffer
     * @param yOff   the offset of the first output sample in {@code y}
     * @param n      the number of output samples to produce
     * @param kernel the 16 tap symmetric interpolation kernel
     */
    public static void interpol(float[] x, int xOff, float[] y, int yOff, int n, float[] kernel) {
        for (var sample = 0; sample < n; sample++) {
            var ret = 0.0f;
            for (var i = 0; i < 8; i++) {
                ret += (x[xOff + sample + i] + x[xOff + sample + 15 - i]) * kernel[i];
            }
            y[yOff + sample] = ret;
        }
    }
}
