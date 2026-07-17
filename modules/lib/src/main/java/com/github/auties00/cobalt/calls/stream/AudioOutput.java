package com.github.auties00.cobalt.calls.stream;

import javax.sound.sampled.Mixer;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import com.github.auties00.cobalt.calls.stream.audio.FfmpegAudioOutput;
import com.github.auties00.cobalt.calls.stream.audio.MicrophoneAudioOutput;
import com.github.auties00.cobalt.calls.stream.audio.SilenceAudioOutput;

/**
 * Defines the local outbound audio source of a call: the application supplied origin of the
 * {@link AudioFrame}s the call engine encodes and transmits to the peer.
 *
 * <p>This is the write side of a call's audio. An embedder supplies one of these when placing or
 * accepting a call; the engine repeatedly {@linkplain #takeAudio() pulls} frames from it on a dedicated
 * virtual thread, encodes each with the negotiated audio codec, and ships it to the peer. The contract
 * has two faces. The application facing face is {@link #writeAudio(AudioFrame)}, by which a programmatic
 * producer (a bot, a bridge between two calls, or a synthetic generator) pushes the audio it wants
 * transmitted. The engine facing face is {@link #takeAudio()} and {@link #shutdown()}: the engine drains
 * frames and is signalled end of stream. A device backed source instead fills itself inside its
 * {@link #takeAudio()} from a capture device and ignores {@link #writeAudio(AudioFrame)}.
 *
 * <p>Frames carry mono 16 bit PCM at 16 kHz as described by {@link AudioFrame}. An implementation
 * decides its own buffering and backpressure policy between the producer and the engine drain; the
 * engine itself only requires that {@link #takeAudio()} block until a frame is available or the source has
 * ended, and that {@link #isLiveCapture()} truthfully report whether the audio is live acoustic
 * capture so the engine can apply acoustic conditioning only where it is warranted. The application
 * never ends the source itself; the engine invokes {@link #shutdown()} when the call ends, which an
 * implementation uses to release any device it bound.
 *
 * @apiNote An embedder implements this interface for a custom audio source, or obtains a bundled
 * implementation from one of the factories on this type: {@link #fromMicrophone()} for live capture,
 * {@link #fromFile(Path)} for a local media file, {@link #fromUri(URI)} for a media stream addressed by
 * URI, and {@link #fromSilence()} for generated silence, the default audio fill. The {@link #takeAudio()} and
 * {@link #shutdown()} methods belong to the engine; application code drives a programmatic source through
 * {@link #writeAudio(AudioFrame)} and never calls the engine facing pair directly.
 */
public interface AudioOutput {
    /**
     * Returns a source bound to the operating system microphone.
     *
     * <p>Each {@link #takeAudio()} captures one 16 kHz mono frame from the default microphone, blocking on the
     * capture line until a full frame is available, until the call ends and the capture line is released.
     * The application does not write to a microphone bound source, and the source reports
     * {@link #isLiveCapture()} as {@code true} so the engine applies echo cancellation and microphone
     * conditioning.
     *
     * @return a microphone bound source
     * @throws IllegalStateException if no capture line is available on the running platform
     */
    static AudioOutput fromMicrophone() {
        return new MicrophoneAudioOutput();
    }

    /**
     * Returns a source bound to the microphone with an explicit capture format and mixer.
     *
     * <p>Captures at the given sample rate and frame size on the given mixer rather than the 16 kHz
     * 160 sample call default on the JVM default device; capturing at a rate other than 16 kHz still
     * produces frames at the requested geometry, and the caller is then responsible for downsampling before
     * the frames reach the call.
     *
     * @param sampleRate     the capture sample rate in hertz; must be at least {@code 1}
     * @param frameSize      the number of samples per emitted frame; must be at least {@code 1}
     * @param preferredMixer the mixer to open the capture line on, or {@code null} for the JVM default
     *                       device
     * @return a microphone bound source at the requested format
     * @throws IllegalArgumentException if {@code sampleRate} or {@code frameSize} is less than {@code 1}
     * @throws IllegalStateException    if no compatible line is available on the running platform
     */
    static AudioOutput fromMicrophone(int sampleRate, int frameSize, Mixer.Info preferredMixer) {
        return new MicrophoneAudioOutput(sampleRate, frameSize, preferredMixer);
    }

    /**
     * Returns a source that transmits the audio track of a media file.
     *
     * <p>Each {@link #takeAudio()} decodes and resamples the next 16 kHz mono frame of the file; the source
     * ends when the file is exhausted or the call ends. Any container the bundled FFmpeg build can decode
     * is accepted.
     *
     * @param path the media file to stream
     * @return a file bound source
     * @throws NullPointerException  if {@code path} is {@code null}
     * @throws IllegalStateException if the file cannot be opened or has no audio stream
     */
    static AudioOutput fromFile(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        return new FfmpegAudioOutput.File(path);
    }

    /**
     * Returns a source that transmits the audio track of a media stream addressed by a URI, with a
     * fifteen second timeout on every blocking network operation.
     *
     * <p>Generalizes {@link #fromFile(Path)} to a local {@code file:} path or an {@code http:}/{@code https:}
     * asset. HTTP and HTTPS are fetched over a JDK connection and fed to the decoder, so the native library
     * carries no network or TLS code; redirects are followed (including {@code http} to {@code https}), the
     * connect and every read are bounded by the timeout, and a seekable resource is range requested so a
     * container whose index trails its media still opens. A faster than real time source is paced to the
     * call's real time send rate exactly as a local file is. The accepted schemes are restricted to
     * {@code file}, {@code http}, and {@code https}, so an application supplied string cannot reach an
     * unintended protocol.
     *
     * @param uri the media stream to open
     * @return a URI bound source
     * @throws NullPointerException     if {@code uri} is {@code null}
     * @throws IllegalArgumentException if the URI has no scheme or its scheme is not permitted
     * @throws IllegalStateException    if the stream cannot be opened or has no audio stream
     */
    static AudioOutput fromUri(URI uri) {
        return fromUri(uri, Duration.ofSeconds(15));
    }

