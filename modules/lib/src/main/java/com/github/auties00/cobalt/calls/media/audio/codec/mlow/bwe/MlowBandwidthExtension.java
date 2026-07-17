package com.github.auties00.cobalt.calls.media.audio.codec.mlow.bwe;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.NlsfBridge;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Deterministic parametric high band (above 16 kHz) bandwidth extension of the MLow speech codec.
 *
 * <p>For the super wideband and full band profiles the decoder reconstructs the 16 to 32 kHz octave from the
 * decoded low band plus a handful of transmitted high band parameters: a high band line spectral frequency
 * vector quantizer (VQ) index of four coefficients and a high band gain VQ index per internal frame. This
 * class turns those into the high band signal, recombines it with the low band through the quadrature mirror
 * allpass filterbank, and optionally upsamples the resulting 32 kHz signal to 48 kHz, the three stages the
 * decoder runs above 16 kHz. The low band decode itself is supplied by the caller; this class is additive and
 * runs only for the profiles above 16 kHz.
 *
 * <p>The high band synthesis ({@link #hbDecode}) produces the parametric high band per subframe: a pseudo
 * random pulse train shaped by the low band excitation envelope for active voiced and unvoiced frames, scaled
 * so the all pole filtered energy matches the dequantized high band gain, filtered through the bandwidth
 * expanded high band LPC, and a final spectral tilt moving average. The gains are dequantized
 * ({@link #hbGainDequant}) from the gain VQ shape against the low band frame and subframe energies measured by
 * a weighting filter ({@link #hbWeightLowBand}). The LSF index is dequantized to an LSF vector
 * ({@link #hbLsfDequant}), interpolated to per subframe filters ({@link #hbLsfInterpolate}), and bandwidth
 * expanded. The high band and low band frames are then recombined by the allpass filterbank synthesis
 * ({@link #filterbankSynthesis}) producing the 32 kHz signal, and the 32 to 48 kHz resampler
 * ({@link #upsample32To48}) lifts that to 48 kHz when the API rate is full band.
 *
 * <p>The entry point is {@link #decodeWideband}: it consumes the postfiltered low band synthesis of one MLow
 * packet, the pre postfilter low band synthesis the high band weighting reads, the low band CELP residual the
 * high band excitation envelope reads, the per frame nyquist gains the high band scaling reads, and the per
 * frame high band parameters parsed from the bitstream, and returns the reconstructed full band PCM at the
 * requested API sample rate. State is threaded across the frames of a packet and across packets of a stream:
 * the high band synthesis filter memories, the envelope smoother, the random seed, the post moving average
 * state, the high band output delay line, the low band weighting memory, the previous high band LSF vector,
 * the allpass filterbank synthesis state, and the upsampler state all persist. Construct one instance per
 * logical stream and feed it every packet in order; {@link #reset()} returns it to the freshly constructed
 * state.
 *
 * <p>Scope is the super wideband and full band profiles (internal 32 kHz, API 32 kHz or 48 kHz) over the
 * 16 kHz low band this package decodes. The arbitrary rate resampler used for other API rates and the neural
 * machine learning bandwidth extension are out of scope; this is the deterministic parametric path. The high
 * band parameter parse ({@link MlowHbParamDecoder}) reads the same range coded stream as the low band decode,
 * interleaved per frame. This type is stateful per stream and is not thread safe.
 *
 * @implNote This implementation reuses the shared filter primitives ({@link Filters#ar4}, {@link Filters#ma3},
 * {@link Filters#ma1}, {@link Filters#ma9}, {@link Filters#allpass2}). The result is not bit exact against a
 * double precision reference decoder, a consequence of Java strict single precision arithmetic versus double
 * precision promotion under fast math, but matches the high band envelope and level to the same tight relative
 * tolerance the low band decode carries. The gain dequantizer uses the fast power approximation
 * {@link #powfFast} reproduced through an integer aliasing trick. The nyquist gain scaling and the high band
 * excitation gain computation follow the LPC postfilter branch. The high band LSF interpolation runs at order
 * {@value MlowHbTables#HB_LPC_ORDER} with a stabilizing tail, sharing the integer {@link NlsfBridge} the low
 * band uses.
 */
public final class MlowBandwidthExtension {
    /**
     * The logger for {@link MlowBandwidthExtension}.
     */
    private static final System.Logger LOGGER = Log.get(MlowBandwidthExtension.class);

    /**
     * The high band linear prediction order, {@code 4}.
     */
    private static final int HB_LPC_ORDER = MlowHbTables.HB_LPC_ORDER;

    /**
     * The high band subframe length in samples at 16 kHz, {@code 16 * 5}.
     */
    private static final int HB_SF_LEN = 16 * 5;

    /**
     * The internal 20 ms frame length in samples at 16 kHz, {@code 16 * 20}.
     */
    private static final int FRAME_LEN = 16 * 20;

    /**
     * The maximum number of high band subframes per internal frame, {@link #FRAME_LEN} divided by
     * {@link #HB_SF_LEN}, {@code 4}.
     */
    private static final int MAX_HB_SUBFR = FRAME_LEN / HB_SF_LEN;

    /**
     * The maximum number of subframes per internal frame, {@code 4}.
     */
    private static final int MAX_N_SUBFR = 4;

    /**
     * The total postfilter delay used to time align the high band with the low band, {@code 8 + 40 = 48}
     * samples.
     */
    private static final int TOT_POSTFILT_DELAY = 8 + 40;

    /**
     * The high band spectral envelope smoothing coefficient for voiced frames.
     */
    private static final float HB_SMTH_COEF_V = 0.95f;

    /**
     * The high band spectral envelope smoothing coefficient for unvoiced frames.
     */
    private static final float HB_SMTH_COEF_UV = 0.994f;

    /**
     * The high band LPC bandwidth expansion factor.
     */
    private static final float HB_LPC_BWE = 0.98f;

    /**
     * The high band gain redistribution log power exponent.
     */
    private static final float GEN_LOG_PWR = 0.2f;

    /**
     * The high band impulse response length used to estimate the all pole gain, {@code 16}.
     */
    private static final int HB_IMP_LEN = 16;

    /**
     * The high band spectral tilt weighting moving average coefficients, four taps.
     */
    private static final float[] HB_WGHT_COEF = {-0.12f, 0.38f, -0.38f, 0.12f};

    /**
     * The high band post moving average coefficients, two taps.
     */
    private static final float[] HB_POST_COEF = {0.95f, 0.05f};

    /**
     * The high band post moving average length, {@code 2}.
     */
    private static final int HB_POST_LEN = 2;

    /**
     * The low band weighting moving average coefficients, ten taps.
     */
    private static final float[] LB_WGHT_COEF = {
            -0.01026285f, 0.08698435f, 0.08000515f, -0.85401285f, 1.6944792f,
            -1.6944792f, 0.85401285f, -0.08000515f, -0.08698435f, 0.01026285f
    };

    /**
     * The low band weighting moving average length, {@code 10}.
     */
    private static final int LB_WGHT_LEN = 10;

    /**
     * The quadrature mirror filterbank low band allpass coefficients, three taps.
     */
    private static final float[] FILTERBANK_L_COEF = {1.0f, 0.60797656f, 0.036630828f};

    /**
     * The quadrature mirror filterbank high band allpass coefficients.
     */
    private static final float[] FILTERBANK_H_COEF = {1.0f, 1.1034178f, 0.2197291f};

    /**
     * The 32 to 48 kHz upsampler half band allpass coefficients, two taps.
     */
    private static final float[] AP_COEFS_32_48 = {0.122f, 0.5579f};

    /**
     * The 32 to 48 kHz upsampler interpolation FIR length, {@code 12}; also the upsampler state length.
     */
    private static final int FIR_N_32_48 = 12;

    /**
     * The 32 to 48 kHz upsampler three phase interpolation FIR, {@code 3} phases of {@value #FIR_N_32_48} taps
     * each.
     */
    private static final float[][] FIR_COEFS_32_48 = {
            {0.0163442f, -0.0791814f, 0.121864f, -0.0224717f, -0.202269f, 1.06805f,
                    0.214212f, -0.251631f, 0.202548f, -0.0721714f, -0.00228103f, 0.00556359f},
            {0.012887f, -0.040294f, 0.00175059f, 0.161718f, -0.358857f, 0.722276f,
                    0.722276f, -0.358857f, 0.161718f, 0.00175059f, -0.040294f, 0.012887f},
            {0.00556359f, -0.00228103f, -0.0721714f, 0.202548f, -0.251631f, 0.214212f,
                    1.06805f, -0.202269f, -0.0224717f, 0.121864f, -0.0791814f, 0.0163442f}
    };

    /**
     * The two times upsampler state length, {@code 2}.
     */
    private static final int UP_2X_STATE_LEN = 2;

    /**
     * The high band spectral envelope all pole filter state, the four samples preceding each subframe's
     * filtered noise.
     */
    private final float[] specEnvState;

    /**
     * The high band post moving average state, one sample.
     */
    private final float[] postMaState;

    /**
     * The high band envelope smoother state.
     */
    private float envSmth;

    /**
     * The high band output delay line carried across packets, holding the {@value #TOT_POSTFILT_DELAY} samples
     * that lead the next packet's high band synthesis.
     */
    private final float[] outState;

    /**
     * The high band pseudo random generator seed.
     */
    private int randSeed;

    /**
     * The low band weighting moving average memory, the nine samples preceding the low band the weighting
     * filter reads.
     */
    private final float[] lbWghtMem;

    /**
     * The previous frame's reconstructed high band LSF vector, the left endpoint of the high band LSF
     * interpolation.
     */
    private final float[] hbLsfPrev;

    /**
     * The quadrature mirror filterbank synthesis state, the eight allpass memory samples (two stages of two
     * taps each, two bands).
     */
    private final float[] filterbankSynState;

    /**
     * The 32 to 48 kHz upsampler state, {@value #FIR_N_32_48} samples.
     */
    private final float[] up3248State;

    /**
     * The high band parameter range decoder, threading the previous high band LSF index for conditional
     * coding.
     */
    private final MlowHbParamDecoder hbParamDecoder;

    /**
     * Constructs a high band bandwidth extension with zeroed state.
     *
     * <p>Every filter memory, the envelope smoother, the random seed, the delay lines, and the previous high
     * band LSF vector start silent, ready to extend the first packet of a stream.
     */
    public MlowBandwidthExtension() {
        this.specEnvState = new float[HB_LPC_ORDER];
        this.postMaState = new float[HB_POST_LEN - 1];
        this.envSmth = 0.0f;
        this.outState = new float[TOT_POSTFILT_DELAY];
        this.randSeed = 0;
        this.lbWghtMem = new float[LB_WGHT_LEN - 1];
        this.hbLsfPrev = new float[HB_LPC_ORDER];
        this.filterbankSynState = new float[(FILTERBANK_L_COEF.length - 1) * 4];
        this.up3248State = new float[FIR_N_32_48];
        this.hbParamDecoder = new MlowHbParamDecoder();
    }

    /**
     * Returns this extension to its freshly constructed state.
     *
     * <p>Zeroes every high band filter memory and delay line, the envelope smoother, the random seed, the low
     * band weighting memory, the previous high band LSF vector, the filterbank synthesis state, and the
     * upsampler state, and resets the high band parameter decoder. Call this between independent decode
     * sessions; do not call it between the packets of one continuous stream, which must thread state.
     */
    public void reset() {
        Arrays.fill(specEnvState, 0.0f);
        Arrays.fill(postMaState, 0.0f);
        this.envSmth = 0.0f;
        Arrays.fill(outState, 0.0f);
        this.randSeed = 0;
        Arrays.fill(lbWghtMem, 0.0f);
        Arrays.fill(hbLsfPrev, 0.0f);
        Arrays.fill(filterbankSynState, 0.0f);
        Arrays.fill(up3248State, 0.0f);
        this.hbParamDecoder.reset();
    }

    /**
     * The high band parameters of one internal frame.
     *
     * @param gainQi the high band gain VQ index
     * @param lsfIdx the high band LSF VQ index
     */
    public record HbFrameParams(int gainQi, int lsfIdx) {
    }

    /**
     * The per frame low band inputs the high band synthesis of one internal frame consumes.
     *
     * <p>Carries everything the high band synthesis and its dequantizers read for one frame: the low band CELP
     * residual the excitation envelope is taken from, the pre postfilter low band synthesis the weighting
     * filter reads, the frame voicing and active voice flags, the nyquist gain from the low band postfilter,
     * and the parsed high band parameters.
     *
     * @param lpcRes              the low band CELP residual of the frame, {@code frameLength} samples
     * @param yPrePostfilter      the pre postfilter low band synthesis of the frame, {@code frameLength} samples
     * @param voiced              {@code true} when the frame is voiced
     * @param codedAsActiveVoice  {@code true} when the packet is coded as active voice
     * @param lsfInterpolIdx      the LSF interpolation index of the frame, selecting the per subframe
     *                            interpolation factors shared with the low band
     * @param nyquistGain         the low band postfilter nyquist gain of the frame
     * @param params              the parsed high band VQ indices of the frame
     */
    public record HbFrameInput(float[] lpcRes, float[] yPrePostfilter, boolean voiced, boolean codedAsActiveVoice,
                               int lsfInterpolIdx, float nyquistGain, HbFrameParams params) {
    }

    /**
     * Reconstructs the full band PCM of one MLow packet from its decoded low band and high band parameters,
     * the high band branch and output stage of the core decode for the profiles above 16 kHz.
     *
     * <p>Runs, per internal frame: the low band weighting and energy measurement ({@link #hbWeightLowBand}),
     * the high band LSF dequantization and interpolation ({@link #hbLsfDequant}, {@link #hbLsfInterpolate})
     * with the per subframe bandwidth expansion, the gain dequantization ({@link #hbGainDequant}), and the high
     * band synthesis ({@link #hbDecode}) into a packet length high band buffer. After all frames the low band
     * and high band are recombined by the quadrature mirror filterbank synthesis ({@link #filterbankSynthesis})
     * into a 32 kHz signal, which is converted to {@code int16} directly for a 32 kHz API rate or upsampled to
     * 48 kHz ({@link #upsample32To48}) for a 48 kHz API rate.
     *
     * @param lowBand           the postfiltered low band synthesis of the packet, one
     *                          {@code numFrames * frameLength} array; the recombination reads it and the
     *                          returned PCM does not alias it
     * @param frames            the per frame low band inputs and high band parameters, in frame order
     * @param frameLength       the low band frame length in samples
     * @param numSubframes      the number of subframes per internal frame
     * @param lowRate           {@code true} for the low rate mode
     * @param apiSampleRateHz   the requested API sample rate, {@code 32000} or {@code 48000}
     * @return the reconstructed full band PCM samples, {@code numFrames * frameLength * (apiSampleRateHz /
     *         16000)} entries
     * @throws IllegalArgumentException if {@code apiSampleRateHz} is neither {@code 32000} nor {@code 48000}
     */
    public short[] decodeWideband(float[] lowBand, HbFrameInput[] frames, int frameLength, int numSubframes,
                                  boolean lowRate, int apiSampleRateHz) {
        if (apiSampleRateHz != 32000 && apiSampleRateHz != 48000) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "mlow wideband decode: unsupported api rate {0}", apiSampleRateHz);
            }
            throw new IllegalArgumentException("unsupported wideband API rate " + apiSampleRateHz);
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "mlow wideband decode: frames={0} frameLength={1} apiRate={2}",
                    frames.length, frameLength, apiSampleRateHz);
        }
        var numFrames = frames.length;
        var numHbSubframes = frameLength / HB_SF_LEN;
        var packetLen16 = numFrames * frameLength;
        var lowRateIx = lowRate ? 1 : 0;

        // yHb holds TOT_POSTFILT_DELAY leading delay samples then the packet's high band frames.
        var yHb = new float[packetLen16 + TOT_POSTFILT_DELAY];
        // The weighting filter needs LB_WGHT_LEN-1 leading history samples, threaded through lbWghtMem,
        // so a per frame contiguous yPrePostfilter buffer suffices.
        for (var frame = 0; frame < numFrames; frame++) {
            var in = frames[frame];
            var voiced = in.voiced() ? 1 : 0;

            var yWght = new float[frameLength];
            var lowNrgFrame = new float[1];
            hbWeightLowBand(numHbSubframes, in.yPrePostfilter(), yWght, lowNrgFrame);

            var hbLsfq = new float[HB_LPC_ORDER];
            hbLsfDequant(in.params().lsfIdx(), voiced, lowRateIx, hbLsfq);

            var aHb = new float[MAX_N_SUBFR][HB_LPC_ORDER + 1];
            hbLsfInterpolate(hbLsfq, in.lsfInterpolIdx(), numSubframes, aHb);
            for (var sf = 0; sf < numSubframes; sf++) {
                bweExpand(aHb[sf], HB_LPC_BWE);
            }

            var hbGains = new float[MAX_HB_SUBFR];
            hbGainDequant(in.params().gainQi(), voiced, lowRateIx, numHbSubframes, hbGains, yWght,
                    lowNrgFrame[0]);

            var hbExcGains = new float[MAX_HB_SUBFR];
            hbDecode(in.lpcRes(), voiced, in.codedAsActiveVoice() ? 1 : 0, in.nyquistGain(), numHbSubframes,
                    numSubframes, hbGains, aHb, frame, numFrames, yHb, TOT_POSTFILT_DELAY + frame * frameLength,
                    hbExcGains, lowRateIx);
        }

        // Recombine the 16 kHz low band with the high band, offset past the leading delay, into the 32 kHz signal.
        var y32 = new float[2 * packetLen16];
        filterbankSynthesis(lowBand, 0, yHb, TOT_POSTFILT_DELAY, packetLen16, y32);

        if (apiSampleRateHz == 32000) {
            var out = new short[2 * packetLen16];
            floatToInt16(y32, out, 2 * packetLen16);
            return out;
        }
        // Full band: 32 kHz to 48 kHz, chunked per 10 ms low band frame (640 samples at 32 kHz).
        var outLen = 3 * packetLen16;
        var out = new short[outLen];
        upsample32To48Chunked(y32, 2 * packetLen16, out);
        return out;
    }

    /**
     * Synthesizes one internal frame's high band into the packet high band buffer.
     *
     * <p>For the first frame of a packet the carried output delay line is copied in front of the frame so the
     * post moving average and recombination read continuous history. Per high band subframe a pseudo random
     * pulse train is generated; for an active frame it is multiplied by the low band excitation envelope, and
     * the smoother state is reset for an inactive frame. The all pole impulse response of the high band LPC is
     * weighted by the spectral tilt moving average to estimate the per subframe gain normalization, the noise
     * is scaled (by the nyquist gain when the LPC postfilter is enabled) so the synthesized energy matches the
     * dequantized high band gain, filtered through the bandwidth expanded high band LPC with carried state, and
     * passed through the post moving average (or copied with its state saved for the low rate mode). The per
     * subframe excitation gain is recorded for the comfort noise model. After the last frame the trailing delay
     * samples are saved for the next packet.
     *
     * @param exc                the frame's low band CELP residual, {@code numHbSubframes * HB_SF_LEN} samples
     * @param voiced             {@code 1} for a voiced frame, {@code 0} otherwise
     * @param codedAsActiveVoice {@code 1} when the packet is coded as active voice
     * @param nyquistGain        the low band postfilter nyquist gain
     * @param numHbSubframes     the number of high band subframes in the frame
     * @param numFcbSubframes    the number of low band (fixed codebook) subframes in the frame
     * @param hbGains            the dequantized per subframe high band gains
     * @param aHb                the per subframe bandwidth expanded high band LPC filters
     * @param frame              the frame index within the packet
     * @param numFrames          the number of frames in the packet
     * @param y                  the packet high band buffer (with {@value #TOT_POSTFILT_DELAY} leading delay)
     * @param yOff               the offset of this frame's first high band sample in {@code y}
     * @param hbExcGain          the per subframe high band excitation gain output
     * @param lowRate            {@code 1} for the low rate mode
     */
    private void hbDecode(float[] exc, int voiced, int codedAsActiveVoice, float nyquistGain, int numHbSubframes,
                          int numFcbSubframes, float[] hbGains, float[][] aHb, int frame, int numFrames,
                          float[] y, int yOff, float[] hbExcGain, int lowRate) {
        if (frame == 0) {
            System.arraycopy(outState, 0, y, yOff - TOT_POSTFILT_DELAY, TOT_POSTFILT_DELAY);
        }
        for (var i = 0; i < numHbSubframes; i++) {
            // The pulse block starts at noiseOff; the HB_LPC_ORDER samples before it carry the AR filter state.
            var noiseBuf = new float[HB_SF_LEN + HB_LPC_ORDER];
            var noiseOff = HB_LPC_ORDER;
            genRandPulses(noiseBuf, noiseOff, HB_SF_LEN);

            if (codedAsActiveVoice != 0) {
                var env = new float[HB_SF_LEN];
                envSmth = getEnv(exc, i * HB_SF_LEN, HB_SF_LEN,
                        voiced != 0 ? HB_SMTH_COEF_V : HB_SMTH_COEF_UV, envSmth, env);
                for (var n = 0; n < HB_SF_LEN; n++) {
                    noiseBuf[noiseOff + n] *= env[n];
                }
            } else {
                envSmth = 0.0f;
            }

            // The impulse starts at impulseOff; the leading samples are the zeroed AR filter state.
            var impulseBuf = new float[HB_IMP_LEN + HB_LPC_ORDER];
            var impulseOff = HB_LPC_ORDER;
            impulseBuf[impulseOff] = 1.0f;
            var aPtr = aHb[i * numFcbSubframes / numHbSubframes];
            Filters.ar4(impulseBuf, impulseOff, HB_IMP_LEN, aPtr, impulseBuf, impulseOff);
            // scratch holds the weighted impulse response used to estimate the gain normalization.
            var scratch = new float[HB_IMP_LEN];
            ma3State(impulseBuf, impulseOff, HB_IMP_LEN, HB_WGHT_COEF, scratch);
            var nrgGain = nrg(scratch, 0, HB_IMP_LEN);
            var scale = (float) Math.sqrt(hbGains[i]
                                          / ((nrgGain * nrg(noiseBuf, noiseOff, HB_SF_LEN)) / HB_SF_LEN + 1e-12f) + 1e-30f);
            scale *= nyquistGain;
            for (var n = 0; n < HB_SF_LEN; n++) {
                noiseBuf[noiseOff + n] *= scale;
            }
            System.arraycopy(specEnvState, 0, noiseBuf, noiseOff - HB_LPC_ORDER, HB_LPC_ORDER);
            Filters.ar4(noiseBuf, noiseOff, HB_SF_LEN, aPtr, noiseBuf, noiseOff);
            System.arraycopy(noiseBuf, noiseOff + HB_SF_LEN - HB_LPC_ORDER, specEnvState, 0, HB_LPC_ORDER);
            if (lowRate != 0) {
                System.arraycopy(noiseBuf, noiseOff, y, yOff, HB_SF_LEN);
                System.arraycopy(y, yOff + HB_SF_LEN - HB_POST_LEN + 1, postMaState, 0, HB_POST_LEN - 1);
            } else {
                Filters.ma1(noiseBuf, noiseOff, HB_SF_LEN, HB_POST_COEF, postMaState, 0, y, yOff);
            }

            if (codedAsActiveVoice != 0 && voiced == 0) {
                hbExcGain[i] = hbGains[i] / (nrgGain + 1e-12f);
                hbExcGain[i] *= nyquistGain * nyquistGain;
            } else {
                hbExcGain[i] = scale * scale;
            }

            yOff += HB_SF_LEN;
        }
        if (frame == numFrames - 1) {
            System.arraycopy(y, yOff - TOT_POSTFILT_DELAY, outState, 0, TOT_POSTFILT_DELAY);
        }
    }

    /**
     * Dequantizes one internal frame's high band gains from the gain VQ shape and the low band energies.
     *
     * <p>Selects the gain shape codebook by frame length, voicing, and rate, indexes the chosen vector, and for
     * each high band subframe forms the subframe target energy by redistributing the subframe low band energy
     * toward the frame energy through the fast power {@link #powfFast} at the table exponent, then scales it by
     * the codebook shape mapped through the bounded generalized exponential ({@link #genExp}).
     *
     * @param gainQi         the gain VQ index
     * @param voiced         the voicing index
     * @param lowRate        the rate index
     * @param numHbSubframes the number of high band subframes
     * @param hbGains        the per subframe gain output
     * @param yWght          the low band weighting filter output (one frame)
     * @param lowNrgFrame    the low band frame level weighted energy
     */
    private void hbGainDequant(int gainQi, int voiced, int lowRate, int numHbSubframes, float[] hbGains,
                               float[] yWght, float lowNrgFrame) {
        var framelen20 = numHbSubframes == MAX_HB_SUBFR ? 1 : 0;
        var cb = MlowHbTables.gainCb(framelen20, voiced, lowRate);
        var cbBase = numHbSubframes * gainQi;
        var pwr = MlowHbTables.gainPwr(framelen20, voiced, lowRate);
        for (var i = 0; i < numHbSubframes; i++) {
            var lowNrgSubframe = nrg(yWght, i * HB_SF_LEN, HB_SF_LEN) / HB_SF_LEN + 1e-12f;
            lowNrgSubframe = lowNrgSubframe * powfFast(lowNrgFrame / lowNrgSubframe, pwr);
            var hbWghtNrg = Math.min(genExp(cb[cbBase + i], GEN_LOG_PWR), 2.0f);
            hbWghtNrg *= lowNrgSubframe;
            hbGains[i] = hbWghtNrg;
        }
    }

    /**
     * Weights one internal frame's pre postfilter low band and measures its energy.
     *
     * <p>Threads the carried low band weighting memory in front of the frame, runs the ninth order weighting
     * moving average over the frame, restores the frame's leading history, saves the trailing samples as the
     * next call's memory, and returns the frame level weighted energy.
     *
     * @param numHbSubframes  the number of high band subframes
     * @param xPrePostfilter  the pre postfilter low band synthesis of the frame (input), {@code framelen}
     *                        samples with {@value #LB_WGHT_LEN}{@code  - 1} leading history slots available
     * @param yWght           the weighted output (one frame)
     * @param lowNrgFrame     a one element array receiving the frame level weighted energy
     */
    private void hbWeightLowBand(int numHbSubframes, float[] xPrePostfilter, float[] yWght, float[] lowNrgFrame) {
        var framelen = numHbSubframes * HB_SF_LEN;
        // The weighting filter needs LB_WGHT_LEN-1 leading history samples; this input array reserves that
        // many leading slots, swaps in lbWghtMem, filters, then restores.
        var hist = LB_WGHT_LEN - 1;
        var work = new float[hist + framelen];
        System.arraycopy(xPrePostfilter, 0, work, hist, framelen);
        System.arraycopy(lbWghtMem, 0, work, 0, hist);
        Filters.ma9(work, hist, framelen, LB_WGHT_COEF, yWght, 0);
        System.arraycopy(work, hist + framelen - hist, lbWghtMem, 0, hist);
        lowNrgFrame[0] = nrg(yWght, 0, framelen) / framelen + 1e-12f;
    }

    /**
     * Dequantizes one high band LSF index to an LSF vector.
     *
     * @param qi      the high band LSF VQ index
     * @param voiced  the voicing index
     * @param lowRate the rate index
     * @param lsf     the high band LSF output, {@value #HB_LPC_ORDER} entries
     */
    private void hbLsfDequant(int qi, int voiced, int lowRate, float[] lsf) {
        var cb = MlowHbTables.lsfCb(voiced, lowRate);
        for (var i = 0; i < HB_LPC_ORDER; i++) {
            lsf[i] = cb[qi * HB_LPC_ORDER + i];
        }
    }

    /**
     * Interpolates one frame's high band LSF vector to per subframe high band LPC filters at order
     * {@value #HB_LPC_ORDER}.
     *
     * <p>Seeds the carried previous high band LSF vector from the current vector on a reset, then for each
     * subframe selects the interpolated vector by the per subframe factor (the current vector at factor
     * {@code 1.0f}, the convex blend otherwise, or a verbatim reuse of the previous subframe's filter at a
     * repeated factor), converts it through {@link #nlsf2aStabilize}, and updates the carried vector to the
     * last interpolated vector.
     *
     * @param lsf          the current frame's high band LSF vector, {@value #HB_LPC_ORDER} entries
     * @param interpolIdx  the LSF interpolation index selecting the per subframe factors
     * @param numSubframes the number of subframes in the frame
     * @param aHb          the per subframe high band LPC output, indexed {@code aHb[subframe][tap]}
     */
    private void hbLsfInterpolate(float[] lsf, int interpolIdx, int numSubframes, float[][] aHb) {
        var interpol = MlowLsfInterpolTables.factors(interpolIdx, numSubframes);
        if (hbLsfPrev[HB_LPC_ORDER - 1] == 0.0f) {
            System.arraycopy(lsf, 0, hbLsfPrev, 0, HB_LPC_ORDER);
        }
        var ilsf = new float[HB_LPC_ORDER];
        var prevFactor = -1.0f;
        for (var j = 0; j < numSubframes; j++) {
            var factor = interpol[j];
            if (factor == prevFactor) {
                System.arraycopy(aHb[j - 1], 0, aHb[j], 0, HB_LPC_ORDER + 1);
            } else {
                if (factor == 1.0f) {
                    System.arraycopy(lsf, 0, ilsf, 0, HB_LPC_ORDER);
                } else {
                    var oneMinus = 1.0f - factor;
                    for (var i = 0; i < HB_LPC_ORDER; i++) {
                        ilsf[i] = hbLsfPrev[i] * oneMinus;
                    }
                    for (var i = 0; i < HB_LPC_ORDER; i++) {
                        ilsf[i] += factor * lsf[i];
                    }
                }
                var a = nlsf2aStabilize(ilsf);
                System.arraycopy(a, 0, aHb[j], 0, HB_LPC_ORDER + 1);
            }
            prevFactor = factor;
        }
        System.arraycopy(ilsf, 0, hbLsfPrev, 0, HB_LPC_ORDER);
    }

    /**
     * Converts an order {@value #HB_LPC_ORDER} high band LSF vector to a stabilized LPC filter.
     *
     * <p>Runs the shared integer NLSF to LPC conversion ({@link NlsfBridge#nlsf2a(float[], int)}) then forces
     * stability by progressive bandwidth expansion ({@link #stabilize}).
     *
     * @param ilsf the interpolated high band LSF vector, {@value #HB_LPC_ORDER} entries
     * @return a freshly allocated stabilized high band LPC filter, {@value #HB_LPC_ORDER}{@code  + 1} taps
     */
    private static float[] nlsf2aStabilize(float[] ilsf) {
        var a = NlsfBridge.nlsf2a(ilsf, HB_LPC_ORDER);
        stabilize(a);
        return a;
    }

    /**
     * Forces a high band LPC filter to be stable in place at order {@value #HB_LPC_ORDER}.
     *
     * <p>Returns immediately when the filter is already stable ({@link #isStable}); otherwise applies
     * progressively stronger bandwidth expansion ({@link #bweExpand}) until it is.
     *
     * @param a the high band LPC filter to stabilize in place, {@value #HB_LPC_ORDER}{@code  + 1} taps
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
    }

    /**
     * Applies a bandwidth expansion to a high band LPC filter in place at order {@value #HB_LPC_ORDER}.
     *
     * <p>Scales each tap by an increasing power of the expansion factor; a non positive factor zeroes every
     * tap past the leading one.
     *
     * @param a   the high band LPC filter, {@value #HB_LPC_ORDER}{@code  + 1} taps
     * @param bwe the bandwidth expansion factor
     */
    private static void bweExpand(float[] a, float bwe) {
        if (bwe <= 0.0f) {
            for (var i = 1; i < HB_LPC_ORDER + 1; i++) {
                a[i] = 0.0f;
            }
            return;
        }
        var c = bwe;
        for (var i = 1; i < HB_LPC_ORDER + 1; i++) {
            a[i] *= c;
            c *= bwe;
        }
    }

    /**
     * Tests whether a high band LPC synthesis filter is stable at order {@value #HB_LPC_ORDER}.
     *
     * <p>Short circuits on the last tap, then runs the double precision Levinson down recursion alternating two
     * scratch buffers, rejecting on a zero denominator or a reflection coefficient whose square exceeds the
     * stability bound.
     *
     * @param a the high band LPC filter, {@value #HB_LPC_ORDER}{@code  + 1} taps
     * @return {@code true} when the filter is stable
     */
    private static boolean isStable(float[] a) {
        final var maxRcStable = 0.9995f;
        if (a[HB_LPC_ORDER] * a[HB_LPC_ORDER] > maxRcStable) {
            return false;
        }
        var a0 = new double[HB_LPC_ORDER];
        var a1 = new double[HB_LPC_ORDER];
        for (var i = 0; i < HB_LPC_ORDER; i++) {
            a0[i] = a[i + 1];
        }
        var m = HB_LPC_ORDER - 1;
        while (true) {
            var den = 1.0 - a0[m] * a0[m];
            if (den == 0.0) {
                return false;
            }
            var invDen = 1.0 / den;
            for (var k = 0; k < m; k++) {
                a1[k] = (a0[k] - a0[m] * a0[m - k - 1]) * invDen;
            }
            if (a1[m - 1] * a1[m - 1] > maxRcStable) {
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
            if (a0[m - 1] * a0[m - 1] > maxRcStable) {
                return false;
            }
            if (--m == 0) {
                return true;
            }
        }
    }

    /**
     * Recombines the low band and high band into a 32 kHz signal through the quadrature mirror allpass
     * filterbank synthesis.
     *
     * <p>Forms the sum and difference of the two bands, runs each through its second order allpass with the
     * carried filterbank state, and interleaves the two allpass outputs to produce the doubled rate signal.
     *
     * @param xL    the low band signal
     * @param xLOff the offset of the low band in {@code xL}
     * @param xH    the high band signal
     * @param xHOff the offset of the high band in {@code xH}
     * @param len   the band length in samples
     * @param y     the interleaved 32 kHz output, {@code 2 * len} samples
     */
    private void filterbankSynthesis(float[] xL, int xLOff, float[] xH, int xHOff, int len, float[] y) {
        var sum = new float[len];
        var diff = new float[len];
        var out0 = new float[len];
        var out1 = new float[len];
        for (var i = 0; i < len; i++) {
            sum[i] = xL[xLOff + i] + xH[xHOff + i];
        }
        Filters.allpass2(sum, 0, len, FILTERBANK_L_COEF, filterbankSynState, 0, out0, 0);
        for (var i = 0; i < len; i++) {
            diff[i] = xL[xLOff + i] - xH[xHOff + i];
        }
        Filters.allpass2(diff, 0, len, FILTERBANK_H_COEF, filterbankSynState, 4, out1, 0);
        for (var i = 0; i < len; i++) {
            y[2 * i] = out0[i];
            y[2 * i + 1] = out1[i];
        }
    }

    /**
     * Upsamples a 32 kHz signal to 48 kHz in chunks of one 10 ms low band frame.
     *
     * <p>Processes the signal one {@value #FRAME_LEN} sample low band frame at a time (the 10 ms chunk,
     * {@value #FRAME_LEN} 32 kHz samples since the 32 kHz buffer holds two samples per 16 kHz sample), running
     * {@link #upsample32To48} on each chunk and converting the {@code 3/2} rate output to {@code int16}.
     *
     * @param y32 the 32 kHz signal
     * @param len the 32 kHz signal length, a multiple of {@value #FRAME_LEN}
     * @param out the {@code int16} 48 kHz output, {@code 3 * len / 2} samples
     */
    private void upsample32To48Chunked(float[] y32, int len, short[] out) {
        var numChunks = len / FRAME_LEN;
        var y32Off = 0;
        var outOff = 0;
        var resampled = new float[3 * FRAME_LEN / 2];
        var chunkOut = new short[3 * FRAME_LEN / 2];
        for (var i = 0; i < numChunks; i++) {
            upsample32To48(y32, y32Off, FRAME_LEN, resampled);
            floatToInt16(resampled, chunkOut, 3 * FRAME_LEN / 2);
            System.arraycopy(chunkOut, 0, out, outOff, 3 * FRAME_LEN / 2);
            y32Off += FRAME_LEN;
            outOff += 3 * FRAME_LEN / 2;
        }
    }

    /**
     * Upsamples one chunk of a 32 kHz signal to 48 kHz.
     *
     * <p>First doubles the rate through the fast two times allpass interpolator ({@link #up2xFast}) with the
     * carried upsampler state held in front of the working buffer, then applies the three phase polyphase
     * interpolation FIR to produce three output samples per two input samples (the net {@code 3/2} rate after
     * the doubling stage).
     *
     * @param x     the 32 kHz signal
     * @param xOff  the offset of the chunk in {@code x}
     * @param xLen  the chunk length in samples, an even count
     * @param y     the 48 kHz output, {@code 3 * xLen / 2} samples
     */
    private void upsample32To48(float[] x, int xOff, int xLen, float[] y) {
        var extra = FIR_N_32_48 - UP_2X_STATE_LEN;
        var xtmp = new float[2 * xLen + extra];
        up2xFast(x, xOff, xLen, up3248State, xtmp, extra, 2 * xLen);
        System.arraycopy(up3248State, UP_2X_STATE_LEN, xtmp, 0, extra);
        System.arraycopy(xtmp, 2 * xLen, up3248State, UP_2X_STATE_LEN, extra);
        for (var i = 0; i < xLen / 2; i++) {
            y[3 * i] = dotProd3248(xtmp, 4 * i, FIR_COEFS_32_48[0]);
            y[3 * i + 1] = dotProd3248(xtmp, 4 * i + 1, FIR_COEFS_32_48[1]);
            y[3 * i + 2] = dotProd3248(xtmp, 4 * i + 2, FIR_COEFS_32_48[2]);
        }
    }

    /**
     * Doubles a signal's sample rate through a two band fast allpass interpolator.
     *
     * <p>Runs the two half band allpass sections in software pipelined pairs, writing the carried state samples
     * and the interpolated samples in order; the {@code state[0]} and {@code state[1]} memories thread across
     * chunks.
     *
     * @param x      the input signal
     * @param xOff   the offset of the input in {@code x}
     * @param xLen   the input length, an even count
     * @param state  the two tap allpass state, threaded across calls
     * @param y      the doubled rate output buffer
     * @param yOff   the offset of the output in {@code y}
     * @param yLen   the output length, {@code 2 * xLen}
     */
    private void up2xFast(float[] x, int xOff, int xLen, float[] state, float[] y, int yOff, int yLen) {
        var statea = state[0];
        var stateb = state[1];
        var ca = AP_COEFS_32_48[0];
        var cb = AP_COEFS_32_48[1];
        var ca2 = ca * ca;
        var cb2 = cb * cb;
        var ca3 = ca + ca2;
        var cb3 = cb + cb2;
        for (var i = 0; i < xLen; i += 2) {
            var x0 = x[xOff + i];
            var x1 = x[xOff + i + 1];
            var tmpa0 = ca * (x0 - statea);
            var tmpb0 = cb * (x0 - stateb);
            var tmpa1 = ca * x1 - ca3 * x0 + ca2 * statea;
            var tmpb1 = cb * x1 - cb3 * x0 + cb2 * stateb;
            y[yOff + 2 * i] = statea + tmpa0;
            y[yOff + 2 * i + 1] = stateb + tmpb0;
            y[yOff + 2 * i + 2] = x0 + tmpa0 + tmpa1;
            y[yOff + 2 * i + 3] = x0 + tmpb0 + tmpb1;
            statea = x1 + tmpa1;
            stateb = x1 + tmpb1;
        }
        state[0] = statea;
        state[1] = stateb;
    }

    /**
     * Computes the {@value #FIR_N_32_48} tap dot product of one upsampler interpolation phase.
     *
     * @param a     the windowed input buffer
     * @param aOff  the offset of the window in {@code a}
     * @param coefs the FIR phase coefficients, {@value #FIR_N_32_48} taps
     * @return the filtered sample
     */
    private static float dotProd3248(float[] a, int aOff, float[] coefs) {
        var ret = 0.0f;
        for (var i = 0; i < FIR_N_32_48; i++) {
            ret += a[aOff + i] * coefs[i];
        }
        return ret;
    }

    /**
     * The pseudo random generator multiplier.
     */
    private static final int RAND_MULTIPLIER = 196314165;

    /**
     * The pseudo random generator increment.
     */
    private static final int RAND_INCREMENT = 907633515;

    /**
     * Generates a block of pseudo random high band pulses.
     *
     * <p>Advances the carried 32 bit linear congruential seed and emits four scaled signed values per step from
     * the seed and its left shifted copies.
     *
     * @param noise    the output buffer
     * @param noiseOff the offset of the pulse block in {@code noise}
     * @param length   the block length
     */
    private void genRandPulses(float[] noise, int noiseOff, int length) {
        var i = 0;
        for (; i < length - 3; i += 4) {
            randSeed = RAND_INCREMENT + randSeed * RAND_MULTIPLIER;
            noise[noiseOff + i] = 8.1e-10f * (float) randSeed;
            noise[noiseOff + i + 1] = 8.1e-10f * (float) (randSeed << 8);
            noise[noiseOff + i + 2] = 8.1e-10f * (float) (randSeed << 16);
            noise[noiseOff + i + 3] = 8.1e-10f * (float) (randSeed << 24);
        }
        for (; i < length; i++) {
            randSeed = RAND_INCREMENT + randSeed * RAND_MULTIPLIER;
            noise[noiseOff + i] = 8.1e-10f * (float) randSeed;
        }
    }

    /**
     * Computes the smoothed excitation envelope of a high band subframe.
     *
     * <p>Operates on the squared excitation through the two pole envelope smoother with the squared smoothing
     * coefficient, emitting a pair wise square root envelope and returning the carried smoother state (the last
     * envelope value).
     *
     * @param exc      the excitation buffer
     * @param excOff   the offset of the subframe excitation in {@code exc}
     * @param len      the subframe length, a multiple of four
     * @param smthCoef the envelope smoothing coefficient (voiced or unvoiced)
     * @param smthState the carried smoother state on entry
     * @param env      the envelope output, {@code len} entries
     * @return the updated smoother state (the last envelope value)
     */
    private static float getEnv(float[] exc, int excOff, int len, float smthCoef, float smthState, float[] env) {
        smthCoef *= smthCoef;
        var state = smthState + 1e-8f;
        state *= state;
        var gainCoef = 1.0f - smthCoef;
        var smthCoef2 = smthCoef * smthCoef;
        var gainSmthCoef = gainCoef * smthCoef;
        for (var i = 0; i < len - 3; i += 4) {
            var tmp0 = exc[excOff + i] * exc[excOff + i] + exc[excOff + i + 1] * exc[excOff + i + 1];
            var tmp1 = exc[excOff + i + 2] * exc[excOff + i + 2] + exc[excOff + i + 3] * exc[excOff + i + 3];
            var y1 = gainCoef * tmp1 + gainSmthCoef * tmp0 + smthCoef2 * state;
            var y0 = gainCoef * tmp0 + smthCoef * state;
            env[i] = env[i + 1] = (float) Math.sqrt(y0);
            env[i + 2] = env[i + 3] = (float) Math.sqrt(y1);
            state = y1;
        }
        return env[len - 1];
    }

    /**
     * Computes the sum of squares of a buffer slice.
     *
     * @param x    the buffer
     * @param off  the offset of the slice in {@code x}
     * @param n    the slice length
     * @return the sum of squares
     */
    private static float nrg(float[] x, int off, int n) {
        var nrg = 0.0f;
        for (var i = 0; i < n; i++) {
            nrg += x[off + i] * x[off + i];
        }
        return nrg;
    }

    /**
     * Runs the third order high band weighting moving average whose state sits in the three samples before the
     * input window.
     *
     * @param x      the input buffer with three leading history samples available
     * @param xOff   the offset of the input window in {@code x}
     * @param n      the window length
     * @param coef   the four moving average coefficients
     * @param y      the output buffer, {@code n} entries from index zero
     */
    private static void ma3State(float[] x, int xOff, int n, float[] coef, float[] y) {
        Filters.ma3(x, xOff, n, coef, y, 0);
    }

    /**
     * Computes a fast power approximation of {@code a} raised to {@code b}.
     *
     * <p>Reinterprets the float {@code a} as an {@code int}, applies an affine map in the integer domain, and
     * reinterprets the result as a float.
     *
     * @param a the base
     * @param b the exponent
     * @return the approximate {@code a} raised to {@code b}
     */
    private static float powfFast(float a, float b) {
        var x = Float.floatToRawIntBits(a);
        var mapped = (int) (b * (x - 1064866805) + 1064866805.0f);
        return Float.intBitsToFloat(mapped);
    }

    /**
     * Computes the bounded generalized exponential.
     *
     * @param x the argument
     * @param a the shape exponent
     * @return {@code (a * x + 1)} raised to {@code 1 / a}
     */
    private static float genExp(float x, float a) {
        return (float) Math.pow(a * x + 1.0f, 1.0f / a);
    }

    /**
     * Converts a float buffer to {@code int16} with a saturating round.
     *
     * @param x the float buffer, nominally in {@code [-1, 1]}
     * @param y the {@code int16} output
     * @param n the count
     */
    private static void floatToInt16(float[] x, short[] y, int n) {
        for (var i = 0; i < n; i++) {
            var v = x[i] * 32767.0f;
            if (v > 32767.0f) {
                v = 32767.0f;
            } else if (v < -32767.0f) {
                v = -32767.0f;
            }
            y[i] = (short) v;
        }
    }
}
