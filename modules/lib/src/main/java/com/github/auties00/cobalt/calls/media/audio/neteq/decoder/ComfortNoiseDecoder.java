package com.github.auties00.cobalt.calls.media.audio.neteq.decoder;

import com.github.auties00.cobalt.calls.media.audio.neteq.ComfortNoiseGenerator;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Renders RFC 3389 comfort noise as a permitted {@link AudioDecoder}, the pseudo decoder the jitter buffer
 * registers for the comfort noise payload type.
 *
 * <p>This decoder adapts the {@link ComfortNoiseGenerator} onto the decoder seam so the jitter buffer can
 * treat a comfort noise payload (a silence insertion descriptor) like any other audio payload type. A
 * {@link #decode(byte[], int, boolean)} call interprets the payload as a descriptor, updating the
 * generator's noise level and spectral shape and synthesizing one frame; a {@link #conceal(int)} call
 * synthesizes one frame from the last descriptor without updating it, which is how a discontinuous gap
 * between descriptors is filled. The forward error correction flag is ignored, since comfort noise carries
 * no redundant copy.
 *
 * <p>The decoder is single writer, driven by the jitter buffer's pull thread; it is pure Java synthesis
 * with no native state.
 *
 * @implNote This implementation wraps {@link ComfortNoiseGenerator}, which performs the synthesis. It
 * covers only RFC 3389 comfort noise carried as its own payload type; codec internal comfort noise is
 * produced instead by the active audio decoder, not by this class.
 */
public final class ComfortNoiseDecoder implements AudioDecoder {
    /**
     * The logger for {@link ComfortNoiseDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(ComfortNoiseDecoder.class);

    /**
     * The output sample rate in hertz this decoder reports.
     */
    private final int sampleRate;

    /**
     * The output channel count this decoder reports.
     */
    private final int channels;

    /**
     * The comfort noise synthesis kernel this decoder drives.
     *
     * <p>Holds the most recent silence insertion descriptor and the synthesis filter memory across frames.
     */
    private final ComfortNoiseGenerator generator;

    /**
     * Whether the decoder has been closed.
     */
    private volatile boolean closed;

    /**
     * Constructs a comfort noise decoder for the given output geometry.
     *
     * @param sampleRate the output sample rate in Hz
     * @param channels   the output channel count, {@code 1} for mono
     */
    public ComfortNoiseDecoder(int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.generator = new ComfortNoiseGenerator();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation treats {@code payload} as a silence insertion descriptor, updates the
     * {@link ComfortNoiseGenerator} from it, and synthesizes one frame; the {@code fec} flag is ignored.
     */
    @Override
    public short[] decode(byte[] payload, int frameSamples, boolean fec) {
        Objects.requireNonNull(payload, "payload cannot be null");
        requireOpen();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "comfort noise descriptor updated, payload={0} frameSamples={1}",
                    payload, frameSamples);
        }
        generator.update(payload);
        return generator.generate(frameSamples);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation synthesizes one frame from the last descriptor without updating it, the
     * continuation of a comfort noise gap when no fresh descriptor has arrived.
     */
    @Override
    public short[] conceal(int frameSamples) {
        requireOpen();
        return generator.generate(frameSamples);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation always returns {@code false}: a comfort noise silence insertion
     * descriptor never carries active speech, so a frame synthesized from it is reported voice inactive.
     */
    @Override
    public boolean packetHasVoiceActivity(byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation clears the {@link ComfortNoiseGenerator} synthesis filter memory.
     */
    @Override
    public void reset() {
        requireOpen();
        generator.reset();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "comfort noise decoder reset");
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
     * @implNote This implementation marks the decoder closed; there is no native state to release.
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
            throw new IllegalStateException("ComfortNoiseDecoder is closed");
        }
    }
}