    /**
     * Returns a source that transmits the audio track of a media stream addressed by a URI, bounding every
     * blocking network operation by the given timeout.
     *
     * <p>Behaves as {@link #fromUri(URI)} but takes the timeout explicitly: if a connect, stream probe, or
     * read makes no progress within {@code ioTimeout}, the operation is aborted and the source ends with an
     * error rather than stalling the call's encode thread. A short timeout fails fast on a dead host; a
     * longer one tolerates a slow link.
     *
     * @param uri       the media stream to open
     * @param ioTimeout the maximum time any single connect, probe, or read may block; must be positive
     * @return a URI bound source
     * @throws NullPointerException     if {@code uri} or {@code ioTimeout} is {@code null}
     * @throws IllegalArgumentException if {@code ioTimeout} is not positive, the URI has no scheme, or its
     *                                  scheme is not permitted
     * @throws IllegalStateException    if the stream cannot be opened or has no audio stream
     */
    static AudioOutput fromUri(URI uri, Duration ioTimeout) {
        Objects.requireNonNull(uri, "uri cannot be null");
        Objects.requireNonNull(ioTimeout, "ioTimeout cannot be null");
        if (ioTimeout.isNegative() || ioTimeout.isZero()) {
            throw new IllegalArgumentException("ioTimeout must be positive, got " + ioTimeout);
        }
        return new FfmpegAudioOutput.Uri(uri, ioTimeout);
    }

    /**
     * Returns a source that transmits continuous silence, the default audio fill.
     *
     * @return a silence bound source
     */
    static AudioOutput fromSilence() {
        return new SilenceAudioOutput();
    }

    /**
     * Returns a source that transmits continuous silence at an explicit frame geometry.
     *
     * @param frameSize           the number of samples per emitted frame; must be at least {@code 1}
     * @param frameDurationMicros the duration of each frame in microseconds; must be at least {@code 1}
     * @return a silence bound source at the given geometry
     * @throws IllegalArgumentException if {@code frameSize} or {@code frameDurationMicros} is less than
     *                                  {@code 1}
     */
    static AudioOutput fromSilence(int frameSize, long frameDurationMicros) {
        return new SilenceAudioOutput(frameSize, frameDurationMicros);
    }

    /**
     * Writes one frame of local audio to transmit.
     *
     * <p>Offers the frame to the engine for encoding and transmission. An implementation chooses
     * whether a full internal buffer blocks the caller (backpressure) or drops the oldest frame, and
     * what happens after {@link #shutdown()} has run; the only universal requirement is that the frame
     * is never {@code null}. A device backed source produces frames inside {@link #takeAudio()} and may
     * treat this method as a no op.
     *
     * @param frame the frame to transmit; never {@code null}
     * @throws NullPointerException if {@code frame} is {@code null}
     * @throws InterruptedException if the calling thread is interrupted while waiting for buffer space
     */
    void writeAudio(AudioFrame frame) throws InterruptedException;

    /**
     * Returns the next frame for the engine to encode, blocking until one is available, or
     * {@code null} once the source has ended.
     *
     * <p>A buffered source returns frames previously supplied through {@link #writeAudio(AudioFrame)}; a
     * device backed source pulls the next frame straight from its capture device or decoder. The
     * method blocks while no frame is ready and returns {@code null} exactly once the source is
     * permanently drained, after which the engine stops pulling.
     *
     * <p>The returned frame's {@linkplain AudioFrame#pcm() sample buffer} may be borrowed from a buffer the
     * source reuses across frames: a device backed source may refill and re offer the same array on the next
     * call, so it is valid only until the next call to this method on the same source. A consumer that needs
     * the samples beyond the next call, such as one that buffers frames in a queue, copies them out; it must
     * neither retain the returned array past the next call nor mutate it. The engine's capture pump copies
     * each frame into its ring before pulling the next, so it satisfies this contract.
     *
     * @return the next frame, or {@code null} at end of stream
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    AudioFrame takeAudio() throws InterruptedException;

    /**
     * Ends the source, unblocking a pending {@link #takeAudio()} and releasing any bound device.
     *
     * <p>Invoked by the engine when the call ends. After it runs, {@link #takeAudio()} returns {@code null}
     * and the implementation releases any capture device or decoder it held. Implementations make this
     * idempotent, since the engine may signal teardown more than once during a racing shutdown.
     */
    void shutdown();

    /**
     * Returns whether this source delivers live acoustic audio captured from a microphone.
     *
     * <p>Live microphone capture carries acoustic echo, where the remote party's audio leaks from the
     * local speaker back into the microphone, together with ambient noise, so the engine conditions it
     * with echo cancellation, denoise, automatic gain control, and voice activity detection. Every
     * other source (a media file, a synthetic tone, silence, or frames an application writes through
     * {@link #writeAudio(AudioFrame)}) is already clean line level audio, and running microphone
     * conditioning over it distorts the signal, so the engine encodes those sources without
     * preprocessing. An implementation returns {@code true} only for a true live microphone capture.
     *
     * @return {@code true} if the source is live microphone capture needing acoustic conditioning,
     *         otherwise {@code false}
     */
    boolean isLiveCapture();
}
