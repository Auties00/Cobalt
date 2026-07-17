package com.github.auties00.cobalt.calls.media.audio.neteq.decoder;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.MlowDecoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.MlowTocByte;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.postfilter.MlowDecodePostfilter;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;
import com.github.auties00.cobalt.calls.media.audio.neteq.LiveNetEq;

/**
 * Decodes MLow low bitrate speech packets into PCM for the {@link LiveNetEq} jitter buffer through the pure
 * Java {@link MlowDecoder} kernel and {@link MlowDecodePostfilter} chain, a permitted {@link AudioDecoder}
 * the jitter buffer registers when a call negotiates the MLow codec.
 *
 * <p>MLow is WhatsApp's in house low bitrate speech codec, a deterministic float CELP vocoder rather than a
 * neural network. Decoding a frame is a classical CELP synthesis: the range coder arithmetically decodes the
 * quantized parameters behind a leading TOC byte naming the frame configuration; the line spectral
 * frequencies are dequantized from a two stage LSF vector quantization back into LPC short term synthesis
 * coefficients; the excitation is rebuilt from a pitch and long term prediction adaptive codebook plus an
 * algebraic fixed codebook of signed unit pulses; and the short term synthesis filter produces the speech.
 * The only machine learning component is an optional, default disabled bandwidth extension (BWE) postfilter,
 * applied after the codec core and not part of decoding a frame.
 *
 * <p>{@link #decode(byte[], int, boolean)} decodes a packet in two stages: the {@link MlowDecoder} kernel
 * produces the synthesis before postfiltering and the per packet decode parameters
 * ({@link MlowDecoder#decodeWithSynthesis(byte[], boolean)}), and the {@link MlowDecodePostfilter} chain runs
 * the harmonic, high pass, and optional LPC postfilters over that synthesis before the signal is scaled to
 * signed 16 bit PCM. {@link #reset()} returns both the kernel and the postfilter chain to their freshly
 * constructed state across a stream discontinuity. The decoder is single writer: the kernel and the
 * postfilter chain both thread state across frames and packets, so it must be driven from the jitter
 * buffer's pull thread and fed every packet of a stream in order.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono, low band path matching the {@link MlowDecoder} kernel. A 60 ms MLow
 * packet decodes to 960 samples regardless of the {@code frameSamples} the jitter buffer requests, because
 * the sample count is fixed by the packet's TOC, not by the caller; the returned array therefore holds the
 * packet's own sample count rather than exactly {@code frameSamples}. {@link #conceal(int)} synthesizes a
 * concealed packet through the MLow packet loss concealment state machine, and
 * {@link #decode(byte[], int, boolean)} with the forward error correction flag reconstructs the previous
 * lost frame from this packet's leading in band redundant copy (falling back to concealment when the packet
 * carries none); there is no encode side counterpart here.
 *
 * @implNote This implementation runs the postfilter chain in this codec to NetEq wrapper, over the kernel's
 * {@link MlowDecoder#decodeWithSynthesis(byte[], boolean) exposed synthesis before postfiltering}, rather
 * than inside the kernel, so the kernel keeps reproducing its postfilter off reference exactly while the
 * receive path matches the live decoder. The {@link MlowDecoder} kernel produces the whole packet's samples
 * and ignores the requested per frame sample count, so this wrapper does not enforce the
 * {@code frameSamples * channels} length the fixed rate codec path assumes; the concealment entry point runs
 * the MLow packet loss concealment state machine and the forward error correction entry point decodes the
 * leading in band redundant copy, both threading the same decoder state as a normal decode. The float to
 * {@code int16} conversion is a truncating cast clamped to {@code [-32767, 32767]}, matching the shipping
 * decode path rather than the rounding scale {@link MlowDecoder#decode(byte[])} applies, because the
 * postfiltered signal is the shipping decode path. The LPC postfilter is disabled by default; a per call
 * value of the LPC postfilter runtime parameter is resolved at session assembly and threaded in through the
 * constructor.
 */
public final class MLowAudioDecoder implements AudioDecoder {
    /**
     * The logger for {@link MLowAudioDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(MLowAudioDecoder.class);

    /**
     * Bit mask of the voice activity flag in the MLow TOC byte.
     *
     * <p>Set in the leading TOC byte of an MLow packet when the frame it heads carries active speech; the
     * same bit the encode side sets.
     */
    private static final int TOC_VAD_MASK = 0x40;

    /**
     * The float to {@code int16} scale factor of {@code 32767}.
     */
    private static final float PCM_SCALE = 32767.0f;

