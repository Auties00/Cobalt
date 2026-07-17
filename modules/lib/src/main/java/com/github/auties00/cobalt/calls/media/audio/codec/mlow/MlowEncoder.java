package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode.CoreEncoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode.LbQuantParams;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode.ParamEncoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode.Vad;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeEncoder;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Encodes low band speech into MLow codec packets, the exact inverse of {@link MlowDecoder}.
 *
 * <p>This is the final stage of the MLow encode pipeline. It wires the parameter back end
 * ({@link ParamEncoder}) and the range coder ({@link MlowRangeEncoder}) into one call that turns a frame's,
 * or a 60 ms packet's three frames', quantized low band parameters into a coded MLow packet that
 * {@link MlowDecoder} decodes back. For each internal 20 ms frame it range encodes the full low band
 * parameter set ({@link ParamEncoder#encodeFrame}), then finalizes the range coder over the whole payload
 * buffer ({@link MlowRangeEncoder#finish()}), strips the trailing zero bytes the decoder fills in, and
 * prepends the table of contents (TOC) byte ({@link #encodeToc}).
 *
 * <p>Two encode entry points exist. {@link #encodeFrames(LbQuantParams[], boolean, int, boolean, boolean,
 * boolean, boolean)} takes the per frame {@link LbQuantParams} the analysis by synthesis front end produces
 * and emits the packet bytes, exposing the final range register through {@link #lastFinalRange()}.
 * {@link #encode(short[])} is the full microphone input path: it runs the voice activity detector
 * ({@link Vad}) and the per packet orchestrator ({@link CoreEncoder}) to turn raw 16 kHz PCM into the range
 * coded payload, then finalizes the packet exactly as {@link #encodeFrames} does. The orchestrator runs the
 * whole feature extraction front end (high pass filtering, windowing, LPC analysis, perceptual weighting,
 * open loop pitch, voice activity and signal mode decision, the bitrate controller, the CELP analysis by
 * synthesis, and the residual energy quantizer) and serializes each frame in the same loop so the bitrate
 * controller feedback sees the real per frame bit count.
 *
 * <p>This encoder is stateful across the frames of a packet and across the packets of one continuous stream:
 * the wrapped {@link ParamEncoder} threads the conditional coding predictors and the pitch lag carry from one
 * frame to the next. Construct one encoder per logical stream and feed it packets in order; {@link #reset()}
 * returns the pipeline to its freshly constructed state.
 *
 * <p>Scope is the 16 kHz, 60 ms (and the 10 and 20 ms sub cases), mono, low band, active voice path at either
 * rate class: the high rate (9600 bps) four subframe path and the low rate (6000 bps) two subframe path,
 * selected by the bitrate the encoder is constructed at (see {@link #MlowEncoder(int)}). The high band
 * (32 and 48 kHz), the inband forward error correction, and the CELT fallback are out of scope and not
 * emitted. The discontinuous transmission comfort noise path is emitted when the encoder is constructed with
 * {@code useDtx} (see {@link #MlowEncoder(int, boolean)}): an inactive packet is coded as a silence insertion
 * descriptor (SID) frame on the {@link Vad} interval schedule and dropped between ticks. This type is stateful
 * per stream and is not thread safe.
 *
 * @implNote This implementation initializes the range coder over the byte after the reserved TOC slot, runs
 * the per frame loop, then finalizes: it computes the byte count as {@code (tell + 7) >> 3}, closes the range
 * coder over the full buffer, and strips trailing zero bytes down to a floor of two, because the decoder's
 * range coder fills any missing byte with zero. The finalize runs over the full buffer, not a shrunk one, so
 * the range coder owns the whole payload. The TOC byte written is the one a single frame packet uses. The
 * conditional coding flag fed to {@link ParamEncoder} is {@code (voiced == prevVoiced) && (frame > 0)}, with
 * {@code prevVoiced} reset to the unvoiced state at the start of each packet.
 */
public final class MlowEncoder {
    /**
     * The logger for {@link MlowEncoder}.
     */
    private static final System.Logger LOGGER = Log.get(MlowEncoder.class);

    /**
     * Sample clock rate of the CELP core in kilohertz; the per frame and per subframe sample counts are
     * products of it.
     */
    private static final int CELP_FS_KHZ = 16;

    /**
     * Maximum payload size in bytes the range coder is allowed to fill; sized generously above the worst case
     * 60 ms high rate packet so the encoder never overruns the buffer on the in scope inputs.
     */
    private static final int MAX_PAYLOAD_BYTES = 1275;

    /**
     * Default target bitrate in bits per second for the PCM input scope; selects the high rate path.
     */
    private static final int DEFAULT_BITRATE_BPS = 9600;

    /**
     * Low rate decision threshold in bits per second for the 16 kHz, 60 ms packet.
     *
     * <p>The PCM input path selects the low rate mode when the target bitrate is at or below this threshold;
     * at the 16 kHz, 60 ms, mono scope the threshold is {@code 8700}, so the 9600 bps target stays high rate
     * and the 6000 bps target selects low rate.
     */
    private static final int LOW_RATE_THR_60MS_16K = 8700;

    /**
     * Samples per internal 20 ms frame at 16 kHz.
     */
    private static final int FRAME_LEN = 320;

    /**
     * The per frame low band parameter serializer used by {@link #encodeFrames}, threading conditional coding
     * and pitch lag state across the frames of a packet and across packets.
     */
    private final ParamEncoder paramEncoder;

    /**
     * The voice activity detector of the PCM input path, stateful across the packets of a stream.
     */
    private final Vad vad;

    /**
     * The per packet PCM to bitstream orchestrator of the PCM input path, stateful across the packets of a
     * stream.
     */
    private final CoreEncoder coreEncoder;

    /**
     * The target bitrate in bits per second of the PCM input path.
     *
     * <p>Fixed at construction; selects the rate class against {@link #LOW_RATE_THR_60MS_16K}. The
     * {@link #encode(short[])} path reads it to set the TOC {@code low_rate} bit, and the orchestrator wires
     * the subframe geometry from it.
     */
    private final int bitRate;

    /**
     * Whether the PCM input path runs the low rate mode.
     *
     * <p>Derived once at construction from {@link #bitRate} against {@link #LOW_RATE_THR_60MS_16K}; written
     * into the TOC byte of every packet {@link #encode(short[])} emits.
     */
    private final boolean lowRate;

    /**
     * Whether discontinuous transmission is enabled.
     *
     * <p>When {@code true}, an entirely inactive packet is coded as a silence insertion descriptor frame and,
     * between the {@link Vad} SID interval ticks, is not transmitted at all: {@link #encode(short[])} returns
     * a zero length payload the caller drops. When {@code false} the encoder always emits a full active voice
     * packet.
     */
    private final boolean useDtx;

    /**
     * The final value of the range coder register after the most recent {@link #encodeFrames} call.
     *
     * <p>Exposed through {@link #lastFinalRange()} as a per packet diagnostic for cross checking against a
     * reference encoder; it is not part of the encode contract.
     */
    private long lastFinalRange;

    /**
     * Constructs an MLow low band encoder at the default high rate bitrate, with a freshly constructed
     * parameter back end and cleared state.
     *
     * <p>The wrapped parameter serializer starts in its reset state, ready to encode the first packet of a
     * stream. The target bitrate is the default {@value #DEFAULT_BITRATE_BPS} bps high rate value; use
     * {@link #MlowEncoder(int)} to encode at a different bitrate (for example the 6000 bps low rate mode).
     */
    public MlowEncoder() {
        this(DEFAULT_BITRATE_BPS);
    }

    /**
     * Constructs an MLow low band encoder at a given target bitrate, with a freshly constructed parameter back
     * end and cleared state.
     *
     * <p>The bitrate selects the rate class against the 8700 bps 60 ms threshold: the 9600 bps target stays
     * high rate (four 5 ms subframes per 20 ms frame), the 6000 bps target selects low rate (two 10 ms
     * subframes per 20 ms frame), and the TOC {@code low_rate} bit and subframe geometry are wired from that
     * decision. The wrapped parameter serializer starts in its reset state, ready to encode the first packet
     * of a stream.
     *
     * @param bitRate the target bitrate in bits per second
     */
    public MlowEncoder(int bitRate) {
        this(bitRate, false);
    }

    /**
     * Constructs an MLow low band encoder at a given target bitrate and discontinuous transmission setting.
     *
     * <p>The bitrate selects the rate class as {@link #MlowEncoder(int)} describes. When {@code useDtx} is
     * {@code true} the PCM input path runs the silence insertion descriptor decision and omits the
     * transmission of silence frames between SID interval ticks; when {@code false} it always emits a full
     * active voice packet.
     *
     * @param bitRate the target bitrate in bits per second
     * @param useDtx  whether discontinuous transmission is enabled
     */
    public MlowEncoder(int bitRate, boolean useDtx) {
        this.bitRate = bitRate;
        this.lowRate = bitRate <= LOW_RATE_THR_60MS_16K;
        this.useDtx = useDtx;
        this.paramEncoder = new ParamEncoder();
        this.vad = new Vad();
        this.coreEncoder = new CoreEncoder(bitRate);
    }

    /**
     * Re targets the encoder's bitrate mid stream.
     *
     * <p>Delegates to {@link CoreEncoder#updateTargetBitrate(int)} so the next packet's
     * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode.BitrateController} sees the new
     * target and steers the instantaneous bitrate toward it through its feedback loop. A call that does not
     * change the target leaves the encode byte for byte identical to the fixed rate path. The rate class
     * ({@link #lowRate}) and the TOC {@code low_rate} bit are not re derived: the rate class is fixed once at
     * construction and only the controller target varies, so the rate class bit written into every packet
     * keeps the construction value while the adaptive target tracks the engine estimate.
     *
     * @param bps the new target bitrate in bits per second
     */
    public void updateTargetBitrate(int bps) {
        coreEncoder.updateTargetBitrate(bps);
    }

    /**
     * Returns this encoder to its freshly constructed state.
     *
     * <p>Resets the wrapped parameter serializer, the voice activity detector, and the orchestrator. Call this
     * between independent encode sessions; do not call it between the packets of one continuous stream, which
     * must thread state.
     */
    public void reset() {
        paramEncoder.reset();
        vad.reset();
        coreEncoder.reset();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls mlow encoder: stream reset");
    }

    /**
     * Returns the final range coder register of the most recent {@link #encodeFrames} call.
     *
     * <p>Two encoders that produced identical bytes also agree on this value; it is the standard cross check
     * that the range coders ran in lockstep. The value is meaningful only after at least one
     * {@link #encodeFrames} call.
     *
     * @return the final range register as a 32 bit unsigned value held in a {@code long}
     */
    public long lastFinalRange() {
        return lastFinalRange;
    }

    /**
     * Encodes one MLow packet from its per frame quantized low band parameters.
     *
     * <p>Range encodes every supplied frame's parameters in order through {@link ParamEncoder#encodeFrame},
     * threading the conditional coding flag ({@code (voiced == prevVoiced) && (frame > 0)} with
     * {@code prevVoiced} reset to unvoiced at the packet start), finalizes the range coder, strips the
     * trailing zero bytes, and prepends the TOC byte. A 60 ms packet supplies three frames; a 20 ms packet
     * supplies one. The cross frame predictors on this encoder advance, so packets must be supplied in stream
     * order.
     *
     * @param frames             the per frame quantized parameters, one entry per internal 20 ms frame
     * @param vad                {@code true} when the packet was produced with detected voice activity
     * @param sampleRateHz       the internal sample rate in hertz, {@code 16000} for the low band scope
     * @param lowRate            {@code true} for the low rate mode, {@code false} for high rate
     * @param fec                {@code true} when the packet carries an inband FEC frame; {@code false} for the
     *                           in scope path
     * @param codedAsActiveVoice {@code true} when the frames are coded as if they may contain voiced energy
     * @param stereo             {@code true} when the packet carries stereo information; {@code false} for the
     *                           mono scope
     * @return the complete MLow packet bytes, beginning with the TOC byte
     * @throws IllegalArgumentException if {@code frames} is empty or its length is not a valid frame count, or
     *                                  if {@code sampleRateHz} is above the 16 kHz low band scope
     */
    public byte[] encodeFrames(LbQuantParams[] frames, boolean vad, int sampleRateHz, boolean lowRate,
                               boolean fec, boolean codedAsActiveVoice, boolean stereo) {
        if (frames == null || frames.length == 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "calls mlow encode: no frames to encode");
            throw new IllegalArgumentException("no frames to encode");
        }
        if (sampleRateHz > 16000) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "calls mlow encode: sample rate {0} out of low-band scope", sampleRateHz);
            }
            throw new IllegalArgumentException(
                    "high-band encode (fs " + sampleRateHz + " Hz) is out of low-band scope");
        }
        var numFrames = frames.length;
        var packetLenMs = packetLenMsForFrames(numFrames);
        var framelen = (packetLenMs > 10 ? 2 : 1) * 10 * CELP_FS_KHZ;
        var numSubfr = 1 << (1 - (lowRate ? 1 : 0) + (packetLenMs > 10 ? 1 : 0));

        var buffer = new byte[1 + MAX_PAYLOAD_BYTES];
        var encoder = new MlowRangeEncoder(buffer, 1, MAX_PAYLOAD_BYTES);

        var prevVoiced = 0;
        for (var frame = 0; frame < numFrames; frame++) {
            var params = frames[frame];
            var voiced = params.voiced() ? 1 : 0;
            var condCoding = (voiced == prevVoiced) && (frame > 0);
            paramEncoder.encodeFrame(encoder, params, framelen, numSubfr, codedAsActiveVoice, condCoding,
                    lowRate, frame, prevVoiced, false);
            prevVoiced = voiced;
        }

        var ret = (encoder.tell() + 7) >> 3;
        encoder.finish();
        lastFinalRange = encoder.finalRange();

        // Strip trailing zeros: the decoder's range coder fills missing bytes with zero. The payload starts at
        // buffer[1], so payload byte index r maps to buffer[1 + r].
        while (ret > 2 && buffer[1 + ret - 1] == 0) {
            ret--;
        }

        buffer[0] = encodeToc(false, vad, sampleRateHz, packetLenMs, lowRate, fec, codedAsActiveVoice, stereo);
        var packet = new byte[1 + ret];
        System.arraycopy(buffer, 0, packet, 0, 1 + ret);
        return packet;
    }

    /**
     * Encodes one MLow packet from 16 bit PCM, the PCM input entry point mirroring {@link MlowDecoder#decode}.
     *
     * <p>Runs the voice activity detector over the packet, then the per packet orchestrator
     * ({@link CoreEncoder#encodePacket}) which runs the full analysis by synthesis front end and serializes
     * every frame in order, then finalizes the range coder, strips the trailing zero bytes, and prepends the
     * TOC byte. The packet length is the number of internal 20 ms frames the input spans: {@value #FRAME_LEN}
     * samples per frame, so a 60 ms packet is {@value #FRAME_LEN}{@code  * 3} samples. The cross packet
     * predictors advance, so packets must be supplied in stream order. The final range register is exposed
     * through {@link #lastFinalRange()}.
     *
     * <p>Scope is the 16 kHz, mono, active voice path at the configured bitrate (high rate at
     * {@value #DEFAULT_BITRATE_BPS} bps, low rate at 6000 bps); the input must be a whole number of 20 ms
     * frames at 16 kHz mono.
     *
     * @param pcm the input PCM samples for one packet at 16 kHz mono, one internal frame per
     *            {@value #FRAME_LEN} samples
     * @return the complete MLow packet bytes, beginning with the TOC byte
     * @throws IllegalArgumentException if {@code pcm} is empty or its length is not a multiple of
     *                                  {@value #FRAME_LEN}
     */
    public byte[] encode(short[] pcm) {
        if (pcm == null || pcm.length == 0 || pcm.length % FRAME_LEN != 0) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "calls mlow encode: invalid pcm length {0}",
                        pcm == null ? -1 : pcm.length);
            }
            throw new IllegalArgumentException(
                    "PCM-in MLow encode requires a whole number of " + FRAME_LEN + "-sample 20 ms frames");
        }
        var framesPerPacket = pcm.length / FRAME_LEN;
        var packetLenMs = packetLenMsForFrames(framesPerPacket);
        var lowRate = this.lowRate;

        var vadDecision = vad.processPacket(pcm, FRAME_LEN, framesPerPacket,
                Vad.ACTIVITY_NO_DECISION, useDtx);

        var buffer = new byte[1 + MAX_PAYLOAD_BYTES];
        var encoder = new MlowRangeEncoder(buffer, 1, MAX_PAYLOAD_BYTES);

        coreEncoder.encodePacket(pcm, FRAME_LEN, framesPerPacket, vadDecision, encoder);

        // Discontinuous transmission: an inactive packet between SID interval ticks serializes no symbols and
        // is not transmitted. The analysis above still advanced the encoder state and the comfort noise
        // candidate ring, so a later SID or speech packet stays exact. Return an empty payload for the caller
        // to drop.
        if (vadDecision.sidFrame() && !vadDecision.sendSidFrame()) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls mlow encode: dtx suppressing silent packet");
            return new byte[0];
        }

        var ret = (encoder.tell() + 7) >> 3;
        encoder.finish();
        lastFinalRange = encoder.finalRange();

        while (ret > 2 && buffer[1 + ret - 1] == 0) {
            ret--;
        }

        buffer[0] = encodeToc(vadDecision.sidFrame(), vadDecision.vad(), 16000, packetLenMs, lowRate, false,
                vadDecision.codedAsActiveVoice(), false);
        var packet = new byte[1 + ret];
        System.arraycopy(buffer, 0, packet, 0, 1 + ret);
        return packet;
    }

    /**
     * Maps an internal frame count to the packet length in milliseconds.
     *
     * @param numFrames the internal 20 ms frame count; 1, 3, or 6
     * @return the packet length in milliseconds; 20, 60, or 120
     * @throws IllegalArgumentException if {@code numFrames} is not a valid frame count
     */
    private static int packetLenMsForFrames(int numFrames) {
        return switch (numFrames) {
            case 1 -> 20;
            case 3 -> 60;
            case 6 -> 120;
            default -> throw new IllegalArgumentException("unsupported internal frame count: " + numFrames);
        };
    }

    /**
     * Builds the MLow TOC byte for a single frame packet.
     *
     * <p>Packs the eight bit fields the encoder uses, including the hangover bit
     * ({@code fec || (!vad && codedAsActiveVoice)}) and the sample rate flag (clear at 16 kHz, set otherwise).
     * The decode side {@link MlowTocByte#decode(int)} reconstructs every field from the returned byte.
     *
     * @param sid                {@code true} for a SID frame; {@code false} for the in scope path
     * @param vad                {@code true} when the packet was produced with detected voice activity
     * @param sampleRateHz       the internal sample rate in hertz, {@code 16000}, {@code 32000}, or
     *                           {@code 48000}
     * @param packetLenMs        the packet length in milliseconds; 10, 20, 60, or 120
     * @param lowRate            {@code true} for the low rate mode, {@code false} for high rate
     * @param fec                {@code true} when the packet carries an inband FEC frame
     * @param codedAsActiveVoice {@code true} when the frames are coded as if they may contain voiced energy
     * @param stereo             {@code true} when the packet carries stereo information
     * @return the packed TOC byte in the low eight bits of the returned {@code byte}
     * @throws IllegalArgumentException if {@code sampleRateHz} or {@code packetLenMs} is not a defined value
     */
    static byte encodeToc(boolean sid, boolean vad, int sampleRateHz, int packetLenMs, boolean lowRate,
                          boolean fec, boolean codedAsActiveVoice, boolean stereo) {
        var b = 0;
        b += (sid ? 1 : 0) << 7;
        b += (vad ? 1 : 0) << 6;
        if (sampleRateHz != 16000 && sampleRateHz != 32000 && sampleRateHz != 48000) {
            throw new IllegalArgumentException("unsupported sample rate: " + sampleRateHz);
        }
        b += sampleRateHz == 16000 ? 0 : (1 << 5);
        var frameSel = switch (packetLenMs) {
            case 10 -> 0;
            case 20 -> 1;
            case 60 -> 2;
            case 120 -> 3;
            default -> throw new IllegalArgumentException("unsupported packet length: " + packetLenMs);
        };
        b += frameSel << 3;
        b += (lowRate ? 1 : 0) << 2;
        b += (fec || (!vad && codedAsActiveVoice)) ? (1 << 1) : 0;
        b += stereo ? 1 : 0;
        return (byte) b;
    }
}
