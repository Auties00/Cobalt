package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import com.github.auties00.cobalt.calls.media.audio.codec.opus.bindings.CobaltOpus;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.util.NativeLibLoader;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import com.github.auties00.cobalt.calls.media.audio.codec.AdaptiveComplexityController;
import com.github.auties00.cobalt.calls.media.audio.codec.AudioCodec;
import com.github.auties00.cobalt.calls.media.audio.codec.AudioCodecStats;
import com.github.auties00.cobalt.calls.media.audio.codec.EncodedAudioFrame;

/**
 * The libopus backed call audio codec: opens an Opus encoder and decoder, applies the WhatsApp control
 * set, classifies each encoded frame, recovers losses through PLC and in band FEC, and adapts encoder
 * complexity to the device's CPU budget.
 *
 * <p>An instance owns one native encoder state and one native decoder state plus reusable PCM and packet
 * scratch buffers, all from a per instance arena, and is single writer per the {@link AudioCodec}
 * contract. On open it applies the bitrate, variable bitrate, complexity, in band FEC, DTX, force
 * channels, signal, least significant bit depth, and maximum bandwidth controls from the supplied
 * {@link OpusCodecParams}; on {@linkplain #modify(OpusCodecParams) modify} it reapplies the mutable
 * subset with the bitrate clamped by the cap derived from the maximum bandwidth. Each
 * {@linkplain #encode(short[], int) encode} measures wall time, classifies the result for voice activity
 * and DTX, feeds the timing to the {@link AdaptiveComplexityController}, and applies a new complexity
 * level only when the controller moves it. Decode runs normally or pulls a forward error correction copy
 * from the packet; {@linkplain #recover(byte[], int) recover} conceals or reconstructs a lost frame.
 *
 * @implNote This implementation reaches libopus through the {@link CobaltOpus} shim: the native vtable
 * indirection over the separate encode, decode, and control entry points collapses to direct shim
 * downcalls, and the variadic {@code opus_encoder_ctl} is reached through the shim's typed
 * {@code cobalt_opus_encoder_set_*} setters. The control set applied is bitrate, variable bitrate,
 * complexity, in band FEC, expected packet loss, DTX, force channels, signal, least significant bit
 * depth, and maximum bandwidth. The speech versus DTX threshold is {@value #SPEECH_THRESHOLD_BYTES}
 * bytes and the voice activity flag is read from the table of contents byte of the encoded packet.
 */
public final class OpusAudioCodec implements AudioCodec {
    static {
        NativeLibLoader.load("cobalt-native", Arena.global());
    }

    /**
     * Capacity, in bytes, of the native packet output buffer for one encoded packet.
     *
     * @implNote This implementation uses 1500, above the 1276 byte RFC 6716 single frame maximum; voice
     * frames are far smaller. The buffer is allocated once so encoding never sizes per call.
     */
    private static final int MAX_PACKET_BYTES = 1500;

    /**
     * Worst case per channel sample count of one Opus frame, a 60 ms frame at 48 kHz.
     *
     * <p>The PCM scratch buffers are sized for this maximum so any legal frame fits without
     * reallocation.
     */
    private static final int MAX_FRAME_SAMPLES = 2880;

    /**
     * Minimum encoded length, in bytes, for a frame to be treated as speech rather than DTX or comfort
     * noise.
     *
     * <p>Any encoder output shorter than this is treated as a discontinuous transmission or comfort
     * noise frame.
     */
    private static final int SPEECH_THRESHOLD_BYTES = 3;

    /**
     * Full scale reference amplitude the {@code -dBov} audio level divides the root mean square energy
     * by.
     *
     * @implNote This implementation uses {@code 32767.0}, the maximum positive 16 bit PCM sample; the
     * integer root mean square is divided by {@code 32767.0}, not {@code 32768.0}, before taking the
     * logarithm.
     */
    private static final double AUDIO_LEVEL_FULL_SCALE = 32767.0;