    /**
     * The number of samples one concealed MLow packet produces, a 60 ms packet at 16 kHz.
     *
     * <p>Concealment synthesizes a whole MLow packet regardless of the {@code frameSamples} the jitter buffer
     * requests per pull, matching the whole packet decode; the jitter buffer's sync buffer serves the
     * requested frame out of it.
     */
    private static final int PLC_PACKET_SAMPLES = 960;

    /**
     * Counts decoded frames to bound the diagnostic peak level logging in {@link #decode(byte[], int, boolean)}.
     *
     * <p>Incremented on each decode until it reaches the logging cap, after which no further frames are
     * logged.
     */
    private static int DIAG_FRAMES = 0;

    /**
     * The pure Java MLow CELP synthesis kernel this decoder drives for the synthesis before postfiltering.
     *
     * <p>Threads state across frames and packets internally, so it is constructed once per stream and driven
     * only from the pull thread.
     */
    private final MlowDecoder kernel;

    /**
     * The pure Java MLow decode postfilter chain this decoder runs over the kernel's synthesis.
     *
     * <p>Threads the harmonic, high pass, and LPC postfilter state across packets, so it is constructed once
     * per stream alongside the kernel and reset with it.
     */
    private final MlowDecodePostfilter postfilter;

    /**
     * Whether the gated LPC postfilter runs; default {@code false}.
     *
     * <p>The harmonic and high pass postfilters always run as the decoder's level lift and harmonic shaping;
     * only the LPC postfilter is gated, so this flag is the per call value of the LPC postfilter runtime
     * parameter resolved at session assembly, defaulting to {@code false} when unset.
     */
    private final boolean lpcPostfilterEnabled;

    /**
     * The output sample rate in hertz this decoder reports.
     */
    private final int sampleRate;

    /**
     * The output channel count this decoder reports.
     */
    private final int channels;

    /**
     * Whether this decoder has been closed; once closed every decode, conceal, and reset call throws.
     *
     * <p>Declared {@code volatile} because {@link #close()} may run on the call teardown thread while the
     * jitter buffer still drives decode and conceal on the pull thread, so the pull thread observes the
     * closed transition without a data race.
     */
    private volatile boolean closed;

    /**
     * Constructs an MLow decoder for the given output geometry with the LPC postfilter disabled, backed by a
     * fresh {@link MlowDecoder} kernel and {@link MlowDecodePostfilter} chain.
     *
     * <p>Equivalent to {@link #MLowAudioDecoder(int, int, boolean)} with the LPC postfilter off, the live
     * client's default. Use the three argument constructor when a per call LPC postfilter value has been
     * resolved.
     *
     * @param sampleRate the output sample rate in Hz the decoder reports
     * @param channels   the output channel count the decoder reports, {@code 1} for mono
     */
    public MLowAudioDecoder(int sampleRate, int channels) {
        this(sampleRate, channels, false);
    }

