package com.github.auties00.cobalt.calls.media.audio.neteq.decoder;

import com.github.auties00.cobalt.calls.media.audio.codec.opus.bindings.CobaltOpus;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.NativeLibLoader;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import com.github.auties00.cobalt.calls.media.audio.neteq.LiveNetEq;

/**
 * Decodes Opus packets into PCM for the {@link LiveNetEq} jitter buffer through libopus over the Foreign
 * Function and Memory API, the default {@link AudioDecoder}.
 *
 * <p>The decoder owns one native {@code OpusDecoder} state, held behind the {@link CobaltOpus} shim as an
 * opaque handle, and a reusable native output buffer, both from a per instance arena.
 * {@link #decode(byte[], int, boolean)} copies the packet into a confined native scratch segment, invokes
 * {@code cobalt_opus_decode} with the requested forward error correction flag, and copies the decoded
 * interleaved PCM back into a fresh array; {@link #conceal(int)} invokes {@code cobalt_opus_decode} with a
 * null packet, the libopus packet loss concealment convention; {@link #reset()} issues
 * {@code cobalt_opus_decoder_reset_state} (libopus {@code OPUS_RESET_STATE}). The decoder is single writer:
 * the shared output buffer and the native state must be driven from the jitter buffer's pull thread.
 * Closing destroys the native state and releases the arena.
 *
 * <p>For the WhatsApp call audio format construct as {@code new OpusAudioDecoder(16000, 1)} and pull 320
 * samples per 20 ms frame, the NetEq get period.
 *
 * @implNote This implementation wraps three libopus decode paths the jitter buffer drives: a normal decode,
 * a packet loss concealment decode with a {@code NULL} packet, and an in band forward error correction
 * decode with {@code decode_fec=1} taken from the following packet. It follows the same arena and buffer
 * reuse pattern used elsewhere in the Opus audio path and adds the forward error correction decode path
 * NetEq needs. The native state and the libopus kernel are not reimplemented; only the decode dispatch the
 * jitter buffer drives is routed through the portable {@link CobaltOpus} shim, which hides the
 * {@code OpusDecoder} state behind an opaque handle and converts the variadic {@code opus_decoder_ctl} reset
 * into the typed {@code cobalt_opus_decoder_reset_state}.
 */
public final class OpusAudioDecoder implements AudioDecoder {
    /**
     * The logger for {@link OpusAudioDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(OpusAudioDecoder.class);

    static {
        NativeLibLoader.load("cobalt-native", Arena.global());
    }

    /**
     * Upper bound, in samples per channel, on a single decoded Opus frame.
     *
     * <p>Fixed at 5760, the sample count of a 60 ms frame at 48 kHz, the largest the Opus format permits, so
     * the reusable output buffer fits any legal frame without reallocation.
     */
    private static final int MAX_FRAME_SAMPLES = 5760;

    /**
     * Per instance arena owning the native decoder state pointer and the reusable PCM output buffer.
     *
     * <p>Shared rather than confined so the segments outlive the constructing call frame; closed by
     * {@link #close()}.
     */
    private final Arena arena;

    /**
     * The output sample rate in hertz the decoder was created with.
     */
    private final int sampleRate;

    /**
     * The channel count the decoder was created with, {@code 1} for mono or {@code 2} for stereo.
     *
     * <p>The interleaved output length is the decoded sample count times this value.
     */
    private final int channels;

    /**
     * Reusable native buffer {@code opus_decode} writes the decoded interleaved PCM into.
     *
     * <p>Sized once at construction for the worst case frame and reused across every decode and conceal
     * call.
     */
    private final MemorySegment pcmBuffer;

    /**
     * Pointer to the native {@code OpusDecoder} state allocated by {@code opus_decoder_create}.
     *
     * <p>Set to {@link MemorySegment#NULL} once destroyed, so {@link #requireOpen()} detects a closed
     * decoder.
     */
    private MemorySegment state;

