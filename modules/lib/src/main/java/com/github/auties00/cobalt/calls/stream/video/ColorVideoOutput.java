package com.github.auties00.cobalt.calls.stream.video;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;
import com.github.auties00.cobalt.calls.stream.audio.SilenceAudioOutput;

/**
 * Transmits a solid color as the local video of a call.
 *
 * <p>This is the generated {@link VideoOutput} returned by {@link VideoOutput#fromColor(int)} and, for
 * black, {@link VideoOutput#fromBlank()}. Every {@link #takeVideo()} yields a {@link VideoFrame} of one constant
 * color in {@link VideoPixelFormat#I420 I420} at the advertised geometry, with a presentation timestamp that
 * never decreases. The stream never ends on its own; it keeps producing the color until the call engine
 * shuts it down, so it is the video counterpart of {@link SilenceAudioOutput}: it makes a call a video call
 * carrying a placeholder picture (a "camera off" fill) rather than a live source.
 *
 * <p>The color is converted once from packed {@code 0xRRGGBB} to a single luma and chroma pair using the
 * limited range BT.601 coefficients, and the resulting I420 planes are filled once at construction. Because
 * the picture is constant, every frame shares that one immutable pixel buffer, so the source allocates no
 * pixels for any individual frame regardless of how long the call runs.
 */
public final class ColorVideoOutput implements VideoOutput {
    /**
     * The logger for {@link ColorVideoOutput}.
     */
    private static final System.Logger LOGGER = Log.get(ColorVideoOutput.class);

    /**
     * Holds the default frame width in pixels the color factories advertise.
     */
    static final int DEFAULT_WIDTH = 640;

    /**
     * Holds the default frame height in pixels the color factories advertise.
     */
    static final int DEFAULT_HEIGHT = 480;

    /**
     * Holds the default advertised frame rate a color source with no explicit rate emits at.
     */
    private static final int DEFAULT_FPS = 30;

    /**
     * Holds the default advertised initial encoder bitrate in bits per second.
     *
     * @implNote This implementation mirrors the encoder seed
     * {@link com.github.auties00.cobalt.calls.media.video.codec.VideoCodecParams#DEFAULT_INIT_TARGET_BITRATE},
     * WhatsApp's {@code vid_rc.max_init_bwe} initial bandwidth estimate; the value is advertised only.
     */
    private static final int DEFAULT_BITRATE_BPS = 350_000;

    /**
     * Holds the advertised frame width in pixels.
     */
    private final int width;

    /**
     * Holds the advertised frame height in pixels.
     */
    private final int height;

    /**
     * Holds the advertised frame rate.
     */
    private final int fps;

    /**
     * Holds the advertised encoder bitrate in bits per second.
     */
    private final int bitrateBps;

    /**
     * Holds the constant I420 planes of the advertised color, shared by every emitted frame since the
     * picture never changes.
     */
    private final byte[] pixels;

    /**
     * Holds the duration one frame represents in microseconds, by which each frame's presentation timestamp
     * advances, derived from the advertised frame rate.
     */
    private final long frameDurationMicros;

    /**
     * Holds the presentation timestamp, in microseconds, of the next frame, advanced atomically so the
     * stream may be drained from any thread.
     */
    private final AtomicLong ptsMicros = new AtomicLong();

    /**
     * Marks the source ended so {@link #takeVideo()} returns {@code null}.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Holds the composed silence audio companion supplying the audio side of this source, since a generated
     * color picture carries no audio; the inherited {@link AudioOutput} methods delegate to it.
     */
    private final AudioOutput audio = AudioOutput.fromSilence();

