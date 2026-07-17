package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.MLowAudioDecoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.MlowDecoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.MlowEncoder;

import java.util.Arrays;
import java.util.Objects;
import com.github.auties00.cobalt.calls.media.audio.codec.AudioCodec;
import com.github.auties00.cobalt.calls.media.audio.codec.AudioCodecStats;
import com.github.auties00.cobalt.calls.media.audio.codec.EncodedAudioFrame;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusAudioCodec;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusCodecParams;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusDefaultAttr;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * The MLow low bitrate speech codec, a permitted {@link AudioCodec} backed by the pure Java
 * {@link MlowEncoder} and {@link MlowDecoder} kernels.
 *
 * <p>MLow is WhatsApp's in house low bitrate speech codec (the {@code smpl} codec), a deterministic float
 * CELP vocoder, not a neural network. A frame is built from a classical CELP chain: linear predictive
 * (LPC) short term spectral modelling whose coefficients are transmitted as line spectral frequencies
 * under a two stage LSF vector quantization; a pitch and long term prediction adaptive codebook for the
 * periodic excitation; an algebraic fixed codebook of signed unit pulses for the residual; and the short
 * term synthesis filter that produces the speech. The quantized parameters are entropy coded with the
 * range coder behind a leading TOC byte naming the frame configuration, so both directions are
 * deterministic arithmetic coding with no learned weights in the path. The only machine learning component
 * is an optional bandwidth extension (BWE) postfilter, disabled by default, that sits after the codec core
 * and is not part of coding a frame.
 *
 * <p>{@link #encode(short[], int)} delegates to {@link MlowEncoder#encode(short[])}, which runs voice
 * activity detection, the per subframe analysis by synthesis search, and the range coded serialization,
 * emitting the packet TOC byte followed by the payload. {@link #decode(byte[], int, boolean)} delegates to
 * {@link MlowDecoder#decode(byte[])}, which parses the TOC byte, decodes every contained internal frame in
 * continuous decoder state order, and scales the synthesized signal to signed 16 bit PCM. Both kernels
 * thread cross frame and cross packet state, so the codec is single writer per the {@link AudioCodec}
 * contract and must be fed every frame of a stream in order. Scope is the SMPL 16 kHz, 60 ms, mono, low
 * band path with the postfilter disabled: a 60 ms MLow packet carries 960 samples regardless of the
 * {@code frameSize} requested, because the sample count is fixed by the packet's TOC.
 *
 * <p>The codec is constructed at the high rate and adapts its target bitrate to the engine bandwidth
 * estimate: {@link #modify(OpusCodecParams)} reads the live target the audio rate control loop writes,
 * clamps it to the MLow rate window {@code [6000, 32000]} bps, and retargets the internal bitrate
 * controller; MLow's own per subframe rate control then steers the instantaneous bitrate toward it. The
 * remaining Opus knobs (complexity, maximum bandwidth, packet loss percentage) do not map to the MLow CELP
 * core and are not applied. Packet loss recovery ({@link #recover(byte[], int)}) and in band forward error
 * correction decode ({@link #decode(byte[], int, boolean)}) delegate to the wrapped {@link MLowAudioDecoder},
 * which synthesizes concealment frames and reconstructs lost frames from the leading in band redundant
 * copy. The 1:1 and group media paths select MLow over {@link OpusAudioCodec} on the per call
 * {@code encode.use_mlow_codec_v1} voip parameter, delivered in the {@code encode} section of the
 * {@code <voip_settings>} document.
 *
 * @implNote This implementation routes encoding through the pure Java {@link MlowEncoder} and decoding
 * through the wrapped {@link MLowAudioDecoder}. The per frame classification flags are read back from the
 * emitted TOC byte ({@code bit 6} is the voice activity flag, {@code bit 1} the in band forward error
 * correction flag); the reported level reproduces the RMS {@code -dBov} magnitude, identical to
 * {@link OpusAudioCodec}'s computation. Both the encoder and decoder produce the whole packet's samples and
 * ignore the requested per frame sample count, so this codec does not enforce the
 * {@code frameSize * channels} length the fixed rate codecs assume beyond requiring a whole packet of input.
 */
public final class MLowAudioCodec implements AudioCodec {
    /**
     * The full scale signed 16 bit magnitude the audio level is measured against, matching
     * {@link OpusAudioCodec}.
     */
    private static final double AUDIO_LEVEL_FULL_SCALE = 32767.0;

    /**
     * The decibel scale factor mapping the captured root mean square ratio to a {@code -dBov} magnitude,
     * matching {@link OpusAudioCodec}.
     */
    private static final double AUDIO_LEVEL_DB_FACTOR = -20.0;

    /**
     * The payload byte threshold below which a frame is treated as discontinuous transmission or comfort
     * noise rather than speech, matching {@link OpusAudioCodec}.
     */
    private static final int SPEECH_THRESHOLD_BYTES = 3;

    /**
     * The TOC byte mask selecting the voice activity flag (bit 6).
     */
    private static final int TOC_VAD_MASK = 0x40;

    /**
     * The TOC byte mask selecting the in band forward error correction flag (bit 1).
     */
    private static final int TOC_FEC_MASK = 0x02;

    /**
     * The lowest target bitrate, in bits per second, MLow is retargeted to, the low rate floor.
     *
     * <p>The {@code [6000, 32000]} window matches the MLow rate range; the engine bandwidth estimate never
     * crosses below the 8700 bps 60 ms low rate threshold on the adaptive path, so the floor is the low rate
     * profile's own target.
     */
    private static final int MIN_TARGET_BITRATE = 6000;

    /**
     * The highest target bitrate, in bits per second, MLow is retargeted to, matching WhatsApp's observed cap.
     */
    private static final int MAX_TARGET_BITRATE = 32000;

    /**
     * The initial target bitrate, in bits per second, the encoder is constructed at before the first
     * {@link #modify(OpusCodecParams)} retargets it.
     *
     * <p>Set to WhatsApp's {@code 25000} bps high quality target, the 16 kHz wideband default the Opus path
     * also seeds from {@link OpusDefaultAttr#WB}; the rate control loop then adapts it downward toward the
     * engine bandwidth estimate under congestion.
     */
    private static final int INITIAL_TARGET_BITRATE = 25000;

    /**
     * The logger for {@link MLowAudioCodec}.
     */
    private static final System.Logger LOGGER = Log.get(MLowAudioCodec.class);

    /**
     * The pure Java MLow CELP analysis by synthesis kernel the encode path delegates to.
     *
     * <p>Threads cross frame and cross packet state internally, so it is constructed once per stream and
     * driven only from the single writer thread.
     */
    private final MlowEncoder encoder;

    /**
     * The MLow decoder the decode, in band forward error correction, and concealment paths delegate to.
     *
     * <p>Wraps the pure Java CELP synthesis kernel and the decode postfilter chain, so the codec's decode
     * side produces the same shipping quality PCM (including packet loss concealment and in band forward
     * error correction recovery) as the receive side {@link MLowAudioDecoder}. Threads cross frame and cross
     * packet state internally, so it is constructed once per stream and driven only from the single writer
     * thread.
     */
    private final MLowAudioDecoder decoder;

    /**
     * The cumulative count of frames passed to {@link #encode(short[], int)}, for the stats snapshot.
     */
    private long totalEncodedFrames;

    /**
     * The cumulative count of packets passed to {@link #decode(byte[], int, boolean)}, for the stats
     * snapshot.
     */
    private long totalDecodedFrames;

    /**
     * Whether the most recently encoded frame was classified as discontinuous (DTX or comfort noise).
     */
    private boolean lastWasDiscontinuous;

    /**
     * The target bitrate, in bits per second, last applied to the {@link MlowEncoder}.
     *
     * <p>Seeded to {@link #INITIAL_TARGET_BITRATE} at construction and updated by
     * {@link #modify(OpusCodecParams)} only when the clamped engine target changes, so a steady target leaves
     * the encoder untouched and its fixed rate output byte for byte identical.
     */
    private int appliedTargetBitrate;

    /**
     * Whether this codec has been closed; once closed every operation throws.
     *
     * <p>Declared {@code volatile} so an operation thread observes a close performed on the call teardown
     * thread without a data race.
     */
    private volatile boolean closed;

    /**
     * Constructs the MLow codec backed by fresh {@link MlowEncoder} and {@link MlowDecoder} kernels.
     *
     * <p>Both kernels start in their reset state, ready to code the first frame of a stream.
     */
    public MLowAudioCodec() {
        this.encoder = new MlowEncoder(INITIAL_TARGET_BITRATE, true);
        // The web and desktop MLow path is fixed at 16 kHz mono, with no runtime sample rate selection, so
        // the decoder rate is a compile time constant rather than a negotiated per call value.
        this.decoder = new MLowAudioDecoder(16_000, 1);
        this.appliedTargetBitrate = INITIAL_TARGET_BITRATE;
        this.closed = false;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to {@link MlowEncoder#encode(short[])} over the first
     * {@code frameSize} samples, which must be a whole number of 20 ms (320 sample) frames at 16 kHz mono
     * (a 60 ms packet is 960 samples). The voice activity and in band FEC flags are read back from the
     * emitted TOC byte; a frame whose payload is below {@link #SPEECH_THRESHOLD_BYTES} bytes is classified
     * discontinuous; the level is the RMS {@code -dBov} of the captured PCM.
     */
    @Override
    public EncodedAudioFrame encode(short[] pcm, int frameSize) {
        Objects.requireNonNull(pcm, "pcm cannot be null");
        requireOpen();
        if (pcm.length < frameSize) {
            throw new IllegalArgumentException("pcm length " + pcm.length + " is below frameSize " + frameSize);
        }
        var frame = pcm.length == frameSize ? pcm : Arrays.copyOf(pcm, frameSize);
        var payload = encoder.encode(frame);
        totalEncodedFrames++;
        var voiceActive = payload.length > 0 && (payload[0] & TOC_VAD_MASK) != 0;
        var hasFec = voiceActive && (payload[0] & TOC_FEC_MASK) != 0;
        // With discontinuous transmission enabled, MlowEncoder returns a zero length payload for a silence
        // frame it suppresses between SID interval ticks (classified discontinuous here, so the sender drops it
        // and advances the media clock) and a small SID comfort noise packet on the interval tick (above the
        // threshold, so it is transmitted with its VAD bit clear); an active voice packet is a full frame.
        var discontinuous = payload.length < SPEECH_THRESHOLD_BYTES;
        lastWasDiscontinuous = discontinuous;
        var levelDbov = audioLevelDbov(pcm, frameSize);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls mlow encode: bytes={0} voiceActive={1} discontinuous={2} fec={3}",
                    payload.length, voiceActive, discontinuous, hasFec);
        }
        return new EncodedAudioFrame(payload, voiceActive, discontinuous, hasFec, levelDbov);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to {@link MLowAudioDecoder#decode(byte[], int, boolean)}, which
     * decodes the whole packet from its TOC byte in continuous decoder state order and runs the shipping
     * postfilter chain. The {@code frameSize} request is not enforced: an MLow packet's sample count is fixed
     * by its TOC (a 60 ms packet yields 960 samples), so the returned array holds the packet's own sample
     * count. A {@code decodeFec} request reconstructs the previous lost frame from this packet's leading in
     * band forward error correction (LBRR) copy, falling back to concealment when the packet carries none.
     */
    @Override
    public short[] decode(byte[] payload, int frameSize, boolean decodeFec) {
        Objects.requireNonNull(payload, "payload cannot be null");
        requireOpen();
        var pcm = decoder.decode(payload, frameSize, decodeFec);
        totalDecodedFrames++;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls mlow decode: bytes={0} decodeFec={1} samples={2}",
                    payload.length, decodeFec, pcm.length);
        }
        return pcm;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to the wrapped {@link MLowAudioDecoder}: a non null
     * {@code nextPayload} reconstructs the lost frame from that packet's in band forward error correction copy
     * through {@link MLowAudioDecoder#decode(byte[], int, boolean)} with the recovery flag set (falling back
     * to concealment when the packet carries no copy), and a {@code null} {@code nextPayload} synthesizes a
     * concealment frame through {@link MLowAudioDecoder#conceal(int)}.
     */
    @Override
    public short[] recover(byte[] nextPayload, int frameSize) {
        requireOpen();
        var pcm = nextPayload != null
                ? decoder.decode(nextPayload, frameSize, true)
                : decoder.conceal(frameSize);
        totalDecodedFrames++;
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "calls mlow recover: concealmentOnly={0} frameSize={1}",
                    nextPayload == null, frameSize);
        }
        return pcm;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation retargets MLow's internal rate controller from the engine bandwidth
     * estimate: it reads the live target {@link OpusCodecParams#defaultBitrate()} the audio rate control loop
     * writes each round (the same field {@link OpusAudioCodec#modify(OpusCodecParams)} reads), clamps it to the
     * MLow rate window {@code [6000, 32000]} bps, and forwards it to {@link MlowEncoder#updateTargetBitrate(int)}
     * so the next packet's bitrate controller steers toward it. The automatic bitrate sentinel
     * {@link OpusCodecParams#BITRATE_AUTO} is ignored because MLow has no encoder chosen target to resolve. A
     * round whose clamped target equals the last applied value is skipped, so a steady bitrate leaves the
     * encoder untouched and its output byte for byte identical to the fixed rate path. The other Opus knobs
     * ({@code complexity}, {@code maxBandwidth}, packet loss percentage) do not map to the MLow CELP core and
     * are not applied.
     */
    @Override
    public void modify(OpusCodecParams params) {
        Objects.requireNonNull(params, "params cannot be null");
        requireOpen();
        var requested = params.defaultBitrate();
        if (requested == OpusCodecParams.BITRATE_AUTO) {
            return;
        }
        var clamped = Math.clamp(requested, MIN_TARGET_BITRATE, MAX_TARGET_BITRATE);
        if (clamped == appliedTargetBitrate) {
            return;
        }
        encoder.updateTargetBitrate(clamped);
        appliedTargetBitrate = clamped;
        if (Log.INFO) LOGGER.log(Level.INFO, "calls mlow audio target bitrate -> {0} bps", clamped);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation reports the encode side and decode side frame counters; the remaining
     * fields are zero because MLow does not surface the per bitstream byte and timing statistics the native
     * Opus codec exposes.
     */
    @Override
    public AudioCodecStats stats() {
        return new AudioCodecStats(totalEncodedFrames, totalDecodedFrames, 0L, 0L, 0L, 0L, 0, 0);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation returns the classification of the most recently encoded frame, read
     * from its TOC byte and payload length.
     */
    @Override
    public boolean lastFrameWasDiscontinuous() {
        return lastWasDiscontinuous;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation marks the codec closed and closes the wrapped decoder; the pure Java
     * encoder and decoder hold no native resource, so closing only releases their references and flips the
     * closed flag. A second call has no effect.
     */
    @Override
    public void close() {
        decoder.close();
        closed = true;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls mlow codec closed");
    }

    /**
     * Returns the captured frame's loudness as a positive {@code -dBov} magnitude in {@code [0, 127]}.
     *
     * <p>Computes the root mean square energy of the first {@code sampleCount} samples and maps it to a
     * decibel magnitude relative to full scale, identical to {@link OpusAudioCodec}'s level so the MLow and
     * Opus encode paths stamp the same audio level on equivalent input. An empty or silent block reports
     * {@link EncodedAudioFrame#SILENCE_LEVEL}.
     *
     * @param pcm         the captured samples; never {@code null}
     * @param sampleCount the number of valid samples at the start of {@code pcm}
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
     * Verifies that the codec is still open.
     *
     * @throws IllegalStateException if the codec has been closed
     */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("MLowAudioCodec is closed");
        }
    }
}