    /**
     * Constructs an MLow decoder for the given output geometry, backed by a fresh {@link MlowDecoder} kernel
     * and {@link MlowDecodePostfilter} chain.
     *
     * <p>The kernel and the postfilter chain start in their reset state, ready to decode the first packet of
     * a stream. The supplied geometry is reported through {@link #sampleRate()} and {@link #channels()}; the
     * in scope MLow path is 16 kHz mono, so the kernel itself produces 16 kHz mono samples regardless of
     * these values. The harmonic and high pass postfilters always run; {@code lpcPostfilterEnabled} gates
     * only the LPC postfilter.
     *
     * @param sampleRate           the output sample rate in Hz the decoder reports
     * @param channels             the output channel count the decoder reports, {@code 1} for mono
     * @param lpcPostfilterEnabled the per call LPC postfilter value; {@code false} for the default off live
     *                             behaviour
     */
    public MLowAudioDecoder(int sampleRate, int channels, boolean lpcPostfilterEnabled) {
        this.kernel = new MlowDecoder();
        this.postfilter = new MlowDecodePostfilter();
        this.lpcPostfilterEnabled = lpcPostfilterEnabled;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation decodes the whole packet to its synthesis through
     * {@link MlowDecoder#decodeWithSynthesis(byte[], boolean)}, runs the {@link MlowDecodePostfilter} chain
     * over each contained MLow packet's synthesis in stream order (so the postfilter state threads exactly as
     * the live decoder's), and converts the postfiltered synthesis to signed 16 bit PCM with a truncating
     * cast clamped to {@code [-32767, 32767]}. The {@code frameSamples} request is not enforced: an MLow
     * packet's sample count is fixed by its TOC (a 60 ms packet yields 960 samples), so the returned array
     * holds the packet's own sample count. A {@code fec} request reconstructs the previous lost frame from
     * this packet's leading in band redundant copy through
     * {@link MlowDecoder#decodeWithSynthesis(byte[], boolean, boolean)}; when the packet carries no redundant
     * copy the request falls back to {@link #conceal(int)}.
     */
    @Override
    public short[] decode(byte[] payload, int frameSamples, boolean fec) {
        Objects.requireNonNull(payload, "payload cannot be null");
        requireOpen();
        var celt = payload.length > 0 && (payload[0] & 0xC0) == 0xC0;
        short[] output;
        if (celt) {
            // A CELT coded packet (TOC top two bits set): the low band decoder does not decode CELT, so
            // conceal it rather than misroute it into the MLow frame decoder, which would throw on its short
            // body and, uncaught, terminate the playback pump.
            if (Log.WARNING) LOGGER.log(Level.WARNING, "mlow decode: celt packet routed to low band decoder, concealing, len={0}", payload.length);
            output = conceal(frameSamples);
        } else if (fec) {
            if (payload.length == 0
                    || !MlowTocByte.decode(payload[0] & 0xFF).fec()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mlow decode: fec requested but no redundant copy, concealing");
                output = conceal(frameSamples);
            } else {
                output = render(kernel.decodeWithSynthesis(payload, lpcPostfilterEnabled, true), false);
            }
        } else {
            output = render(kernel.decodeWithSynthesis(payload, lpcPostfilterEnabled), false);
        }
        if (Log.TRACE && DIAG_FRAMES < 400) {
            DIAG_FRAMES++;
            var peak = 0;
            for (var s : output) {
                var a = Math.abs(s);
                if (a > peak) {
                    peak = a;
                }
            }
            LOGGER.log(Level.TRACE, "mlow decode frame #{0} len={1} celt={2} fec={3} out={4} peak={5}",
                    DIAG_FRAMES, payload.length, celt, fec, output.length, peak);
        }
        return output;
    }

    /**
     * Runs the postfilter chain over each contained packet's synthesis and scales it to signed 16 bit PCM,
     * the shared render tail of {@link #decode(byte[], int, boolean)} and {@link #conceal(int)}.
     *
     * <p>The postfilter chain threads its harmonic, high pass, and optional LPC state across packets exactly
     * as the live decoder does, so a concealed packet's synthesis is shaped by the same postfilters as a
     * decoded one, and each contained packet's synthesis is converted with the truncating clamp of
     * {@link #toInt16(float)}.
     *
     * @param result the per packet synthesis and decode parameters
     * @param lost   whether the packet was concealed rather than decoded, so the concealment comfort noise
     *               overlays the postfiltered signal
     * @return the postfiltered, scaled signed 16 bit PCM
     */
    private short[] render(MlowDecoder.DecodeResult result, boolean lost) {
        var totalSamples = 0;
        for (var packet : result.packets()) {
            totalSamples += packet.synthesis().length;
        }
        var pcm = new short[totalSamples];
        var offset = 0;
        for (var packet : result.packets()) {
            var synthesis = packet.synthesis();
            postfilter.process(synthesis, packet.numFrames(), packet.lpc(), packet.numSubframes(),
                    packet.subframeLength(), packet.lagsPerPacket(), packet.normalizedBitratePerFrame(),
                    packet.voiced(), packet.lowRate(), lpcPostfilterEnabled);
            kernel.applyComfortNoiseAndLossInfo(synthesis, lost);
            for (var i = 0; i < synthesis.length; i++) {
                pcm[offset + i] = toInt16(synthesis[i]);
            }
            offset += synthesis.length;
        }
        return pcm;
    }

    /**
     * Scales one postfiltered float sample to a clamped signed 16 bit value.
     *
     * <p>Multiplies by {@value #PCM_SCALE}, clamps to {@code [-32767, 32767]}, and truncates toward zero with
     * a cast to {@code int16}, matching the conversion the shipping decode path applies after its postfilter
     * chain.
     *
     * @param sample the postfiltered float sample, nominally in {@code [-1, 1]}
     * @return the truncated and clamped {@code int16} sample
     */
    private static short toInt16(float sample) {
        var scaled = sample * PCM_SCALE;
        if (scaled > PCM_SCALE) {
            scaled = PCM_SCALE;
        } else if (scaled < -PCM_SCALE) {
            scaled = -PCM_SCALE;
        }
        return (short) (int) scaled;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation synthesizes a whole concealed MLow packet through
     * {@link MlowDecoder#concealWithSynthesis(int, boolean)} and runs the same postfilter and scaling chain
     * as a normal decode. The {@code frameSamples} request is not enforced: concealment produces one MLow
     * packet ({@value #PLC_PACKET_SAMPLES} samples for the 60 ms packet), so the returned array holds the
     * packet's sample count and the jitter buffer's sync buffer serves the requested frame out of it,
     * matching {@link #decode(byte[], int, boolean)}.
     */
    @Override
    public short[] conceal(int frameSamples) {
        requireOpen();
        var result = kernel.concealWithSynthesis(PLC_PACKET_SAMPLES, lpcPostfilterEnabled);
        return render(result, true);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation mirrors the branch selection of {@link #decode(byte[], int, boolean)}
     * (CELT concealment, forward error correction downgrade, and the normal decode) but writes the scaled
     * PCM straight into {@code destination} through
     * {@link #renderInto(MlowDecoder.DecodeResult, boolean, short[])}, sparing the fresh array
     * {@link #decode(byte[], int, boolean)} allocates.
     */
    @Override
    public int decode(byte[] payload, int frameSamples, boolean fec, short[] destination) {
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        requireOpen();
        var celt = payload.length > 0 && (payload[0] & 0xC0) == 0xC0;
        if (celt) {
            return concealInto(destination);
        }
        if (fec) {
            if (payload.length == 0
                    || !MlowTocByte.decode(payload[0] & 0xFF).fec()) {
                return concealInto(destination);
            }
            return renderInto(kernel.decodeWithSynthesis(payload, lpcPostfilterEnabled, true), false, destination);
        }
        return renderInto(kernel.decodeWithSynthesis(payload, lpcPostfilterEnabled), false, destination);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation runs the same concealment synthesis as {@link #conceal(int)} but writes
     * the scaled PCM straight into {@code destination}.
     */
    @Override
    public int conceal(int frameSamples, short[] destination) {
        Objects.requireNonNull(destination, "destination cannot be null");
        requireOpen();
        return concealInto(destination);
    }

    /**
     * Synthesizes one concealment packet into {@code destination}, the fill into caller tail shared by
     * {@link #conceal(int, short[])} and the concealment branches of
     * {@link #decode(byte[], int, boolean, short[])}.
     *
     * @param destination the buffer to write the concealment PCM into
     * @return the number of samples written
     */
    private int concealInto(short[] destination) {
        var result = kernel.concealWithSynthesis(PLC_PACKET_SAMPLES, lpcPostfilterEnabled);
        return renderInto(result, true, destination);
    }

    /**
     * Runs the postfilter chain over each contained packet's synthesis and scales it to signed 16 bit PCM
     * written straight into {@code destination}, the fill into caller form of
     * {@link #render(MlowDecoder.DecodeResult, boolean)}.
     *
     * @param result      the per packet synthesis and decode parameters
     * @param lost        whether the packet was concealed rather than decoded
     * @param destination the buffer to write the scaled PCM into
     * @return the number of samples written
     */
    private int renderInto(MlowDecoder.DecodeResult result, boolean lost, short[] destination) {
        var offset = 0;
        for (var packet : result.packets()) {
            var synthesis = packet.synthesis();
            postfilter.process(synthesis, packet.numFrames(), packet.lpc(), packet.numSubframes(),
                    packet.subframeLength(), packet.lagsPerPacket(), packet.normalizedBitratePerFrame(),
                    packet.voiced(), packet.lowRate(), lpcPostfilterEnabled);
            kernel.applyComfortNoiseAndLossInfo(synthesis, lost);
            for (var i = 0; i < synthesis.length; i++) {
                destination[offset + i] = toInt16(synthesis[i]);
            }
            offset += synthesis.length;
        }
        return offset;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation reads the voice activity bit ({@link #TOC_VAD_MASK}) of the packet's
     * leading TOC byte, the same flag the encode side sets. An empty packet is reported inactive.
     */
    @Override
    public boolean packetHasVoiceActivity(byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        return payload.length > 0 && (payload[0] & TOC_VAD_MASK) != 0;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation resets both the CELP kernel ({@link MlowDecoder#reset()}) and the
     * postfilter chain ({@link MlowDecodePostfilter#reset()}), returning the whole decode pipeline to its
     * freshly constructed state without releasing any resource.
     */
    @Override
    public void reset() {
        requireOpen();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mlow audio decoder reset");
        kernel.reset();
        postfilter.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sampleRate() {
        return sampleRate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int channels() {
        return channels;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation marks the decoder closed; the kernel and the postfilter chain hold no
     * native resource, so closing only flips the closed flag. A second call has no effect.
     */
    @Override
    public void close() {
        closed = true;
    }

    /**
     * Verifies that the decoder is still open.
     *
     * @throws IllegalStateException if the decoder has been closed
     */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("MLowAudioDecoder is closed");
        }
    }
}