    /**
     * Constructs a solid color source at the default {@value #DEFAULT_WIDTH} by {@value #DEFAULT_HEIGHT}
     * geometry, default frame rate, and default bitrate.
     *
     * @param rgb the color as packed {@code 0xRRGGBB}
     */
    public ColorVideoOutput(int rgb) {
        this(rgb, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Constructs a solid color source at the given resolution, the default frame rate, and the default
     * bitrate.
     *
     * @param rgb    the color as packed {@code 0xRRGGBB}
     * @param width  the frame width in pixels; even and at least {@code 2}
     * @param height the frame height in pixels; even and at least {@code 2}
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or below {@code 2}
     */
    public ColorVideoOutput(int rgb, int width, int height) {
        this(rgb, width, height, DEFAULT_FPS, DEFAULT_BITRATE_BPS);
    }

    /**
     * Constructs a solid color source at the given color and geometry.
     *
     * <p>Converts the color to I420 and fills the plane buffer once; each {@link #takeVideo()} then reuses it.
     *
     * @param rgb        the color as packed {@code 0xRRGGBB}
     * @param width      the frame width in pixels; even and at least {@code 2}
     * @param height     the frame height in pixels; even and at least {@code 2}
     * @param fps        the target frame rate; at least {@code 1}
     * @param bitrateBps the target encoder bitrate in bits per second; at least {@code 1}
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or below {@code 2}, or
     *                                  {@code fps} or {@code bitrateBps} is below {@code 1}
     */
    public ColorVideoOutput(int rgb, int width, int height, int fps, int bitrateBps) {
        if (width < 2 || width % 2 != 0) {
            throw new IllegalArgumentException("width must be even and >= 2, got " + width);
        }
        if (height < 2 || height % 2 != 0) {
            throw new IllegalArgumentException("height must be even and >= 2, got " + height);
        }
        if (fps < 1) {
            throw new IllegalArgumentException("fps must be >= 1, got " + fps);
        }
        if (bitrateBps < 1) {
            throw new IllegalArgumentException("bitrateBps must be >= 1, got " + bitrateBps);
        }
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitrateBps = bitrateBps;
        this.frameDurationMicros = 1_000_000L / fps;
        this.pixels = i420(rgb, width, height);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "color video output created, {0}x{1}@{2}fps", width, height, fps);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A generated source produces its frames inside {@link #takeVideo()} and ignores application writes, so
     * this does nothing.
     *
     * @param frame the frame that would be written; ignored
     */
    @Override
    public void writeVideo(VideoFrame frame) {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a frame over the shared constant pixel buffer and advances the presentation timestamp by
     * the configured frame duration. Never returns {@code null}, so the stream ends only when
     * {@link #shutdown()} runs.
     *
     * @return a new frame of the advertised color; never {@code null} until shut down
     * @implNote This implementation returns one frame per call with no sleep: the call engine's capture
     * loop paces outbound video to wall clock using each frame's running presentation timestamp, so the
     * color is transmitted at its natural rate without this stream having to sleep.
     */
    @Override
    public VideoFrame takeVideo() {
        if (closed.get()) {
            return null;
        }
        var pts = ptsMicros.getAndAdd(frameDurationMicros);
        return new VideoFrame(pixels, VideoPixelFormat.I420, width, height, pts);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the source ended so the next {@link #takeVideo()} returns {@code null}, and ends the audio
     * companion. Idempotent.
     */
    @Override
    public void shutdown() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "shutting down color video output");
        closed.set(true);
        audio.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ignored: the audio companion generates its own frames inside {@link #takeAudio()}.
     *
     * @param frame the frame that would be written; ignored
     * @throws InterruptedException if the calling thread is interrupted while waiting for buffer space
     */
    @Override
    public void writeAudio(AudioFrame frame) throws InterruptedException {
        audio.writeAudio(frame);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Yields a silence frame from the composed audio companion, so a solid color video call carries
     * silent audio unless the caller supplies a separate audio source.
     *
     * @return a silence frame; never {@code null} until shut down
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    @Override
    public AudioFrame takeAudio() throws InterruptedException {
        return audio.takeAudio();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code false}; generated silence is not live acoustic capture
     */
    @Override
    public boolean isLiveCapture() {
        return audio.isLiveCapture();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int width() {
        return width;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int height() {
        return height;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int fps() {
        return fps;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int bitrateBps() {
        return bitrateBps;
    }

    /**
     * Builds the I420 plane buffer for a solid color at the given geometry.
     *
     * <p>Converts the packed color to one luma and one chroma pair, then fills the full resolution Y plane
     * with the luma and each half resolution chroma plane with its value.
     *
     * @param rgb    the color as packed {@code 0xRRGGBB}
     * @param width  the frame width in pixels
     * @param height the frame height in pixels
     * @return the filled I420 planes, {@code width*height + 2*(width/2)*(height/2)} bytes
     */
    private static byte[] i420(int rgb, int width, int height) {
        var r = (rgb >> 16) & 0xFF;
        var g = (rgb >> 8) & 0xFF;
        var b = rgb & 0xFF;
        var y = clamp((int) Math.round(0.257 * r + 0.504 * g + 0.098 * b) + 16);
        var u = clamp((int) Math.round(-0.148 * r - 0.291 * g + 0.439 * b) + 128);
        var v = clamp((int) Math.round(0.439 * r - 0.368 * g - 0.071 * b) + 128);
        var lumaBytes = width * height;
        var chromaBytes = (width / 2) * (height / 2);
        var planes = new byte[lumaBytes + 2 * chromaBytes];
        Arrays.fill(planes, 0, lumaBytes, (byte) y);
        Arrays.fill(planes, lumaBytes, lumaBytes + chromaBytes, (byte) u);
        Arrays.fill(planes, lumaBytes + chromaBytes, planes.length, (byte) v);
        return planes;
    }

    /**
     * Clamps a color component to the {@code [0, 255]} byte range.
     *
     * @param value the component value
     * @return {@code value} bounded to {@code [0, 255]}
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
