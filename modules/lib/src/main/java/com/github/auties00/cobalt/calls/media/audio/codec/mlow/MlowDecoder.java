package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.CelpSynthesizer;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.NoiseGenerator;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.ResNrgDequantizer;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.LpcInterpolator;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.SubframeLpc;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.param.ParamDecoder;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reconstructs PCM audio from coded MLow low band speech packets, joining the parameter front end to the float
 * synthesis back end in one call that turns a coded MLow packet into reconstructed samples.
 *
 * <p>For each internal 20 ms frame the decoder runs, in order:
 * <ol>
 * <li>parameter decode ({@link ParamDecoder}): voicing, LSF indices, pulses, gains, pitch lags;</li>
 * <li>per subframe LSF interpolation and LPC stabilization ({@link SubframeLpc});</li>
 * <li>pitch lag dequantization to fractional sample lags ({@code lagIndex * 0.5f} plus the minimum pitch
 * lag);</li>
 * <li>fixed codebook excitation scatter ({@link CelpSynthesizer#genExcitation}), then per subframe the
 * adaptive codebook long term prediction contribution ({@link CelpSynthesizer#celpDecode}), the shaped noise
 * floor ({@link NoiseGenerator#genNoise}) added to the excitation, the optional unvoiced pulse shaping ARMA,
 * and the order {@value #LPC_ORDER} auto regressive short term synthesis filter ({@link Filters#ar16})
 * producing the subframe speech;</li>
 * <li>a frame level fixed second order ARMA high pass on the assembled frame.</li>
 * </ol>
 * The synthesized frames of a packet are concatenated, scaled to {@code int16}, and returned.
 *
 * <p>This decoder is stateful across the frames of a packet and across packets of one continuous stream. The
 * LPC synthesis memory (the last {@value #LPC_ORDER} synthesized samples), the unvoiced residual energy
 * tracker, the unvoiced pulse shaping ARMA state, and the high pass ARMA state are all threaded from one frame
 * to the next, as is every cross frame state inside the wrapped {@link ParamDecoder}, {@link SubframeLpc},
 * {@link CelpSynthesizer}, and {@link NoiseGenerator}. Construct one decoder per logical stream and feed it
 * every packet in order; {@link #reset()} returns the whole pipeline to the freshly constructed state.
 *
 * <p>The path covers 16 kHz, 60 ms (and the 10 ms and 20 ms sub cases), mono, low band audio with an LPC order
 * of {@value #LPC_ORDER}. The {@link #decode(byte[])} and {@link #decodeFloat(byte[])} entry points apply the
 * fixed second order high pass and return finished PCM; {@link #decodeWithSynthesis(byte[], boolean)} returns
 * the pre high pass synthesis together with the decode parameters a separate {@code MlowDecodePostfilter} pass
 * consumes, and {@link #concealWithSynthesis(int, boolean)} fabricates a lost packet from the concealment
 * state. A packet announcing a sample rate above 16 kHz is rejected by {@link ParamDecoder}. This type is
 * stateful per stream and is not thread safe.
 *
 * @implNote The LPC synthesis memory leads the frame's output window: a contiguous buffer holds the previous
 * frame's trailing {@value #LPC_ORDER} samples in front of the current frame so {@link Filters#ar16} reads its
 * history by the same {@code y[n - i]} arithmetic. The float synthesis path is not bit exact against a double
 * precision reference (double precision promotion under fast math versus Java strict single precision),
 * matching the float tolerance carried from the filter and LPC stages.
 */
public final class MlowDecoder {
    /**
     * The logger for {@link MlowDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(MlowDecoder.class);

    /**
     * Linear prediction order of the MLow short term filter, and the count of synthesis memory history samples
     * carried across frames.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Sample clock rate of the CELP core in kilohertz. The per lag subframe length and the minimum pitch lag
     * are products of it.
     */
    private static final int CELP_FS_KHZ = 16;

    /**
     * Minimum pitch lag in samples ({@code 2 ms} times {@value #CELP_FS_KHZ} kHz, so {@code 32}), added to the
     * half sample quantized lag index to recover the fractional pitch lag.
     */
    private static final int MIN_PITCH_LAG = 2 * CELP_FS_KHZ;

    /**
     * Length in samples of one pitch lag subframe, {@code 40} samples (2.5 ms). The frame's lag count is the
     * frame length divided by it.
     */
    private static final int LAG_SUBFRLEN = 40;

    /**
     * Maximum low band subframe length in samples ({@code 10 ms} times {@code 16 kHz}, so {@code 160}), the
     * ceiling every in scope subframe length (80 or 160) stays within, and the size of the reusable per
     * subframe {@link #excSubframe} and {@link #noise} scratch buffers.
     */
    private static final int MAX_SF_LEN = 160;

    /**
     * Number of taps per adaptive codebook gain vector, the stride of the flattened per subframe gain array the
     * concealment state maintenance consumes.
     */
    private static final int PLC_ACBG_M = 2;

    /**
     * The unvoiced pulse shaping coefficient table, indexed by low rate flag, then by the moving average (0) or
     * auto regressive (1) row, then by tap.
     *
     * <p>For the high rate mode both stages are the identity ({@code {1.0f, 0.0f}}), so the leading coefficient
     * guard disables the shaping; for the low rate mode the moving average numerator is {@code {0.5f, 0.1665f}}
     * and the auto regressive denominator is {@code {1.0f, -0.333f}}.
     */
    private static final float[][][] UV_PULSE_SHAPING_COEFS = {
            {{1.0f, 0.0f}, {1.0f, 0.0f}},
            {{0.5f, 0.1665f}, {1.0f, -0.333f}}
    };

    /**
     * The low rate tilt postfilter two tap moving average coefficients, indexed by voicing.
     *
     * <p>The unvoiced row is the identity {@code {1.0f, 0.0f}}, which the leading coefficient guard treats as
     * no tilt and which leaves the excitation untouched; the voiced row {@code {0.84f, 0.16f}} is the de tilt
     * applied to the excitation before the short term synthesis filter. The tilt is the low rate alternative to
     * the LPC postfilter and runs only when the LPC postfilter is disabled.
     */
    private static final float[][] POST_TILT_COEFS = {
            {1.0f, 0.0f},
            {0.84f, 0.16f}
    };

    /**
     * The fixed second order high pass moving average (numerator) coefficients.
     *
     * <p>These, together with {@link #HP_A2}, form the high pass applied in place of the harmonic high pass
     * postfilter on the finished PCM path.
     */
    private static final float[] HP_B2 = {0.99049276f, -1.9809836f, 0.99049276f};

    /**
     * The fixed second order high pass auto regressive (denominator) coefficients; index zero is the monic
     * {@code 1.0f}.
     */
    private static final float[] HP_A2 = {1.0f, -1.9808896f, 0.9810795f};

    /**
     * The float to {@code int16} scale multiplier applied to the synthesis before clamping to the signed 16 bit
     * range.
     */
    private static final float PCM_SCALE = 32767.0f;

    /**
     * The MLow multiframe table of contents indicator mask ({@code 0x82}, the SID and FEC bits).
     *
     * <p>A non CELT table of contents byte with both the SID and FEC bits set marks a multiframe MLow packet
     * whose second byte is the contained frame count; otherwise the packet carries a single self contained MLow
     * frame. The CELT case (top two bits set) is excluded by {@link #isCelt(int)} before this mask is applied.
     */
    private static final int MULTI_TOC_MASK = 0x82;

    /**
     * The per frame low band parameter decoder, threading conditional coding and previous frame state across
     * the frames of a packet and across packets.
     */
    private final ParamDecoder paramDecoder;

    /**
     * The per subframe LSF interpolator and LPC stabilizer, threading the previous frame LSF vector.
     */
    private final SubframeLpc subframeLpc;

    /**
     * The CELP excitation synthesizer, owning the adaptive codebook history ring.
     */
    private final CelpSynthesizer celpSynthesizer;

    /**
     * The shaped noise generator, owning the noise envelope and shaping filter smoothing state.
     */
    private final NoiseGenerator noiseGenerator;

    /**
     * The packet loss concealment and comfort noise state machine.
     *
     * <p>Maintained on every decoded good frame so a subsequent lost frame can extrapolate from it; the good
     * decode output is unaffected because the good frame maintenance only records state.
     */
    private final MlowPlc plc;

    /**
     * The most recently decoded good packet's table of contents, the geometry a subsequent concealed packet
     * inherits (subframe count and rate mode), or {@code null} before the first packet.
     */
    private MlowTocByte lastToc;

    /**
     * The LPC synthesis memory: the last {@value #LPC_ORDER} synthesized output samples of the previous frame,
     * the history the next frame's short term synthesis filter reads.
     */
    private final float[] lpcSynthMem;

    /**
     * The unvoiced pulse shaping ARMA filter state, two taps (the moving average memory and the auto regressive
     * memory).
     */
    private final float[] uvPulseShapingState;

    /**
     * The fixed high pass ARMA filter state, four taps (the two tap moving average memory and the two tap auto
     * regressive memory).
     */
    private final float[] hpArma2State;

    /**
     * The previous frame's reconstructed unvoiced residual energy.
     *
     * <p>Updated after every unvoiced subframe; read only by the DTX residual energy interpolation, so it is
     * tracked for state fidelity but does not affect the good decode output.
     */
    private float prevNrgres;

    /**
     * The low rate tilt postfilter single tap moving average memory, held as a single element vector for
     * {@link Filters#ma1}.
     *
     * <p>Threaded across the subframes, frames, and packets of the
     * {@link #decodeWithSynthesis(byte[], boolean) synthesis capture path}, which applies the tilt postfilter;
     * the {@link #decode(byte[])} path does not run the tilt and never reads or writes this state.
     */
    private final float[] tiltState;

    /**
     * Reusable per subframe excitation buffer, sized to {@value #MAX_SF_LEN}.
     *
     * <p>Every subframe fully overwrites its {@code [0, subframeLength)} region before any read, so one
     * instance owned buffer replaces the per subframe allocation without changing any consumed value; the
     * decode is single threaded per stream, so the single owner is safe.
     */
    private final float[] excSubframe;

    /**
     * Reusable per subframe noise buffer, sized to {@value #MAX_SF_LEN}.
     *
     * <p>{@link NoiseGenerator#genNoise} fully defines its {@code [0, subframeLength)} region on every call
     * before it is added into {@link #excSubframe}, so one instance owned buffer replaces the per subframe
     * allocation.
     */
    private final float[] noise;

    /**
     * Constructs an MLow low band decoder with a freshly constructed parameter front end and synthesis back end
     * and zeroed cross frame state.
     *
     * <p>The wrapped parameter decoder, LSF interpolator, CELP synthesizer, and noise generator all start in
     * their reset state, and the LPC synthesis memory and filter states start silent, ready to decode the first
     * packet of a stream.
     */
    public MlowDecoder() {
        this.paramDecoder = new ParamDecoder();
        this.subframeLpc = new SubframeLpc();
        this.celpSynthesizer = new CelpSynthesizer();
        this.noiseGenerator = new NoiseGenerator();
        this.plc = new MlowPlc();
        this.lpcSynthMem = new float[LPC_ORDER];
        this.uvPulseShapingState = new float[2];
        this.hpArma2State = new float[4];
        this.tiltState = new float[1];
        this.excSubframe = new float[MAX_SF_LEN];
        this.noise = new float[MAX_SF_LEN];
    }

    /**
     * Returns this decoder to its freshly constructed state.
     *
     * <p>Resets the wrapped parameter decoder, LSF interpolator, CELP synthesizer, and noise generator and
     * zeroes the LPC synthesis memory, the unvoiced pulse shaping state, the high pass state, and the residual
     * energy tracker. Call this between independent decode sessions; do not call it between the packets of one
     * continuous stream, which must thread state.
     */
    public void reset() {
        paramDecoder.reset();
        subframeLpc.reset();
        celpSynthesizer.reset();
        noiseGenerator.reset();
        plc.resetStream();
        Arrays.fill(lpcSynthMem, 0.0f);
        Arrays.fill(uvPulseShapingState, 0.0f);
        Arrays.fill(hpArma2State, 0.0f);
        prevNrgres = 0.0f;
        tiltState[0] = 0.0f;
        lastToc = null;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls mlow decoder: stream reset");
    }

    /**
     * Overlays the concealment comfort noise on one postfiltered packet and folds the packet into the loss
     * burst length.
     *
     * <p>Runs after the postfilter chain, so it operates on the harmonic postfiltered low band signal. On a
     * clean packet with no recent loss it adds nothing and only clears the loss burst length; during and just
     * after a loss it overlays a spectrally matched noise floor faded in and out at the loss boundaries.
     *
     * @param synthesis the packet's postfiltered low band synthesis, mutated in place
     * @param lost      whether this packet was concealed rather than decoded
     */
    public void applyComfortNoiseAndLossInfo(float[] synthesis, boolean lost) {
        var lostFlag = lost ? MlowPlc.FLAG_PACKET_LOST : 0;
        plc.addComfortNoise(synthesis, 0, synthesis.length, lostFlag);
        plc.updateLossInfo(lostFlag, synthesis.length / CELP_FS_KHZ);
    }

    /**
     * Decodes one MLow packet to reconstructed 16 bit PCM.
     *
     * <p>Parses the packet's leading table of contents byte, dispatches a single frame or multiframe MLow
     * packet, decodes every contained MLow frame in order through {@link #decodeFrameFloat(byte[], int, int)},
     * concatenates the float output, and scales it to {@code int16} with a rounding clamp. The cross packet
     * state on this decoder advances, so packets must be supplied in stream order.
     *
     * @param packet the complete MLow packet bytes, beginning with the table of contents byte
     * @return the reconstructed PCM samples for the packet, {@code numFrames * frameLength} entries
     * @throws IllegalArgumentException if {@code packet} is empty, malformed, or announces a sample rate
     *                                  above 16 kHz
     */
    public short[] decode(byte[] packet) {
        var pcm = decodeFloat(packet);
        var out = new short[pcm.length];
        for (var i = 0; i < pcm.length; i++) {
            out[i] = toInt16(pcm[i]);
        }
        return out;
    }

    /**
     * Decodes one MLow packet to reconstructed floating point PCM, the float precision form of
     * {@link #decode(byte[])}.
     *
     * <p>Identical to {@link #decode(byte[])} but returns the synthesized signal at full single precision
     * before the {@code int16} scale and clamp. A caller that wants the raw float reconstruction (for a direct
     * comparison against a float reference pipeline, or for further float domain processing) uses this;
     * {@link #decode(byte[])} is the integer PCM convenience over it.
     *
     * @param packet the complete MLow packet bytes, beginning with the table of contents byte
     * @return the reconstructed float PCM samples for the packet, nominally in {@code [-1, 1]}
     * @throws IllegalArgumentException if {@code packet} is empty, malformed, or announces a sample rate
     *                                  above 16 kHz
     */
    public float[] decodeFloat(byte[] packet) {
        if (packet == null || packet.length < 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "calls mlow decode: empty packet");
            throw new IllegalArgumentException("empty MLow packet");
        }
        var toc = packet[0] & 0xFF;
        if (isMultiframe(toc)) {
            return decodeMultiframe(packet);
        }
        return decodeFrameFloat(packet, 0, packet.length);
    }

    /**
     * Decodes one MLow packet to its pre postfilter float synthesis together with the per packet decode
     * parameters an {@code MlowDecodePostfilter} chain consumes, the synthesis and parameters form of
     * {@link #decodeFloat(byte[])}.
     *
     * <p>Runs the same CELP synthesis as {@link #decode(byte[])} and {@link #decodeFloat(byte[])}, advancing the
     * same cross frame and cross packet state, but instead of high pass filtering and scaling the result it
     * returns, per contained self contained MLow packet, the synthesis (every internal frame's post
     * {@link Filters#ar16}, post tilt samples concatenated) and the LPC coefficient sets, pitch lags, per frame
     * normalized bitrate, voicing, and rate flags that the postfilter chain reads. A multiframe MLow container
     * yields one {@link PacketSynthesis} per contained MLow packet, each the unit the postfilter chain operates
     * over.
     *
     * <p>The returned synthesis is the exact input handed to the postfilter chain: the kernel synthesis with the
     * low rate tilt postfilter applied (the tilt interleaved with the residual synthesis when the LPC
     * postfilter is disabled), captured before the high pass {@link #decodeFloat(byte[])} would apply. When
     * {@code lpcPostfilterEnabled} is {@code true} the tilt is not applied, since the LPC postfilter is the
     * tilt's alternative. A caller that wants the postfilter on reconstruction runs the synthesis of every
     * returned {@link PacketSynthesis} through one stream scoped {@code MlowDecodePostfilter} in order with the
     * same {@code lpcPostfilterEnabled}, then scales to {@code int16}; a caller that wants the bare postfilter
     * off, no tilt PCM uses {@link #decode(byte[])} directly.
     *
     * @param packet               the complete MLow packet bytes, beginning with the table of contents byte
     * @param lpcPostfilterEnabled {@code true} when the postfilter chain will run the LPC postfilter, which
     *                             suppresses the low rate tilt the synthesis would otherwise carry
     * @return the per contained packet pre postfilter synthesis and decode parameters, in stream order
     * @throws IllegalArgumentException if {@code packet} is empty, malformed, or announces a sample rate
     *                                  above 16 kHz
     */
    public DecodeResult decodeWithSynthesis(byte[] packet, boolean lpcPostfilterEnabled) {
        return decodeWithSynthesis(packet, lpcPostfilterEnabled, false);
    }

    /**
     * Decodes one MLow packet to its pre postfilter synthesis, optionally reconstructing the previous lost
     * frame from this packet's leading in band forward error correction copy, the recovery capable form of
     * {@link #decodeWithSynthesis(byte[], boolean)}.
     *
     * <p>When {@code fecRecovery} is {@code true} the packet is decoded through its leading low bitrate
     * redundancy frames rather than its primary frames, reconstructing the frame the previous packet carried.
     * Forward error correction recovery applies only to a single self contained MLow packet, not to a
     * multiframe container.
     *
     * @param packet               the complete MLow packet bytes, beginning with the table of contents byte
     * @param lpcPostfilterEnabled {@code true} when the LPC postfilter suppresses the low rate tilt
     * @param fecRecovery          {@code true} to decode the leading redundancy frames instead of the primary
     *                             frames
     * @return the per contained packet pre postfilter synthesis and decode parameters, in stream order
     * @throws IllegalArgumentException if {@code packet} is empty, malformed, or announces a sample rate
     *                                  above 16 kHz
     */
    public DecodeResult decodeWithSynthesis(byte[] packet, boolean lpcPostfilterEnabled, boolean fecRecovery) {
        if (packet == null || packet.length < 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "calls mlow decode: empty packet");
            throw new IllegalArgumentException("empty MLow packet");
        }
        List<PacketSynthesis> packets = new ArrayList<>();
        var toc = packet[0] & 0xFF;
        if (isMultiframe(toc)) {
            decodeMultiframeWithSynthesis(packet, packets, lpcPostfilterEnabled);
        } else {
            packets.add(decodeFrameWithSynthesis(packet, 0, packet.length, lpcPostfilterEnabled, fecRecovery));
        }
        return new DecodeResult(List.copyOf(packets));
    }

    /**
     * Decodes a multiframe MLow packet to the pre postfilter synthesis of each contained MLow packet, the
     * synthesis and parameters form of {@link #decodeMultiframe(byte[])}.
     *
     * <p>Walks the multiframe layout exactly as {@link #decodeMultiframe(byte[])}, but decodes each contained
     * single frame MLow packet through {@link #decodeFrameWithSynthesis(byte[], int, int, boolean, boolean)} and
     * appends its {@link PacketSynthesis} to {@code out}, threading the cross frame state.
     *
     * @param packet               the complete multiframe MLow packet
     * @param out                  the accumulator the per contained packet synthesis blocks are appended to
     * @param lpcPostfilterEnabled {@code true} when the LPC postfilter suppresses the low rate tilt
     * @throws IllegalArgumentException if the multiframe layout is malformed
     */
    private void decodeMultiframeWithSynthesis(byte[] packet, List<PacketSynthesis> out,
                                               boolean lpcPostfilterEnabled) {
        if (packet.length < 2) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "calls mlow decode: truncated multiframe packet");
            throw new IllegalArgumentException("truncated multiframe MLow packet");
        }
        var numFrames = packet[1] & 0xFF;
        if (numFrames < 2) {
            throw new IllegalArgumentException("multiframe MLow packet with fewer than two frames");
        }
        var sizes = new int[numFrames];
        var pos = 2;
        for (var i = 0; i < numFrames - 1; i++) {
            var first = packet[pos] & 0xFF;
            if (first < 252) {
                sizes[i] = first;
                pos += 1;
            } else {
                if (pos + 1 >= packet.length) {
                    throw new IllegalArgumentException("truncated multiframe MLow size field");
                }
                sizes[i] = 4 * (packet[pos + 1] & 0xFF) + first;
                pos += 2;
            }
            if (sizes[i] <= 0) {
                throw new IllegalArgumentException("malformed multiframe MLow frame size");
            }
        }
        var consumed = 0;
        for (var i = 0; i < numFrames - 1; i++) {
            consumed += sizes[i];
        }
        sizes[numFrames - 1] = packet.length - pos - consumed;
        if (sizes[numFrames - 1] <= 0) {
            throw new IllegalArgumentException("malformed multiframe MLow last-frame size");
        }
        for (var i = 0; i < numFrames; i++) {
            out.add(decodeFrameWithSynthesis(packet, pos, sizes[i], lpcPostfilterEnabled, false));
            pos += sizes[i];
        }
    }

    /**
     * Decodes one self contained single frame MLow packet to its pre postfilter synthesis and decode
     * parameters, the synthesis and parameters form of {@link #decodeFrameFloat(byte[], int, int)}.
     *
     * <p>Decodes the table of contents and the packet's internal 20 ms frames through {@link ParamDecoder},
     * then runs the per frame synthesis loop ({@link #synthesizeFrameWithCapture}) over each decoded frame,
     * accumulating the pre high pass synthesis, the per frame LPC coefficient sets, the per frame pitch lags,
     * and the per frame normalized bitrate. The cross frame state advances exactly as the finished PCM path;
     * the only difference is that the high pass is not applied and the synthesis is returned at full single
     * precision. The packet level voicing and rate flags carried by the returned {@link PacketSynthesis} are
     * those of the packet's first internal frame, matching the postfilter chain's per packet gamma selection.
     *
     * @param packet               the backing array holding the single frame packet
     * @param offset               the offset of the table of contents byte within {@code packet}
     * @param length               the length of the single frame packet, including the table of contents byte
     * @param lpcPostfilterEnabled {@code true} when the LPC postfilter suppresses the low rate tilt
     * @param fecRecovery          {@code true} to decode the leading redundancy frames instead of the primary
     *                             frames
     * @return the pre postfilter synthesis and decode parameters of the packet
     * @throws IllegalArgumentException if the packet announces a sample rate above 16 kHz
     */
    private PacketSynthesis decodeFrameWithSynthesis(byte[] packet, int offset, int length,
                                                     boolean lpcPostfilterEnabled, boolean fecRecovery) {
        var tocByte = MlowTocByte.decode(packet[offset] & 0xFF);
        if (fecRecovery && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "calls mlow decode: fec recovery, bytes={0}", length);
        }
        plc.reset(tocByte.sid());
        lastToc = tocByte;
        var decodedFrames =
                paramDecoder.decodePacket(tocByte, packet, offset, length, fecRecovery);

        var frameLength = tocByte.frameLength16();
        var numSubframes = tocByte.numSubframes();
        var subframeLength = frameLength / numSubframes;
        var lowRate = tocByte.lowRate();
        var lagsPerSubframe = subframeLength / LAG_SUBFRLEN;
        var lagsPerFrame = frameLength / LAG_SUBFRLEN;
        var numFrames = decodedFrames.length;

        var synthesis = new float[numFrames * frameLength];
        var lpc = new float[numFrames][][];
        var lagsPerPacket = new float[numFrames * lagsPerFrame];
        var normalizedBitratePerFrame = new float[numFrames];
        for (var frame = 0; frame < numFrames; frame++) {
            var frameLpc = new float[numSubframes][];
            var frameLags = new float[lagsPerFrame];
            var frameNormalizedBitrate = new float[1];
            var frameOut = synthesizeFrameWithCapture(decodedFrames[frame], tocByte, frame, frameLength,
                    numSubframes, subframeLength, lowRate, lagsPerSubframe, lagsPerFrame, lpcPostfilterEnabled,
                    frameLpc, frameLags, frameNormalizedBitrate);
            System.arraycopy(frameOut, 0, synthesis, frame * frameLength, frameLength);
            lpc[frame] = frameLpc;
            System.arraycopy(frameLags, 0, lagsPerPacket, frame * lagsPerFrame, lagsPerFrame);
            normalizedBitratePerFrame[frame] = frameNormalizedBitrate[0];
        }

        var voiced = numFrames > 0 && decodedFrames[0].voiced();
        return new PacketSynthesis(synthesis, numFrames, lpc, numSubframes, subframeLength, lagsPerPacket,
                normalizedBitratePerFrame, voiced, lowRate);
    }

    /**
     * Conceals one lost MLow packet to its pre postfilter synthesis and decode parameters, the concealment
     * counterpart of {@link #decodeWithSynthesis(byte[], boolean)}.
     *
     * <p>Fabricates a plausible continuation of the stream from the concealment state the good frames left: for
     * each contained 20 ms frame it fills the concealed low band parameters ({@link MlowPlc#concealCelp}),
     * cross fades the adaptive codebook history to hide pitch lag instability ({@link MlowPlc#blendLtp}), then
     * runs the same CELP synthesis loop the good path runs, with the loss mode excitation decay and shaped noise
     * injection ({@link MlowPlc#decayExc}) replacing the reset call. The concealed packet inherits its geometry
     * (subframe count and rate mode) from the last good packet's table of contents.
     *
     * @param packetSamples        the number of samples to conceal, a whole number of 20 ms frames (960 for a
     *                             60 ms packet)
     * @param lpcPostfilterEnabled {@code true} when the LPC postfilter suppresses the low rate tilt
     * @return the concealed packet's pre postfilter synthesis and decode parameters, a single element result
     */
    public DecodeResult concealWithSynthesis(int packetSamples, boolean lpcPostfilterEnabled) {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "calls mlow packet loss concealment: samples={0}", packetSamples);
        var lowRate = lastToc != null && lastToc.lowRate();
        var frameLength = 20 * CELP_FS_KHZ;
        var numFrames = packetSamples / frameLength;
        var numSubframes = 1 << (2 - (lowRate ? 1 : 0));
        var subframeLength = frameLength / numSubframes;
        var lagsPerSubframe = subframeLength / LAG_SUBFRLEN;
        var lagsPerFrame = frameLength / LAG_SUBFRLEN;
        var voiced = plc.voiced();

        var synthesis = new float[numFrames * frameLength];
        var lpc = new float[numFrames][][];
        var lagsPerPacket = new float[numFrames * lagsPerFrame];
        var normalizedBitratePerFrame = new float[numFrames];
        for (var frame = 0; frame < numFrames; frame++) {
            var frameLpc = new float[numSubframes][];
            var frameLags = new float[lagsPerFrame];
            var frameNormalizedBitrate = new float[1];
            var frameOut = concealFrameSynthesis(frameLength, numSubframes, subframeLength, lowRate,
                    lagsPerSubframe, lagsPerFrame, lpcPostfilterEnabled, frameLpc, frameLags,
                    frameNormalizedBitrate);
            System.arraycopy(frameOut, 0, synthesis, frame * frameLength, frameLength);
            lpc[frame] = frameLpc;
            System.arraycopy(frameLags, 0, lagsPerPacket, frame * lagsPerFrame, lagsPerFrame);
            normalizedBitratePerFrame[frame] = frameNormalizedBitrate[0];
        }

        var packet = new PacketSynthesis(synthesis, numFrames, lpc, numSubframes, subframeLength,
                lagsPerPacket, normalizedBitratePerFrame, voiced, lowRate);
        return new DecodeResult(List.of(packet));
    }

    /**
     * Synthesizes one concealed 20 ms frame from the concealment state, the loss mode counterpart of
     * {@link #synthesizeFrameWithCapture}.
     *
     * <p>Fabricates the frame's low band parameters through {@link MlowPlc#concealCelp} (repeating the last
     * pitch cycle with a lowered gain and drifting lag), cross fades the adaptive codebook history through
     * {@link MlowPlc#blendLtp}, and records the recovery reference through {@link MlowPlc#updateRecoveryInfo}.
     * The synthesis then mirrors {@link #synthesizeFrameWithCapture} with two differences: the fixed codebook
     * excitation is empty (concealment carries no pulses, so the excitation is the adaptive codebook plus the
     * shaped noise), and the per subframe excitation decay call runs in its loss mode, muting unvoiced
     * excitation or injecting energy matched noise on voiced excitation.
     *
     * @param frameLength          the frame length in samples
     * @param numSubframes         the subframe count
     * @param subframeLength       the subframe length in samples
     * @param lowRate              {@code true} for the low rate mode
     * @param lagsPerSubframe      the number of lag subframes per subframe
     * @param lagsPerFrame         the number of lag subframes per frame
     * @param lpcPostfilterEnabled {@code true} when the LPC postfilter suppresses the low rate tilt
     * @param lpcOut               the per subframe LPC capture buffer
     * @param lagsOut              the frame lag capture buffer
     * @param normalizedBitrateOut the single entry normalized bitrate capture buffer
     * @return the concealed frame's pre high pass float PCM
     */
    private float[] concealFrameSynthesis(int frameLength, int numSubframes, int subframeLength, boolean lowRate,
                                          int lagsPerSubframe, int lagsPerFrame, boolean lpcPostfilterEnabled,
                                          float[][] lpcOut, float[] lagsOut, float[] normalizedBitrateOut) {
        var aFlat = new float[numSubframes * (LPC_ORDER + 1)];
        var lsfsFlat = new float[numSubframes * LPC_ORDER];
        var acbGainsFlat = new float[numSubframes * PLC_ACBG_M];
        var lags = new float[lagsPerFrame];
        var cp = plc.concealCelp(aFlat, lsfsFlat, acbGainsFlat, lags, numSubframes,
                subframeLength);
        var voiced = cp.voiced();

        plc.blendLtp(celpSynthesizer.acbState(), lags[0]);
        plc.updateRecoveryInfo(voiced);

        var normalizedBitrate = normalizedBitrate(cp.nPulses(), frameLength);
        var lpcRes = new float[frameLength];

        var y = new float[LPC_ORDER + frameLength];
        System.arraycopy(lpcSynthMem, 0, y, 0, LPC_ORDER);

        for (var sf = 0; sf < numSubframes; sf++) {
            var acbGain = new float[]{acbGainsFlat[sf * PLC_ACBG_M], acbGainsFlat[sf * PLC_ACBG_M + 1]};
            var lsfSf = new float[LPC_ORDER];
            System.arraycopy(lsfsFlat, sf * LPC_ORDER, lsfSf, 0, LPC_ORDER);
            var aSf = new float[LPC_ORDER + 1];
            System.arraycopy(aFlat, sf * (LPC_ORDER + 1), aSf, 0, LPC_ORDER + 1);

            System.arraycopy(lpcRes, sf * subframeLength, excSubframe, 0, subframeLength);
            celpSynthesizer.celpDecode(voiced, acbGain, sliceLags(lags, sf * lagsPerSubframe, lagsPerSubframe),
                    lagsPerSubframe, subframeLength, lowRate, normalizedBitrate, excSubframe);

            var nrgres = ResNrgDequantizer.dequantizeResnrg(cp.nrgresDbqQ14(), subframeLength);
            if (!voiced) {
                prevNrgres = nrgres;
            }

            noiseGenerator.genNoise(excSubframe, subframeLength, voiced, cp.sfPulses(), nrgres,
                    cp.fcbgIdx(), lsfSf, normalizedBitrate, noise);

            var lowRateIx = lowRate ? 1 : 0;
            if (!voiced && cp.sfPulses() > 0 && UV_PULSE_SHAPING_COEFS[lowRateIx][0][0] < 1.0f) {
                Filters.arma1(excSubframe, 0, subframeLength, UV_PULSE_SHAPING_COEFS[lowRateIx][0],
                        UV_PULSE_SHAPING_COEFS[lowRateIx][1], uvPulseShapingState, 0);
            } else {
                uvPulseShapingState[0] = 0.0f;
                uvPulseShapingState[1] = 0.0f;
            }

            for (var i = 0; i < subframeLength; i++) {
                excSubframe[i] += noise[i];
            }

            var voicedIx = voiced ? 1 : 0;
            if (!lpcPostfilterEnabled) {
                if (lowRate && POST_TILT_COEFS[voicedIx][0] < 1.0f) {
                    var tilted = new float[subframeLength];
                    Filters.ma1(excSubframe, 0, subframeLength, POST_TILT_COEFS[voicedIx], tiltState, 0,
                            tilted, 0);
                    System.arraycopy(tilted, 0, excSubframe, 0, subframeLength);
                } else {
                    tiltState[0] = excSubframe[subframeLength - 1];
                }
            }

            plc.decayExc(excSubframe, 0, subframeLength, false, voiced);
            var excNrg = 0.0f;
            for (var i = 0; i < subframeLength; i++) {
                excNrg += excSubframe[i] * excSubframe[i];
            }
            plc.updateNrg(excNrg, subframeLength);

            Filters.ar16(excSubframe, 0, subframeLength, aSf, y, LPC_ORDER + sf * subframeLength);
            lpcOut[sf] = aSf.clone();
        }

        System.arraycopy(y, frameLength, lpcSynthMem, 0, LPC_ORDER);
        var frameOut = new float[frameLength];
        System.arraycopy(y, LPC_ORDER, frameOut, 0, frameLength);
        plc.carryConcealStateEmph(frameOut[frameLength - 1]);
        System.arraycopy(lags, 0, lagsOut, 0, lagsPerFrame);
        normalizedBitrateOut[0] = normalizedBitrate;
        return frameOut;
    }

    /**
     * Synthesizes one internal 20 ms frame to its pre high pass float PCM and captures the decode parameters the
     * postfilter chain reads, the parameter capturing form of
     * {@link #synthesizeFrame(ParamDecoder.DecodedFrame, MlowTocByte, int, int, int, boolean, int, int)}.
     *
     * <p>Runs the identical synthesis as {@link #synthesizeFrame} up to and including the short term synthesis
     * filter and the synthesis memory save, but additionally applies the low rate tilt postfilter to the
     * excitation before the short term synthesis filter (when the frame is low rate, the tilt coefficient is
     * non trivial, and the LPC postfilter is disabled), returns the assembled frame before the high pass, and
     * writes the per subframe LPC coefficient sets, the frame's pitch lags, and the frame's normalized bitrate
     * into the supplied capture buffers. The tilt memory ({@link #tiltState}) threads across subframes, frames,
     * and packets.
     *
     * @param df                       the decoded parameters of this frame
     * @param toc                      the decoded table of contents of the packet
     * @param frameNum                 the index of this frame within the packet
     * @param frameLength              the frame length in samples
     * @param numSubframes             the subframe count of the frame
     * @param subframeLength           the subframe length in samples
     * @param lowRate                  {@code true} for the low rate mode
     * @param lagsPerSubframe          the number of lag subframes per subframe
     * @param lagsPerFrame             the number of lag subframes per frame
     * @param lpcPostfilterEnabled     {@code true} when the LPC postfilter suppresses the low rate tilt
     * @param lpcOut                   the per subframe LPC capture buffer, filled with each subframe's
     *                                 {@value #LPC_ORDER} plus one coefficient set
     * @param lagsOut                  the frame lag capture buffer of {@code lagsPerFrame} entries
     * @param normalizedBitrateOut     the single entry normalized bitrate capture buffer
     * @return the pre high pass float PCM of the frame, {@code frameLength} entries
     */
    private float[] synthesizeFrameWithCapture(ParamDecoder.DecodedFrame df, MlowTocByte toc, int frameNum,
                                               int frameLength, int numSubframes, int subframeLength,
                                               boolean lowRate, int lagsPerSubframe, int lagsPerFrame,
                                               boolean lpcPostfilterEnabled, float[][] lpcOut, float[] lagsOut,
                                               float[] normalizedBitrateOut) {
        var voiced = df.voiced();
        // On the first frame after a loss the previous frame LSF vector is adapted toward the concealment or
        // comfort noise spectrum before interpolation; on a clean stream this does nothing.
        if (frameNum == 0) {
            plc.adaptLsf(subframeLpc.previousLsf(), LPC_ORDER);
        }
        var interpolated = subframeLpc.process(df, toc);
        var a = interpolated.lpc();
        var lsfs = interpolated.lsf();
        // Bandwidth expand the recovered filters on the first good voiced frames after a voiced loss; does
        // nothing on a clean stream.
        plc.bweRecover(voiced, a, numSubframes, frameLength / CELP_FS_KHZ);

        var lags = new float[lagsPerFrame];
        for (var i = 0; i < lagsPerFrame; i++) {
            lags[i] = voiced ? df.laginds()[i] * 0.5f + MIN_PITCH_LAG : 0.0f;
        }

        // Dequantize every subframe's adaptive codebook gains up front so the concealment state maintenance
        // sees the whole frame's gains before the synthesis loop runs.
        var acbGainsPerSf = new float[numSubframes][];
        var acbGainsFlat = new float[numSubframes * PLC_ACBG_M];
        for (var sf = 0; sf < numSubframes; sf++) {
            acbGainsPerSf[sf] = CelpSynthesizer.acbDequant(lowRate, df.acbgIdx()[sf]);
            acbGainsFlat[sf * PLC_ACBG_M] = acbGainsPerSf[sf][0];
            acbGainsFlat[sf * PLC_ACBG_M + 1] = acbGainsPerSf[sf][1];
        }
        plc.updateCelp(voiced, df.nrgresDbqQ14()[numSubframes - 1], df.sfPulses()[numSubframes - 1],
                df.fcbgIdx()[numSubframes - 1], acbGainsFlat, a[numSubframes - 1], lsfs[numSubframes - 1],
                lags, lagsPerFrame, numSubframes, subframeLength);

        var normalizedBitrate = normalizedBitrate(df.nPulses(), frameLength);

        var lpcRes = new float[frameLength];
        celpSynthesizer.genExcitation(df.fcbgIdx(), voiced, numSubframes, subframeLength,
                df.nPositions(), df.positions(), df.posPulses(), lpcRes);

        // y holds LPC_ORDER history samples then the frame's frameLength output samples.
        var y = new float[LPC_ORDER + frameLength];
        System.arraycopy(lpcSynthMem, 0, y, 0, LPC_ORDER);

        var nrgresLinear = new float[numSubframes];
        for (var sf = 0; sf < numSubframes; sf++) {
            var acbGain = acbGainsPerSf[sf];
            System.arraycopy(lpcRes, sf * subframeLength, excSubframe, 0, subframeLength);
            celpSynthesizer.celpDecode(voiced, acbGain, sliceLags(lags, sf * lagsPerSubframe, lagsPerSubframe),
                    lagsPerSubframe, subframeLength, lowRate, normalizedBitrate, excSubframe);

            var nrgres = ResNrgDequantizer.dequantizeResnrg(df.nrgresDbqQ14()[sf], subframeLength);
            nrgresLinear[sf] = nrgres;
            if (!voiced) {
                prevNrgres = nrgres;
            }

            noiseGenerator.genNoise(excSubframe, subframeLength, voiced, df.sfPulses()[sf], nrgres,
                    df.fcbgIdx()[sf], lsfs[sf], normalizedBitrate, noise);

            var lowRateIx = lowRate ? 1 : 0;
            if (!voiced && df.sfPulses()[sf] > 0 && UV_PULSE_SHAPING_COEFS[lowRateIx][0][0] < 1.0f) {
                Filters.arma1(excSubframe, 0, subframeLength, UV_PULSE_SHAPING_COEFS[lowRateIx][0],
                        UV_PULSE_SHAPING_COEFS[lowRateIx][1], uvPulseShapingState, 0);
            } else {
                uvPulseShapingState[0] = 0.0f;
                uvPulseShapingState[1] = 0.0f;
            }

            for (var i = 0; i < subframeLength; i++) {
                excSubframe[i] += noise[i];
            }

            var voicedIx = voiced ? 1 : 0;
            if (!lpcPostfilterEnabled) {
                if (lowRate && POST_TILT_COEFS[voicedIx][0] < 1.0f) {
                    var tilted = new float[subframeLength];
                    Filters.ma1(excSubframe, 0, subframeLength, POST_TILT_COEFS[voicedIx], tiltState, 0,
                            tilted, 0);
                    System.arraycopy(tilted, 0, excSubframe, 0, subframeLength);
                } else {
                    tiltState[0] = excSubframe[subframeLength - 1];
                }
            }

            // Concealment excitation maintenance on a good frame: the reset call clears the attenuation
            // accumulator without touching the excitation, and the energy is recorded for a future lost
            // frame's pitch candidate search; neither alters the good decode output.
            plc.decayExc(excSubframe, 0, subframeLength, true, voiced);
            var excNrg = 0.0f;
            for (var i = 0; i < subframeLength; i++) {
                excNrg += excSubframe[i] * excSubframe[i];
            }
            plc.updateNrg(excNrg, subframeLength);

            Filters.ar16(excSubframe, 0, subframeLength, a[sf], y, LPC_ORDER + sf * subframeLength);
            lpcOut[sf] = a[sf].clone();
        }

        System.arraycopy(y, frameLength, lpcSynthMem, 0, LPC_ORDER);

        var frameOut = new float[frameLength];
        System.arraycopy(y, LPC_ORDER, frameOut, 0, frameLength);

        // Maintain the comfort noise background candidate model from this frame's synthesis; on an inactive
        // (non voice) frame the lowest energy subframe is recorded as a candidate a later concealment draws
        // its noise floor from.
        var lsfsFlat = new float[numSubframes * LPC_ORDER];
        for (var sf = 0; sf < numSubframes; sf++) {
            System.arraycopy(lsfs[sf], 0, lsfsFlat, sf * LPC_ORDER, LPC_ORDER);
        }
        plc.updateCng(frameOut, 0, voiced, toc.vad(), numSubframes, subframeLength, nrgresLinear, lsfsFlat);

        System.arraycopy(lags, 0, lagsOut, 0, lagsPerFrame);
        normalizedBitrateOut[0] = normalizedBitrate;
        return frameOut;
    }

    /**
     * The pre postfilter synthesis and decode parameters of one decoded MLow packet, the input one
     * {@code MlowDecodePostfilter} pass over a self contained MLow packet consumes.
     *
     * <p>Carries every value the postfilter chain's entry point needs: the synthesis (every internal frame's
     * post {@link Filters#ar16}, post tilt samples concatenated), the per frame per subframe LPC coefficient
     * sets, the per frame pitch lags, the per frame normalized bitrate, and the packet's voicing and rate
     * flags. The {@link #synthesis()} array is owned by the caller and is the buffer the postfilter rewrites in
     * place.
     *
     * @param synthesis                  the pre postfilter synthesis, {@code numFrames * frameLength} samples
     * @param numFrames                  the number of internal frames in the packet
     * @param lpc                        the per frame per subframe LPC coefficient sets, indexed
     *                                   {@code [frame][subframe][0..LPC_ORDER]} with index zero the monic
     *                                   {@code 1.0f}
     * @param numSubframes               the number of subframes per internal frame
     * @param subframeLength             the subframe length in samples
     * @param lagsPerPacket              the packet's pitch lags, one per lag subframe, frames concatenated
     * @param normalizedBitratePerFrame  the per frame normalized bitrate, {@code numFrames} entries
     * @param voiced                     {@code true} when the packet's first internal frame is voiced
     * @param lowRate                    {@code true} for the low rate mode
     */
    public record PacketSynthesis(float[] synthesis, int numFrames, float[][][] lpc, int numSubframes,
                                  int subframeLength, float[] lagsPerPacket, float[] normalizedBitratePerFrame,
                                  boolean voiced, boolean lowRate) {
    }

    /**
     * The result of {@link #decodeWithSynthesis(byte[], boolean)}: the per contained packet pre postfilter
     * synthesis and decode parameters of one decoded MLow packet, in stream order.
     *
     * <p>A single frame MLow packet yields one {@link PacketSynthesis}; a multiframe MLow container yields one
     * per contained MLow packet. A caller runs each {@link PacketSynthesis} through a stream scoped
     * {@code MlowDecodePostfilter} in list order to reproduce the live decode.
     *
     * @param packets the per contained packet synthesis blocks, in stream order
     */
    public record DecodeResult(List<PacketSynthesis> packets) {
    }

    /**
     * Decodes a multiframe MLow packet by splitting it into its contained single frame packets and decoding
     * each in stream order.
     *
     * <p>A multiframe MLow packet is a one byte multiframe table of contents, a one byte contained frame count,
     * a sequence of self delimiting per frame sizes (all but the last, whose size is the remaining bytes), and
     * then the contained single frame MLow packets back to back, each with its own table of contents and body.
     * This method walks that layout, decodes each contained frame through
     * {@link #decodeFrameFloat(byte[], int, int)} threading the cross frame state, and concatenates the float
     * output.
     *
     * @param packet the complete multiframe MLow packet
     * @return the reconstructed float PCM for every contained frame, concatenated in order
     * @throws IllegalArgumentException if the multiframe layout is malformed
     */
    private float[] decodeMultiframe(byte[] packet) {
        if (packet.length < 2) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "calls mlow decode: truncated multiframe packet");
            throw new IllegalArgumentException("truncated multiframe MLow packet");
        }
        var numFrames = packet[1] & 0xFF;
        if (numFrames < 2) {
            throw new IllegalArgumentException("multiframe MLow packet with fewer than two frames");
        }
        var sizes = new int[numFrames];
        var pos = 2;
        for (var i = 0; i < numFrames - 1; i++) {
            var first = packet[pos] & 0xFF;
            if (first < 252) {
                sizes[i] = first;
                pos += 1;
            } else {
                if (pos + 1 >= packet.length) {
                    throw new IllegalArgumentException("truncated multiframe MLow size field");
                }
                sizes[i] = 4 * (packet[pos + 1] & 0xFF) + first;
                pos += 2;
            }
            if (sizes[i] <= 0) {
                throw new IllegalArgumentException("malformed multiframe MLow frame size");
            }
        }
        var consumed = 0;
        for (var i = 0; i < numFrames - 1; i++) {
            consumed += sizes[i];
        }
        sizes[numFrames - 1] = packet.length - pos - consumed;
        if (sizes[numFrames - 1] <= 0) {
            throw new IllegalArgumentException("malformed multiframe MLow last-frame size");
        }
        var frames = new float[numFrames][];
        var total = 0;
        for (var i = 0; i < numFrames; i++) {
            frames[i] = decodeFrameFloat(packet, pos, sizes[i]);
            total += frames[i].length;
            pos += sizes[i];
        }
        var out = new float[total];
        var off = 0;
        for (var frame : frames) {
            System.arraycopy(frame, 0, out, off, frame.length);
            off += frame.length;
        }
        return out;
    }

    /**
     * Decodes one self contained single frame MLow packet (table of contents plus body) to reconstructed float
     * PCM.
     *
     * <p>Decodes the table of contents, then decodes the packet's internal 20 ms frames through
     * {@link ParamDecoder}, and runs the per frame synthesis loop ({@link #synthesizeFrame}) over each decoded
     * frame, concatenating the float output. The cross frame state on this decoder advances. A SID packet
     * decodes only its first internal frame.
     *
     * @param packet the backing array holding the single frame packet
     * @param offset the offset of the table of contents byte within {@code packet}
     * @param length the length of the single frame packet, including the table of contents byte
     * @return the reconstructed float PCM for the packet
     * @throws IllegalArgumentException if the packet announces a sample rate above 16 kHz
     */
    private float[] decodeFrameFloat(byte[] packet, int offset, int length) {
        var tocByte = MlowTocByte.decode(packet[offset] & 0xFF);
        // TODO: wire MlowBandwidthExtension: on the above 16 kHz (SWB/FB) branch ParamDecoder currently rejects, instantiate MlowBandwidthExtension and call decodeWideband(low band, LPC residual, nyquist gains, MlowHbParamDecoder HbFrameInput) to synthesize 32/48 kHz PCM, gated on the packet announced sample rate from tocByte
        // TODO: wire MlowLsfInterpolTables: reachable via decodeWideband, then hbLsfInterpolate, then MlowLsfInterpolTables.factors once the SWB bandwidth extension path is wired here and in MLowAudioDecoder.decode
        var decodedFrames = paramDecoder.decodePacket(tocByte, packet, offset, length);

        var frameLength = tocByte.frameLength16();
        var numSubframes = tocByte.numSubframes();
        var subframeLength = frameLength / numSubframes;
        var lowRate = tocByte.lowRate();
        var lagsPerSubframe = subframeLength / LAG_SUBFRLEN;
        var lagsPerFrame = frameLength / LAG_SUBFRLEN;

        var out = new float[decodedFrames.length * frameLength];
        for (var frame = 0; frame < decodedFrames.length; frame++) {
            var frameOut = synthesizeFrame(decodedFrames[frame], tocByte, frameLength, numSubframes,
                    subframeLength, lowRate, lagsPerSubframe, lagsPerFrame);
            System.arraycopy(frameOut, 0, out, frame * frameLength, frameLength);
        }
        return out;
    }

    /**
     * Synthesizes one internal 20 ms frame to float PCM, the body of the per frame decode loop.
     *
     * <p>Interpolates the frame's LSF to per subframe LPC, dequantizes the pitch lags, builds the fixed
     * codebook excitation, then per subframe adds the adaptive codebook contribution and the shaped noise,
     * applies the optional unvoiced pulse shaping ARMA, and runs the short term synthesis filter to produce the
     * subframe speech against the carried LPC synthesis memory. After the subframes the frame's trailing
     * {@value #LPC_ORDER} samples are saved as the next frame's synthesis memory and the assembled frame is high
     * pass filtered.
     *
     * @param df              the decoded parameters of this frame
     * @param toc             the decoded table of contents of the packet
     * @param frameLength     the frame length in samples
     * @param numSubframes    the subframe count of the frame
     * @param subframeLength  the subframe length in samples
     * @param lowRate         {@code true} for the low rate mode
     * @param lagsPerSubframe the number of lag subframes per subframe
     * @param lagsPerFrame    the number of lag subframes per frame
     * @return the high pass filtered float PCM of the frame, {@code frameLength} entries
     */
    private float[] synthesizeFrame(ParamDecoder.DecodedFrame df, MlowTocByte toc, int frameLength,
                                    int numSubframes, int subframeLength, boolean lowRate,
                                    int lagsPerSubframe, int lagsPerFrame) {
        var voiced = df.voiced();
        var interpolated = subframeLpc.process(df, toc);
        var a = interpolated.lpc();
        var lsfs = interpolated.lsf();

        var lags = new float[lagsPerFrame];
        for (var i = 0; i < lagsPerFrame; i++) {
            lags[i] = voiced ? df.laginds()[i] * 0.5f + MIN_PITCH_LAG : 0.0f;
        }

        var normalizedBitrate = normalizedBitrate(df.nPulses(), frameLength);

        var lpcRes = new float[frameLength];
        celpSynthesizer.genExcitation(df.fcbgIdx(), voiced, numSubframes, subframeLength,
                df.nPositions(), df.positions(), df.posPulses(), lpcRes);

        // y holds LPC_ORDER history samples then the frame's frameLength output samples.
        var y = new float[LPC_ORDER + frameLength];
        System.arraycopy(lpcSynthMem, 0, y, 0, LPC_ORDER);

        for (var sf = 0; sf < numSubframes; sf++) {
            var acbGain = CelpSynthesizer.acbDequant(lowRate, df.acbgIdx()[sf]);
            System.arraycopy(lpcRes, sf * subframeLength, excSubframe, 0, subframeLength);
            celpSynthesizer.celpDecode(voiced, acbGain, sliceLags(lags, sf * lagsPerSubframe, lagsPerSubframe),
                    lagsPerSubframe, subframeLength, lowRate, normalizedBitrate, excSubframe);

            var nrgres = ResNrgDequantizer.dequantizeResnrg(df.nrgresDbqQ14()[sf], subframeLength);
            if (!voiced) {
                prevNrgres = nrgres;
            }

            noiseGenerator.genNoise(excSubframe, subframeLength, voiced, df.sfPulses()[sf], nrgres,
                    df.fcbgIdx()[sf], lsfs[sf], normalizedBitrate, noise);

            var lowRateIx = lowRate ? 1 : 0;
            if (!voiced && df.sfPulses()[sf] > 0 && UV_PULSE_SHAPING_COEFS[lowRateIx][0][0] < 1.0f) {
                Filters.arma1(excSubframe, 0, subframeLength, UV_PULSE_SHAPING_COEFS[lowRateIx][0],
                        UV_PULSE_SHAPING_COEFS[lowRateIx][1], uvPulseShapingState, 0);
            } else {
                uvPulseShapingState[0] = 0.0f;
                uvPulseShapingState[1] = 0.0f;
            }

            for (var i = 0; i < subframeLength; i++) {
                excSubframe[i] += noise[i];
            }

            Filters.ar16(excSubframe, 0, subframeLength, a[sf], y, LPC_ORDER + sf * subframeLength);
        }

        System.arraycopy(y, frameLength, lpcSynthMem, 0, LPC_ORDER);

        var frameOut = new float[frameLength];
        System.arraycopy(y, LPC_ORDER, frameOut, 0, frameLength);
        Filters.arma2(frameOut, 0, frameLength, HP_B2, HP_A2, hpArma2State, 0);
        return frameOut;
    }

    /**
     * Extracts one subframe's slice of the frame lag array.
     *
     * @param lags   the frame's lag array
     * @param offset the index of the subframe's first lag
     * @param length the number of lags spanning the subframe
     * @return a freshly allocated copy of the subframe's lags
     */
    private static float[] sliceLags(float[] lags, int offset, int length) {
        var out = new float[length];
        System.arraycopy(lags, offset, out, 0, length);
        return out;
    }

    /**
     * Computes the frame's normalized bitrate from its pulse count.
     *
     * <p>Forms the pulse density per 20 ms, applies a base 2 logarithm and sigmoid mapping, and returns the
     * normalized bitrate in {@code [0, 1]} that the adaptive codebook high boost and the unvoiced noise
     * envelope interpolate against.
     *
     * @param numPulses   the frame's total pulse count
     * @param frameLength the frame length in samples
     * @return the normalized bitrate in {@code [0, 1]}
     */
    private static float normalizedBitrate(int numPulses, int frameLength) {
        var pulsesPer20ms = (numPulses * frameLength) / (20.0f * 16.0f);
        var x = 1.4f * (float) (Math.log(pulsesPer20ms + 1.0f) / Math.log(2.0)) - 6.5f;
        return sigmoid(x);
    }

    /**
     * Computes the numerically guarded logistic sigmoid.
     *
     * <p>Saturates to one above {@code 80} and to zero below {@code -80} to keep the exponential finite, as the
     * normalized bitrate mapping requires.
     *
     * @param x the argument
     * @return the logistic value in {@code [0, 1]}
     */
    private static float sigmoid(float x) {
        if (x > 80.0f) {
            return 1.0f;
        }
        if (x < -80.0f) {
            return 0.0f;
        }
        return (float) (1.0 / (1.0 + Math.exp(-x)));
    }

    /**
     * Returns whether a table of contents byte marks a CELT coded packet.
     *
     * <p>A table of contents byte whose top two bits are both set is a CELT packet. The low band decoder does
     * not decode CELT, so a CELT packet must be handled separately rather than fed to the MLow frame decoder or
     * mistaken for a multiframe container.
     *
     * @param toc the table of contents byte, read from the low eight bits
     * @return {@code true} when the packet is CELT coded
     */
    private static boolean isCelt(int toc) {
        return (toc & 0xC0) == 0xC0;
    }

    /**
     * Returns whether a table of contents byte marks a multiframe MLow packet.
     *
     * <p>A non {@linkplain #isCelt(int) CELT} MLow table of contents byte with both the SID and FEC bits of
     * {@link #MULTI_TOC_MASK} set is a multiframe packet whose second byte is the contained frame count; a CELT
     * byte is never multiframe. Otherwise the packet carries a single self contained MLow frame.
     *
     * @param toc the table of contents byte, read from the low eight bits
     * @return {@code true} when the packet is a multiframe MLow packet
     */
    private static boolean isMultiframe(int toc) {
        return !isCelt(toc) && (toc & MULTI_TOC_MASK) == MULTI_TOC_MASK;
    }

    /**
     * Scales one float sample to a rounded signed 16 bit value clamped to {@code [-32768, 32767]}.
     *
     * @param sample the float sample, nominally in {@code [-1, 1]}
     * @return the rounded and clamped {@code int16} sample
     */
    private static short toInt16(float sample) {
        var v = Math.round(sample * PCM_SCALE);
        if (v > 32767) {
            v = 32767;
        } else if (v < -32768) {
            v = -32768;
        }
        return (short) v;
    }
}
