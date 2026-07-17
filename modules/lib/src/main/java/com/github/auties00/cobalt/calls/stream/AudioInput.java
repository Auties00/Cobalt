package com.github.auties00.cobalt.calls.stream;

import javax.sound.sampled.Mixer;
import java.nio.file.Path;
import java.util.Objects;
import com.github.auties00.cobalt.calls.stream.audio.DiscardAudioInput;
import com.github.auties00.cobalt.calls.stream.audio.SpeakerAudioInput;
import com.github.auties00.cobalt.calls.stream.audio.WavFileAudioInput;

/**
 * Defines the remote inbound audio sink of a call: the application supplied destination for the
 * {@link AudioFrame}s the call engine decodes from the peer.
 *
 * <p>This is the read side of a call's audio. The engine decodes each audio packet received from the
 * peer and {@linkplain #offerAudio(AudioFrame) delivers} the resulting frame to this sink; the embedder
 * decides what becomes of it. The contract has two faces. The engine facing face is
 * {@link #offerAudio(AudioFrame)} and {@link #shutdown()}, by which the engine fills the sink and signals
 * the end of the stream. The application facing face is {@link #readAudio()}, by which a programmatic
 * consumer (a bot or a bridge between two calls) pulls received frames to forward or analyse them; a
 * sink backed by a device instead renders each frame to its playback device inside
 * {@link #offerAudio(AudioFrame)} and is not read from.
 *
 * <p>Frames carry mono 16 bit PCM at 16 kHz as described by {@link AudioFrame}. An implementation
 * decides its own buffering policy between the engine fill and the consumer; a buffered sink typically
 * prefers freshness, dropping the oldest buffered frame rather than stalling the decoder when the
 * consumer falls behind, so playback latency stays bounded. The application never ends the sink
 * itself; the engine invokes {@link #shutdown()} when the call ends, which an implementation uses to
 * finalize or release any device it bound.
 *
 * @apiNote An embedder implements this interface to consume or render received audio, or obtains a
 * bundled implementation from one of the factories on this type: {@link #discard()} to drop the received
 * audio, {@link #toSpeaker()} to render to the speaker, and {@link #toWav(Path)} to record to a WAV file.
 * The {@link #offerAudio(AudioFrame)} and {@link #shutdown()} methods belong to the engine; application code
 * drives a programmatic sink through {@link #readAudio()} and never calls the engine facing pair directly.
 */
public interface AudioInput {
    /**
     * Returns a sink that discards the received audio.
     *
     * <p>Every {@link #offerAudio(AudioFrame)} is dropped and {@link #readAudio()} yields nothing, so this is the
     * sink to install when a call needs no inbound audio. {@link #readAudio()} blocks until the call ends and
     * then returns {@code null}, so a consumer that drains it in a loop terminates cleanly.
     *
     * @return a discarding sink
     */
    static AudioInput discard() {
        return new DiscardAudioInput();
    }

    /**
     * Returns a sink bound to the operating system speaker.
     *
     * <p>Each {@link #offerAudio(AudioFrame)} renders the frame to the default output device, blocking while
     * the line buffer is full, until the call ends and the playback line is released. The application does
     * not read a sink bound to the speaker.
     *
     * @return a sink bound to the speaker
     * @throws IllegalStateException if no playback line is available on the running platform
     */
    static AudioInput toSpeaker() {
        return new SpeakerAudioInput();
    }

    /**
     * Returns a sink bound to the speaker with an explicit sample rate and output device.
     *
     * <p>Renders at the given sample rate on the given mixer rather than the 16 kHz call default on the
     * JVM default device; a decoder emitting at a different rate must be resampled before its frames reach
     * this sink or the audio plays at the wrong pitch.
     *
     * @param sampleRate     the playback sample rate in hertz; must be at least {@code 1}
     * @param preferredMixer the mixer to acquire the playback line from, or {@code null} for the JVM
     *                       default output device
     * @return a sink bound to the speaker at the requested format
     * @throws IllegalArgumentException if {@code sampleRate} is less than {@code 1}
     * @throws IllegalStateException    if no compatible line is available on the running platform
     */
    static AudioInput toSpeaker(int sampleRate, Mixer.Info preferredMixer) {
        return new SpeakerAudioInput(sampleRate, preferredMixer);
    }

    /**
     * Returns a sink that records the received audio to a WAV file.
     *
     * <p>Each {@link #offerAudio(AudioFrame)} appends the frame to the file; the file is finalized when the
     * call ends. The application does not read a sink bound to a file.
     *
     * @param path the WAV file to write
     * @return a sink bound to a file
     * @throws NullPointerException  if {@code path} is {@code null}
     * @throws IllegalStateException if the file cannot be created
     */
    static AudioInput toWav(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        return new WavFileAudioInput(path);
    }

    /**
     * Delivers one decoded remote frame for the application to consume.
     *
     * <p>Invoked by the engine for each frame it decodes from the peer. A buffered sink enqueues the
     * frame for {@link #readAudio()}, dropping the oldest buffered frame if the consumer is behind; a sink
     * backed by a device renders the frame straight to its playback device or file. After
     * {@link #shutdown()} has run an implementation discards the frame. The frame is never
     * {@code null}.
     *
     * <p>The frame's {@linkplain AudioFrame#pcm() sample buffer} may be borrowed from a pool the engine
     * reuses across frames, so it is valid only for the duration of this call. A sink that renders or writes
     * the frame synchronously before returning needs no copy; a sink that buffers it for a later
     * {@link #readAudio()} or hands it to another thread copies the samples out first (or copies the whole frame),
     * since the engine may refill the buffer on a subsequent decode.
     *
     * @param frame the decoded frame; never {@code null}
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    void offerAudio(AudioFrame frame);

    /**
     * Returns the next frame of received remote audio, blocking until one is available, or
     * {@code null} once the call has ended.
     *
     * <p>Returns frames previously delivered through {@link #offerAudio(AudioFrame)} in order. The method
     * blocks while no frame is ready and returns {@code null} exactly once the sink has been
     * {@linkplain #shutdown() ended} and drained. A sink backed by a device renders inside
     * {@link #offerAudio(AudioFrame)} and is not read from.
     *
     * <p>The returned frame's {@linkplain AudioFrame#pcm() sample buffer} is borrowed from a pool the
     * engine reuses across frames: it is valid only until the next call to this method on the same
     * input, after which the engine may refill and re offer it. A consumer that needs the samples beyond
     * the next read copies them out; it must never retain the returned array past the next read nor
     * mutate it.
     *
     * @return the next frame, or {@code null} once the stream has ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    AudioFrame readAudio() throws InterruptedException;

    /**
     * Ends the sink, unblocking a pending {@link #readAudio()} and finalizing any bound device.
     *
     * <p>Invoked by the engine when the call ends. After it runs, {@link #readAudio()} returns {@code null}
     * once drained and the implementation finalizes or releases any playback device or file it held.
     * Implementations make this idempotent, since the engine may signal teardown more than once during
     * a racing shutdown.
     */
    void shutdown();
}
