package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeEncoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.EncoderTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Turns one packet of 16 kHz mono speech into a range coded MLow payload, the per packet PCM to bitstream
 * orchestrator of the MLow speech encoder for the primary base encoder ({@code smpl_core_encode} together with
 * {@code smpl_base_encode}).
 *
 * <p>This is the analysis brain of the encoder. It wires every analysis block in the {@code encode} package
 * into the exact per frame and per subframe chain the encoder runs, and serializes each frame through
 * {@link ParamEncoder} in the same loop so the bitrate controller feedback sees the real per frame bit count:
 * <ul>
 *   <li>scale the input to float and high pass it through the second order ARMA corner filter, threading the
 *       look back memory across packets ({@link #lpcBufMem}, {@link #hpState});</li>
 *   <li>per 20 ms frame: window the look back buffer and run the short term linear prediction analysis and
 *       bandwidth expansion ({@link LpcAnalysis}); run the perceptual spectral smoother for each 5 ms subframe
 *       pair and interpolate the in between subframe correlations ({@link PerceptualModel}); build the pitch
 *       perceptual weighting filters and filter the speech into the long term prediction buffer
 *       ({@link LpcAnalysis#percAc2a}, {@link #hpMa16Monic}); estimate the open loop pitch lags
 *       ({@link OpenLoopPitch}); classify the frame voiced or unvoiced ({@link SignalModeClassifier}); compute
 *       the per subframe weighted energies; run {@link #baseEncode} to fill one {@link LbQuantParams}; serialize
 *       it ({@link ParamEncoder#encodeFrame}); close the bitrate controller feedback loop
 *       ({@link BitrateController#updateScale}).</li>
 * </ul>
 *
 * <p>The voice activity decision ({@link Vad}) is run upstream by the caller and supplied as a
 * {@link Vad.VadDecision}; this orchestrator consumes its per frame speech activity probabilities and the
 * packet {@code codedAsActiveVoice} flag.
 *
 * <p>Scope is the SMPL 16 kHz / 60 ms / mono low band active voice path at the shipped highest complexity
 * ({@code complexity == 9}, {@code lsf_surv == 8}, {@code pitch_numstates1 == 30}, {@code perc_resp_len == 32},
 * {@code pitch_perc_resp_len == 17}). Both rate classes run here, selected once at construction against the
 * 8700 bps 60 ms threshold ({@link #LOW_RATE_THR_60MS_16K}): the high rate (9600 bps) path runs four 5 ms
 * subframes per 20 ms frame and the low rate (6000 bps) path runs two 10 ms subframes, with the rate distortion
 * weight, the perceptual emphasis, the line spectral and gain codebooks, the nonflatness threshold, the open
 * loop pitch rate bias weight, and the CELP pitch sharpening selected by the rate. The high band (32/48 kHz
 * band split and high band encode), the secondary base encoder, and the in band forward error correction are
 * out of scope and not run. One orchestrator carries the state of a single logical stream; construct one per
 * stream, feed it every packet in order, and call {@link #reset()} between independent streams. This type is
 * stateful and is not thread safe.
 *
 * <p>The discontinuous transmission silence insertion descriptor (SID) path is run: when the {@link Vad}
 * decision marks the packet a SID frame, an inactive unvoiced first frame is recorded into a three slot
 * comfort noise candidate ring ({@code dtx_state.energy}/{@code LbQuantParams}), and on the SID interval tick
 * the lowest energy candidate is encoded once for the whole packet with its excitation pulses suppressed
 * ({@code SID == true}); a silence frame between ticks serializes nothing so the caller drops it. The analysis
 * runs on every frame regardless, so the encoder state and the candidate ring stay in step across the gap.
 *
 * @implNote This implementation reproduces the native cross packet and cross frame state field for field: the
 * high pass look back memory ({@link #lpcBufMem}) and filter state ({@link #hpState}), the previous LSF carry
 * driven through {@link EncoderLsfInterp}, the pitch estimator carry, the perceptual correlation interpolation
 * carry ({@link #percCorrsPrev}), the signal mode hysteresis ({@link SignalModeClassifier}), the bitrate
 * controller feedback ({@link BitrateController}), the parameter encoder conditional predictors
 * ({@link ParamEncoder}), the CELP adaptive codebook ring and conditional coding predictors
 * ({@link CelpEncoder}), the nonflatness energy state ({@link #nonflatnessState}), and the cross packet
 * previous voicing carry ({@link #prevVoicedCarry}). The high pass coefficients are computed once at
 * construction by {@link #computeHpCoefs(int)} at the fixed corner frequency; every float step there is single
 * precision in native source order. The look back buffer geometry reproduces the native pointer arithmetic
 * exactly so the windowed analysis spans land on the same samples. The survivor distribution
 * {@link #distributeFcbSurv(int, int)} and the residual energy alternative interpolation search reproduce the
 * native comparison order including the left to right {@code smpl_sum_vec} reduction. The per frame serialize is
 * interleaved with the analysis because the bitrate controller's adjustment factor for frame {@code n + 1}
 * depends on the bits frame {@code n} actually spent.
 */
public final class CoreEncoder {
    /**
     * Sample clock rate of the CELP core in kilohertz, the native {@code SMPL_CELP_FS_KHZ}.
     */
    private static final int CELP_FS_KHZ = 16;

    /**
     * Samples per internal 20 ms frame at 16 kHz, the native {@code framelen} for a 16 kHz / 20 ms frame.
     */
    private static final int FRAME_LEN_SAMPLES = 20 * CELP_FS_KHZ;

    /**
     * Linear prediction order of the MLow short term filter, the native {@code SMPL_LPC_ORDER}.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Look back memory carried before the current frame for windowing, the native {@code SMPL_WINPREV_LPC_WB_LEN}.
     */
    private static final int WINPREV_LPC_WB_LEN = 16 * 4;

    /**
     * Short trailing analysis look ahead in samples, the native {@code SMPL_WINNEXT_WB_LEN}.
     */
    private static final int WINNEXT_WB_LEN = 16 * 2;

    /**
     * Long trailing analysis look ahead in samples, the native {@code SMPL_WINNEXT_WB_LONG_LEN}.
     */
    private static final int WINNEXT_WB_LONG_LEN = 16 * 4;

    /**
     * Total look back and look ahead memory length carried across packets, the native {@code SMPL_LPC_BUF_MEM_LEN}.
     */
    private static final int LPC_BUF_MEM_LEN = WINPREV_LPC_WB_LEN + WINNEXT_WB_LEN + 16;

    /**
     * Length of the 20 ms frame analysis look back buffer, the native {@code lpcbuf_len} for a 60 ms packet.
     */
    private static final int LPC_BUF_LEN = 448;

    /**
     * Perceptual response length at the shipped highest complexity, the native {@code SMPL_PERC_RESP_LEN}.
     */
    private static final int PERC_RESP_LEN = 16 * 2;

    /**
     * Perceptual emphasis filter overlap, the native {@code SMPL_PERC_EMPH_V_LEN}.
     */
    private static final int PERC_EMPH_V_LEN = 2;

    /**
     * Length of one perceptual correlation vector, the native {@code perc_resp_len + SMPL_PERC_EMPH_V_LEN - 1}.
     */
    private static final int PERC_CORRS_LEN = PERC_RESP_LEN + PERC_EMPH_V_LEN - 1;

    /**
     * Pitch perceptual response length at the shipped highest complexity, the native {@code pitch_perc_resp_len}.
     */
    private static final int PITCH_PERC_RESP_LEN = 17;

    /**
     * Perceptual weighting regularization, the native {@code SMPL_PERC_REG}.
     */
    private static final float PERC_REG = 1e-3f;

    /**
     * Lag subframe length in samples, the native {@code SMPL_LAG_SUBFRLEN}.
     */
    private static final int LAG_SUBFRLEN = 40;

    /**
     * Pitch look ahead length in samples, the native {@code SMPL_PITCH_LOOKAHEAD_LEN}.
     */
    private static final int PITCH_LOOKAHEAD_LEN = 7;

    /**
     * Maximum pitch period in samples, the native {@code SMPL_MAX_PITCH_LEN}.
     */
    private static final int MAX_PITCH_LEN = 320;

    /**
     * Total interpolation delay padding of the pitch resampler, the native {@code SMPL_PITCH_TOT_INTERPOL_DELAY_LEN}.
     */
    private static final int PITCH_TOT_INTERPOL_DELAY_LEN = 12;

    /**
     * Length of the long term prediction buffer, the native {@code SMPL_MAX_LTP_BUF_LEN}.
     */
    private static final int MAX_LTP_BUF_LEN = 659;

    /**
     * Nonflatness cross frame energy state length, the native {@code SMPL_NON_FLAT_STATE_LEN}.
     */
    private static final int NON_FLAT_STATE_LEN = 5;

    /**
     * Maximum survivors kept per pulse stage, the native {@code SMPL_FCB_SRV_MAX}.
     */
    private static final int FCB_SRV_MAX = 4;

    /**
     * Survivor budget table length, sized for the maximum pulses per subframe, the native {@code numsurv[160]}.
     */
    private static final int NUMSURV_LEN = 160;

    /**
     * High pass corner frequency in Hertz, the native {@code SMPL_ENC_HP_FCORNER_3DB_HZ}.
     */
    private static final int HP_FCORNER_3DB_HZ = 35;

    /**
     * Survivor count kept by the pitch block track tournament at complexity nine, the native
     * {@code pitch_numstates1}.
     */
    private static final int PITCH_NUMSTATES1 = 30;

    /**
     * Line spectral frequency survivor count at complexity nine, the native {@code lsf_surv}.
     */
    private static final int LSF_SURV = 8;

    /**
     * Fixed codebook total survivor budget per 20 ms at complexity nine, the native
     * {@code fcb_tot_surv_20ms_max}.
     *
     * <p>At the shipped highest complexity the native budget is {@code 130} for both rate classes
     * ({@code fcb_tot_surv_20ms_max[0] == fcb_tot_surv_20ms_max[1] == 130}), so this single constant serves both
     * the high rate and low rate survivor distribution; the low rate path further scales it down for voiced
     * subframes by the pitch lag ratio in {@link #baseEncode}.
     */
    private static final int FCB_TOT_SURV_20MS_MAX = 130;

    /**
     * Speech activity importance shaping factor, the native {@code enc_status->subFrameImportanceFactor}.
     */
    private static final float SUBFRAME_IMPORTANCE_FACTOR = 0.2f;

    /**
     * Internal sample rate in Hertz, the native {@code enc_status->internalSampleRate} for the low band scope.
     */
    private static final int INTERNAL_SAMPLE_RATE = 16000;

    /**
     * Encoder complexity, the native {@code enc_status->complexity} for the shipped highest setting.
     */
    private static final int COMPLEXITY = 9;

    /**
     * Fixed unvoiced nonflatness threshold of the low rate path, the native {@code SMPL_UV_NONFLATNESS_THR}.
     *
     * <p>On the low rate path {@code smpl_base_encode} sets the unvoiced nonflatness threshold to this constant
     * directly, bypassing the bitrate and speech activity dependent {@code smpl_get_hr_nonflat_thres} the high
     * rate path uses.
     */
    private static final float UV_NONFLATNESS_THR = 0.5f;

    /**
     * Low rate decision threshold in bits per second for the 16 kHz / 60 ms packet, the native
     * {@code smpl_low_rate_thr[0][2]}.
     *
     * <p>The encoder selects the low rate path when {@code mainBitRate <= low_rate_thr}. At the locked
     * 16 kHz / 60 ms / mono scope the threshold is {@code 8700}, so the 9600 bps target stays high rate and the
     * 6000 bps target selects low rate.
     */
    private static final int LOW_RATE_THR_60MS_16K = 8700;

    /**
     * Number of comfort noise candidate frames the discontinuous transmission state keeps, the native
     * {@code SMPL_DTX_NO_CANDIDATES}.
     *
     * <p>The core encoder records the quantized low band parameters and energy of the most recent inactive
     * unvoiced frames in a three slot ring; when a silence insertion descriptor packet must be sent it encodes
     * the lowest energy of these candidates, so the comfort noise the peer synthesizes matches the quietest
     * recent background rather than the frame that happened to fall on the SID interval.
     */
    private static final int DTX_NO_CANDIDATES = 3;

    /**
     * The logger for {@link CoreEncoder}.
     */
    private static final System.Logger LOGGER = Log.get(CoreEncoder.class);

    /**
     * The high pass moving average coefficients, the native {@code hp_b2}, computed once at construction.
     */
    private final float[] hpB2;

    /**
     * The high pass autoregressive coefficients, the native {@code hp_a2}, computed once at construction.
     */
    private final float[] hpA2;

    /**
     * The short term linear prediction analysis stage, shared and immutable.
     */
    private final LpcAnalysis lpc;

    /**
     * The line spectral frequency vector quantizer, shared and immutable.
     */
    private final LsfQuantizer lsfQuantizer;

    /**
     * The perceptual spectral smoother, stateful across the subframes and frames of a stream.
     */
    private final PerceptualModel percModel;

    /**
     * The open loop pitch estimator, stateful across the frames of a stream.
     */
    private final OpenLoopPitch pitch;

    /**
     * The voiced or unvoiced signal mode classifier, stateful across the frames of a stream.
     */
    private final SignalModeClassifier signalMode;

    /**
     * The per subframe target bitrate controller, stateful across the stream.
     */
    private final BitrateController rateCtrl;

    /**
     * The analysis by synthesis CELP encoder, stateful across the subframes of a stream.
     */
    private final CelpEncoder celp;

    /**
     * The unvoiced residual energy quantizer, stateless.
     */
    private final NrgResQuantizer nrgRes;

    /**
     * The encode side per subframe line spectral frequency interpolator, stateful across the frames.
     */
    private final EncoderLsfInterp lsfInterp;

    /**
     * The per frame low band parameter serializer, stateful across the frames and packets of a stream.
     */
    private final ParamEncoder paramEncoder;

    /**
     * The high pass look back memory carried across packets, the native {@code enc_state->lpc_buf_mem}.
     */
    private final float[] lpcBufMem;

    /**
     * The high pass ARMA filter state carried across packets, the native {@code enc_state->hp_arma2_state}.
     */
    private final float[] hpState;

    /**
     * The long term prediction buffer carried across frames, the native {@code enc_state->ltp_buf}.
     */
    private final float[] ltpBuf;

    /**
     * The perceptual correlation interpolation carry, the native {@code enc_state->perc_corrs_prev}.
     */
    private final float[] percCorrsPrev;

    /**
     * The nonflatness cross frame energy state, the native {@code enc_state->nonflatness_state}.
     */
    private final float[] nonflatnessState;

    /**
     * Reused per frame scratch for the windowed look back buffer, sized {@link #LPC_BUF_LEN}.
     *
     * <p>Fully overwritten by {@link LpcAnalysis#window} each frame before {@link LpcAnalysis#analyze} reads it
     * read only and does not retain it, so this single owner thread buffer is reused across frames instead of
     * reallocated.
     */
    private final float[] windowedScratch = new float[LPC_BUF_LEN];

    /**
     * The previous packet's last frame voicing decision, the native {@code enc_state->prev_voiced}.
     */
    private int prevVoicedCarry;

    /**
     * The per slot energy of the comfort noise candidates, the native {@code dtx_state.energy}.
     *
     * <p>Seeded to {@link Float#MAX_VALUE} so an unfilled slot never wins the lowest energy selection.
     */
    private final float[] candidateEnergy = new float[DTX_NO_CANDIDATES];

    /**
     * The per slot rate class of the comfort noise candidates, the native {@code dtx_state.lowRate}.
     *
     * <p>{@code 1} for a low rate candidate, {@code 0} for high rate; the silence insertion descriptor
     * selection considers only candidates whose rate class matches the current packet.
     */
    private final int[] candidateLowRate = new int[DTX_NO_CANDIDATES];

    /**
     * The per slot quantized low band parameters of the comfort noise candidates, the native
     * {@code dtx_state.LbQuantParams}.
     *
     * <p>Each entry is the immutable {@link LbQuantParams} of a recent inactive unvoiced frame; a silence
     * frame encodes the lowest energy of these candidates again as its comfort noise.
     */
    private final LbQuantParams[] candidateParams = new LbQuantParams[DTX_NO_CANDIDATES];

    /**
     * The ring write cursor of the comfort noise candidate slots, the native
     * {@code dtx_state.candidate_insert_idx}.
     *
     * <p>Advanced modulo {@link #DTX_NO_CANDIDATES} before each store, so the first candidate lands in slot
     * one exactly as the native {@code (candidate_insert_idx + 1) % SMPL_DTX_NO_CANDIDATES}.
     */
    private int candidateInsertIdx;

    /**
     * The target bitrate in bits per second, the native {@code enc_status->bitRate} and {@code mainBitRate}.
     *
     * <p>Seeded at construction and retargeted mid stream by {@link #updateTargetBitrate(int)} from the engine
     * bandwidth estimate, the native {@code mainBitRate} the encoder API rewrites each rate control round. Every
     * {@link BitrateController#control} and {@link BitrateController#updateScale} call reads it freshly, so the
     * next frame's pulse budget and feedback target track the new value; the controller reseeds its one shot
     * per rate scale whenever the target it sees changes.
     */
    private int bitRate;

    /**
     * Whether the low rate encode path is active, the native {@code lowRate}.
     *
     * <p>Derived once at construction from {@link #bitRate} against {@link #LOW_RATE_THR_60MS_16K}: {@code true}
     * for the 6000 bps target, {@code false} for the 9600 bps target. The low rate path runs two 10 ms subframes
     * per 20 ms frame (the native {@code subfrlen = 10 * SMPL_CELP_FS_KHZ}, {@code numsubfrs = framelen /
     * subfrlen}) and selects the low rate codebooks, rate distortion weights, perceptual emphasis, and
     * nonflatness threshold throughout {@link #baseEncode}.
     */
    private final boolean lowRate;

    /**
     * The fixed codebook subframe length in samples, the native {@code enc_settings->subfrlen}.
     *
     * <p>{@code 5 * SMPL_CELP_FS_KHZ} (80) on the high rate path, {@code 10 * SMPL_CELP_FS_KHZ} (160) on the
     * low rate path. Set once at construction from {@link #lowRate}.
     */
    private final int subfrlen;

    /**
     * The number of fixed codebook subframes per 20 ms frame, the native {@code enc_settings->numsubfrs}.
     *
     * <p>{@code framelen / subfrlen}: four on the high rate path, two on the low rate path. Set once at
     * construction from {@link #lowRate}.
     */
    private final int numsubfrs;

    /**
     * The current packet's payload length in milliseconds, the native {@code enc_status->payloadSize_ms}.
     *
     * <p>Set by {@link #encodePacket} before the per frame loop; the bitrate controller indexes its model and
     * threshold tables by this whole packet length, not the frame length.
     */
    private int packetMs;

    /**
     * Constructs an orchestrator for the given target bitrate, the native {@code smpl_core_encoder_init} plus the
     * fixed encoder control.
     *
     * <p>Allocates every analysis block, computes the high pass coefficients, and zeroes all cross packet state.
     * The bitrate is fixed for the life of the orchestrator and selects the rate class against the 8700 bps 60 ms
     * threshold ({@link #LOW_RATE_THR_60MS_16K}): the 9600 bps target stays high rate (four 5 ms subframes), the
     * 6000 bps target selects low rate (two 10 ms subframes). The subframe geometry, the open loop pitch rate
     * bias weight, and the CELP analysis by synthesis path are wired from that decision once here.
     *
     * @param bitRate the target bitrate in bits per second, the native {@code enc_status->mainBitRate}
     */
    public CoreEncoder(int bitRate) {
        this.bitRate = bitRate;
        this.lowRate = bitRate <= LOW_RATE_THR_60MS_16K;
        this.subfrlen = (lowRate ? 10 : 5) * CELP_FS_KHZ;
        this.numsubfrs = FRAME_LEN_SAMPLES / subfrlen;
        var hp = computeHpCoefs(HP_FCORNER_3DB_HZ);
        this.hpB2 = hp[0];
        this.hpA2 = hp[1];
        this.lpc = new LpcAnalysis();
        this.lsfQuantizer = new LsfQuantizer();
        this.percModel = new PerceptualModel();
        this.pitch = new OpenLoopPitch(PITCH_NUMSTATES1, lowRate);
        this.signalMode = new SignalModeClassifier();
        this.rateCtrl = new BitrateController();
        this.celp = new CelpEncoder(subfrlen, numsubfrs * 3, lowRate);
        this.nrgRes = new NrgResQuantizer();
        this.lsfInterp = new EncoderLsfInterp();
        this.paramEncoder = new ParamEncoder();
        this.lpcBufMem = new float[LPC_BUF_MEM_LEN + WINNEXT_WB_LEN];
        this.hpState = new float[(3 - 1) * 2];
        this.ltpBuf = new float[MAX_LTP_BUF_LEN];
        this.percCorrsPrev = new float[PERC_CORRS_LEN];
        this.nonflatnessState = new float[NON_FLAT_STATE_LEN];
        this.prevVoicedCarry = 0;
        Arrays.fill(candidateEnergy, Float.MAX_VALUE);
        this.candidateInsertIdx = 0;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "core encoder constructed: bitRate={0} lowRate={1} subfrlen={2} numsubfrs={3}",
                    bitRate, lowRate, subfrlen, numsubfrs);
        }
    }

    /**
     * Retargets the controller's main bitrate mid stream, the native rewrite of {@code enc_status->mainBitRate}
     * the encoder API performs each rate control round.
     *
     * <p>Stores the new target so the next {@link #encodePacket} passes it into every
     * {@link BitrateController#control} and {@link BitrateController#updateScale} call; the controller seeds a
     * fresh one shot per rate scale the first time it sees the changed target and then integrates the feedback
     * loop toward it, which is how the native encoder threads a changing target without reopening the codec. A
     * call that does not change the target is a no op, so the byte exact fixed rate behaviour is preserved
     * whenever the engine holds the bitrate steady.
     *
     * <p>The rate class ({@link #lowRate}) and the subframe geometry it selects are not recomputed: the native
     * encoder API fixes {@code lowRate} once at codec open from the initial bitrate and only the controller's
     * {@code mainBitRate} varies thereafter, so the constructed CELP, pitch, and subframe wiring stay coherent
     * across the retarget. The adaptive audio target range is well above the 8700 bps 60 ms low rate threshold,
     * so a high rate stream stays high rate across its whole life.
     *
     * @param bps the new target bitrate in bits per second, the native {@code mainBitRate}
     */
    public void updateTargetBitrate(int bps) {
        if (Log.DEBUG && bps != bitRate) {
            LOGGER.log(Level.DEBUG, "core encoder bitrate retarget: {0} -> {1}", bitRate, bps);
        }
        this.bitRate = bps;
    }

    /**
     * Returns this orchestrator to its freshly constructed state, the native {@code smpl_core_encoder_init}.
     *
     * <p>Resets every stateful analysis block and zeroes all cross packet look back, filter, and carry state.
     * Call this between independent streams; do not call it between the packets of one continuous stream, which
     * must thread state.
     */
    public void reset() {
        percModel.reset();
        pitch.reset();
        signalMode.reset();
        rateCtrl.init();
        celp.reset();
        lsfInterp.reset();
        paramEncoder.reset();
        Arrays.fill(lpcBufMem, 0.0f);
        Arrays.fill(hpState, 0.0f);
        Arrays.fill(ltpBuf, 0.0f);
        Arrays.fill(percCorrsPrev, 0.0f);
        Arrays.fill(nonflatnessState, 0.0f);
        prevVoicedCarry = 0;
        Arrays.fill(candidateEnergy, Float.MAX_VALUE);
        Arrays.fill(candidateLowRate, 0);
        Arrays.fill(candidateParams, null);
        candidateInsertIdx = 0;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "core encoder reset");
        }
    }

    /**
     * Encodes one packet of 16 kHz mono speech into a range coded payload, {@code smpl_core_encode}.
     *
     * <p>High passes the input, then loops the packet's frames in order: per frame it runs the short term and
     * perceptual analysis, the pitch estimate, the voicing decision, {@link #baseEncode} to fill one
     * {@link LbQuantParams}, the parameter serialize, and the bitrate controller feedback update. The supplied
     * {@code encoder} receives every frame's range coded symbols in order; its byte position after the call is
     * the encoded payload length before zero stripping. The cross frame and cross packet state advances across
     * the call.
     *
     * @param pcm             the packet samples, {@code framesPerPacket * frameLen} entries at 16 kHz mono
     * @param frameLen        the samples per 20 ms frame, the native {@code framelen} (320)
     * @param framesPerPacket the number of frames in the packet, the native {@code frames_per_packet} (3 for
     *                        60 ms)
     * @param vad             the packet voice activity decision from {@link Vad#processPacket}
     * @param encoder         the range encoder positioned at the payload's first symbol
     */
    public void encodePacket(short[] pcm, int frameLen, int framesPerPacket, Vad.VadDecision vad,
                             MlowRangeEncoder encoder) {
        var xLen = framesPerPacket * frameLen;
        var codedAsActiveVoice = vad.codedAsActiveVoice();
        // A regular packet always transmits; a SID frame transmits only on the SID interval tick, and an
        // inactive frame between ticks serializes nothing (the caller drops the empty result). The analysis
        // below still runs on a non transmitted frame so the encoder state and the comfort noise candidate
        // list advance.
        var sidFrame = vad.sidFrame();
        var sendPacket = !sidFrame || vad.sendSidFrame();
        packetMs = framesPerPacket == 1 ? 20 : framesPerPacket == 3 ? 60 : framesPerPacket == 6 ? 120 : 10;
        var frameMs = packetMs == 10 ? 10 : 20;

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "core encode packet: packetMs={0} framesPerPacket={1} bitRate={2} "
                    + "codedAsActiveVoice={3} sidFrame={4} sendPacket={5}",
                    packetMs, framesPerPacket, bitRate, codedAsActiveVoice, sidFrame, sendPacket);
        }

        var x = new float[xLen];
        for (var i = 0; i < xLen; i++) {
            x[i] = pcm[i] / 32768.0f;
        }

        // xhpPacketBuf holds the carried lpcBufMem (LPC_BUF_MEM_LEN + WINNEXT_WB_LEN samples) followed by the
        // high passed x of xLen samples. The trailing WINNEXT_WB_LONG_LEN slot is the look ahead the last
        // frame's window and the last subframe pair's perceptual model read; the native buffer holds stale
        // data there, but the last frame uses the short window whose taper zeroes that region, so a zeroed
        // tail is exact.
        var xhpPacketBuf = new float[LPC_BUF_MEM_LEN + WINNEXT_WB_LEN + xLen + WINNEXT_WB_LONG_LEN];
        System.arraycopy(lpcBufMem, 0, xhpPacketBuf, 0, LPC_BUF_MEM_LEN + WINNEXT_WB_LEN);
        var xIn16k = LPC_BUF_MEM_LEN + WINNEXT_WB_LEN;
        hpArma2(x, 0, xLen, xhpPacketBuf, xIn16k);

        var prevVoiced = 0;
        var lastVoiced = 0;

        for (var numframe = 0; numframe < framesPerPacket; numframe++) {
            var framelen = frameLen;
            var subfrlen = this.subfrlen;
            var numsubfrs = this.numsubfrs;
            var shorter = WINNEXT_WB_LONG_LEN - WINNEXT_WB_LEN;

            var bitsBefore = encoder.tell();

            var xhpFrame = xIn16k - WINNEXT_WB_LEN + framelen * numframe;
            var lpcbuf = xhpFrame + framelen + WINNEXT_WB_LONG_LEN - LPC_BUF_LEN;

            // Frame energy for the comfort noise candidate ranking; the lowest energy inactive candidate
            // becomes the SID frame's comfort noise.
            var frameEnergy = nrg(xhpPacketBuf, xhpFrame, framelen);

            var longWindow = numframe < (framesPerPacket - 1);
            // TODO: reuse the remaining per frame encode tree scratch (percCorrs, spans, ltpSlice, wnrgs) as
            //  single owner instance fields with out param handoffs. Left as fresh allocations for now because
            //  their length is numsubfrs or framelen dependent rather than a compile time constant; the fixed
            //  size windowed buffer is pooled in windowedScratch, proven fully overwritten by LpcAnalysis.window
            //  before analyze reads it and never aliased past the frame.
            var windowed = windowedScratch;
            lpc.window(xhpPacketBuf, lpcbuf, longWindow, windowed);
            var lpcResult = lpc.analyze(windowed, LPC_BUF_LEN);
            var aBuf = lpcResult.lpc();
            // The analysis already ran the single 512 point FFT; reuse its power spectrum byproduct rather
            // than transforming the identical windowed buffer again.
            var lpcbufF2 = lpcResult.f2();

            // Perceptual model. The encode path branches on subfrlen: 5 ms subframes (high rate) compute the
            // model for every second subframe over a two subframe span and interpolate the in between subframe
            // from the carried previous result; 10 ms subframes (low rate) compute the model for every subframe
            // directly over its own span and never interpolate (so the percCorrsPrev carry is not touched on
            // the low rate path).
            var percCorrs = new float[numsubfrs][PERC_CORRS_LEN];
            if (subfrlen == 5 * CELP_FS_KHZ) {
                for (var numsubfr = 1; numsubfr < numsubfrs; numsubfr += 2) {
                    var tSubfr = LPC_BUF_LEN - framelen - shorter + (numsubfr - 1) * subfrlen;
                    var tSubfrLen = 2 * subfrlen + shorter;
                    var isLastSubfr = (numframe == (framesPerPacket - 1)) && (numsubfr == (numsubfrs - 1));
                    var span = new float[tSubfrLen];
                    System.arraycopy(windowedSpanFromBuf(xhpPacketBuf, lpcbuf + tSubfr, tSubfrLen), 0, span, 0, tSubfrLen);
                    percModel.model(span, tSubfrLen, isLastSubfr, percCorrs[numsubfr], PERC_CORRS_LEN);
                    for (var i = 0; i < PERC_CORRS_LEN; i++) {
                        percCorrs[numsubfr - 1][i] = 0.5f * (percCorrs[numsubfr][i] + percCorrsPrev[i]);
                    }
                    System.arraycopy(percCorrs[numsubfr], 0, percCorrsPrev, 0, PERC_CORRS_LEN);
                }
            } else {
                for (var numsubfr = 0; numsubfr < numsubfrs; numsubfr++) {
                    var tSubfr = LPC_BUF_LEN - framelen - shorter + numsubfr * subfrlen;
                    var tSubfrLen = subfrlen + shorter;
                    var isLastSubfr = (numframe == (framesPerPacket - 1)) && (numsubfr == (numsubfrs - 1));
                    var span = new float[tSubfrLen];
                    System.arraycopy(windowedSpanFromBuf(xhpPacketBuf, lpcbuf + tSubfr, tSubfrLen), 0, span, 0, tSubfrLen);
                    percModel.model(span, tSubfrLen, isLastSubfr, percCorrs[numsubfr], PERC_CORRS_LEN);
                }
            }

            // Pitch perceptual weighting filters and weighted speech into ltp_buf.
            var percWghtRespsPitch = new float[numsubfrs][];
            for (var i = 0; i < numsubfrs; i++) {
                percWghtRespsPitch[i] = LpcAnalysis.percAc2a(percCorrs[i], EncoderTables.PERC_EMPH_PITCH,
                        PITCH_PERC_RESP_LEN, PERC_REG);
            }
            var ltpBufLen = framelen + MAX_PITCH_LEN + PITCH_LOOKAHEAD_LEN + PITCH_TOT_INTERPOL_DELAY_LEN;
            System.arraycopy(ltpBuf, framelen, ltpBuf, 0,
                    MAX_LTP_BUF_LEN - framelen - PITCH_LOOKAHEAD_LEN);
            var wSpeech = MAX_LTP_BUF_LEN - numsubfrs * subfrlen - PITCH_LOOKAHEAD_LEN;
            for (var i = 0; i < numsubfrs; i++) {
                hpMa16Monic(xhpPacketBuf, xhpFrame + i * subfrlen, subfrlen, percWghtRespsPitch[i],
                        ltpBuf, wSpeech + i * subfrlen);
            }
            hpMa16Monic(xhpPacketBuf, xhpFrame + framelen, PITCH_LOOKAHEAD_LEN,
                    percWghtRespsPitch[numsubfrs - 1], ltpBuf, MAX_LTP_BUF_LEN - PITCH_LOOKAHEAD_LEN);

            // Open loop pitch estimate.
            var ltpSlice = new float[ltpBufLen];
            System.arraycopy(ltpBuf, MAX_LTP_BUF_LEN - ltpBufLen, ltpSlice, 0, ltpBufLen);
            var pitchResult = pitch.estimate(ltpSlice, ltpBufLen, PITCH_LOOKAHEAD_LEN,
                    lpcbufF2, codedAsActiveVoice, (framelen / LAG_SUBFRLEN));
            var lags = pitchResult.lags();

            // Voiced vs unvoiced.
            var spActProb = vad.speechActivityQ8()[numframe] / 256.0f;
            var voicingStrength = signalMode.classify(pitchResult.pitchCorr(), lags, pitchResult.avgLag(),
                    pitchResult.harmStrength(), lpcbufF2, spActProb);
            var voiced = (voicingStrength > 0.0f) && codedAsActiveVoice ? 1 : 0;
            if (voiced == 0) {
                Arrays.fill(lags, 0, (framelen / LAG_SUBFRLEN), 0.0f);
            }

            // Per subframe weighted energies.
            var wnrgs = new float[numsubfrs];
            for (var i = 0; i < numsubfrs; i++) {
                wnrgs[i] = nrg(ltpBuf, wSpeech + i * subfrlen, subfrlen);
            }

            var condCoding = (voiced == prevVoiced) && (numframe > 0);

            var params = baseEncode(xhpPacketBuf, xhpFrame, aBuf, lpcResult.lsf(), framelen, numsubfrs,
                    subfrlen, numframe, condCoding, voiced, voicingStrength, pitchResult, lags, wnrgs, percCorrs,
                    spActProb, codedAsActiveVoice, prevVoiced);

            // Record this frame as a comfort noise candidate when it is inactive (or in hangover) and unvoiced.
            // Only the first frame of a packet is eligible, since the conditional coding of the later frames'
            // line spectral frequencies makes them not self contained.
            var activity = vad.frameActivities()[numframe];
            if ((activity == Vad.FrameActivity.INACTIVE || activity == Vad.FrameActivity.HANGOVER)
                    && voiced == 0 && numframe == 0) {
                candidateInsertIdx = (candidateInsertIdx + 1) % DTX_NO_CANDIDATES;
                candidateEnergy[candidateInsertIdx] = frameEnergy;
                candidateLowRate[candidateInsertIdx] = lowRate ? 1 : 0;
                candidateParams[candidateInsertIdx] = params;
            }

            // Serialize this frame's parameters. A regular packet encodes every frame; a SID packet encodes the
            // lowest energy comfort noise candidate once (frame zero) with pulses suppressed; a non transmitted
            // frame (a silent frame, or a SID packet's later frames) serializes nothing.
            if (sendPacket && !sidFrame) {
                paramEncoder.encodeFrame(encoder, params, framelen, numsubfrs, codedAsActiveVoice, condCoding,
                        lowRate, numframe, prevVoiced, false);
            } else if (sendPacket && sidFrame && numframe == 0) {
                var minIdx = -1;
                var minNrg = Float.MAX_VALUE;
                for (var i = 0; i < DTX_NO_CANDIDATES; i++) {
                    if (candidateEnergy[i] < minNrg && candidateLowRate[i] == (lowRate ? 1 : 0)) {
                        minIdx = i;
                        minNrg = candidateEnergy[i];
                    }
                }
                if (minIdx >= 0) {
                    paramEncoder.encodeFrame(encoder, candidateParams[minIdx], framelen, numsubfrs, false, false,
                            lowRate, numframe, prevVoiced, true);
                } else if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "core encode: sid frame due but no comfort noise candidate for lowRate={0}",
                            lowRate);
                }
            }

            // Close the bitrate controller feedback loop with the bits this frame actually spent.
            var bitsThisFrame = encoder.tell() - bitsBefore;
            var bitsUsed = new float[]{0.0f, bitsThisFrame};
            rateCtrl.updateScale(frameMs, framesPerPacket, bitsUsed, 0, bitRate, codedAsActiveVoice);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "core encode frame: numframe={0} voiced={1} bits={2}",
                        numframe, voiced, bitsThisFrame);
            }

            // Reset pitch lag carry after unvoiced frames and at the last frame of the packet.
            if (voiced == 0 || numframe == (framesPerPacket - 1)) {
                pitch.reset();
            }

            prevVoiced = voiced;
            lastVoiced = voiced;
        }

        // Carry the tail of the high passed packet buffer into the cross packet look back memory so the next
        // packet's windowing sees it.
        System.arraycopy(xhpPacketBuf, xLen, lpcBufMem, 0, LPC_BUF_MEM_LEN + WINNEXT_WB_LEN);
        prevVoicedCarry = lastVoiced;
    }

    /**
     * Runs the per frame analysis and quantization body, the port of {@code smpl_base_encode}.
     *
     * <p>Quantizes the line spectral frequencies (conditional or not), interpolates the per subframe filters with
     * the alternative index residual energy search, builds the perceptual weighting responses for the voiced or
     * unvoiced class, derives the unvoiced nonflatness threshold, and for each fixed codebook subframe runs the
     * bitrate controller and the CELP search; for an unvoiced frame quantizes the residual energies. Assembles
     * the frame's {@link LbQuantParams}.
     *
     * @param xhpPacketBuf       the high passed packet buffer
     * @param xhpFrame           the offset of this frame within {@code xhpPacketBuf}
     * @param aBuf               the bandwidth expanded LPC filter of this frame
     * @param lsf                the analysis line spectral frequencies of {@code aBuf}, already computed by the
     *                           analysis stage and threaded into the quantizer so the filter is converted once
     * @param framelen           the frame length in samples
     * @param numsubfrs          the number of fixed codebook subframes
     * @param subfrlen           the fixed codebook subframe length in samples
     * @param numframe           the frame index within the packet
     * @param condCoding         {@code true} to take the conditional quantization path
     * @param voiced             {@code 1} for a voiced frame, {@code 0} otherwise
     * @param voicingStrength    the frame's voicing strength
     * @param pitchResult        the open loop pitch estimate
     * @param lags               the per lag subframe pitch lags (zeroed for an unvoiced frame)
     * @param wnrgs              the per subframe weighted energies
     * @param percCorrs          the per subframe perceptual correlations of this frame
     * @param spActProb          the frame's speech activity probability
     * @param codedAsActiveVoice {@code true} when the packet is coded as active voice
     * @param prevVoiced         the previous frame's voicing decision
     * @return the frame's quantized low band parameters
     */
    private LbQuantParams baseEncode(float[] xhpPacketBuf, int xhpFrame, float[] aBuf, float[] lsf, int framelen,
                                     int numsubfrs, int subfrlen, int numframe, boolean condCoding, int voiced,
                                     float voicingStrength, OpenLoopPitch.Result pitchResult, float[] lags,
                                     float[] wnrgs, float[][] percCorrs, float spActProb,
                                     boolean codedAsActiveVoice, int prevVoiced) {
        var lowRate = this.lowRate;
        var lowRateIdx = lowRate ? 1 : 0;
        // The rate distortion weight adjustment normalizes against 5000 bps on the low rate path and
        // 14000 bps on the high rate path.
        var rdwAdj = (float) Math.sqrt(bitRate / (lowRate ? 5000.0f : 14000.0f));

        // The conditional quantizer reads the previous LSF the interpolator carries; read it before the
        // interpolate calls, because the commit below advances that carry.
        var quant = condCoding
                ? lsfQuantizer.quantCond(LSF_SURV, aBuf, lsfInterp.peekCarry(), lsf, rdwAdj, voiced, lowRateIdx)
                : lsfQuantizer.quant(LSF_SURV, aBuf, lsf, rdwAdj, voiced, lowRateIdx);
        var qlsf = quant.lsf();
        var wlsf = quant.wlsf();
        var lsfIdx = quant.indices();

        var lsfInterpolIdx = 0;
        var cand0 = lsfInterp.interpolate(qlsf, numsubfrs, 0);
        var chosen = cand0;
        var predcoefs = cand0.lpc();
        var reslpc = computeReslpc(xhpPacketBuf, xhpFrame, predcoefs, numsubfrs, subfrlen);

        // Alternative interpolation search.
        if (codedAsActiveVoice && numsubfrs > 1) {
            var nrgs1 = new float[numsubfrs];
            for (var i = 0; i < numsubfrs; i++) {
                nrgs1[i] = (float) Math.sqrt(nrg(reslpc, i * subfrlen, subfrlen) + 1e-30f);
            }
            var cand1 = lsfInterp.interpolate(qlsf, numsubfrs, 1);
            var predcoefs2 = cand1.lpc();
            var reslpc2 = computeReslpc(xhpPacketBuf, xhpFrame, predcoefs2, numsubfrs, subfrlen);
            var nrgs2 = new float[numsubfrs];
            for (var i = 0; i < numsubfrs; i++) {
                nrgs2[i] = (float) Math.sqrt(nrg(reslpc2, i * subfrlen, subfrlen) + 1e-30f);
            }
            if (sumVec(nrgs2, numsubfrs) < sumVec(nrgs1, numsubfrs) * 0.998f) {
                lsfInterpolIdx = 1;
                chosen = cand1;
                predcoefs = predcoefs2;
                reslpc = reslpc2;
            }
        }
        lsfInterp.commit(chosen);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "core encode base: voiced={0} condCoding={1} lsfInterpolIdx={2}",
                    voiced, condCoding, lsfInterpolIdx);
        }

        // Perceptual weighting responses for the analysis by synthesis search. The perceptual emphasis is
        // selected by rate class and by the voiced or unvoiced decision.
        var percWghtResp = new float[numsubfrs][];
        var percEmph = voiced == 1 ? EncoderTables.PERC_EMPH_V : EncoderTables.PERC_EMPH_UV;
        for (var i = 0; i < numsubfrs; i++) {
            percWghtResp[i] = LpcAnalysis.percAc2a(percCorrs[i], percEmph[lowRateIdx], PERC_RESP_LEN, PERC_REG);
        }

        // The unvoiced nonflatness threshold is the fixed UV_NONFLATNESS_THR on the low rate path and the
        // bitrate and speech activity dependent value on high rate.
        var spActProbUsed = 1.0f; // useSpActFlatnessThres == 0
        var uvNonflatnessThres = lowRate
                ? UV_NONFLATNESS_THR
                : BitrateController.hrNonflatThres(bitRate, spActProbUsed);
        var nonflat = uvNonflatnessThres + 0.1f;
        // Per subframe nonflatness: recompute over each subframe span for unvoiced frames, threading the
        // cross frame state; voiced subframes keep the carried value.
        var nonflatness = new float[numsubfrs];
        for (var numsubfr = 0; numsubfr < numsubfrs; numsubfr++) {
            if (voiced == 0) {
                nonflat = BitrateController.nonflatness(reslpc, numsubfr * subfrlen, subfrlen, wlsf,
                        nonflatnessState);
            }
            nonflatness[numsubfr] = nonflat;
        }

        var prevVoicedSf = (numframe == 0) ? prevVoicedCarry : prevVoiced;
        var lagSfPerFcbSf = subfrlen / LAG_SUBFRLEN;
        var nrgres = new float[numsubfrs];
        var pulses = new short[framelen];
        var sfPulses = new int[numsubfrs];
        var acbgIdx = new int[numsubfrs];
        var fcbgIdx = new int[numsubfrs];
        var voicedBool = voiced == 1;

        // TODO: reuse the per subframe Result holders and flatten the boxed param vectors into mutable
        //  single owner holders across CoreEncoder/CelpEncoder/FcbSearch/AcbSearch/OpenLoopPitch/ParamEncoder.
        //  Left as per iteration allocations for now: proving no consumer retains a Result alias across the
        //  next subframe or frame across these kernels was not completed here, and an unproven mutable holder
        //  reuse would fail the MlowBitIdentity golden.
        for (var numsubfr = 0; numsubfr < numsubfrs; numsubfr++) {
            var wnrgNext = (numsubfr < (numsubfrs - 1)) ? wnrgs[numsubfr + 1] : wnrgs[numsubfr];
            var alloc = rateCtrl.control(false, codedAsActiveVoice, spActProb,
                    nonflatness[numsubfr], voicingStrength, voiced, wnrgs[numsubfr], wnrgNext, lowRate,
                    framelen, subfrlen, INTERNAL_SAMPLE_RATE, packetMs, 0, bitRate, COMPLEXITY, false,
                    false, SUBFRAME_IMPORTANCE_FACTOR);
            var maxPulses = alloc.maxPulsesPerSubfr().clone();
            var importance = alloc.subfrImportance();

            if (voiced == 0 && prevVoicedSf == 0 && nonflatness[numsubfr] < uvNonflatnessThres) {
                maxPulses[0] = 0;
                maxPulses[1] = 0;
            }

            var lagind = numsubfr * lagSfPerFcbSf;
            var totSurv = 1000 * (FCB_TOT_SURV_20MS_MAX * subfrlen) / (20 * 16000);
            // A low rate voiced subframe scales the survivor budget down toward zero as the pitch lag shrinks
            // below the subframe length.
            if (lowRate && voiced == 1) {
                var scale = Math.min(lags[lagind + lagSfPerFcbSf - 1] / subfrlen, 1.0f);
                totSurv = Math.round(totSurv * scale);
            }
            var numsurv = distributeFcbSurv(maxPulses[1], totSurv);

            var subLags = new float[lagSfPerFcbSf];
            System.arraycopy(lags, lagind, subLags, 0, lagSfPerFcbSf);
            var se = celp.encodeSubframe(voicedBool, reslpc, numsubfr * subfrlen,
                    predcoefs[numsubfr], percWghtResp[numsubfr], subLags, importance, maxPulses, numsurv);
            sfPulses[numsubfr] = se.nPulses();
            acbgIdx[numsubfr] = se.acbgIdx();
            fcbgIdx[numsubfr] = se.fcbgIdx();
            var subPulses = se.pulses();
            for (var i = 0; i < se.nPulses(); i++) {
                var signed = subPulses[i];
                var sign = 1 + 2 * (signed >> 15);
                var pos = (signed * sign) - 1;
                pulses[numsubfr * subfrlen + pos] += (short) sign;
            }
            nrgres[numsubfr] = voiced == 1 ? 0.0f : nrg(reslpc, numsubfr * subfrlen, subfrlen) / subfrlen;
        }

        var nrgresFrameQi = 0;
        var nrgresShapeQi = 0;
        var nrgresDbqQ14 = new int[numsubfrs];
        if (voiced == 0) {
            var nr = nrgRes.quantize(nrgres, numsubfrs);
            nrgresFrameQi = nr.nrgresFrameQi();
            nrgresShapeQi = nr.nrgresShapeQi();
            nrgresDbqQ14 = nr.nrgresDbqQ14();
        }

        var laginds = pitchResult.laginds().clone();
        var blocksegsIx = pitchResult.blocksegIdx();

        return new LbQuantParams(voiced == 1, lsfIdx, lsfInterpolIdx, pulses, sfPulses, acbgIdx, fcbgIdx,
                laginds, blocksegsIx, nrgresFrameQi, nrgresShapeQi, nrgresDbqQ14);
    }

    /**
     * Computes the per subframe LPC residual, the native {@code smpl_filt_ma16_monic} loop of
     * {@code smpl_base_encode}.
     *
     * @param xhpPacketBuf the high passed packet buffer
     * @param xhpFrame     the offset of this frame within {@code xhpPacketBuf}
     * @param predcoefs    the per subframe interpolated LPC filters
     * @param numsubfrs    the number of fixed codebook subframes
     * @param subfrlen     the fixed codebook subframe length
     * @return the whole frame residual, {@code numsubfrs * subfrlen} entries
     */
    private static float[] computeReslpc(float[] xhpPacketBuf, int xhpFrame, float[][] predcoefs, int numsubfrs,
                                         int subfrlen) {
        var reslpc = new float[numsubfrs * subfrlen];
        for (var i = 0; i < numsubfrs; i++) {
            hpMa16Monic(xhpPacketBuf, xhpFrame + i * subfrlen, subfrlen, predcoefs[i], reslpc,
                    i * subfrlen);
        }
        return reslpc;
    }

    /**
     * Distributes the fixed codebook survivor budget across the pulse stages, {@code smpl_distribute_fcb_surv}.
     *
     * <p>Each stage starts with one survivor; the extra budget is spread evenly across all but the last stage,
     * capped at {@value #FCB_SRV_MAX} survivors per stage, then any remaining budget is added from the top stage
     * downward. A {@code maxPulses} of one or zero collapses to a single survivor.
     *
     * @param maxPulses the per subframe pulse budget, the native {@code max_pulses}
     * @param totSurv   the total survivor budget, the native {@code tot_surv}
     * @return the per stage survivor counts, {@value #NUMSURV_LEN} entries
     */
    private static short[] distributeFcbSurv(int maxPulses, int totSurv) {
        var numsurv = new short[NUMSURV_LEN];
        if (maxPulses <= 1) {
            numsurv[0] = 1;
            return numsurv;
        }
        for (var i = 0; i < maxPulses; i++) {
            numsurv[i] = 1;
        }
        var sumSurv = maxPulses;
        var extraSurv = totSurv - maxPulses;
        var extra = Math.min(extraSurv / (maxPulses - 1), FCB_SRV_MAX - 1);
        for (var i = 0; i < maxPulses - 1; i++) {
            numsurv[i] += (short) extra;
        }
        sumSurv += extra * (maxPulses - 1);
        var ix = maxPulses - 2;
        while (sumSurv < totSurv) {
            if (numsurv[ix] < FCB_SRV_MAX) {
                numsurv[ix] += 1;
                sumSurv += 1;
            }
            ix -= 1;
            if (ix < 0) {
                break;
            }
        }
        return numsurv;
    }

    /**
     * Copies a span out of the packet buffer for the perceptual model, a bounds safe slice.
     *
     * @param buf    the source buffer
     * @param off    the span offset
     * @param length the span length
     * @return a freshly allocated copy of the span
     */
    private static float[] windowedSpanFromBuf(float[] buf, int off, int length) {
        var out = new float[length];
        System.arraycopy(buf, off, out, 0, length);
        return out;
    }

    /**
     * Computes the sum of squares of a value run with the native fast math four wide reduction, {@code smpl_nrg}.
     *
     * <p>Four lane accumulators each sum {@code x[4k + lane]^2}, reduced as {@code (lane0 + lane2) + (lane1 +
     * lane3)} with a left to right scalar tail, the SSE2 schedule the {@code -Ofast} build emits.
     *
     * @param x      the values, read from {@code offset}
     * @param offset the first value offset within {@code x}
     * @param length the run length
     * @return the single precision sum of squares
     */
    private static float nrg(float[] x, int offset, int length) {
        var lane0 = 0.0f;
        var lane1 = 0.0f;
        var lane2 = 0.0f;
        var lane3 = 0.0f;
        var vecEnd = length & ~3;
        for (var n = 0; n < vecEnd; n += 4) {
            var x0 = x[offset + n];
            var x1 = x[offset + n + 1];
            var x2 = x[offset + n + 2];
            var x3 = x[offset + n + 3];
            lane0 += x0 * x0;
            lane1 += x1 * x1;
            lane2 += x2 * x2;
            lane3 += x3 * x3;
        }
        var total = (lane0 + lane2) + (lane1 + lane3);
        for (var n = vecEnd; n < length; n++) {
            total += x[offset + n] * x[offset + n];
        }
        return total;
    }

    /**
     * Sums a value run left to right in single precision, {@code smpl_sum_vec}.
     *
     * @param x      the values
     * @param length the run length
     * @return the running sum
     */
    private static float sumVec(float[] x, int length) {
        var sum = 0.0f;
        for (var i = 0; i < length; i++) {
            sum += x[i];
        }
        return sum;
    }

    /**
     * Computes the high pass corner filter coefficients, the port of {@code smpl_get_hp_coefs} then
     * {@code smpl_calc_hp_coefs}.
     *
     * <p>Reproduces the native single precision arithmetic in source order: the cosine approximation
     * {@code 1 - 0.5 x^2}, the autoregressive frequency and radius polynomials, and the gain normalization. The
     * corner is clamped to {@code [5, 1500]} Hz and divided by the 16 kHz sample rate before the polynomial
     * evaluation.
     *
     * @param fcorner3dBHz the high pass corner frequency in Hertz
     * @return a two element array {@code {coefMa, coefAr}}, each three single precision taps
     */
    private static float[][] computeHpCoefs(int fcorner3dBHz) {
        var fc = Math.min(Math.max((float) fcorner3dBHz, 5.0f), 1500.0f);
        var maf = 0.1f;
        var arf0 = 0.728508218f;
        var arf1 = 0.476039848f;
        var arr0 = -4.363803713f;
        var arr1 = 8.441854006f;
        var f = fc / 16000.0f;
        var pi = 3.1415926535897f;

        var coefMa = new float[3];
        var coefAr = new float[3];
        coefMa[0] = 1.0f;
        coefMa[1] = -2.0f * cosApprox(2.0f * pi * maf * f);
        coefMa[2] = 1.0f;
        var far = arf0 * f + arf1 * f * f;
        var rar = arr0 * f + arr1 * f * f;
        coefAr[0] = 1.0f;
        coefAr[1] = -2.0f * cosApprox(2.0f * pi * far) * (1.0f + rar);
        coefAr[2] = 1.0f + (2.0f * rar + rar * rar);
        var sc = (1.0f - coefAr[1] + coefAr[2]) / (1.0f - coefMa[1] + coefMa[2]);
        for (var i = 0; i < 3; i++) {
            coefMa[i] *= sc;
        }
        return new float[][]{coefMa, coefAr};
    }

    /**
     * The cosine approximation of the high pass coefficient design, the native {@code cos_approx}.
     *
     * @param x the argument in radians
     * @return {@code 1 - 0.5 * x * x} in single precision
     */
    private static float cosApprox(float x) {
        return 1.0f - 0.5f * x * x;
    }

    /**
     * Applies the second order ARMA high pass into a separate output buffer, the native {@code smpl_filt_arma2}
     * at the {@code -Ofast} optimization the smpl translation units are built with.
     *
     * <p>Runs the {@code -Ofast} faithful moving average stage {@link #hpMa2} into {@code out} then the
     * {@code -Ofast} faithful autoregressive stage {@link #hpAr2} over {@code out} in place, threading
     * {@link #hpState} (the four element filter memory: two MA, two AR). The input and output are distinct, so no
     * temporary buffer is needed. This is the encoder's input high pass; reproducing the {@code -Ofast}
     * reassociation of both stages here is load bearing because the high passed signal feeds the entire packet
     * analysis and a single rounding difference propagates recursively through the AR filter and then through
     * every downstream feature.
     *
     * @param x      the raw input samples
     * @param xOff   the offset of the first input sample in {@code x}
     * @param n      the number of samples to filter
     * @param out    the destination buffer
     * @param outOff the offset of the first output sample in {@code out}
     */
    private void hpArma2(float[] x, int xOff, int n, float[] out, int outOff) {
        hpMa2(x, xOff, n, hpB2, hpState, 0, out, outOff);
        hpAr2(out, outOff, n, hpA2, hpState, 2);
    }

    /**
     * Applies the second order moving average (FIR) stage of the high pass, the native {@code smpl_filt_ma2} as
     * the {@code -Ofast} GCC build compiles it.
     *
     * <p>The body taps are the per element products the native helper loops emit ({@code y[i] = coef[0] x[i]},
     * then {@code y[1 + i] += coef[1] x[i]}, then {@code y[2 + i] += coef[2] x[i]}), which carry no reassociation
     * freedom. The first output sample folds in the carried state: the native filter kernel at {@code -Ofast}
     * groups the two state products before the input product, emitting
     * {@code y[0] = (coef[1] state[0] + coef[2] state[1]) + coef[0] x[0]}, not the source order
     * {@code (coef[0] x[0] + coef[1] state[0]) + coef[2] state[1]}. That grouping is a single ULP difference that
     * the recursive autoregressive stage then amplifies, so it is load bearing. The shared {@link Filters#ma2}
     * keeps the source order form for the decode path; this private copy carries the {@code -Ofast} grouping the
     * encoder's high pass requires.
     *
     * @param x        the raw input samples
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter; must be greater than one
     * @param coef     the three moving average coefficients
     * @param state    the filter memory vector, two moving average entries from {@code stateOff}
     * @param stateOff the offset of the moving average memory in {@code state}
     * @param y        the destination buffer, distinct from {@code x}
     * @param yOff     the offset of the first output sample in {@code y}
     */
    private static void hpMa2(float[] x, int xOff, int n, float[] coef, float[] state, int stateOff,
                              float[] y, int yOff) {
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
        y[yOff] = (coef[1] * state[stateOff] + coef[2] * state[stateOff + 1]) + coef[0] * x[xOff];
        y[yOff + 1] += coef[2] * state[stateOff];
        state[stateOff] = x[xOff + n - 1];
        state[stateOff + 1] = x[xOff + n - 2];
    }

    /**
     * Applies the second order autoregressive recurrence in place, the native {@code smpl_filt_ar2} as the
     * {@code -Ofast} GCC build compiles it.
     *
     * <p>The native filter kernels are built with {@code -Ofast}, so the four sample unrolled impulse response
     * form is reassociated by {@code -ffast-math}: the impulse coefficients are formed with the reassociated
     * groupings the compiler chose ({@code imp3 = (2 ar1 ar2) + ar1^3} with {@code 2 ar1 ar2} as
     * {@code ymp2 + ymp2}, {@code imp4 = (3 ar1^2 ar2 + ar1^4) + ar2^2}, {@code ymp3 = (c2 - ar1^2) c2}), and each
     * of the four output lanes accumulates in the exact tree the {@code -Ofast} object emits:
     * <ul>
     *   <li>{@code y[n+0] = (imp1 y[n-1] + ymp1 y[n-2]) + x[n]};</li>
     *   <li>{@code y[n+1] = (imp2 y[n-1] + ymp2 y[n-2]) + (imp1 x[n] + x[n+1])};</li>
     *   <li>{@code y[n+2] = ((imp3 y[n-1] + ymp3 y[n-2]) + x[n+2]) + (imp1 x[n+1] + imp2 x[n])};</li>
     *   <li>{@code y[n+3] = ((imp4 y[n-1] + ymp4 y[n-2]) + x[n+3]) + ((imp1 x[n+2] + imp2 x[n+1]) + imp3 x[n])}.</li>
     * </ul>
     * The remainder loop is the plain direct form recurrence the {@code -Ofast} object also emits. This
     * reproduction is byte exact against the {@code -Ofast} oracle across the full packet. The shared
     * {@link Filters#ar2} keeps the source order form for the decode path; this private copy carries the
     * {@code -Ofast} reassociation the encoder's recursive high pass requires.
     *
     * @param y        the buffer to filter in place
     * @param yOff     the offset of the first sample in {@code y}
     * @param n        the number of samples to filter; must be greater than one
     * @param coef     the three AR coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param state    the filter memory vector, two AR memory entries from {@code stateOff}
     * @param stateOff the offset of the AR memory in {@code state}
     */
    private static void hpAr2(float[] y, int yOff, int n, float[] coef, float[] state, int stateOff) {
        var ytmp0 = state[stateOff + 1];
        var ytmp1 = state[stateOff];
        var c1 = coef[1];
        var c2 = coef[2];
        var ar1 = -c1;
        var ar2 = -c2;
        var ar1_2 = ar1 * ar1;
        var ar1_3 = ar1 * ar1_2;
        var ar1_4 = ar1 * ar1_3;
        var imp1 = ar1;
        var imp2 = ar1_2 - c2;
        var ymp2 = c1 * c2;
        var imp3 = (ymp2 + ymp2) + ar1_3;
        var imp4 = (3.0f * ar1_2 * ar2 + ar1_4) + c2 * c2;
        var ymp1 = ar2;
        var ymp3 = (c2 - ar1_2) * c2;
        var ymp4 = ar2 * imp3;
        var i = 0;
        for (; i < n - 3; i += 4) {
            var x0 = y[yOff + i];
            var x1 = y[yOff + i + 1];
            var x2 = y[yOff + i + 2];
            var x3 = y[yOff + i + 3];
            var y0 = (imp1 * ytmp1 + ymp1 * ytmp0) + x0;
            var y1 = (imp2 * ytmp1 + ymp2 * ytmp0) + (imp1 * x0 + x1);
            var y2 = ((imp3 * ytmp1 + ymp3 * ytmp0) + x2) + (imp1 * x1 + imp2 * x0);
            var y3 = ((imp4 * ytmp1 + ymp4 * ytmp0) + x3) + ((imp1 * x2 + imp2 * x1) + imp3 * x0);
            y[yOff + i] = y0;
            y[yOff + i + 1] = y1;
            y[yOff + i + 2] = y2;
            y[yOff + i + 3] = y3;
            ytmp0 = y2;
            ytmp1 = y3;
        }
        for (; i < n; i++) {
            var v = y[yOff + i] + ar1 * ytmp1 + ar2 * ytmp0;
            y[yOff + i] = v;
            ytmp0 = ytmp1;
            ytmp1 = v;
        }
        state[stateOff + 1] = ytmp0;
        state[stateOff] = ytmp1;
    }

    /**
     * Applies a 16th order monic moving average (FIR) filter whose memory sits in the 16 samples before the
     * window, the native {@code smpl_filt_ma16_monic} as the {@code -Ofast} GCC build compiles it.
     *
     * <p>Computes {@code y[n] = x[n] + sum(coef[i] * x[n - i], i = 1 .. 16)} reading the 16 history samples
     * from {@code x[xOff - 16 .. xOff - 1]} in the same backing array. The native filter kernels are built
     * with {@code -Ofast}, so {@code -ffast-math} lets the compiler reassociate the 16 tap accumulation and the
     * SSE2 auto vectorizer packs four output samples per iteration. Each output then sums its products not in
     * the source order {@code 1, 2, ..., 16} but in the lane grouped tree the {@code -Ofast} object emits, which
     * runs two interleaved packed accumulator chains and folds in the monic {@code x[n]} tap last:
     * <ul>
     *   <li>chain {@code A = p16 + (p13 + (p10 + (p8 + (p2 + p1))))};</li>
     *   <li>chain {@code B = p15 + (p12 + (p9 + (p6 + p5)))};</li>
     *   <li>chain {@code C = x[n] + (p14 + (p11 + (p7 + (p4 + p3))))};</li>
     *   <li>result {@code y[n] = C + (A + B)},</li>
     * </ul>
     * where {@code pi = coef[i] * x[n - i]}. This reproduction is byte exact against the {@code -Ofast} oracle
     * for every call the encoder makes on this pitch feature path (the per subframe weighted speech fill and the
     * pitch look ahead fill into {@link #ltpBuf}), because the disjoint input and output arrays always select
     * the vectorized object path. It mirrors {@link #hpAr2} for the autoregressive high pass: the shared
     * {@link Filters#ma16Monic} keeps the source order form for the bit exact decode path, while this private
     * copy carries the {@code -Ofast} reassociation the encoder's pitch features require, the 1 ULP per frame of
     * which feeds {@code pitchCorr}, {@code harmStrength}, and the {@link SignalModeClassifier} and otherwise
     * accumulates over a stream until it flips a CELP survivor and breaks byte identity.
     *
     * @param x    the input buffer with 16 history samples before {@code xOff}
     * @param xOff the offset of the first filtered sample in {@code x}; at least 16
     * @param n    the number of samples to filter
     * @param coef the 17 filter coefficients; {@code coef[0]} is the monic {@code 1.0f}
     * @param y    the output buffer, distinct from {@code x}
     * @param yOff the offset of the first output sample in {@code y}
     */
    private static void hpMa16Monic(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        var c1 = coef[1];
        var c2 = coef[2];
        var c3 = coef[3];
        var c4 = coef[4];
        var c5 = coef[5];
        var c6 = coef[6];
        var c7 = coef[7];
        var c8 = coef[8];
        var c9 = coef[9];
        var c10 = coef[10];
        var c11 = coef[11];
        var c12 = coef[12];
        var c13 = coef[13];
        var c14 = coef[14];
        var c15 = coef[15];
        var c16 = coef[16];
        for (var sample = 0; sample < n; sample++) {
            var idx = xOff + sample;
            var p1 = c1 * x[idx - 1];
            var p2 = c2 * x[idx - 2];
            var p3 = c3 * x[idx - 3];
            var p4 = c4 * x[idx - 4];
            var p5 = c5 * x[idx - 5];
            var p6 = c6 * x[idx - 6];
            var p7 = c7 * x[idx - 7];
            var p8 = c8 * x[idx - 8];
            var p9 = c9 * x[idx - 9];
            var p10 = c10 * x[idx - 10];
            var p11 = c11 * x[idx - 11];
            var p12 = c12 * x[idx - 12];
            var p13 = c13 * x[idx - 13];
            var p14 = c14 * x[idx - 14];
            var p15 = c15 * x[idx - 15];
            var p16 = c16 * x[idx - 16];
            var a = p16 + (p13 + (p10 + (p8 + (p2 + p1))));
            var b = p15 + (p12 + (p9 + (p6 + p5)));
            var c = x[idx] + (p14 + (p11 + (p7 + (p4 + p3))));
            y[yOff + sample] = c + (a + b);
        }
    }
}
