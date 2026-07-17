package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.dsp.Pffft;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Perceptual weighting spectral model of the MLow speech encoder.
 *
 * <p>The analysis by synthesis search of the CELP core ({@link AcbSearch} adaptive codebook and
 * {@link FcbSearch} fixed codebook stages) does not minimize raw waveform error; it minimizes a
 * perceptually weighted error, so that quantization noise is shaped to follow the masking envelope of the
 * speech spectrum and stays below audibility. This model produces the smoothed perceptual autocorrelation
 * that {@link LpcAnalysis#percAc2a(float[], float, int, float)} then turns into the short weighting filter
 * the search weights its target and synthesis against. One call covers one analysis span (a 5 ms subframe
 * pair, a {@code 2 * subfrlen + shorter} sample look) and updates the rolling look back buffer the model
 * keeps across calls.
 *
 * <p>The model is a spectral autocorrelation smoother. For each call the chain is:
 * <ul>
 *   <li>shift the {@value #NFFT} sample rolling buffer left by {@code xsubfrLen - 32} samples and append the
 *       new {@code xsubfr} span, so the buffer ends with the most recent analysis look ({@link #model});</li>
 *   <li>apply the perceptual analysis window (leading sine taper {@link #PERC_WIN1_20MS}, verbatim middle,
 *       trailing cosine taper {@link LpcAnalysisTables#WIN3_LONG} or {@link LpcAnalysisTables#WIN3_SHORT})
 *       over the full {@value #NFFT} sample window ({@link #window});</li>
 *   <li>take the {@value #NFFT} point real forward FFT ({@link Pffft#transformOrdered}) and form the power
 *       spectrum in place;</li>
 *   <li>smooth the power spectrum forward then backward across frequency with the per bin one pole
 *       coefficients {@link #SMTH_COEF}, which widen the masking skirt toward higher frequencies
 *       ({@link #smoothFilter});</li>
 *   <li>take the {@value #NFFT} point real backward FFT of the smoothed spectrum, scaling by
 *       {@code 1 / }{@value #NFFT}, to recover the smoothed autocorrelation.</li>
 * </ul>
 *
 * <p>Float reproducibility matters here. The smoothed autocorrelation feeds the perceptual weighting filter
 * that scales every analysis by synthesis distortion, so a differing rounding would move the weighted target
 * and could flip an adaptive codebook or fixed codebook survivor and desynchronize the bitstream. The power
 * spectrum fold, the forward then backward smoother, and the final scale are strict left to right single
 * precision and are transcribed exactly. The only reassociation sensitive arithmetic is inside the FFT, the
 * bit faithful {@link Pffft} port, used here for both the forward and the backward ordered transform. The
 * window and smoothing tables are captured as exact float constants because the reference transcendental
 * functions are not bit reproducible by the JDK; {@link #PERC_WIN1_20MS} is the perceptual sine window, the
 * trailing cosine tapers are shared with {@link LpcAnalysisTables}, and {@link #SMTH_COEF} is the
 * precomputed masking skirt coefficient table.
 *
 * <p>Scope is the SMPL 16 kHz / 60 ms / mono high rate configuration with 20 ms frames and 5 ms subframes:
 * the FFT is {@value #NFFT} points, the analysis window is the full {@value #NFFT} sample span (no leading
 * samples are skipped), and each call appends a {@value #XSUBFR_PAIR_LEN} sample span. A
 * {@link PerceptualModel} instance owns one {@link Pffft} setup and the rolling look back buffer; the buffer
 * is mutable state carried across calls, so construct one model per logical stream, feed it every subframe
 * span in order, and call {@link #reset()} between independent streams. This type is stateful per stream and
 * is not thread safe.
 *
 * @implNote This implementation captures the perceptual sine window, the trailing cosine tapers, and the
 * smoothing coefficients as exact float constants rather than regenerating them at runtime, so the analysis
 * matches the reference codec bit for bit. The forward and backward transforms run through
 * {@link Pffft#transformOrdered}, whose arithmetic is bit identical to the reference FFT, and the window
 * selection is fixed to the 20 ms perceptual window, the only window the 60 ms high rate scope uses.
 */
public final class PerceptualModel {
    /**
     * The logger for {@link PerceptualModel}.
     */
    private static final System.Logger LOGGER = Log.get(PerceptualModel.class);

    /**
     * Length of the perceptual weighting FFT, {@code 512 + 64} samples.
     */
    private static final int NFFT = 512 + 64;

    /**
     * Length of one analysis look back update span for 5 ms subframes, {@code 2 * 80 + 32} samples.
     */
    private static final int XSUBFR_PAIR_LEN = 192;

    /**
     * Number of trailing samples the buffer shift retains, the difference between the long and short
     * trailing look ahead ({@code 64 - 32}).
     */
    private static final int SHORTER = 32;

    /**
     * Length of the leading perceptual sine taper window segment for a 20 ms frame.
     */
    private static final int PERC_WIN1_20MS_LEN = 352;

    /**
     * Length of the short trailing cosine taper segment ({@code 2} ms).
     */
    private static final int WIN3_SHORT_LEN = 32;

    /**
     * Length of the long trailing cosine taper segment ({@code 4} ms).
     */
    private static final int WIN3_LONG_LEN = 64;

    /**
     * Full perceptual analysis window length for a 20 ms frame, {@code 192 + 320 + 64} samples; equals
     * {@value #NFFT}, so no leading samples are skipped and the whole buffer is windowed.
     */
    private static final int WIN_LEN_20MS = NFFT;

    /**
     * Real forward and backward FFT setup for the {@value #NFFT} point spectral smoothing.
     */
    private final Pffft fft;

    /**
     * The rolling analysis look back buffer.
     *
     * <p>Holds the {@value #NFFT} most recent samples ending at the current analysis look. Starts zeroed and
     * is shifted and appended on every {@link #model(float[], int, boolean, float[], int)} call.
     */
    private final float[] buffer;

    /**
     * Windowing scratch buffer, reused across calls.
     */
    private final float[] bufWin;

    /**
     * Spectrum scratch buffer, reused across calls.
     */
    private final float[] spectrum;

    /**
     * Constructs a perceptual model with a fresh {@value #NFFT} point FFT setup and a zeroed look back
     * buffer.
     *
     * <p>The FFT twiddle tables are computed by {@link Pffft}; the window and smoothing tables are the shared
     * immutable constants. The rolling buffer starts zeroed.
     */
    public PerceptualModel() {
        this.fft = new Pffft(NFFT, Pffft.REAL);
        this.buffer = new float[NFFT];
        this.bufWin = new float[NFFT];
        this.spectrum = new float[NFFT];
    }

    /**
     * Returns this model to its freshly constructed state.
     *
     * <p>Zeroes the rolling look back buffer; the FFT setup and the tables are stateless across streams. Call
     * this between independent streams; do not call it between the spans of one stream, which must thread the
     * rolling buffer.
     */
    public void reset() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "perceptual model reset");
        }
        Arrays.fill(buffer, 0.0f);
    }

    /**
     * Computes the smoothed perceptual autocorrelation for one analysis span.
     *
     * <p>Shifts the rolling buffer left by {@code xsubfrLen - }{@value #SHORTER} samples and appends the
     * {@code xsubfr} span, windows the full {@value #NFFT} sample buffer with the 20 ms perceptual window
     * (long trailing taper unless {@code isLastSubfr}), forward transforms it, folds the result into a power
     * spectrum, smooths the spectrum forward then backward across frequency, backward transforms the smoothed
     * spectrum, and scales by {@code 1 / }{@value #NFFT} into {@code r}. The first {@code lenR} lags of the
     * resulting autocorrelation are written. The {@code xsubfr} input is read but not modified.
     *
     * @param xsubfr      the new analysis span samples, at least {@code xsubfrLen} entries
     * @param xsubfrLen   the number of samples in {@code xsubfr}, {@value #XSUBFR_PAIR_LEN} for a 5 ms
     *                    subframe pair, at most {@value #NFFT}
     * @param isLastSubfr {@code true} when this span is the last subframe of the last frame of the packet,
     *                    which selects the short trailing taper with trailing zeros
     * @param r           the destination autocorrelation, at least {@code lenR} entries
     * @param lenR        the number of autocorrelation lags to write
     */
    public void model(float[] xsubfr, int xsubfrLen, boolean isLastSubfr, float[] r, int lenR) {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "perceptual model analysis span xsubfrLen={0} isLastSubfr={1} lenR={2}",
                    xsubfrLen, isLastSubfr, lenR);
        }
        var retain = NFFT - xsubfrLen;
        System.arraycopy(buffer, xsubfrLen - SHORTER, buffer, 0, retain);
        System.arraycopy(xsubfr, 0, buffer, retain, xsubfrLen);

        window(buffer, isLastSubfr, bufWin);

        fft.transformOrdered(bufWin, spectrum, null, true);
        spectrum[0] = spectrum[0] * spectrum[0];
        spectrum[1] = spectrum[1] * spectrum[1];
        for (var i = 1; i < NFFT / 2; i++) {
            spectrum[2 * i] = spectrum[2 * i] * spectrum[2 * i] + spectrum[2 * i + 1] * spectrum[2 * i + 1];
            spectrum[2 * i + 1] = 0.0f;
        }
        smoothFilter(spectrum);
        fft.transformOrdered(spectrum, bufWin, null, false);

        var scale = 1.0f / NFFT;
        for (var i = 0; i < lenR; i++) {
            r[i] = bufWin[i] * scale;
        }
    }

    /**
     * Applies the 20 ms perceptual analysis window to the rolling buffer.
     *
     * <p>The leading {@value #PERC_WIN1_20MS_LEN} samples are multiplied by the perceptual sine taper, the
     * middle samples are copied verbatim, and the trailing {@value #WIN3_LONG_LEN} samples are multiplied by
     * the cosine taper; when {@code isLastSubfr} is set the short taper is used and the remaining trailing
     * samples are zeroed. The tapers are {@link #PERC_WIN1_20MS}, {@link LpcAnalysisTables#WIN3_LONG} and
     * {@link LpcAnalysisTables#WIN3_SHORT}. The window length is {@value #WIN_LEN_20MS}, equal to
     * {@value #NFFT}, so there are no skipped leading samples.
     *
     * @param in          the rolling buffer of {@value #NFFT} samples
     * @param isLastSubfr {@code true} to use the short trailing taper with trailing zeros, that is the last
     *                    subframe of the packet
     * @param out         the destination for the {@value #NFFT} sample windowed buffer
     */
    private static void window(float[] in, boolean isLastSubfr, float[] out) {
        var longWin = !isLastSubfr;
        var len = WIN_LEN_20MS;
        var win1 = PERC_WIN1_20MS;
        for (var i = 0; i < PERC_WIN1_20MS_LEN; i++) {
            out[i] = in[i] * win1[i];
        }
        var midCount = len - PERC_WIN1_20MS_LEN - WIN3_LONG_LEN;
        System.arraycopy(in, PERC_WIN1_20MS_LEN, out, PERC_WIN1_20MS_LEN, midCount);

        var win3len = longWin ? WIN3_LONG_LEN : WIN3_SHORT_LEN;
        var win3 = longWin ? LpcAnalysisTables.WIN3_LONG : LpcAnalysisTables.WIN3_SHORT;
        var tailStart = len - WIN3_LONG_LEN;
        for (var i = 0; i < win3len; i++) {
            out[tailStart + i] = in[tailStart + i] * win3[i];
        }
        if (!longWin) {
            var zStart = len - WIN3_LONG_LEN + WIN3_SHORT_LEN;
            for (var i = zStart; i < len; i++) {
                out[i] = 0.0f;
            }
        }
    }

    /**
     * Smooths a power spectrum forward then backward across frequency.
     *
     * <p>The spectrum is in the interleaved real FFT layout: {@code f[0]} is the DC power, {@code f[1]} is the
     * Nyquist power, and {@code f[2 * i]} is the power of bin {@code i} for {@code i} in
     * {@code [1, }{@value #NFFT}{@code / 2)} (the imaginary slots are zero). A forward one pole pass runs from
     * bin {@code 1} up to Nyquist and a backward pass runs from Nyquist down to DC, each blending the running
     * value toward the bin power by the per bin coefficient {@link #SMTH_COEF}, which grows with frequency so
     * the masking skirt widens toward higher frequencies. The DC and Nyquist bins are folded in at the ends of
     * the two passes. The smoothing is in place and strictly sequential.
     *
     * @param f the interleaved power spectrum to smooth in place, {@value #NFFT} entries
     */
    private static void smoothFilter(float[] f) {
        var half = NFFT / 2;
        var coef = SMTH_COEF;
        var smooth = f[0];
        for (var i = 1; i < half; i++) {
            var value = f[2 * i];
            smooth = value + coef[i] * (smooth - value);
            f[2 * i] = smooth;
        }
        f[1] = f[1] + coef[half] * (smooth - f[1]);
        smooth = f[1];
        for (var i = half - 1; i > 0; i--) {
            var value = f[2 * i];
            smooth = value + coef[i] * (smooth - value);
            f[2 * i] = smooth;
        }
        f[0] = f[0] + coef[0] * (smooth - f[0]);
    }

    /**
     * The leading perceptual sine taper analysis window for a 20 ms frame, {@value #PERC_WIN1_20MS_LEN}
     * entries.
     *
     * <p>These are the sine window values {@code sin((i + 1) / (N + 1) * PI / 2)} for {@code N == 352},
     * captured as exact float constants because the reference sine window generation is not bit reproducible
     * by the JDK transcendentals.
     */
    static final float[] PERC_WIN1_20MS = {
            0x1.239fdp-8f, 0x1.239f14p-7f, 0x1.b56cc6p-7f, 0x1.239c1ep-6f, 0x1.6c8062p-6f, 0x1.b562ccp-6f, 0x1.fe42fap-6f,
            0x1.23904cp-5f, 0x1.47fd9ep-5f, 0x1.6c6948p-5f, 0x1.90d31ap-5f, 0x1.b53ae4p-5f, 0x1.d9a074p-5f, 0x1.fe039cp-5f,
            0x1.11321ap-4f, 0x1.236102p-4f, 0x1.358e7p-4f, 0x1.47ba4cp-4f, 0x1.59e47ep-4f, 0x1.6c0cfp-4f, 0x1.7e3388p-4f,
            0x1.905832p-4f, 0x1.a27ad4p-4f, 0x1.b49b5ap-4f, 0x1.c6b9a6p-4f, 0x1.d8d5a2p-4f, 0x1.eaef3ap-4f, 0x1.fd0654p-4f,
            0x1.078d7p-3f, 0x1.10965ep-3f, 0x1.199de8p-3f, 0x1.22a406p-3f, 0x1.2ba8acp-3f, 0x1.34abcep-3f, 0x1.3dad5cp-3f,
            0x1.46ad5p-3f, 0x1.4fab9ap-3f, 0x1.58a832p-3f, 0x1.61a30cp-3f, 0x1.6a9c1ap-3f, 0x1.73935p-3f, 0x1.7c88a4p-3f,
            0x1.857c0cp-3f, 0x1.8e6d7ap-3f, 0x1.975ce4p-3f, 0x1.a04a3ap-3f, 0x1.a93578p-3f, 0x1.b21e8ep-3f, 0x1.bb056ep-3f,
            0x1.c3ea1p-3f, 0x1.cccc66p-3f, 0x1.d5ac68p-3f, 0x1.de8a08p-3f, 0x1.e7653ap-3f, 0x1.f03df4p-3f, 0x1.f9142ap-3f,
            0x1.00f3eap-2f, 0x1.055c72p-2f, 0x1.09c3a4p-2f, 0x1.0e297ep-2f, 0x1.128df8p-2f, 0x1.16f11p-2f, 0x1.1b52bcp-2f,
            0x1.1fb2fap-2f, 0x1.2411c2p-2f, 0x1.286f1p-2f, 0x1.2ccadcp-2f, 0x1.312524p-2f, 0x1.357ddep-2f, 0x1.39d506p-2f,
            0x1.3e2a96p-2f, 0x1.427e8cp-2f, 0x1.46d0dcp-2f, 0x1.4b2186p-2f, 0x1.4f7082p-2f, 0x1.53bdcap-2f, 0x1.58095ap-2f,
            0x1.5c532ep-2f, 0x1.609b3cp-2f, 0x1.64e17ep-2f, 0x1.6925f4p-2f, 0x1.6d6894p-2f, 0x1.71a95ap-2f, 0x1.75e84p-2f,
            0x1.7a254p-2f, 0x1.7e6056p-2f, 0x1.82997ep-2f, 0x1.86d0aep-2f, 0x1.8b05e4p-2f, 0x1.8f3918p-2f, 0x1.936a46p-2f,
            0x1.979968p-2f, 0x1.9bc67ap-2f, 0x1.9ff178p-2f, 0x1.a41a5ap-2f, 0x1.a84118p-2f, 0x1.ac65bp-2f, 0x1.b0881cp-2f,
            0x1.b4a858p-2f, 0x1.b8c65cp-2f, 0x1.bce224p-2f, 0x1.c0fbacp-2f, 0x1.c512ecp-2f, 0x1.c927ep-2f, 0x1.cd3a84p-2f,
            0x1.d14adp-2f, 0x1.d558cp-2f, 0x1.d9645p-2f, 0x1.dd6d7ap-2f, 0x1.e17438p-2f, 0x1.e57884p-2f, 0x1.e97a5cp-2f,
            0x1.ed79bap-2f, 0x1.f17694p-2f, 0x1.f570eep-2f, 0x1.f968bap-2f, 0x1.fd5df6p-2f, 0x1.00a84ep-1f, 0x1.02a054p-1f,
            0x1.04970ap-1f, 0x1.068c6ep-1f, 0x1.08807ep-1f, 0x1.0a7336p-1f, 0x1.0c6496p-1f, 0x1.0e5498p-1f, 0x1.10433cp-1f,
            0x1.12307ep-1f, 0x1.141c5cp-1f, 0x1.1606d4p-1f, 0x1.17efe4p-1f, 0x1.19d788p-1f, 0x1.1bbdbep-1f, 0x1.1da284p-1f,
            0x1.1f85d8p-1f, 0x1.2167b8p-1f, 0x1.23481ep-1f, 0x1.25270cp-1f, 0x1.27047cp-1f, 0x1.28e06ep-1f, 0x1.2abadep-1f,
            0x1.2c93cap-1f, 0x1.2e6b3p-1f, 0x1.30410ep-1f, 0x1.321562p-1f, 0x1.33e828p-1f, 0x1.35b95ep-1f, 0x1.378904p-1f,
            0x1.395714p-1f, 0x1.3b238ep-1f, 0x1.3cee6ep-1f, 0x1.3eb7b4p-1f, 0x1.407f5cp-1f, 0x1.424564p-1f, 0x1.4409cap-1f,
            0x1.45cc8ap-1f, 0x1.478da6p-1f, 0x1.494d18p-1f, 0x1.4b0adep-1f, 0x1.4cc6f6p-1f, 0x1.4e816p-1f, 0x1.503a16p-1f,
            0x1.51f118p-1f, 0x1.53a664p-1f, 0x1.5559f8p-1f, 0x1.570bdp-1f, 0x1.58bbeap-1f, 0x1.5a6a46p-1f, 0x1.5c16ep-1f,
            0x1.5dc1b6p-1f, 0x1.5f6ac6p-1f, 0x1.61120ep-1f, 0x1.62b78cp-1f, 0x1.645b4p-1f, 0x1.65fd24p-1f, 0x1.679d36p-1f,
            0x1.693b76p-1f, 0x1.6ad7e2p-1f, 0x1.6c7276p-1f, 0x1.6e0b32p-1f, 0x1.6fa212p-1f, 0x1.713718p-1f, 0x1.72ca3ap-1f,
            0x1.745b8p-1f, 0x1.75eadep-1f, 0x1.77785ap-1f, 0x1.7903ecp-1f, 0x1.7a8d98p-1f, 0x1.7c1558p-1f, 0x1.7d9b2ap-1f,
            0x1.7f1f0ep-1f, 0x1.80a0fep-1f, 0x1.8220fep-1f, 0x1.839f06p-1f, 0x1.851b18p-1f, 0x1.86953p-1f, 0x1.880d5p-1f,
            0x1.898372p-1f, 0x1.8af796p-1f, 0x1.8c69b6p-1f, 0x1.8dd9d8p-1f, 0x1.8f47f2p-1f, 0x1.90b40ap-1f, 0x1.921e16p-1f,
            0x1.93861cp-1f, 0x1.94ec12p-1f, 0x1.964ffep-1f, 0x1.97b1d8p-1f, 0x1.9911a4p-1f, 0x1.9a6f5ep-1f, 0x1.9bcafep-1f,
            0x1.9d248cp-1f, 0x1.9e7cp-1f, 0x1.9fd15cp-1f, 0x1.a1249ap-1f, 0x1.a275bcp-1f, 0x1.a3c4bep-1f, 0x1.a511a2p-1f,
            0x1.a65c6p-1f, 0x1.a7a4fcp-1f, 0x1.a8eb7p-1f, 0x1.aa2fcp-1f, 0x1.ab71e4p-1f, 0x1.acb1ep-1f, 0x1.adefacp-1f,
            0x1.af2b4ep-1f, 0x1.b064cp-1f, 0x1.b19bfep-1f, 0x1.b2d10cp-1f, 0x1.b403e4p-1f, 0x1.b5348ap-1f, 0x1.b662f2p-1f,
            0x1.b78f28p-1f, 0x1.b8b91ep-1f, 0x1.b9e0dcp-1f, 0x1.bb065ap-1f, 0x1.bc299cp-1f, 0x1.bd4a98p-1f, 0x1.be6958p-1f,
            0x1.bf85dp-1f, 0x1.c0a008p-1f, 0x1.c1b7f6p-1f, 0x1.c2cdap-1f, 0x1.c3e0fep-1f, 0x1.c4f214p-1f, 0x1.c600dap-1f,
            0x1.c70d58p-1f, 0x1.c81784p-1f, 0x1.c91f62p-1f, 0x1.ca24ecp-1f, 0x1.cb2828p-1f, 0x1.cc290ep-1f, 0x1.cd279ep-1f,
            0x1.ce23d8p-1f, 0x1.cf1dbap-1f, 0x1.d01544p-1f, 0x1.d10a74p-1f, 0x1.d1fd48p-1f, 0x1.d2edbep-1f, 0x1.d3dbd8p-1f,
            0x1.d4c792p-1f, 0x1.d5b0eap-1f, 0x1.d697e2p-1f, 0x1.d77c78p-1f, 0x1.d85eaap-1f, 0x1.d93e76p-1f, 0x1.da1bdep-1f,
            0x1.daf6dcp-1f, 0x1.dbcf74p-1f, 0x1.dca5ap-1f, 0x1.dd7966p-1f, 0x1.de4abap-1f, 0x1.df19a6p-1f, 0x1.dfe622p-1f,
            0x1.e0b032p-1f, 0x1.e177cep-1f, 0x1.e23cfep-1f, 0x1.e2ffb8p-1f, 0x1.e3c002p-1f, 0x1.e47dd8p-1f, 0x1.e53938p-1f,
            0x1.e5f224p-1f, 0x1.e6a898p-1f, 0x1.e75c94p-1f, 0x1.e80e18p-1f, 0x1.e8bd24p-1f, 0x1.e969b4p-1f, 0x1.ea13cap-1f,
            0x1.eabb64p-1f, 0x1.eb608p-1f, 0x1.ec032p-1f, 0x1.eca34p-1f, 0x1.ed40e2p-1f, 0x1.eddc02p-1f, 0x1.ee74a2p-1f,
            0x1.ef0ac2p-1f, 0x1.ef9e5ep-1f, 0x1.f02f78p-1f, 0x1.f0be0cp-1f, 0x1.f14a1ep-1f, 0x1.f1d3a8p-1f, 0x1.f25abp-1f,
            0x1.f2df2cp-1f, 0x1.f36124p-1f, 0x1.f3e094p-1f, 0x1.f45d7ap-1f, 0x1.f4d7d8p-1f, 0x1.f54facp-1f, 0x1.f5c4f4p-1f,
            0x1.f637b2p-1f, 0x1.f6a7e2p-1f, 0x1.f7158ap-1f, 0x1.f780a2p-1f, 0x1.f7e92ep-1f, 0x1.f84f2ap-1f, 0x1.f8b29ap-1f,
            0x1.f9137ap-1f, 0x1.f971cap-1f, 0x1.f9cd8cp-1f, 0x1.fa26bcp-1f, 0x1.fa7d5cp-1f, 0x1.fad16ap-1f, 0x1.fb22e6p-1f,
            0x1.fb71dp-1f, 0x1.fbbe28p-1f, 0x1.fc07eep-1f, 0x1.fc4f2p-1f, 0x1.fc93bep-1f, 0x1.fcd5c8p-1f, 0x1.fd153ep-1f,
            0x1.fd522p-1f, 0x1.fd8c6cp-1f, 0x1.fdc422p-1f, 0x1.fdf944p-1f, 0x1.fe2bdp-1f, 0x1.fe5bc6p-1f, 0x1.fe8926p-1f,
            0x1.feb3eep-1f, 0x1.fedc2p-1f, 0x1.ff01bcp-1f, 0x1.ff24cp-1f, 0x1.ff452ep-1f, 0x1.ff6302p-1f, 0x1.ff7e4p-1f,
            0x1.ff96e6p-1f, 0x1.ffacf6p-1f, 0x1.ffc06cp-1f, 0x1.ffd14ap-1f, 0x1.ffdf9p-1f, 0x1.ffeb3cp-1f, 0x1.fff452p-1f,
            0x1.fffadp-1f, 0x1.fffeb4p-1f,
    };

    /**
     * The per bin frequency smoothing coefficients of the masking skirt, {@value #NFFT}{@code / 2 + 1}
     * entries.
     *
     * <p>Entry {@code i} is {@code w / (w + 1)} where {@code w = mask * (fsStep * i + melFc) / fsStep} and
     * {@code fsStep = 16000 / }{@value #NFFT}, with {@code mask} the perceptual mask smoothing factor and
     * {@code melFc} the mel center frequency; it is the one pole blend factor of
     * {@link #smoothFilter(float[])} at frequency bin {@code i}. The coefficients increase with frequency,
     * widening the smoothing toward higher bins. They are captured as exact float constants.
     */
    static final float[] SMTH_COEF = {
            0x1.24a2b8p-1f, 0x1.2f0134p-1f, 0x1.387012p-1f, 0x1.410e5ep-1f, 0x1.48f5fcp-1f, 0x1.503cb6p-1f, 0x1.56f5p-1f,
            0x1.5d2e9ap-1f, 0x1.62f6fep-1f, 0x1.6859d6p-1f, 0x1.6d613cp-1f, 0x1.7215fcp-1f, 0x1.767fdp-1f, 0x1.7aa57ap-1f,
            0x1.7e8cfap-1f, 0x1.823b9ep-1f, 0x1.85b61ap-1f, 0x1.8900aap-1f, 0x1.8c1f12p-1f, 0x1.8f14bp-1f, 0x1.91e494p-1f,
            0x1.94917ap-1f, 0x1.971dep-1f, 0x1.998c0cp-1f, 0x1.9bde08p-1f, 0x1.9e15b8p-1f, 0x1.a034ccp-1f, 0x1.a23cd8p-1f,
            0x1.a42f4cp-1f, 0x1.a60d78p-1f, 0x1.a7d892p-1f, 0x1.a991b8p-1f, 0x1.ab39f6p-1f, 0x1.acd23cp-1f, 0x1.ae5b6ep-1f,
            0x1.afd65ep-1f, 0x1.b143cep-1f, 0x1.b2a476p-1f, 0x1.b3f8fep-1f, 0x1.b54202p-1f, 0x1.b68014p-1f, 0x1.b7b3cp-1f,
            0x1.b8dd86p-1f, 0x1.b9fddep-1f, 0x1.bb153ap-1f, 0x1.bc24p-1f, 0x1.bd2a98p-1f, 0x1.be295ep-1f, 0x1.bf20aap-1f,
            0x1.c010cep-1f, 0x1.c0fa1ep-1f, 0x1.c1dcdap-1f, 0x1.c2b94ep-1f, 0x1.c38fbap-1f, 0x1.c46062p-1f, 0x1.c52b78p-1f,
            0x1.c5f13cp-1f, 0x1.c6b1dep-1f, 0x1.c76d92p-1f, 0x1.c82488p-1f, 0x1.c8d6eep-1f, 0x1.c984eep-1f, 0x1.ca2eb2p-1f,
            0x1.cad46p-1f, 0x1.cb761ep-1f, 0x1.cc141p-1f, 0x1.ccae58p-1f, 0x1.cd4514p-1f, 0x1.cdd866p-1f, 0x1.ce686ap-1f,
            0x1.cef53ap-1f, 0x1.cf7ef6p-1f, 0x1.d005b2p-1f, 0x1.d0898cp-1f, 0x1.d10a98p-1f, 0x1.d188eep-1f, 0x1.d204a2p-1f,
            0x1.d27dccp-1f, 0x1.d2f47cp-1f, 0x1.d368c8p-1f, 0x1.d3dac2p-1f, 0x1.d44a7ap-1f, 0x1.d4b804p-1f, 0x1.d5236cp-1f,
            0x1.d58cc6p-1f, 0x1.d5f41ep-1f, 0x1.d65982p-1f, 0x1.d6bd04p-1f, 0x1.d71eaep-1f, 0x1.d77e8ep-1f, 0x1.d7dcbp-1f,
            0x1.d83922p-1f, 0x1.d893ecp-1f, 0x1.d8ed1cp-1f, 0x1.d944bcp-1f, 0x1.d99ad8p-1f, 0x1.d9ef76p-1f, 0x1.da42a4p-1f,
            0x1.da9468p-1f, 0x1.dae4cep-1f, 0x1.db33dcp-1f, 0x1.db819cp-1f, 0x1.dbce18p-1f, 0x1.dc1954p-1f, 0x1.dc635ap-1f,
            0x1.dcac32p-1f, 0x1.dcf3e2p-1f, 0x1.dd3a72p-1f, 0x1.dd7fe6p-1f, 0x1.ddc44ap-1f, 0x1.de079ep-1f, 0x1.de49ecp-1f,
            0x1.de8b3ap-1f, 0x1.decb8cp-1f, 0x1.df0aeap-1f, 0x1.df4956p-1f, 0x1.df86d8p-1f, 0x1.dfc374p-1f, 0x1.dfff3p-1f,
            0x1.e03a12p-1f, 0x1.e0741ap-1f, 0x1.e0ad4ep-1f, 0x1.e0e5b8p-1f, 0x1.e11d58p-1f, 0x1.e1543p-1f, 0x1.e18a46p-1f,
            0x1.e1bfap-1f, 0x1.e1f442p-1f, 0x1.e2282ep-1f, 0x1.e25b66p-1f, 0x1.e28dfp-1f, 0x1.e2bfcep-1f, 0x1.e2f104p-1f,
            0x1.e32198p-1f, 0x1.e35188p-1f, 0x1.e380dap-1f, 0x1.e3af92p-1f, 0x1.e3ddb2p-1f, 0x1.e40b3cp-1f, 0x1.e43834p-1f,
            0x1.e4649cp-1f, 0x1.e49076p-1f, 0x1.e4bbc6p-1f, 0x1.e4e68ep-1f, 0x1.e510d2p-1f, 0x1.e53a92p-1f, 0x1.e563dp-1f,
            0x1.e58c92p-1f, 0x1.e5b4d6p-1f, 0x1.e5dca2p-1f, 0x1.e603f4p-1f, 0x1.e62ad2p-1f, 0x1.e6513cp-1f, 0x1.e67734p-1f,
            0x1.e69cbep-1f, 0x1.e6c1d8p-1f, 0x1.e6e688p-1f, 0x1.e70accp-1f, 0x1.e72eaap-1f, 0x1.e7522p-1f, 0x1.e77532p-1f,
            0x1.e797ep-1f, 0x1.e7ba2ep-1f, 0x1.e7dc1ap-1f, 0x1.e7fdaap-1f, 0x1.e81edcp-1f, 0x1.e83fb2p-1f, 0x1.e8603p-1f,
            0x1.e88054p-1f, 0x1.e8a022p-1f, 0x1.e8bf98p-1f, 0x1.e8debcp-1f, 0x1.e8fd8ep-1f, 0x1.e91c0cp-1f, 0x1.e93a3ap-1f,
            0x1.e9581ap-1f, 0x1.e975acp-1f, 0x1.e992fp-1f, 0x1.e9afeap-1f, 0x1.e9cc98p-1f, 0x1.e9e8fcp-1f, 0x1.ea051ap-1f,
            0x1.ea20eep-1f, 0x1.ea3c7ep-1f, 0x1.ea57c8p-1f, 0x1.ea72dp-1f, 0x1.ea8d92p-1f, 0x1.eaa814p-1f, 0x1.eac252p-1f,
            0x1.eadc52p-1f, 0x1.eaf612p-1f, 0x1.eb0f94p-1f, 0x1.eb28d8p-1f, 0x1.eb41ep-1f, 0x1.eb5aacp-1f, 0x1.eb733cp-1f,
            0x1.eb8b94p-1f, 0x1.eba3bp-1f, 0x1.ebbb94p-1f, 0x1.ebd342p-1f, 0x1.ebeab6p-1f, 0x1.ec01f6p-1f, 0x1.ec19p-1f,
            0x1.ec2fd6p-1f, 0x1.ec4676p-1f, 0x1.ec5ce4p-1f, 0x1.ec731ep-1f, 0x1.ec8926p-1f, 0x1.ec9efcp-1f, 0x1.ecb4a4p-1f,
            0x1.ecca1ap-1f, 0x1.ecdf6p-1f, 0x1.ecf478p-1f, 0x1.ed096p-1f, 0x1.ed1e1cp-1f, 0x1.ed32aap-1f, 0x1.ed470cp-1f,
            0x1.ed5b42p-1f, 0x1.ed6f4cp-1f, 0x1.ed832cp-1f, 0x1.ed96ep-1f, 0x1.edaa6cp-1f, 0x1.edbdcep-1f, 0x1.edd108p-1f,
            0x1.ede418p-1f, 0x1.edf7p-1f, 0x1.ee09c2p-1f, 0x1.ee1c5ep-1f, 0x1.ee2ed2p-1f, 0x1.ee412p-1f, 0x1.ee534ap-1f,
            0x1.ee654ep-1f, 0x1.ee772cp-1f, 0x1.ee88e8p-1f, 0x1.ee9a8p-1f, 0x1.eeabf4p-1f, 0x1.eebd44p-1f, 0x1.eece74p-1f,
            0x1.eedf8p-1f, 0x1.eef06cp-1f, 0x1.ef0136p-1f, 0x1.ef11dep-1f, 0x1.ef2266p-1f, 0x1.ef32cep-1f, 0x1.ef4318p-1f,
            0x1.ef534p-1f, 0x1.ef634ap-1f, 0x1.ef7336p-1f, 0x1.ef8302p-1f, 0x1.ef92bp-1f, 0x1.efa242p-1f, 0x1.efb1b6p-1f,
            0x1.efc10cp-1f, 0x1.efd046p-1f, 0x1.efdf62p-1f, 0x1.efee64p-1f, 0x1.effd4ap-1f, 0x1.f00c14p-1f, 0x1.f01acp-1f,
            0x1.f02956p-1f, 0x1.f037dp-1f, 0x1.f0462ep-1f, 0x1.f05474p-1f, 0x1.f0629ep-1f, 0x1.f070aep-1f, 0x1.f07ea6p-1f,
            0x1.f08c86p-1f, 0x1.f09a4cp-1f, 0x1.f0a7fap-1f, 0x1.f0b59p-1f, 0x1.f0c30cp-1f, 0x1.f0d072p-1f, 0x1.f0ddc2p-1f,
            0x1.f0eaf8p-1f, 0x1.f0f818p-1f, 0x1.f10522p-1f, 0x1.f11214p-1f, 0x1.f11ef2p-1f, 0x1.f12bb8p-1f, 0x1.f13868p-1f,
            0x1.f14504p-1f, 0x1.f1518ap-1f, 0x1.f15dfap-1f, 0x1.f16a54p-1f, 0x1.f1769cp-1f, 0x1.f182cep-1f, 0x1.f18eeap-1f,
            0x1.f19af4p-1f, 0x1.f1a6eap-1f,
    };
}
