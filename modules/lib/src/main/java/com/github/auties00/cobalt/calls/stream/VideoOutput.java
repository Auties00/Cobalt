package com.github.auties00.cobalt.calls.stream;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import com.github.auties00.cobalt.calls.stream.video.CameraVideoOutput;
import com.github.auties00.cobalt.calls.stream.video.ColorVideoOutput;
import com.github.auties00.cobalt.calls.stream.video.FfmpegVideoOutput;
import com.github.auties00.cobalt.calls.stream.video.ScreenVideoOutput;

/**
 * Defines the local outbound video source of a call: the application supplied origin of the
 * {@link VideoFrame}s the call engine encodes and transmits, together with the geometry it is encoded
 * and advertised at.
 *
 * <p>This is the write side of a call's video, and its presence on a placed or accepted call is what
 * makes the call a video call; supplying no video source makes the call audio only. The engine
 * repeatedly {@linkplain #takeVideo() pulls} frames on a dedicated virtual thread, encodes each with the
 * negotiated video codec at this source's {@link #width()} by {@link #height()} at {@link #fps()} and
 * {@link #bitrateBps()}, and ships it to the peer. The contract has two faces. The application facing
 * face is {@link #writeVideo(VideoFrame)}, by which a programmatic producer pushes the picture it wants
 * transmitted. The engine facing face is {@link #takeVideo()} and {@link #shutdown()}. A device backed
 * source instead fills itself inside its {@link #takeVideo()} from a camera, screen, or media file and
 * ignores {@link #writeVideo(VideoFrame)}.
 *
 * <p>Frames carry planar 4:2:0 pixels as described by {@link VideoFrame}; the resolution carried by an
 * individual frame may differ from this source's advertised {@link #width()} by {@link #height()},
 * and the engine scales captured frames to the advertised geometry. The geometry an implementation
 * reports is fixed for the lifetime of the source: the engine reads it once when the source is
 * installed to size the encoder and advertise the stream to the peer. An implementation decides its
 * own buffering and backpressure policy between the producer and the engine drain. The application
 * never ends the source itself; the engine invokes {@link #shutdown()} when the call ends, which an
 * implementation uses to release any device it bound.
 *
 * @apiNote An embedder implements this interface for a custom video source, or obtains a bundled
 * implementation from one of the factories on this type: {@link #fromCamera()} for live webcam capture,
 * {@link #fromScreen()} for screen sharing, {@link #fromFile(Path)} for a local media file,
 * {@link #fromUri(URI)} for a media stream addressed by URI, and {@link #fromColor(int)} or
 * {@link #fromBlank()} for a generated solid color "camera off" fill, the default video source. A generated
 * source has no device to probe, so it requires explicit even geometry; a media source, or a camera or
 * screen opened in its native mode, instead detects its input's own native resolution, capping the longer
 * side to {@code 1280} and rounding both dimensions to even, so a 16:9 source stays 16:9 rather than being
 * squished to a fixed default. A camera opened at a requested geometry through
 * {@link #fromCamera(String, int, int, int)} instead advertises the resolution the device delivers, so no
 * rescale is needed. A screen share source is announced to the peer as a screen share video stream carrying
 * its detected source resolution. The {@link #takeVideo()} and
 * {@link #shutdown()} methods belong to the engine; application code drives a programmatic source
 * through {@link #writeVideo(VideoFrame)} and never calls the engine facing pair directly.
 */
public interface VideoOutput extends AudioOutput {
    /**
     * Returns a source that transmits a solid color at a default {@code 640x480} 30 fps geometry.
     *
     * <p>Every frame carries the one color, so this is the "camera off" placeholder that keeps a call a
     * video call without a live source; {@link #fromBlank()} is the black special case. The stream never
     * ends on its own.
     *
     * @param rgb the color as packed {@code 0xRRGGBB}, for example {@code 0xFF0000} for red
     * @return a solid color source at the default geometry
     */
    static VideoOutput fromColor(int rgb) {
        return new ColorVideoOutput(rgb);
    }