    /**
     * Decibel factor mapping the ratio of root mean square to full scale onto the {@code -dBov} level.
     *
     * @implNote This implementation uses {@code -20.0}, the standard amplitude ratio decibel factor
     * applied as {@code -20 * log10(rms / 32767)}.
     */
    private static final double AUDIO_LEVEL_DB_FACTOR = -20.0;

    /**
     * Per second encode time budget, in milliseconds of encode time per second of audio, seeding the
     * {@link AdaptiveComplexityController}.
     *
     * @implNote This implementation uses the compiled in WhatsApp default of {@code 10} ms/s: the
     * complexity budget defaults to 10 milliseconds of encode time per second of audio when unset, and
     * any configured value is clamped to the range {@code 1..100}. The controller compares this against
     * the average encode time per second in the same unit.
     */
    private static final long ENCODE_BUDGET_MILLIS_PER_SECOND = 10;

    /**
     * The logger for {@link OpusAudioCodec}.
     */
    private static final System.Logger LOGGER = Log.get(OpusAudioCodec.class);

    /**
     * Per instance arena owning the native encoder and decoder states and the scratch buffers.
     */
    private final Arena arena;

    /**
     * The codec parameters this codec was opened with, updated on {@link #modify(OpusCodecParams)}.
     */
    private OpusCodecParams params;

    /**
     * The channel count, cached from the parameters for the PCM length checks.
     */
    private final int channels;

    /**
     * Pointer to the native {@code OpusEncoder} state, or {@link MemorySegment#NULL} once closed.
     */
    private MemorySegment encoder;

    /**
     * Pointer to the native {@code OpusDecoder} state, or {@link MemorySegment#NULL} once closed.
     */
    private MemorySegment decoder;

    /**
     * Reusable native buffer the caller's PCM is copied into before encoding.
     */
    private final MemorySegment pcmInBuf;

    /**
     * Reusable native buffer the decoded PCM is read back from.
     */
    private final MemorySegment pcmOutBuf;

    /**
     * Reusable native buffer the encoded packet bytes are written into.
     */
    private final MemorySegment packetBuf;

    /**
     * Reusable native buffer the input packet is copied into for decode, recover, and FEC inspection.
     */
    private final MemorySegment packetInBuf;

    /**
     * The adaptive complexity controller deriving the encoder complexity from rolling encode time.
     */
    private final AdaptiveComplexityController complexityController;

    /**
     * The in band forward error correction policy deciding, per lost frame, whether to reconstruct from
     * the following packet's LBRR copy or fall back to packet loss concealment.
     */
    private final OpusInbandFecPacker fecPacker;

    /**
     * Cumulative count of frames passed to the encoder.
     */
    private long totalEncodedFrames;

    /**
     * Cumulative count of frames passed to the decoder.
     */
    private long totalDecodedFrames;

    /**
     * Decode side count of frames reconstructed from in band FEC.
     */
    private long fecFrames;

    /**
     * Decode side count of frames filled by packet loss concealment.
     */
    private long plcFrames;

    /**
     * Sum of native encode wall time, in microseconds, across the codec lifetime.
     */
    private long lifetimeEncodeMicros;

    /**
     * Sum of native decode wall time, in microseconds, across the codec lifetime.
     */
    private long lifetimeDecodeMicros;

    /**
     * Sum of encoded frame sizes, in bytes, across the codec lifetime, for the observed bitrate average.
     */
    private long lifetimeEncodedBytes;

    /**
     * The encoder target bitrate, in bits per second, currently applied to the native encoder.
     *
     * <p>Set to the open target on construction and to the bandwidth capped target on each
     * {@link #modify(OpusCodecParams)}, so it tracks the value last handed to
     * {@code cobalt_opus_encoder_set_bitrate}. Sampled per encode into the running target bitrate average.
     */
    private int appliedTargetBitrate;

    /**
     * Running sum of the per frame applied target bitrate, in bits per second, across the codec lifetime.
     *
     * <p>Accumulated only on frames encoded while the applied target is a concrete value (not
     * {@link OpusCodecParams#BITRATE_AUTO}), forming the numerator of the average target bitrate.
     */
    private long lifetimeTargetBitrateSum;