    /**
     * Constructs an Opus decoder for the given output sample rate and channel count.
     *
     * <p>Allocates the native decoder state and the reusable output buffer from a fresh shared arena; if
     * libopus reports a nonzero error code or a null state pointer, the partial native state and the arena
     * are released before the exception propagates.
     *
     * @param sampleRate the output sample rate in Hz; one of {@code 8000}, {@code 12000}, {@code 16000},
     *                   {@code 24000}, {@code 48000}
     * @param channels   the channel count, {@code 1} for mono or {@code 2} for stereo
     * @throws IllegalArgumentException   if {@code channels} is not {@code 1} or {@code 2}
     * @throws WhatsAppCallException.Opus if libopus rejects the configuration
     * @throws UnsatisfiedLinkError       if libopus cannot be loaded
     */
    public OpusAudioDecoder(int sampleRate, int channels) {
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("channels must be 1 or 2, got " + channels);
        }
        this.sampleRate = sampleRate;
        this.channels = channels;
        // TODO: pool per call native scratch through a WaCallMemoryArena as WhatsApp does, rather than a
        //  fresh FFM Arena per codec/decoder; the shared pooling path is not yet wired into the Opus, MLow,
        //  and video FFM bring up sites.
        this.arena = Arena.ofShared();
        try {
            var outHandle = arena.allocate(ValueLayout.ADDRESS);
            int rc;
            try {
                rc = CobaltOpus.cobalt_opus_decoder_create(sampleRate, channels, outHandle);
            } catch (Throwable t) {
                throw new WhatsAppCallException.Opus("cobalt_opus_decoder_create failed", t);
            }
            this.state = outHandle.get(ValueLayout.ADDRESS, 0);
            if (rc != CobaltOpus.COBALT_OPUS_OK() || state.equals(MemorySegment.NULL)) {
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decoder_create", rc);
            }
            this.pcmBuffer = arena.allocate((long) MAX_FRAME_SAMPLES * channels * 2);
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "opus decoder create failed sampleRate=" + sampleRate + " channels=" + channels, e);
            destroyState();
            arena.close();
            throw e;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opus decoder created sampleRate={0} channels={1}", sampleRate, channels);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation copies the packet into a confined native scratch segment and calls
     * {@code cobalt_opus_decode} with {@code decode_fec} set from {@code fec}; a {@code true} flag
     * reconstructs the previous lost frame from this packet's in band LBRR copy. A negative return is turned
     * into a thrown exception.
     */
    @Override
    public short[] decode(byte[] payload, int frameSamples, boolean fec) {
        Objects.requireNonNull(payload, "payload cannot be null");
        requireOpen();
        try (var scratch = Arena.ofConfined()) {
            var data = scratch.allocate(Math.max(payload.length, 1));
            MemorySegment.copy(payload, 0, data, ValueLayout.JAVA_BYTE, 0, payload.length);
            int samples;
            try {
                samples = CobaltOpus.cobalt_opus_decode(state, data, payload.length, pcmBuffer, frameSamples, fec ? 1 : 0);
            } catch (Throwable t) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "cobalt_opus_decode failed frameSamples=" + frameSamples + " fec=" + fec, t);
                throw new WhatsAppCallException.Opus("cobalt_opus_decode failed", t);
            }
            if (samples < 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "opus decode failed rc={0} frameSamples={1} fec={2}", samples, frameSamples, fec);
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode", samples);
            }
            return copyOut(samples);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation copies the decoded PCM out of the native buffer straight into
     * {@code destination}, sparing the fresh array {@link #decode(byte[], int, boolean)} allocates; the
     * decode itself is identical.
     */
    @Override
    public int decode(byte[] payload, int frameSamples, boolean fec, short[] destination) {
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        requireOpen();
        try (var scratch = Arena.ofConfined()) {
            var data = scratch.allocate(Math.max(payload.length, 1));
            MemorySegment.copy(payload, 0, data, ValueLayout.JAVA_BYTE, 0, payload.length);
            int samples;
            try {
                samples = CobaltOpus.cobalt_opus_decode(state, data, payload.length, pcmBuffer, frameSamples, fec ? 1 : 0);
            } catch (Throwable t) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "cobalt_opus_decode failed frameSamples=" + frameSamples + " fec=" + fec, t);
                throw new WhatsAppCallException.Opus("cobalt_opus_decode failed", t);
            }
            if (samples < 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "opus decode failed rc={0} frameSamples={1} fec={2}", samples, frameSamples, fec);
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode", samples);
            }
            return copyOutInto(samples, destination);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation fuses the decode and the voice activity read onto one confined native
     * scratch segment: it copies the packet in once, decodes it, then, when the decode is not a forward
     * error correction pass and the packet is nonempty, reads {@code cobalt_opus_packet_get_bandwidth} from
     * that same segment, saving the second scratch allocation and copy the separate
     * {@link #decode(byte[], int, boolean)} and {@link #packetHasVoiceActivity(byte[])} calls would perform.
     * The PCM and the verdict are bit identical to those two calls.
     */
    @Override
    public DecodedFrame decodeWithVoiceActivity(byte[] payload, int frameSamples, boolean fec) {
        Objects.requireNonNull(payload, "payload cannot be null");
        requireOpen();
        try (var scratch = Arena.ofConfined()) {
            var data = scratch.allocate(Math.max(payload.length, 1));
            MemorySegment.copy(payload, 0, data, ValueLayout.JAVA_BYTE, 0, payload.length);
            int samples;
            try {
                samples = CobaltOpus.cobalt_opus_decode(state, data, payload.length, pcmBuffer, frameSamples, fec ? 1 : 0);
            } catch (Throwable t) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "cobalt_opus_decode failed frameSamples=" + frameSamples + " fec=" + fec, t);
                throw new WhatsAppCallException.Opus("cobalt_opus_decode failed", t);
            }
            if (samples < 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "opus decode failed rc={0} frameSamples={1} fec={2}", samples, frameSamples, fec);
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode", samples);
            }
            var pcm = copyOut(samples);
            var voiceActive = false;
            if (!fec && payload.length != 0) {
                try {
                    voiceActive = CobaltOpus.cobalt_opus_packet_get_bandwidth(data) >= 0;
                } catch (Throwable _) {
                    voiceActive = false;
                }
            }
            return new DecodedFrame(pcm, voiceActive);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation is the fill into caller form of
     * {@link #decodeWithVoiceActivity(byte[], int, boolean)}: it fuses the decode and the voice activity
     * read onto one confined native scratch segment exactly as that method does, but copies the PCM straight
     * into {@code destination} rather than a fresh array. The written PCM and the verdict are bit identical.
     */
    @Override
    public DecodedInto decodeWithVoiceActivityInto(byte[] payload, int frameSamples, boolean fec, short[] destination) {
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        requireOpen();
        try (var scratch = Arena.ofConfined()) {
            var data = scratch.allocate(Math.max(payload.length, 1));
            MemorySegment.copy(payload, 0, data, ValueLayout.JAVA_BYTE, 0, payload.length);
            int samples;
            try {
                samples = CobaltOpus.cobalt_opus_decode(state, data, payload.length, pcmBuffer, frameSamples, fec ? 1 : 0);
            } catch (Throwable t) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "cobalt_opus_decode failed frameSamples=" + frameSamples + " fec=" + fec, t);
                throw new WhatsAppCallException.Opus("cobalt_opus_decode failed", t);
            }
            if (samples < 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "opus decode failed rc={0} frameSamples={1} fec={2}", samples, frameSamples, fec);
                throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode", samples);
            }
            var length = copyOutInto(samples, destination);
            var voiceActive = false;
            if (!fec && payload.length != 0) {
                try {
                    voiceActive = CobaltOpus.cobalt_opus_packet_get_bandwidth(data) >= 0;
                } catch (Throwable _) {
                    voiceActive = false;
                }
            }
            return new DecodedInto(length, voiceActive);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation calls {@code cobalt_opus_decode} with a {@link MemorySegment#NULL}
     * packet and zero length, the libopus convention requesting packet loss concealment for one missing
     * frame; the codec synthesizes plausible audio from its decode history.
     */
    @Override
    public short[] conceal(int frameSamples) {
        requireOpen();
        int samples;
        try {
            samples = CobaltOpus.cobalt_opus_decode(state, MemorySegment.NULL, 0, pcmBuffer, frameSamples, 0);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "cobalt_opus_decode (PLC) failed frameSamples=" + frameSamples, t);
            throw new WhatsAppCallException.Opus("cobalt_opus_decode (PLC) failed", t);
        }
        if (samples < 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "opus plc conceal failed rc={0} frameSamples={1}", samples, frameSamples);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode (PLC)", samples);
        }
        return copyOut(samples);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation runs the same {@link MemorySegment#NULL} packet loss concealment decode
     * as {@link #conceal(int)} but copies the synthesized PCM straight into {@code destination}.
     */
    @Override
    public int conceal(int frameSamples, short[] destination) {
        Objects.requireNonNull(destination, "destination cannot be null");
        requireOpen();
        int samples;
        try {
            samples = CobaltOpus.cobalt_opus_decode(state, MemorySegment.NULL, 0, pcmBuffer, frameSamples, 0);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "cobalt_opus_decode (PLC) failed frameSamples=" + frameSamples, t);
            throw new WhatsAppCallException.Opus("cobalt_opus_decode (PLC) failed", t);
        }
        if (samples < 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "opus plc conceal failed rc={0} frameSamples={1}", samples, frameSamples);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decode (PLC)", samples);
        }
        return copyOutInto(samples, destination);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation covers the standard Opus path (not the MLow path): it copies the packet
     * into a confined native scratch segment and treats a nonnegative {@code cobalt_opus_packet_get_bandwidth}
     * as voice activity, deriving speech presence from the packet bandwidth. A more precise read of the SILK
     * internal VAD would require the patched libopus expert CTLs the public binding does not expose. An empty
     * packet or a native parse failure reports inactive.
     */
    @Override
    public boolean packetHasVoiceActivity(byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (payload.length == 0) {
            return false;
        }
        try (var scratch = Arena.ofConfined()) {
            var data = scratch.allocate(payload.length);
            MemorySegment.copy(payload, 0, data, ValueLayout.JAVA_BYTE, 0, payload.length);
            return CobaltOpus.cobalt_opus_packet_get_bandwidth(data) >= 0;
        } catch (Throwable _) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation calls {@code cobalt_opus_decoder_reset_state}, which applies libopus
     * {@code OPUS_RESET_STATE} through {@code opus_decoder_ctl} on the C side, clearing decode history
     * including the concealment buffer without changing the sample rate or channel count.
     */
    @Override
    public void reset() {
        requireOpen();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opus decoder reset");
        int rc;
        try {
            rc = CobaltOpus.cobalt_opus_decoder_reset_state(state);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "cobalt_opus_decoder_reset_state failed", t);
            throw new WhatsAppCallException.Opus("cobalt_opus_decoder_reset_state failed", t);
        }
        if (rc != CobaltOpus.COBALT_OPUS_OK()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "opus decoder reset failed rc={0}", rc);
            throw WhatsAppCallException.Opus.fromErr("cobalt_opus_decoder_reset_state", rc);
        }
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
     * @implNote This implementation destroys the native state and releases the per instance arena; a second
     * call after the state is destroyed returns without effect.
     */
    @Override
    public void close() {
        if (state == null || state.equals(MemorySegment.NULL)) {
            return;
        }
        destroyState();
        arena.close();
    }

    /**
     * Copies the decoded interleaved PCM out of the native buffer into a fresh array.
     *
     * @param samples the per channel sample count libopus decoded
     * @return a fresh array of {@code samples * channels} signed 16 bit samples
     */
    private short[] copyOut(int samples) {
        var out = new short[samples * channels];
        MemorySegment.copy(pcmBuffer, ValueLayout.JAVA_SHORT, 0, out, 0, out.length);
        return out;
    }

    /**
     * Copies the decoded interleaved PCM out of the native buffer into a caller supplied array.
     *
     * @param samples     the per channel sample count libopus decoded
     * @param destination the array to copy the {@code samples * channels} signed 16 bit samples into
     * @return the number of samples copied, {@code samples * channels}
     */
    private int copyOutInto(int samples, short[] destination) {
        var total = samples * channels;
        MemorySegment.copy(pcmBuffer, ValueLayout.JAVA_SHORT, 0, destination, 0, total);
        return total;
    }

    /**
     * Verifies that the decoder is still open.
     *
     * <p>Treats both a {@code null} state field and a {@link MemorySegment#NULL} state pointer as closed.
     *
     * @throws IllegalStateException if the decoder has been closed
     */
    private void requireOpen() {
        if (state == null || state.equals(MemorySegment.NULL)) {
            throw new IllegalStateException("OpusAudioDecoder is closed");
        }
    }

    /**
     * Destroys the native decoder state if it is still live.
     *
     * <p>Calls {@code cobalt_opus_decoder_destroy} and nulls the state pointer; any throwable from the
     * native call is swallowed so this can run safely from a failed constructor or from {@link #close()}.
     */
    private void destroyState() {
        if (state == null || state.equals(MemorySegment.NULL)) {
            return;
        }
        try {
            CobaltOpus.cobalt_opus_decoder_destroy(state);
        } catch (Throwable _) {
        }
        state = MemorySegment.NULL;
    }
}