    /**
     * Returns a source that transmits a solid color at the given resolution, 30 fps, with the recovered
     * WhatsApp initial bitrate.
     *
     * @param rgb    the color as packed {@code 0xRRGGBB}
     * @param width  the frame width in pixels; even and at least {@code 2}
     * @param height the frame height in pixels; even and at least {@code 2}
     * @return a solid color source at the given resolution
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or below {@code 2}
     */
    static VideoOutput fromColor(int rgb, int width, int height) {
        return new ColorVideoOutput(rgb, width, height);
    }

    /**
     * Returns a source that transmits a solid black picture at a default {@code 640x480} 30 fps geometry.
     *
     * <p>This is the video counterpart of {@link AudioOutput#fromSilence()}: it makes a call a video call
     * carrying a blank "camera off" picture rather than a live source, and is the default video fill. The
     * stream never ends on its own.
     *
     * @return a black source at the default geometry
     */
    static VideoOutput fromBlank() {
        return fromColor(0x000000);
    }

    /**
     * Returns a source that transmits the video track of a media file, advertising its detected native
     * resolution.
     *
     * <p>The file's decoded frames are scaled to the advertised geometry by the encoder. The advertised
     * geometry is the file's own native video resolution, capped to {@code 1280} on the longer side and
     * rounded to even, so a 16:9 file is advertised as 16:9 rather than squished to a fixed default.
     *
     * @param path the media file to stream
     * @return a file bound source advertising its detected native resolution
     * @throws NullPointerException  if {@code path} is {@code null}
     * @throws IllegalStateException if the file cannot be opened or has no video stream
     */
    static VideoOutput fromFile(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        return new FfmpegVideoOutput.File(path);
    }

    /**
     * Returns a source that transmits the video track of a media stream addressed by a URI, advertising its
     * detected native resolution, with a fifteen second timeout on every blocking network operation.
     *
     * <p>Generalizes {@link #fromFile(Path)} to a local {@code file:} path or an {@code http:}/{@code https:}
     * asset. HTTP and HTTPS are fetched over a JDK connection and fed to the decoder, so the native library
     * carries no network or TLS code; redirects are followed (including {@code http} to {@code https}), the
     * connect and every read are bounded by the timeout, and a seekable resource is fetched with range
     * requests so a
     * container whose index trails its media still opens. The stream's decoded frames are scaled to the
     * advertised geometry by the encoder, which is the stream's own native video resolution capped to
     * {@code 1280} on the longer side and rounded to even. The accepted schemes are restricted to
     * {@code file}, {@code http}, and {@code https}, so an application supplied string cannot reach an
     * unintended protocol.
     *
     * @param uri the media stream to open
     * @return a URI bound source advertising its detected native resolution
     * @throws NullPointerException     if {@code uri} is {@code null}
     * @throws IllegalArgumentException if the URI has no scheme or its scheme is not permitted
     * @throws IllegalStateException    if the stream cannot be opened or has no video stream
     */
    static VideoOutput fromUri(URI uri) {
        return fromUri(uri, Duration.ofSeconds(15));
    }

    /**
     * Returns a source that transmits the video track of a media stream addressed by a URI, advertising its
     * detected native resolution and bounding every blocking network operation by the given timeout.
     *
     * <p>If a connect, stream probe, or read makes no progress within {@code ioTimeout}, the operation is
     * aborted and the source ends with an error rather than stalling the call's encode thread. The
     * advertised geometry is the stream's own native video resolution, capped to {@code 1280} on the longer
     * side and rounded to even.
     *
     * @param uri       the media stream to open
     * @param ioTimeout the maximum time any single connect, probe, or read may block; must be positive
     * @return a URI bound source advertising its detected native resolution
     * @throws NullPointerException     if {@code uri} or {@code ioTimeout} is {@code null}
     * @throws IllegalArgumentException if {@code ioTimeout} is not positive, the URI has no scheme, or its
     *                                  scheme is not permitted
     * @throws IllegalStateException    if the stream cannot be opened or has no video stream
     */
    static VideoOutput fromUri(URI uri, Duration ioTimeout) {
        Objects.requireNonNull(uri, "uri cannot be null");
        Objects.requireNonNull(ioTimeout, "ioTimeout cannot be null");
        if (ioTimeout.isNegative() || ioTimeout.isZero()) {
            throw new IllegalArgumentException("ioTimeout must be positive, got " + ioTimeout);
        }
        return new FfmpegVideoOutput.Uri(uri, ioTimeout);
    }

