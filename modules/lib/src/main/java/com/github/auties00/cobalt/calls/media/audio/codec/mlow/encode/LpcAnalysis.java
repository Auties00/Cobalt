package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.dsp.Pffft;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.A2nlsfBridge;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Short term linear prediction analysis front end of the MLow speech encoder.
 *
 * <p>This stage turns one windowed frame of high pass filtered 16 kHz speech into the line spectral
 * frequency vector that the quantizer searches and into the bandwidth expanded prediction filter the rest
 * of the encoder synthesizes against. The chain, in order:
 * <ul>
 *   <li>apply the analysis window to the {@value #LPC_BUF_LEN_20MS}-sample look back buffer
 *       ({@link #window(float[], int, boolean, float[])});</li>
 *   <li>take the {@value #NFFT}-point real forward FFT of the zero padded windowed buffer and form the
 *       power spectrum {@code F2};</li>
 *   <li>recover the first {@value #LPC_ORDER}{@code  + 1} autocorrelation lags from {@code F2} by the
 *       brute force inverse DCT;</li>
 *   <li>run the Levinson reflection coefficient recursion on the regularized autocorrelation and convert the
 *       reflection coefficients to the monic prediction filter;</li>
 *   <li>bandwidth expand the filter by {@value #LPC_BWE} per lag;</li>
 *   <li>convert the expanded filter to normalized line spectral frequencies through the SILK integer core
 *       ({@link A2nlsfBridge#a2nlsf(float[])}).</li>
 * </ul>
 *
 * <p>Float reproducibility. The line spectral frequencies feed a rate distortion vector quantizer whose
 * survivor search is sensitive to the least significant float bit; a single differing rounding upstream can
 * flip a stage 1 or stage 2 index and desynchronize the bitstream. For that reason every step is ordered to
 * be bit exact: the FFT is {@link Pffft} (the bit faithful SSE port), the brute force DCT and the reflection
 * recursion run in {@code double}, the power spectrum and autocorrelation are formed in a fixed loop order,
 * and the integer NLSF core is bit exact.
 *
 * <p>The brute force DCT reduction and every cosine table dot product accumulate even indexed terms into one
 * running sum and odd indexed terms into another, summed once at the end, reproduced by
 * {@link #dotPaired(double[], int, double[])}; a plain scalar left to right sum rounds differently and,
 * through the near singular reflection recursion on resonant frames, would flip a quantizer index. The
 * reflection recursion regularization is applied in {@code double}. The cosine and window tables are the
 * captured immutable constants in {@link LpcAnalysisTables}.
 *
 * <p>Scope is the SMPL 16 kHz / 60 ms / mono configuration with 20 ms frames: the look back buffer is
 * {@value #LPC_BUF_LEN_20MS} samples, the FFT is {@value #NFFT} points, and the prediction order is
 * {@value #LPC_ORDER}. The window and cosine tables are the shared immutable constants of
 * {@link LpcAnalysisTables}; a {@link LpcAnalysis} instance additionally owns one {@link Pffft} setup, is
 * immutable after construction, and lets the analysis methods allocate their own scratch, so a single
 * instance may be shared across concurrent frames.
 *
 * @implNote This implementation forms the two wide pairwise reduction of
 * {@link #dotPaired(double[], int, double[])} and the {@code double} regularization in
 * {@link #ac2rc(double[], int, double)} as the load bearing details for bit exact reproduction of the
 * reference arithmetic; the per element power spectrum fold and the {@code float} {@link #rc2a(float[], int,
 * float[])} are order independent.
 */
public final class LpcAnalysis {
    /**
     * The logger for {@link LpcAnalysis}.
     */
    private static final System.Logger LOGGER = Log.get(LpcAnalysis.class);

    /**
     * Linear prediction order of the MLow short term filter.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Length of the real forward FFT used for the spectral autocorrelation.
     */
    private static final int NFFT = 512;

    /**
     * Length of the 20 ms frame analysis look back buffer fed to the window.
     */
    private static final int LPC_BUF_LEN_20MS = 448;

    /**
     * Autocorrelation regularization added to lag zero before the reflection recursion.
     */
    private static final float LPC_REG = 5e-7f;

    /**
     * Per lag bandwidth expansion factor applied to the analysis filter.
     */
    private static final float LPC_BWE = 0.9999f;

    /**
     * Length of the leading sine taper window segment for a 20 ms frame.
     */
    private static final int LPC_WIN1_20MS_LEN = 264;

    /**
     * Length of the short trailing cosine taper segment ({@code 2} ms).
     */
    private static final int WIN3_SHORT_LEN = 32;

    /**
     * Length of the long trailing cosine taper segment ({@code 4} ms).
     */
    private static final int WIN3_LONG_LEN = 64;

    /**
     * Number of {@code double} entries in one brute force DCT cosine row, {@value #NFFT}{@code / 4}.
     */
    private static final int DCT_ROW_LEN = NFFT / 4;

    /**
     * Index of the first {@code Csumdiff} row within the flat {@link LpcAnalysisTables#DCT} table.
     *
     * <p>The eight {@code Cdif} rows (for the odd lags) precede the four {@code Csumdiff} rows.
     */
    private static final int CSUMDIFF_ROW0 = LPC_ORDER / 2;

    /**
     * Index of the first {@code Csumsum} row within the flat {@link LpcAnalysisTables#DCT} table.
     *
     * <p>The eight {@code Cdif} rows and the four {@code Csumdiff} rows precede the four {@code Csumsum} rows.
     */
    private static final int CSUMSUM_ROW0 = LPC_ORDER / 2 + LPC_ORDER / 4;

    /**
     * Real forward FFT setup for the {@value #NFFT}-point spectral analysis.
     */
    private final Pffft fft;

    /**
     * Result of one short term analysis: the raw and bandwidth expanded prediction filters, the line
     * spectral frequencies of the expanded filter, and the autocorrelation lags.
     *
     * <p>The two filters are monic ({@code a[0] == 1}) with {@value #LPC_ORDER} taps following.
     * {@code lpcRaw} is the filter straight out of the reflection recursion; {@code lpc} is that filter
     * after the per lag {@value #LPC_BWE} bandwidth expansion and is the one the quantizer and synthesis
     * consume. {@code lsf} is the line spectral frequency vector of {@code lpc} in radians within
     * {@code (0, PI)}. {@code autocorr} is the regularization free autocorrelation that the brute force DCT
     * recovered, retained for the perceptual and validation paths. {@code f2} is the magnitude squared power
     * spectrum ({@value #NFFT}{@code / 2 + 1} bins) formed by the single forward FFT this analysis already
     * runs, emitted as a byproduct so callers that need the same spectrum (the pitch estimator and the signal
     * mode classifier) do not re transform the identical windowed buffer.
     *
     * @param lpcRaw   the monic analysis filter before bandwidth expansion, {@value #LPC_ORDER}{@code  + 1}
     *                 entries
     * @param lpc      the bandwidth expanded monic analysis filter, {@value #LPC_ORDER}{@code  + 1} entries
     * @param lsf      the line spectral frequencies of {@code lpc}, {@value #LPC_ORDER} entries in radians
     * @param autocorr the recovered autocorrelation lags, {@value #LPC_ORDER}{@code  + 1} entries
     * @param f2       the magnitude squared power spectrum, {@value #NFFT}{@code / 2 + 1} bins, identical to
     *                 {@link #powerSpectrum(float[], int)} on the same windowed buffer
     */
    public record Result(float[] lpcRaw, float[] lpc, float[] lsf, double[] autocorr, float[] f2) {
    }

    /**
     * Builds the analysis front end, allocating the {@value #NFFT}-point real FFT setup.
     *
     * <p>The window and brute force DCT cosine tables are the captured immutable constants in
     * {@link LpcAnalysisTables}; only the FFT twiddle setup is computed here, by {@link Pffft}.
     */
    public LpcAnalysis() {
        this.fft = new Pffft(NFFT, Pffft.REAL);
    }

    /**
     * Applies the analysis window to a 20 ms frame look back buffer.
     *
     * <p>The leading {@value #LPC_WIN1_20MS_LEN} samples are multiplied by the sine taper, the middle samples
     * are copied verbatim, and the trailing {@value #WIN3_LONG_LEN} samples are multiplied by the cosine
     * taper; when {@code longWin} is {@code false} the short taper is used and the remaining trailing samples
     * are zeroed. The tapers are {@link LpcAnalysisTables#WIN1_20MS}, {@link LpcAnalysisTables#WIN3_LONG} and
     * {@link LpcAnalysisTables#WIN3_SHORT}. The output buffer must hold at least {@value #LPC_BUF_LEN_20MS}
     * samples.
     *
     * @param in      the look back buffer, at least {@value #LPC_BUF_LEN_20MS} samples from {@code inOff}
     * @param inOff   the offset of the first windowed sample in {@code in}
     * @param longWin {@code true} to use the long trailing taper (more frames follow in the packet),
     *                {@code false} for the short taper with trailing zeros
     * @param out     the destination for the {@value #LPC_BUF_LEN_20MS}-sample windowed buffer
     */
    public void window(float[] in, int inOff, boolean longWin, float[] out) {
        var len = LPC_BUF_LEN_20MS;
        var win1 = LpcAnalysisTables.WIN1_20MS;
        for (var i = 0; i < LPC_WIN1_20MS_LEN; i++) {
            out[i] = in[inOff + i] * win1[i];
        }
        var midCount = len - LPC_WIN1_20MS_LEN - WIN3_LONG_LEN;
        System.arraycopy(in, inOff + LPC_WIN1_20MS_LEN, out, LPC_WIN1_20MS_LEN, midCount);

        var win3len = longWin ? WIN3_LONG_LEN : WIN3_SHORT_LEN;
        var win3 = longWin ? LpcAnalysisTables.WIN3_LONG : LpcAnalysisTables.WIN3_SHORT;
        var tailStart = len - WIN3_LONG_LEN;
        for (var i = 0; i < win3len; i++) {
            out[tailStart + i] = in[inOff + tailStart + i] * win3[i];
        }
        if (!longWin) {
            var zStart = len - WIN3_LONG_LEN + WIN3_SHORT_LEN;
            for (var i = zStart; i < len; i++) {
                out[i] = 0.0f;
            }
        }
    }

    /**
     * Runs the full short term analysis on a windowed frame, producing the prediction filters and the line
     * spectral frequencies.
     *
     * <p>The {@code windowed} buffer is zero padded to {@value #NFFT} samples, transformed, reduced to a
     * power spectrum, and brute force DCT'd to the autocorrelation; the reflection recursion and the monic
     * conversion recover the prediction filter; bandwidth expansion and the integer NLSF core complete the
     * chain. The input buffer is not modified.
     *
     * @param windowed the windowed look back buffer of {@value #LPC_BUF_LEN_20MS} samples
     * @return the raw and bandwidth expanded filters, the line spectral frequencies, and the autocorrelation
     */
    public Result analyze(float[] windowed) {
        return analyze(windowed, LPC_BUF_LEN_20MS);
    }

    /**
     * Runs the full short term analysis on the first {@code len} samples of a windowed buffer.
     *
     * <p>This is the length parameterized form of {@link #analyze(float[])} for callers that window a buffer
     * shorter than {@value #LPC_BUF_LEN_20MS}; {@code len} must not exceed {@value #NFFT}.
     *
     * @param windowed the windowed buffer
     * @param len      the number of valid samples in {@code windowed}, at most {@value #NFFT}
     * @return the raw and bandwidth expanded filters, the line spectral frequencies, and the autocorrelation
     */
    public Result analyze(float[] windowed, int len) {
        var f2 = powerSpectrum(windowed, len);
        var r = bruteDctFromPowerSpectrum(f2);

        var lpcRaw = new float[LPC_ORDER + 1];
        var rc = ac2rc(r, LPC_ORDER, LPC_REG);
        rc2a(rc, LPC_ORDER, lpcRaw);

        var lpc = lpcRaw.clone();
        bweExpand(lpc, LPC_ORDER, LPC_BWE);

        var lsf = A2nlsfBridge.a2nlsf(lpc);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "lpc analysis: samples={0}", len);
        }
        return new Result(lpcRaw, lpc, lsf, r, f2);
    }

    /**
     * Computes the magnitude squared power spectrum of a windowed buffer.
     *
     * <p>The buffer is zero padded to {@value #NFFT}, forward transformed, and folded into the half spectrum
     * {@code F2[i]} for {@code i} in {@code [0, }{@value #NFFT}{@code / 2]}: bin zero is {@code F[0]^2}, the
     * Nyquist bin is {@code F[1]^2}, and bin {@code i} is {@code F[2i]^2 + F[2i+1]^2}. The result is
     * {@value #NFFT}{@code / 2 + 1} single precision bins. This is the spectrum {@link #analyze(float[], int)}
     * runs once and returns as {@link Result#f2()} for the autocorrelation and for the pitch estimator and
     * signal mode classifier to read directly.
     *
     * @param windowed the windowed look back buffer
     * @param len      the number of valid samples in {@code windowed}, at most {@value #NFFT}
     * @return the {@value #NFFT}{@code / 2 + 1}-bin magnitude squared power spectrum
     */
    public float[] powerSpectrum(float[] windowed, int len) {
        var xBuf = new float[NFFT];
        System.arraycopy(windowed, 0, xBuf, 0, len);
        var f = new float[NFFT];
        fft.transformOrdered(xBuf, f, null, true);
        var f2 = new float[NFFT / 2 + 1];
        f2[0] = f[0] * f[0];
        f2[NFFT / 2] = f[1] * f[1];
        for (var i = 1; i < NFFT / 2; i++) {
            f2[i] = f[2 * i] * f[2 * i] + f[2 * i + 1] * f[2 * i + 1];
        }
        return f2;
    }

    /**
     * Recovers the first {@value #LPC_ORDER}{@code  + 1} autocorrelation lags from an already computed power
     * spectrum through the spectral brute force DCT.
     *
     * <p>The single precision power spectrum {@link #powerSpectrum(float[], int)} produced from the one
     * forward FFT is widened to {@code double} bin by bin and combined with the precomputed cosine tables to
     * yield the autocorrelation. The widening {@code (double) f2[i]} is bit identical to forming the spectrum
     * directly in {@code double} because each bin was already rounded to a {@code float} by the fold, so a
     * single FFT feeds both the power spectrum byproduct and the autocorrelation.
     *
     * @param f2 the single precision power spectrum, {@value #NFFT}{@code / 2 + 1} bins
     * @return the autocorrelation lags zero through {@value #LPC_ORDER} in {@code double}
     */
    private static double[] bruteDctFromPowerSpectrum(float[] f2) {
        var f2d = new double[NFFT / 2 + 1];
        for (var i = 0; i < f2d.length; i++) {
            f2d[i] = f2[i];
        }
        return bruteDct(f2d);
    }

    /**
     * Folds the power spectrum and applies the precomputed cosine tables to recover the autocorrelation.
     *
     * <p>Lag zero is the spectral mean correction; odd lags use the eight {@code Cdif} rows, the
     * {@code 2 + 4 * j} lags use the four {@code Csumdiff} rows, and the {@code 4 + 4 * j} lags use the four
     * {@code Csumsum} rows, all packed in order in {@link LpcAnalysisTables#DCT} with {@value #DCT_ROW_LEN}
     * entries per row.
     *
     * <p>The {@code f2sum} reduction and every cosine table dot product accumulate even indexed terms into one
     * running sum and odd indexed terms into another, summed once at the end. This pairwise reduction is
     * reproduced by {@link #dotPaired(double[], int, double[])} and by the paired {@code f2sum} accumulation
     * here, because a plain left to right sum rounds differently and, through the ill conditioned reflection
     * recursion that follows, would move a line spectral frequency root enough to flip a quantizer index. The
     * per element fold arrays ({@code f2dif}, {@code f2sumsum}, {@code f2sumdif}) have no cross element
     * accumulation, so their order does not matter.
     *
     * @param f2 the {@value #NFFT}{@code  / 2 + 1}-bin power spectrum in {@code double}
     * @return the autocorrelation lags zero through {@value #LPC_ORDER}
     */
    private static double[] bruteDct(double[] f2) {
        var quarter = NFFT / 4;
        var half = NFFT / 2;
        var dct = LpcAnalysisTables.DCT;
        var f2dif = new double[quarter];
        var f2sumsum = new double[quarter];
        var f2sumdif = new double[quarter];
        var sumEven = 0.0;
        var sumOdd = 0.0;
        for (var n = 0; n < quarter; n += 2) {
            sumEven += f2[n] + f2[quarter + n];
            sumOdd += f2[n + 1] + f2[quarter + n + 1];
            f2dif[n] = f2[n] - f2[half - n];
            f2dif[n + 1] = f2[n + 1] - f2[half - n - 1];
            f2sumsum[n] = f2[n] + f2[half - n] + f2[quarter + n] + f2[quarter - n];
            f2sumsum[n + 1] = f2[n + 1] + f2[half - n - 1] + f2[quarter + n + 1] + f2[quarter - n - 1];
            f2sumdif[n] = f2[n] + f2[half - n] - f2[quarter + n] - f2[quarter - n];
            f2sumdif[n + 1] = f2[n + 1] + f2[half - n - 1] - f2[quarter + n + 1] - f2[quarter - n - 1];
        }
        var f2sum = sumEven + sumOdd;
        f2dif[0] *= 0.5;

        var r = new double[LPC_ORDER + 1];
        r[0] = (2.0 * f2sum - f2[0] + f2[half]) / NFFT;
        for (var j = 0; j < LPC_ORDER / 2; j++) {
            r[1 + j * 2] = dotPaired(dct, j * DCT_ROW_LEN, f2dif);
        }
        for (var j = 0; j < LPC_ORDER / 4; j++) {
            r[2 + j * 4] = dotPaired(dct, (CSUMDIFF_ROW0 + j) * DCT_ROW_LEN, f2sumdif);
        }
        for (var j = 0; j < LPC_ORDER / 4; j++) {
            r[4 + j * 4] = dotPaired(dct, (CSUMSUM_ROW0 + j) * DCT_ROW_LEN, f2sumsum);
        }
        return r;
    }

    /**
     * Computes a {@value #DCT_ROW_LEN}-term cosine table dot product with the two wide pairwise reduction
     * required for bit exact reproduction.
     *
     * <p>Even indexed products accumulate into one running sum and odd indexed products into a second; the two
     * sums are added once at the end. A scalar left to right sum does not reproduce this bit for bit.
     *
     * @param tab  the flat cosine table
     * @param base the offset of the row within {@code tab}
     * @param x    the folded power spectrum vector of {@value #DCT_ROW_LEN} entries
     * @return the dot product of the row and {@code x}
     */
    private static double dotPaired(double[] tab, int base, double[] x) {
        var accEven = 0.0;
        var accOdd = 0.0;
        for (var k = 0; k < x.length; k += 2) {
            accEven += tab[base + k] * x[k];
            accOdd += tab[base + k + 1] * x[k + 1];
        }
        return accEven + accOdd;
    }

    /**
     * Converts a regularized autocorrelation to reflection coefficients.
     *
     * <p>Lag zero is scaled by {@code 1 + reg} before the recursion. The recursion runs in {@code double};
     * if a reflection magnitude would exceed one the coefficient is clamped to plus or minus one and the
     * recursion stops. The returned coefficients are {@code float}.
     *
     * <p>The regularization is applied in {@code double}: {@code reg} is a {@code double}, so
     * {@link #LPC_REG} is widened from {@code float} before {@code 1.0 + reg}. Reproducing that widening
     * matters because the reflection recursion is near singular on resonant frames, where the leading
     * reflection coefficient sits within one unit in the last place of one; computing {@code 1 + reg} in
     * {@code float} instead would shift the lag zero scale by a single bit and that bit would then amplify
     * through the recursion into a line spectral frequency index flip.
     *
     * @param corr  the autocorrelation lags zero through {@code order} in {@code double}
     * @param order the prediction order
     * @param reg   the lag zero regularization fraction, widened to {@code double} before use
     * @return the {@code order} reflection coefficients in {@code float}
     */
    private static float[] ac2rc(double[] corr, int order, double reg) {
        var c0 = new double[order + 1];
        var c1 = new double[order + 1];
        System.arraycopy(corr, 0, c0, 0, order + 1);
        c0[0] *= (1.0 + reg);
        System.arraycopy(c0, 0, c1, 0, order + 1);

        var rc = new float[order];
        for (var k = 0; k < order; k++) {
            if (c0[k + 1] > c1[0]) {
                rc[k] = -1.0f;
                break;
            }
            if (c0[k + 1] < -c1[0]) {
                rc[k] = 1.0f;
                break;
            }
            if (c1[0] == 0.0) {
                break;
            }
            var rcTmp = -c0[k + 1] / c1[0];
            rc[k] = (float) rcTmp;
            for (var n = 0; n < (order - k); n++) {
                var cTmp1 = c0[n + k + 1];
                var cTmp2 = c1[n];
                c0[n + k + 1] = cTmp1 + cTmp2 * rcTmp;
                c1[n] = cTmp2 + cTmp1 * rcTmp;
            }
        }
        return rc;
    }

    /**
     * Converts reflection coefficients to the monic prediction filter.
     *
     * <p>Builds {@code a[0] == 1} and the {@code order} taps by the in place Levinson step in {@code float},
     * iterating the symmetric butterfly over each half of the partial filter.
     *
     * @param rc    the reflection coefficients
     * @param order the prediction order
     * @param a     the destination monic filter of at least {@code order + 1} entries
     */
    private static void rc2a(float[] rc, int order, float[] a) {
        for (var i = 1; i <= order; i++) {
            a[i] = 0.0f;
        }
        a[0] = 1.0f;
        for (var k = 0; k < order; k++) {
            var rcTmp = rc[k];
            for (var n = 0; n < (k + 1) / 2; n++) {
                var tmp1 = a[n + 1];
                var tmp2 = a[k - n];
                a[n + 1] = tmp1 + tmp2 * rcTmp;
                a[k - n] = tmp2 + tmp1 * rcTmp;
            }
            a[k + 1] = rcTmp;
        }
    }

    /**
     * Bandwidth expands a monic prediction filter in place.
     *
     * <p>Multiplies tap {@code i} by {@code bwe^i} for {@code i} in {@code [1, order]} using a running
     * product. A non positive {@code bwe} zeros every tap.
     *
     * @param a     the monic filter to expand in place, at least {@code order + 1} entries
     * @param order the prediction order
     * @param bwe   the per lag expansion factor
     */
    private static void bweExpand(float[] a, int order, float bwe) {
        if (bwe <= 0.0f) {
            for (var i = 1; i <= order; i++) {
                a[i] = 0.0f;
            }
            return;
        }
        var c = bwe;
        for (var i = 1; i < order + 1; i++) {
            a[i] *= c;
            c *= bwe;
        }
    }

    /**
     * Maximum perceptual weighting response length; the size of the response buffer.
     */
    private static final int MAX_L_RESP = 33;

    /**
     * Computes the perceptual weighting prediction filter from a perceptual autocorrelation.
     *
     * <p>The perceptual analysis by synthesis search weights its distortion by a short prediction filter
     * derived from the smoothed perceptual autocorrelation. This method applies the second order
     * emphasis moving average filter (taps {@code {emph, 1 + emph*emph, emph}}, seeded from {@code r[0]} and
     * {@code r[1]}) to lags {@code r[1 ...]} through {@link Filters#ma2(float[], int, int, float[], float[],
     * int, float[], int)}, runs the reflection recursion on the resulting length {@code (respLen - 1)}
     * autocorrelation, and converts the reflection coefficients to the monic filter. The reflection recursion
     * shares the bit exact {@code double} core of {@link #ac2rc(double[], int, double)}.
     *
     * @param r       the perceptual autocorrelation, at least {@code respLen + 1} entries
     * @param emph    the perceptual emphasis coefficient (voiced or unvoiced, per rate)
     * @param respLen the perceptual response length, the order of the returned filter plus one
     * @param reg     the lag zero regularization fraction
     * @return the monic perceptual weighting filter of {@code respLen} entries
     */
    public static float[] percAc2a(float[] r, float emph, int respLen, float reg) {
        var b = new float[]{emph, 1.0f + emph * emph, emph};
        var state = new float[]{r[0], r[1]};
        var rEmph = new float[MAX_L_RESP];
        Filters.ma2(r, 1, respLen, b, state, 0, rEmph, 0);

        var rc = ac2rcFloat(rEmph, respLen - 1, reg);
        var a = new float[MAX_L_RESP];
        rc2a(rc, respLen - 1, a);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "perceptual weighting filter: emph={0} respLen={1}", emph, respLen);
        }
        return a;
    }

    /**
     * Converts a {@code float} autocorrelation to reflection coefficients by widening to {@code double}.
     *
     * <p>Widens {@code corr} and the regularization to {@code double} and defers to
     * {@link #ac2rc(double[], int, double)}.
     *
     * @param corr  the autocorrelation lags zero through {@code order} in {@code float}
     * @param order the prediction order
     * @param reg   the lag zero regularization fraction
     * @return the {@code order} reflection coefficients in {@code float}
     */
    private static float[] ac2rcFloat(float[] corr, int order, float reg) {
        var corrDbl = new double[order + 1];
        for (var i = 0; i < order + 1; i++) {
            corrDbl[i] = corr[i];
        }
        return ac2rc(corrDbl, order, reg);
    }
}