    /**
     * Count of frames contributing to {@link #lifetimeTargetBitrateSum}.
     *
     * <p>Increments per encode whenever the applied target is a concrete value; the denominator of the
     * average target bitrate, kept separate from {@link #totalEncodedFrames} so a frame whose target is
     * {@link OpusCodecParams#BITRATE_AUTO} does not bias the average.
     */
    private long targetBitrateSamples;

    /**
     * Whether the most recent encoded frame was a discontinuous transmission frame.
     */
    private boolean lastWasDiscontinuous;

    /**
     * Opens an Opus codec configured by the given parameters.
     *
     * <p>Allocates the native encoder and decoder states and the scratch buffers from a fresh shared
     * arena, then applies the full open control set. The complexity controller is seeded with the
     * parameter complexity and the {@link #ENCODE_BUDGET_MILLIS_PER_SECOND} encode time budget. If
     * libopus rejects the configuration the partially built native state and the arena are released
     * before the exception propagates.
     *
     * @param params the codec parameters to open with
     * @throws NullPointerException       if {@code params} is {@code null}
     * @throws WhatsAppCallException.Opus if libopus rejects the configuration
     * @throws UnsatisfiedLinkError       if libopus cannot be loaded
     */
    public OpusAudioCodec(OpusCodecParams params) {
        this.params = Objects.requireNonNull(params, "params cannot be null");
        this.channels = params.channels();
        this.arena = Arena.ofShared();
        try {
            var outHandle = arena.allocate(ValueLayout.ADDRESS);
            this.encoder = createEncoder(params, outHandle);
            this.decoder = createDecoder(params, outHandle);
            this.pcmInBuf = arena.allocate((long) MAX_FRAME_SAMPLES * channels * 2);
            this.pcmOutBuf = arena.allocate((long) MAX_FRAME_SAMPLES * channels * 2);
            this.packetBuf = arena.allocate(MAX_PACKET_BYTES);
            this.packetInBuf = arena.allocate(MAX_PACKET_BYTES);
            this.complexityController = new AdaptiveComplexityController(
                    ENCODE_BUDGET_MILLIS_PER_SECOND, params.complexity());
            this.fecPacker = new OpusInbandFecPacker();
            applyOpenControls(params);
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "calls opus codec opened: sampleRate={0} channels={1} application={2} bitrate={3}",
                        params.sampleRate(), channels, params.application(), params.defaultBitrate());
            }
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus codec open failed", e);
            destroyStates();
            arena.close();
            throw e;
        }
    }

    /**
     * Creates the native Opus encoder for the given parameters.
     *
     * @param params    the codec parameters carrying the sample rate, channels, and application mode
     * @param outHandle the reusable single pointer out handle segment the shim writes the state into
     * @return the encoder state handle
     * @throws WhatsAppCallException.Opus if {@code cobalt_opus_encoder_create} fails or returns null
     */
    private MemorySegment createEncoder(OpusCodecParams params, MemorySegment outHandle) {
        int rc;
        try {
            rc = CobaltOpus.cobalt_opus_encoder_create(params.sampleRate(), channels, params.application().toNative(), outHandle);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus encoder create failed", t);
            throw new WhatsAppCallException.Opus("cobalt_opus_encoder_create failed", t);
        }
        var state = outHandle.get(ValueLayout.ADDRESS, 0);
        if (rc != CobaltOpus.COBALT_OPUS_OK() || state.equals(MemorySegment.NULL)) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus encoder create rejected: rc={0}", rc);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_encoder_create", rc);
        }
        return state;
    }

    /**
     * Creates the native Opus decoder for the given parameters.
     *
     * @param params    the codec parameters carrying the sample rate and channels
     * @param outHandle the reusable single pointer out handle segment the shim writes the state into
     * @return the decoder state handle
     * @throws WhatsAppCallException.Opus if {@code cobalt_opus_decoder_create} fails or returns null
     */
    private MemorySegment createDecoder(OpusCodecParams params, MemorySegment outHandle) {
        int rc;
        try {
            rc = CobaltOpus.cobalt_opus_decoder_create(params.sampleRate(), channels, outHandle);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus decoder create failed", t);
            throw new WhatsAppCallException.Opus("cobalt_opus_decoder_create failed", t);
        }
        var state = outHandle.get(ValueLayout.ADDRESS, 0);
        if (rc != CobaltOpus.COBALT_OPUS_OK() || state.equals(MemorySegment.NULL)) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus decoder create rejected: rc={0}", rc);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decoder_create", rc);
        }
        return state;
    }

    /**
     * Applies the full encoder control set from the given parameters on open.
     *
     * <p>Issues the bitrate, variable bitrate, complexity, in band FEC, expected packet loss, DTX, force
     * channels, signal, least significant bit depth, and maximum bandwidth controls, in that order,
     * through the typed {@link CobaltOpus} setters.
     *
     * @param params the parameters whose fields select the control values
     */
    private void applyOpenControls(OpusCodecParams params) {
        // TODO: the WhatsApp patched extended SILK and CELT CTLs the native open path also issues (in
        //  wa_opus.cc) are not applied here: RE (re/calls) shows they are WhatsApp-fork custom encoder-ctl
        //  request numbers that stock system libopus rejects, so applying them faithfully would need Cobalt to
        //  bundle WhatsApp's patched libopus rather than the stock library, not merely the recovered request
        //  numbers. Blocked on that library choice, not on recovery alone.
        var signal = params.signalVoice() ? CobaltOpus.COBALT_OPUS_SIGNAL_VOICE() : CobaltOpus.COBALT_OPUS_SIGNAL_MUSIC();
        encApply("set_bitrate", CobaltOpus.cobalt_opus_encoder_set_bitrate(encoder, params.defaultBitrate()));
        this.appliedTargetBitrate = params.defaultBitrate();
        encApply("set_vbr", CobaltOpus.cobalt_opus_encoder_set_vbr(encoder, params.variableBitrate() ? 1 : 0));
        encApply("set_complexity", CobaltOpus.cobalt_opus_encoder_set_complexity(encoder, params.complexity()));
        encApply("set_inband_fec", CobaltOpus.cobalt_opus_encoder_set_inband_fec(encoder, params.inbandFec() ? 1 : 0));
        encApply("set_packet_loss_perc", CobaltOpus.cobalt_opus_encoder_set_packet_loss_perc(encoder, params.packetLossPercent()));
        encApply("set_dtx", CobaltOpus.cobalt_opus_encoder_set_dtx(encoder, params.discontinuousTransmission() ? 1 : 0));
        encApply("set_force_channels", CobaltOpus.cobalt_opus_encoder_set_force_channels(encoder, params.forceChannels()));
        encApply("set_signal", CobaltOpus.cobalt_opus_encoder_set_signal(encoder, signal));
        encApply("set_lsb_depth", CobaltOpus.cobalt_opus_encoder_set_lsb_depth(encoder, params.lsbDepth()));
        encApply("set_max_bandwidth", CobaltOpus.cobalt_opus_encoder_set_max_bandwidth(encoder, params.maxBandwidth().toNative()));
    }

    @Override
    public EncodedAudioFrame encode(short[] pcm, int frameSize) {
        Objects.requireNonNull(pcm, "pcm cannot be null");
        requireOpen();
        var required = frameSize * channels;
        if (pcm.length < required) {
            throw new WhatsAppCallException.Opus(
                    "pcm length " + pcm.length + " is below frameSize*channels " + required);
        }
        MemorySegment.copy(pcm, 0, pcmInBuf, ValueLayout.JAVA_SHORT, 0, required);
        var t0 = System.nanoTime();
        int written;
        try {
            written = CobaltOpus.cobalt_opus_encode(encoder, pcmInBuf, frameSize, packetBuf, MAX_PACKET_BYTES);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus encode failed", t);
            throw new WhatsAppCallException.Opus("cobalt_opus_encode failed", t);
        }
        var encodeMicros = (System.nanoTime() - t0) / 1000L;
        if (written < 0) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus encode rejected: rc={0}", written);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_encode", written);
        }
        totalEncodedFrames++;
        lifetimeEncodeMicros += encodeMicros;
        lifetimeEncodedBytes += written;
        // TODO: a frame encoded while the target is BITRATE_AUTO is left out of the average target
        //  bitrate; the shim exposes no OPUS_GET_BITRATE read to resolve the libopus chosen value.
        if (appliedTargetBitrate != OpusCodecParams.BITRATE_AUTO) {
            lifetimeTargetBitrateSum += appliedTargetBitrate;
            targetBitrateSamples++;
        }
        adaptComplexity(encodeMicros);
        if (written == 0) {
            // A discontinuous transmission frame carries no packet: the sender drops it before reading
            // its level and it never reaches the wire, so reuse a shared empty payload and skip the
            // per sample level scan whose result would go unread.
            lastWasDiscontinuous = true;
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls opus encode: discontinuous frame, bytes=0");
            return new EncodedAudioFrame(DataUtils.EMPTY_BYTE_ARRAY, false, true, false, EncodedAudioFrame.SILENCE_LEVEL);
        }
        var payload = new byte[written];
        MemorySegment.copy(packetBuf, ValueLayout.JAVA_BYTE, 0, payload, 0, written);
        var discontinuous = written < SPEECH_THRESHOLD_BYTES;
        lastWasDiscontinuous = discontinuous;
        var voiceActive = !discontinuous && packetHasVoiceActivity(payload);
        var hasFec = !discontinuous && packetHasInbandFec(payload);
        var levelDbov = audioLevelDbov(pcm, required);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls opus encode: bytes={0} voiceActive={1} discontinuous={2} fec={3}",
                    written, voiceActive, discontinuous, hasFec);
        }
        return new EncodedAudioFrame(payload, voiceActive, discontinuous, hasFec, levelDbov);
    }

    /**
     * Measures the audio level of one captured block as a positive {@code -dBov} magnitude.
     *
     * <p>Computes the root mean square energy of the interleaved 16 bit samples, divides it by the full
     * scale amplitude, and maps the ratio onto a {@code [0, 127]} magnitude where {@code 0} is the
     * loudest possible signal and {@code 127} is silence; a block with no measurable energy reports
     * {@link EncodedAudioFrame#SILENCE_LEVEL}. The level reflects the captured loudness rather than the
     * encoder output, so it is well defined even for a discontinuous transmission frame.
     *
     * @implNote This implementation sums the squares of every sample, takes
     * {@code rms = (int) sqrt(sumSquares / count)} truncated toward zero, and returns
     * {@code (int) clamp(-20 * log10(rms / 32767.0), 0, 127) & 0x7f}; when {@code rms / 32767.0} is not
     * positive it returns {@code 127}. The squared sum is divided by the sample element count, so a
     * stereo block divides by {@code frameSize * channels} rather than by the per channel frame count.
     *
     * @param pcm         the captured interleaved samples; never {@code null}
     * @param sampleCount the number of valid interleaved samples at the start of {@code pcm}
     * @return the {@code -dBov} level magnitude in {@code [0, 127]}
     */
    private static int audioLevelDbov(short[] pcm, int sampleCount) {
        if (sampleCount < 1) {
            return EncodedAudioFrame.SILENCE_LEVEL;
        }
        var sumSquares = 0.0;
        for (var index = 0; index < sampleCount; index++) {
            double sample = pcm[index];
            sumSquares += sample * sample;
        }
        var rms = (int) Math.sqrt(sumSquares / sampleCount);
        var ratio = rms / AUDIO_LEVEL_FULL_SCALE;
        if (ratio <= 0.0) {
            return EncodedAudioFrame.SILENCE_LEVEL;
        }
        var level = AUDIO_LEVEL_DB_FACTOR * Math.log10(ratio);
        return (int) Math.clamp(level, 0.0, EncodedAudioFrame.SILENCE_LEVEL) & 0x7F;
    }

    /**
     * Feeds the frame's encode time to the complexity controller and applies a new level when it moves.
     *
     * @param encodeMicros the native encode wall time of the frame, in microseconds
     */
    private void adaptComplexity(long encodeMicros) {
        complexityController.recordEncode(encodeMicros, params.frameMillis());
        if (complexityController.complexityChanged()) {
            encApply("set_complexity", CobaltOpus.cobalt_opus_encoder_set_complexity(encoder, complexityController.complexity()));
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls opus adaptive complexity -> {0}", complexityController.complexity());
        }
    }

    @Override
    public short[] decode(byte[] payload, int frameSize, boolean decodeFec) {
        Objects.requireNonNull(payload, "payload cannot be null");
        requireOpen();
        MemorySegment.copy(payload, 0, packetInBuf, ValueLayout.JAVA_BYTE, 0, payload.length);
        var t0 = System.nanoTime();
        int decoded;
        try {
            decoded = CobaltOpus.cobalt_opus_decode(decoder, packetInBuf, payload.length, pcmOutBuf, frameSize, decodeFec ? 1 : 0);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus decode failed", t);
            throw new WhatsAppCallException.Opus("cobalt_opus_decode failed", t);
        }
        lifetimeDecodeMicros += (System.nanoTime() - t0) / 1000L;
        if (decoded < 0) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus decode rejected: rc={0}", decoded);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode", decoded);
        }
        totalDecodedFrames++;
        if (decodeFec) {
            fecFrames++;
        }
        var pcm = readPcm(decoded);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls opus decode: bytes={0} decodeFec={1} samples={2}",
                    payload.length, decodeFec, pcm.length);
        }
        return pcm;
    }

    @Override
    public short[] recover(byte[] nextPayload, int frameSize) {
        requireOpen();
        var t0 = System.nanoTime();
        var decodeFec = fecPacker.shouldDecodeFec(nextPayload != null);
        int decoded;
        try {
            if (decodeFec) {
                MemorySegment.copy(nextPayload, 0, packetInBuf, ValueLayout.JAVA_BYTE, 0, nextPayload.length);
                decoded = CobaltOpus.cobalt_opus_decode(decoder, packetInBuf, nextPayload.length, pcmOutBuf, frameSize, 1);
            } else {
                decoded = CobaltOpus.cobalt_opus_decode(decoder, MemorySegment.NULL, 0, pcmOutBuf, frameSize, 0);
            }
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus decode recovery failed", t);
            throw new WhatsAppCallException.Opus("cobalt_opus_decode recovery failed", t);
        }
        lifetimeDecodeMicros += (System.nanoTime() - t0) / 1000L;
        if (decoded < 0) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus decode recovery rejected: rc={0}", decoded);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode recovery", decoded);
        }
        if (decodeFec) {
            fecFrames++;
        } else {
            plcFrames++;
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "calls opus recover: concealmentOnly={0} frameSize={1}",
                    nextPayload == null, frameSize);
        }
        return readPcm(decoded);
    }

    /**
     * Copies the decoded samples from the native output buffer into a fresh heap array.
     *
     * @param decodedSamples the per channel sample count libopus reported
     * @return the decoded interleaved PCM samples
     */
    private short[] readPcm(int decodedSamples) {
        var total = decodedSamples * channels;
        var out = new short[total];
        MemorySegment.copy(pcmOutBuf, ValueLayout.JAVA_SHORT, 0, out, 0, total);
        return out;
    }

    @Override
    public void modify(OpusCodecParams params) {
        Objects.requireNonNull(params, "params cannot be null");
        requireOpen();
        var cappedBitrate = capBitrate(params.defaultBitrate());
        encApply("set_bitrate", CobaltOpus.cobalt_opus_encoder_set_bitrate(encoder, cappedBitrate));
        this.appliedTargetBitrate = cappedBitrate;
        encApply("set_packet_loss_perc", CobaltOpus.cobalt_opus_encoder_set_packet_loss_perc(encoder, params.packetLossPercent()));
        encApply("set_vbr", CobaltOpus.cobalt_opus_encoder_set_vbr(encoder, params.variableBitrate() ? 1 : 0));
        encApply("set_complexity", CobaltOpus.cobalt_opus_encoder_set_complexity(encoder, params.complexity()));
        encApply("set_max_bandwidth", CobaltOpus.cobalt_opus_encoder_set_max_bandwidth(encoder, params.maxBandwidth().toNative()));
        this.params = params;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "calls opus codec modify: bitrate={0} packetLossPercent={1} complexity={2} maxBandwidth={3}",
                    cappedBitrate, params.packetLossPercent(), params.complexity(), params.maxBandwidth());
        }
    }

    /**
     * Clamps a requested target bitrate by the cap implied by the maximum bandwidth ceiling.
     *
     * <p>An automatic bitrate is passed through unchanged. Otherwise the requested value is clamped to
     * the open parameters' {@linkplain OpusCodecParams#maxBitrate() maximum bitrate}, which the
     * {@link OpusDefaultAttr} table derives per sample rate; the maximum bandwidth ceiling further bounds
     * the effective rate libopus reaches.
     *
     * @implNote This implementation clamps an explicit bitrate to the per sample rate maximum the
     * {@link OpusDefaultAttr} table supplies and leaves the narrower bandwidth bound to libopus itself.
     *
     * @param requestedBitrate the requested target bitrate, or {@link OpusCodecParams#BITRATE_AUTO}
     * @return the clamped target bitrate
     */
    private int capBitrate(int requestedBitrate) {
        if (requestedBitrate == OpusCodecParams.BITRATE_AUTO) {
            return requestedBitrate;
        }
        return Math.min(requestedBitrate, params.maxBitrate());
    }

    @Override
    public AudioCodecStats stats() {
        var avgEncode = totalEncodedFrames == 0 ? 0 : lifetimeEncodeMicros / totalEncodedFrames;
        var avgDecode = totalDecodedFrames == 0 ? 0 : lifetimeDecodeMicros / totalDecodedFrames;
        return new AudioCodecStats(
                totalEncodedFrames,
                totalDecodedFrames,
                fecFrames,
                plcFrames,
                avgEncode,
                avgDecode,
                avgTargetBitrate(),
                observedBitrate());
    }

    /**
     * Derives the running average encoder target bitrate from the per frame target bitrate sum.
     *
     * @implNote This implementation computes the average as the running target bitrate sum divided by
     * its sample count, yielding {@code 0} when no frame has been sampled. It samples the target last
     * applied through {@code cobalt_opus_encoder_set_bitrate}, which equals the resolved target for
     * every concrete bitrate; a frame encoded while the target is {@link OpusCodecParams#BITRATE_AUTO}
     * is not sampled.
     *
     * @return the running average target bitrate in bits per second, or {@code 0} before the first
     * sampled frame
     */
    private int avgTargetBitrate() {
        if (targetBitrateSamples == 0) {
            return 0;
        }
        return (int) (lifetimeTargetBitrateSum / targetBitrateSamples);
    }

    /**
     * Derives the observed bitrate from the running average encoded frame size and the frame duration.
     *
     * @implNote This implementation takes the average bytes per frame times eight bits, scaled from per
     * frame to per second by the frame duration in milliseconds.
     *
     * @return the observed bitrate in bits per second, or {@code 0} before the first frame
     */
    private int observedBitrate() {
        if (totalEncodedFrames == 0 || params.frameMillis() <= 0) {
            return 0;
        }
        var avgBytes = (double) lifetimeEncodedBytes / totalEncodedFrames;
        return (int) Math.round(avgBytes * 8.0 * 1000.0 / params.frameMillis());
    }

    @Override
    public boolean lastFrameWasDiscontinuous() {
        return lastWasDiscontinuous;
    }

    /**
     * Inspects the encoded packet for the voice activity flag.
     *
     * @implNote This implementation derives speech presence from the packet bandwidth, treating a
     * decodable bandwidth as voice activity, matching the standard Opus path.
     *
     * @param payload the encoded packet bytes
     * @return whether the packet is voice active
     */
    private boolean packetHasVoiceActivity(byte[] payload) {
        if (payload.length < SPEECH_THRESHOLD_BYTES) {
            return false;
        }
        MemorySegment.copy(payload, 0, packetInBuf, ValueLayout.JAVA_BYTE, 0, payload.length);
        try {
            return CobaltOpus.cobalt_opus_packet_get_bandwidth(packetInBuf) >= 0;
        } catch (Throwable t) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls opus packet bandwidth probe failed", t);
            return false;
        }
    }

    /**
     * Inspects the encoded packet for an in band forward error correction (LBRR) copy of the previous
     * frame.
     *
     * @implNote This implementation reads the per packet LBRR flag directly from the encoded packet
     * through {@code cobalt_opus_packet_has_lbrr}, which parses the Opus packet and reads the per SILK
     * frame LBRR flag at the bit positions the table of contents byte selects, rather than approximating
     * it from the encoder's in band FEC configuration. A native parse failure falls back to
     * {@code false}; the result feeds FEC statistics only, not interop.
     *
     * @param payload the encoded packet bytes
     * @return whether the packet carries an in band FEC (LBRR) copy of the previous frame
     */
    private boolean packetHasInbandFec(byte[] payload) {
        if (payload.length == 0) {
            return false;
        }
        MemorySegment.copy(payload, 0, packetInBuf, ValueLayout.JAVA_BYTE, 0, payload.length);
        try {
            return CobaltOpus.cobalt_opus_packet_has_lbrr(packetInBuf, payload.length) == 1;
        } catch (Throwable t) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls opus packet lbrr probe failed", t);
            return false;
        }
    }

    /**
     * Checks the return code of one typed {@link CobaltOpus} encoder setter, throwing on failure.
     *
     * <p>The typed {@code cobalt_opus_encoder_set_*} shim functions apply the matching variadic
     * {@code opus_encoder_ctl} request on the C side and return the libopus result directly; this turns
     * any code other than {@code COBALT_OPUS_OK} into a thrown exception carrying libopus's textual
     * description.
     *
     * @param control a short name of the control for the error message (e.g. {@code "set_bitrate"})
     * @param rc      the return code from the {@code cobalt_opus_encoder_set_*} call
     * @throws WhatsAppCallException.Opus if {@code rc} is not {@code COBALT_OPUS_OK}
     */
    private void encApply(String control, int rc) {
        if (rc != CobaltOpus.COBALT_OPUS_OK()) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls opus encoder control failed: control={0} rc={1}", control, rc);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_encoder_" + control, rc);
        }
    }

    /**
     * Verifies that both native states are still live.
     *
     * @throws IllegalStateException if the codec has been closed
     */
    private void requireOpen() {
        if (encoder == null || encoder.equals(MemorySegment.NULL)
                || decoder == null || decoder.equals(MemorySegment.NULL)) {
            throw new IllegalStateException("OpusAudioCodec is closed");
        }
    }

    /**
     * Destroys the native encoder and decoder states if live, swallowing any native error.
     *
     * <p>Used from a failed constructor and from {@link #close()}; nulls each pointer to
     * {@link MemorySegment#NULL} so a later call detects the closed codec.
     */
    private void destroyStates() {
        if (encoder != null && !encoder.equals(MemorySegment.NULL)) {
            try {
                CobaltOpus.cobalt_opus_encoder_destroy(encoder);
            } catch (Throwable t) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "calls opus encoder destroy failed", t);
            }
            encoder = MemorySegment.NULL;
        }
        if (decoder != null && !decoder.equals(MemorySegment.NULL)) {
            try {
                CobaltOpus.cobalt_opus_decoder_destroy(decoder);
            } catch (Throwable t) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "calls opus decoder destroy failed", t);
            }
            decoder = MemorySegment.NULL;
        }
    }

    @Override
    public void close() {
        if (encoder == null || encoder.equals(MemorySegment.NULL)) {
            return;
        }
        destroyStates();
        arena.close();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls opus codec closed");
    }
}