    /**
     * Returns a source bound to the platform's default camera, advertising its detected native resolution.
     *
     * <p>Each {@link #takeVideo()} captures one planar 4:2:0 frame from the default camera, blocking on the
     * device until a frame is available, until the call ends and the device is released; the captured
     * frames are scaled to the advertised geometry by the encoder. The advertised geometry is the camera's
     * own native resolution, capped to {@code 1280} on the longer side and rounded to even.
     *
     * @return a camera bound source advertising its detected native resolution
     * @throws IllegalStateException if no camera is available on the running platform
     */
    static VideoOutput fromCamera() {
        return new CameraVideoOutput();
    }

    /**
     * Returns a source bound to a named camera, advertising its detected native resolution.
     *
     * <p>Opens the named device through the platform's default libavdevice input format; the captured
     * frames are scaled to the advertised geometry by the encoder, which is the camera's own native
     * resolution capped to {@code 1280} on the longer side and rounded to even.
     *
     * @param deviceUrl the device URL or name (for example {@code "/dev/video1"} on Linux, {@code "1"} on
     *                  macOS, or {@code "Integrated Camera"} on Windows)
     * @return a camera bound source advertising its detected native resolution
     * @throws NullPointerException  if {@code deviceUrl} is {@code null}
     * @throws IllegalStateException if the device cannot be opened or has no video stream
     */
    static VideoOutput fromCamera(String deviceUrl) {
        return new CameraVideoOutput(deviceUrl);
    }

    /**
     * Returns a source bound to a named camera at a requested capture geometry, advertising the resolution
     * the device actually delivers.
     *
     * <p>Requests {@code width} by {@code height} at {@code fps} from the device as a capture mode, so the
     * driver produces frames already at that resolution and the engine never rescales them; the source
     * advertises the resolution the device delivered, which equals the request when the device supports it.
     * Prefer this over {@link #fromCamera(String)} when the encode resolution is known, since it avoids the
     * cost of capturing at the native resolution and then rescaling that the native mode factory pays.
     *
     * @param deviceUrl the device URL or name (for example {@code "/dev/video1"} on Linux, {@code "1"} on
     *                  macOS, or {@code "Integrated Camera"} on Windows)
     * @param width     the requested capture width in pixels; even and at least {@code 2}
     * @param height    the requested capture height in pixels; even and at least {@code 2}
     * @param fps       the requested capture frame rate; at least {@code 1}
     * @return a camera bound source advertising the delivered resolution
     * @throws NullPointerException     if {@code deviceUrl} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or below {@code 2}, or
     *                                  {@code fps} is below {@code 1}
     * @throws IllegalStateException    if the device cannot be opened at the requested geometry or has no
     *                                  video stream
     */
    static VideoOutput fromCamera(String deviceUrl, int width, int height, int fps) {
        if (fps < 1) {
            throw new IllegalArgumentException("fps must be at least 1, got " + fps);
        }
        return new CameraVideoOutput(deviceUrl, width, height, fps, 0);
    }

    /**
     * Returns a source bound to an explicit libavdevice input format and URL pair, advertising its detected
     * native resolution.
     *
     * <p>This is the power user factory for when neither {@link #fromCamera()} nor {@link #fromCamera(String)}
     * selects the right device: it opens {@code url} through the named {@code indev} input format.
     *
     * @param indev the libavdevice input format name (for example {@code "v4l2"}, {@code "avfoundation"}, or
     *              {@code "dshow"})
     * @param url   the device URL
     * @return a camera bound source advertising its detected native resolution
     * @throws NullPointerException  if {@code indev} or {@code url} is {@code null}
     * @throws IllegalStateException if the named input format is unavailable, the device cannot be opened, or
     *                               it has no video stream
     */
    static VideoOutput fromCamera(String indev, String url) {
        return new CameraVideoOutput(indev, url);
    }

    /**
     * Returns a source that shares the platform's default screen, advertising its detected native
     * resolution.
     *
     * <p>Each {@link #takeVideo()} captures the default display as one planar 4:2:0 frame, until the call ends.
     * The engine announces this to the peer as a screen share video stream carrying the display's own
     * native resolution, capped to {@code 1280} on the longer side and rounded to even.
     *
     * @return a screen share source advertising its detected native resolution
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    static VideoOutput fromScreen() {
        return ScreenVideoOutput.primary();
    }

    /**
     * Writes one frame of local video to transmit.
     *
     * <p>Offers the frame to the engine for encoding and transmission. An implementation chooses
     * whether a full internal buffer blocks the caller (backpressure) or drops a frame, and what
     * happens after {@link #shutdown()} has run; the only universal requirement is that the frame is
     * never {@code null}. A device backed source produces frames inside {@link #takeVideo()} and may ignore
     * this method.
     *
     * @param frame the frame to transmit; never {@code null}
     * @throws NullPointerException if {@code frame} is {@code null}
     * @throws InterruptedException if the calling thread is interrupted while waiting for buffer space
     */
    void writeVideo(VideoFrame frame) throws InterruptedException;

    /**
     * Returns the next frame for the engine to encode, blocking until one is available, or
     * {@code null} once the source has ended.
     *
     * <p>A buffered source returns frames previously supplied through {@link #writeVideo(VideoFrame)}; a
     * device backed source pulls the next frame straight from its capture device or decoder. The
     * method blocks while no frame is ready and returns {@code null} exactly once the source is
     * permanently drained, after which the engine stops pulling.
     *
     * <p>The returned frame's {@linkplain VideoFrame#pixels() pixel buffer} may be borrowed from a buffer
     * the source reuses across frames: a device backed source may refill and offer the same array again on
     * the next call, so it is valid only until the next call to this method on the same source. A consumer
     * that
     * needs the pixels beyond the next call, such as one that buffers frames in a queue, copies them out; it
     * must neither retain the returned array past the next call nor mutate it. The engine's encode path
     * copies each frame into the codec before pulling the next, so it satisfies this contract.
     *
     * @return the next frame, or {@code null} at end of stream
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    VideoFrame takeVideo() throws InterruptedException;

    /**
     * Ends the source, unblocking a pending {@link #takeVideo()} and releasing any bound device.
     *
     * <p>Invoked by the engine when the call ends. After it runs, {@link #takeVideo()} returns {@code null}
     * and the implementation releases any capture device or decoder it held. Implementations make this
     * idempotent, since the engine may signal teardown more than once during a racing shutdown.
     */
    void shutdown();

    /**
     * Returns the frame width in pixels the engine encodes and advertises this video at.
     *
     * <p>Read once when the source is installed; even and at least {@code 2}. The engine scales
     * captured frames whose native width differs to this advertised width. A media source, or a camera or
     * screen opened in its native mode, reports its input's detected native width capped to {@code 1280} on
     * the longer side and rounded to even; a camera opened at a requested geometry reports the width the
     * device delivered; and a generated source reports the explicit width it was given.
     *
     * @return the advertised frame width in pixels
     */
    int width();

    /**
     * Returns the frame height in pixels the engine encodes and advertises this video at.
     *
     * <p>Read once when the source is installed; even and at least {@code 2}. The engine scales
     * captured frames whose native height differs to this advertised height. A media source, or a camera or
     * screen opened in its native mode, reports its input's detected native height capped to {@code 1280} on
     * the longer side and rounded to even; a camera opened at a requested geometry reports the height the
     * device delivered; and a generated source reports the explicit height it was given.
     *
     * @return the advertised frame height in pixels
     */
    int height();

    /**
     * Returns the target frame rate the engine encodes this video at.
     *
     * <p>Read once when the source is installed; at least {@code 1}. The engine uses it to pace the
     * encoder and as the rate it advertises to the peer.
     *
     * @return the target frame rate in frames per second
     */
    int fps();

    /**
     * Returns the target encoder bitrate in bits per second.
     *
     * <p>Read once when the source is installed; at least {@code 1}. It is the encoder's starting
     * target, which the engine's rate controller may adapt downward as the network dictates.
     *
     * @return the target bitrate in bits per second
     */
    int bitrateBps();
}
