package com.github.auties00.cobalt.calls.stream.video;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecParameters;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFrame;
import com.github.auties00.cobalt.util.ffmpeg.AVInputFormat;
import com.github.auties00.cobalt.util.ffmpeg.AVPacket;
import com.github.auties00.cobalt.util.ffmpeg.AVStream;
import com.github.auties00.cobalt.util.ffmpeg.Ffmpeg;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegError;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegLoader;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;

/**
 * Captures an operating system camera as the local video of a call.
 *
 * <p>This is the device backed {@link VideoOutput} returned by {@link VideoOutput#fromCamera()}. Its
 * constructor opens a libavdevice capture device, demuxes its video stream, decodes it, and arranges to
 * deliver each decoded picture as {@link VideoPixelFormat#I420 I420} so the output matches
 * {@link VideoFrame}'s layout.
 *
 * <p>There are two ways to size the capture, and they differ in whether any scaling happens per frame:
 *
 * <ul>
 *   <li><b>Requested geometry</b> ({@link #CameraVideoOutput(String, int, int, int, int)}): the desired
 *       {@code width}, {@code height}, and {@code fps} are passed to the device as a capture mode, so the
 *       driver produces frames already at that resolution. The source then advertises the resolution the
 *       device actually delivered, so no libswscale rescale is ever needed; when the device already emits
 *       I420 the planes are copied straight through with no conversion at all.</li>
 *   <li><b>Native geometry</b> ({@link #CameraVideoOutput()}, {@link #CameraVideoOutput(String)},
 *       {@link #CameraVideoOutput(String, String)}): no mode is requested, so the device runs at its own
 *       default resolution. That resolution is advertised capped to {@code 1280} on the longer side and
 *       rounded to even, and each captured picture is rescaled to the capped geometry with libswscale, so a
 *       camera with a high resolution does not blow up the encoder.</li>
 * </ul>
 *
 * <p>In both cases the pixel format is converted to I420 when the device does not already emit it; only the
 * native path additionally scales. The same pipeline backs the screen grab sources created through
 * {@link ScreenVideoOutput}, which always use the native path because a screen grabber's {@code video_size}
 * crops rather than downscales.
 *
 * <h2>Platform defaults</h2>
 *
 * <ul>
 *   <li>Linux: {@code v4l2} on {@code /dev/video0}</li>
 *   <li>macOS: {@code avfoundation} on device index {@code 0}</li>
 *   <li>Windows: requires explicit selection, so {@link #CameraVideoOutput(String)} or
 *       {@link #CameraVideoOutput(String, int, int, int, int)} must be given the device's friendly name
 *       (for example {@code "Integrated Camera"}), because libavdevice's {@code dshow} input format has
 *       no stable default index</li>
 * </ul>
 *
 * <p>Capture is blocking: {@link #takeVideo()} returns once a frame is available, or {@code null} when the
 * device closes. {@link #shutdown()} releases the operating system device and the native decoder.
 *
 * <p>As a {@link VideoOutput} it is also an {@link AudioOutput}; a camera carries no audio, so the audio
 * side is generated silence. Pass a real audio source through the output-only or input-only call overloads
 * to send live audio alongside the camera.
 *
 * @implNote This implementation configures the capture device to the resolution it will encode at rather
 * than grabbing native and rescaling, matching WhatsApp, through the {@code video_size}/{@code framerate}
 * libavdevice options when a geometry is requested, and falls back to a capped rescale only when the caller
 * lets the device pick its native mode. Each frame carries a monotonic zero based {@link VideoFrame#ptsMicros()}
 * measured from {@link System#nanoTime()} on the first frame rather than absolute wall clock time, so the
 * derived RTP timestamp never moves backward. The frame path allocates nothing per frame once running: each
 * captured picture is written into one reusable I420 pixel buffer (and, on the rescale path, through one
 * reusable libswscale scratch buffer), and that pixel buffer is lent to the returned {@link VideoFrame} under
 * the {@link VideoOutput#takeVideo()} borrow contract.
 */
public sealed class CameraVideoOutput implements VideoOutput
        permits ScreenVideoOutput {
    /**
     * The logger for {@link CameraVideoOutput}.
     */
    private static final System.Logger LOGGER = Log.get(CameraVideoOutput.class);

    /**
     * Holds the advertised frame rate a native path capture source reports, since the device's blocking read
     * paces the stream to its own rate when no {@code framerate} is requested.
     */
    private static final int DEFAULT_FPS = 30;

    /**
     * Holds the advertised initial encoder bitrate in bits per second.
     *
     * @implNote This mirrors the encoder seed
     * {@link com.github.auties00.cobalt.calls.media.video.codec.VideoCodecParams#DEFAULT_INIT_TARGET_BITRATE},
     * WhatsApp's {@code vid_rc.max_init_bwe}; it is advertised only.
     */
    private static final int DEFAULT_BITRATE_BPS = 350_000;

    /**
     * Holds the libswscale destination stride alignment in bytes.
     *
     * @implNote This implementation pads each destination plane's stride up to this boundary so libswscale's
     * SIMD write paths, which round each row up to their vector width, never spill past a plane or the
     * destination buffer for a width that is not itself a multiple of the vector width; {@code 32} covers the
     * AVX2 routines the bundled build may select.
     */
    private static final int SWS_DST_ALIGN = 32;

    /**
     * Holds the advertised frame width in pixels: the resolution the device actually delivers when a geometry
     * is requested, or the native width capped to {@code 1280} on the longer side and rounded to even
     * otherwise.
     */
    private final int width;

    /**
     * Holds the advertised frame height in pixels: the resolution the device actually delivers when a
     * geometry is requested, or the native height capped and rounded to even otherwise.
     */
    private final int height;

    /**
     * Holds the advertised frame rate in frames per second: the requested rate, or the default when the
     * device runs in its native mode.
     */
    private final int fps;

    /**
     * Holds the advertised initial encoder bitrate in bits per second.
     */
    private final int bitrate;

    /**
     * Guards {@link #shutdown()} so the device is released at most once, and read by {@link #takeVideo()} to end
     * once the source is closed.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Holds the composed silence audio companion supplying the audio side of this source, since a camera
     * carries no audio; the inherited {@link AudioOutput} methods delegate to it.
     */
    private final AudioOutput audio = AudioOutput.fromSilence();

    /**
     * Holds the arena owning every native allocation this source makes, closed when the source shuts
     * down.
     */
    private final Arena arena;

    /**
     * Holds the libavformat input context pointer, which owns the capture device handle.
     */
    private final MemorySegment formatCtx;

    /**
     * Holds the libavcodec decoder context pointer.
     */
    private final MemorySegment codecCtx;

    /**
     * Holds the reusable demuxer packet pointer driven by the read loop.
     */
    private final MemorySegment packet;

    /**
     * Holds the reusable decoded frame pointer that the decoder writes into.
     */
    private final MemorySegment frame;

    /**
     * Holds the index of the video stream chosen from the device's container.
     */
    private final int streamIndex;

    /**
     * Holds the reusable tightly packed I420 pixel buffer every converted frame is written into and lent
     * out over.
     *
     * <p>The advertised geometry is fixed for the source's life, so every frame occupies exactly
     * {@link #width()} by {@link #height()} I420 bytes; this one buffer is filled afresh by each
     * {@link #takeVideo()} and lent to the returned {@link VideoFrame} rather than allocating per frame. The lend
     * is governed by the {@link VideoOutput#takeVideo()} borrow contract: the buffer is valid only until the next
     * {@link #takeVideo()}, so a consumer that retains a frame past that point copies the pixels out first.
     */
    private final byte[] pixelBuffer;

    /**
     * Holds the libswscale converter pointer, lazily built and rebuilt whenever the captured
     * {@code (width, height, source pixel format)} triple changes between frames; its destination is
     * fixed at the advertised {@link #width()} by {@link #height()} geometry. It stays {@code null} for a
     * requested geometry source whose device already emits I420, which never touches libswscale.
     */
    private MemorySegment swsCtx;

    /**
     * Holds the captured source width the current {@link #swsCtx} converter was built for.
     */
    private int swsW;

    /**
     * Holds the captured source height the current {@link #swsCtx} converter was built for.
     */
    private int swsH;

    /**
     * Holds the captured source pixel format the current {@link #swsCtx} converter was built for.
     */
    private int swsFmt;

    /**
     * Holds the reusable libswscale destination plane buffer, or {@code null} until the first scale.
     *
     * <p>Its geometry is the advertised {@link #width()} by {@link #height()} with each plane stride padded
     * to {@link #SWS_DST_ALIGN}, fixed for the source's life, so it is allocated once from {@link #arena} on
     * the first {@link #scaleToI420(int, int, int)} and reused by every later scale rather than opening a
     * confined arena per frame and allocating a full destination buffer each time. It stays {@code null} for
     * a requested geometry source whose device already emits I420, which never scales.
     */
    private MemorySegment scaleBuf;

    /**
     * Holds the reusable {@code uint8_t *dst[8]} destination plane pointer array handed to {@code sws_scale},
     * pointing into {@link #scaleBuf}; built once alongside it.
     */
    private MemorySegment scaleDstData;

    /**
     * Holds the reusable {@code int dstStride[8]} destination stride array handed to {@code sws_scale};
     * built once alongside {@link #scaleBuf}.
     */
    private MemorySegment scaleDstStride;

    /**
     * Holds the padded Y plane stride, in bytes, of {@link #scaleBuf}.
     */
    private int scaleYStride;

    /**
     * Holds the padded chroma plane stride, in bytes, of {@link #scaleBuf}.
     */
    private int scaleCStride;

    /**
     * Holds the byte offset of the U plane within {@link #scaleBuf}, equal to its padded Y plane size.
     */
    private long scaleYPlaneBytes;

    /**
     * Holds the byte size of each padded chroma plane within {@link #scaleBuf}.
     */
    private long scaleCPlaneBytes;

    /**
     * Holds the {@link System#nanoTime()} reading taken on the first converted frame, or
     * {@link Long#MIN_VALUE} until the first frame is converted.
     *
     * <p>Each frame's presentation timestamp is the elapsed time since this base, so the source clock
     * starts at zero on the first captured frame and only ever increases, independent of any wall clock
     * step.
     */
    private long ptsBaseNanos = Long.MIN_VALUE;

    /**
     * Opens the platform's default camera in its native mode and begins capturing.
     *
     * <p>Equivalent to {@link #CameraVideoOutput(String, String)} with the platform's default input format
     * and default device URL. The device runs at its own default resolution, advertised capped to
     * {@code 1280} on the longer side.
     *
     * @throws UnsupportedOperationException if the platform has no conventional default camera (such as
     *                                       Windows)
     * @throws IllegalStateException         if the device cannot be opened or has no video stream
     */
    public CameraVideoOutput() {
        this(defaultIndev(), defaultUrl());
    }

    /**
     * Opens a named camera in its native mode using the platform's default libavdevice input format and
     * begins capturing.
     *
     * <p>Uses {@code v4l2} on Linux, {@code avfoundation} on macOS, and {@code dshow} on Windows. On
     * Windows the argument is treated as a device friendly name and prefixed with {@code "video="} as
     * {@code dshow} requires. The device runs at its own default resolution, advertised capped to
     * {@code 1280} on the longer side.
     *
     * @param deviceUrl the device URL or name (for example {@code "/dev/video1"} on Linux, {@code "1"} on
     *                  macOS, or {@code "Integrated Camera"} on Windows)
     * @throws NullPointerException  if {@code deviceUrl} is {@code null}
     * @throws IllegalStateException if the device cannot be opened or has no video stream
     */
    public CameraVideoOutput(String deviceUrl) {
        this(defaultIndev(), normalizeUrlForPlatform(deviceUrl));
    }

    /**
     * Opens a named camera at a requested capture geometry and begins capturing.
     *
     * <p>Requests {@code width} by {@code height} at {@code fps} from the device through the platform's
     * default libavdevice input format, so the driver produces frames already at that resolution and no
     * rescale is needed per frame; the source advertises the resolution the device actually delivered, which
     * equals the request when the device supports it. A {@code fps} or {@code bitrate} that is not positive
     * selects the default. This is the constructor to use when the encode resolution is known, since it avoids the
     * cost of capturing native then rescaling that the native mode constructors incur.
     *
     * @param deviceUrl the device URL or name (for example {@code "/dev/video1"} on Linux, {@code "1"} on
     *                  macOS, or {@code "Integrated Camera"} on Windows)
     * @param width     the requested capture width in pixels; even and at least {@code 2}
     * @param height    the requested capture height in pixels; even and at least {@code 2}
     * @param fps       the requested capture frame rate, or a value that is not positive for the default
     * @param bitrate   the advertised initial encoder bitrate in bits per second, or a value that is not positive for the
     *                  default
     * @throws NullPointerException     if {@code deviceUrl} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or below {@code 2}
     * @throws IllegalStateException    if the device cannot be opened at the requested geometry or has no
     *                                  video stream
     */
    public CameraVideoOutput(String deviceUrl, int width, int height, int fps, int bitrate) {
        this(openDevice(defaultIndev(), normalizeUrlForPlatform(deviceUrl),
                        requireEven(width, "width"), requireEven(height, "height"), fps),
                fps > 0 ? fps : DEFAULT_FPS,
                bitrate > 0 ? bitrate : DEFAULT_BITRATE_BPS);
    }

    /**
     * Opens an explicit libavdevice input format and URL pair in its native mode and prepares the demux and
     * decode pipeline.
     *
     * <p>This is the constructor for cases where neither {@link #CameraVideoOutput()} nor
     * {@link #CameraVideoOutput(String)} selects the right device. The device runs at its own default
     * resolution, advertised capped to {@code 1280} on the longer side and rounded to even, at the default
     * frame rate and the default initial bitrate.
     *
     * @param indev the libavdevice input format name (for example {@code "v4l2"})
     * @param url   the device URL
     * @throws NullPointerException  if {@code indev} or {@code url} is {@code null}
     * @throws IllegalStateException if the named input format is unavailable in this build, the device
     *                               cannot be opened, or it has no video stream
     */
    public CameraVideoOutput(String indev, String url) {
        this(openDevice(indev, url, 0, 0, 0), DEFAULT_FPS, DEFAULT_BITRATE_BPS);
    }

    /**
     * Adopts the native handles of an already opened capture device and advertises its geometry.
     *
     * <p>The {@link #openDevice(String, String, int, int, int)} probe runs the whole open and decode
     * preparation sequence ahead of this constructor and reports the device's delivered geometry through the
     * {@link CapturedInput} it returns; this constructor adopts that geometry together with the probe's
     * demuxer, decoder, packet, and frame handles and its arena, and records the advertised frame rate and
     * bitrate. It also allocates the reusable {@link #pixelBuffer} sized to the advertised I420 geometry,
     * which every {@link #takeVideo()} refills and lends. The probe already releases its arena on any open
     * failure, so reaching this constructor means the pipeline is ready and nothing leaks here.
     *
     * @param in      the opened and probed capture device whose geometry and native handles this source
     *                adopts
     * @param fps     the advertised frame rate in frames per second
     * @param bitrate the advertised initial encoder bitrate in bits per second
     */
    private CameraVideoOutput(CapturedInput in, int fps, int bitrate) {
        this.width = in.width();
        this.height = in.height();
        this.fps = fps;
        this.bitrate = bitrate;
        this.arena = in.arena();
        this.formatCtx = in.formatCtx();
        this.codecCtx = in.codecCtx();
        this.packet = in.packet();
        this.frame = in.frame();
        this.streamIndex = in.streamIndex();
        this.pixelBuffer = new byte[in.width() * in.height()
                + 2 * ((in.width() / 2) * (in.height() / 2))];
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads packets from the device until one decodes into a frame, converts that frame to
     * {@link VideoPixelFormat#I420 I420}, and returns it. Packets belonging to other streams and decoder
     * "need more input" results are skipped transparently. Returns {@code null} when the device reports
     * an unrecoverable end of input or once {@link #shutdown()} has ended the source.
     *
     * <p>The returned frame's pixels are lent from the {@link #pixelBuffer} this source reuses across calls,
     * so a caller that retains the frame past the next {@link #takeVideo()} copies the pixels out first, per the
     * {@link VideoOutput#takeVideo()} borrow contract.
     *
     * @return {@inheritDoc}
     * @implNote This implementation does not sleep between frames: the device read blocks until a frame
     * is available, which paces a live camera to its native frame rate. It reuses one destination pixel
     * buffer, and on the rescale path one libswscale scratch buffer, for the source's life, so a steady
     * capture allocates nothing on the frame path.
     */
    @Override
    public VideoFrame takeVideo() {
        if (closed.get()) {
            return null;
        }
        while (true) {
            int read;
            try {
                read = Ffmpeg.av_read_frame(formatCtx, packet);
            } catch (RuntimeException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "camera device read raised, ending capture", e);
                return null;
            }
            if (read < 0) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "camera device read ended, code={0}", read);
                return null;
            }
            try {
                if (AVPacket.stream_index(packet) != streamIndex) {
                    continue;
                }
                var sent = Ffmpeg.avcodec_send_packet(codecCtx, packet);
                if (sent < 0 && !FFmpegError.isAgain(sent)) {
                    if (Log.ERROR) {
                        LOGGER.log(Level.ERROR, "camera decode send_packet failed: {0}",
                                FFmpegError.describe(sent));
                    }
                    throw new IllegalStateException("avcodec_send_packet failed: "
                            + FFmpegError.describe(sent));
                }
                var got = Ffmpeg.avcodec_receive_frame(codecCtx, frame);
                if (FFmpegError.isAgain(got)) {
                    continue;
                }
                if (got < 0) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "camera decoder receive_frame failed, code={0}", got);
                    }
                    return null;
                }
                try {
                    return convertCurrentFrame();
                } finally {
                    Ffmpeg.av_frame_unref(frame);
                }
            } finally {
                Ffmpeg.av_packet_unref(packet);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the source ended and frees the libswscale converter and every libav* allocation, then
     * closes the owning arena. Guards each pointer against {@code null} and a zero address, so the call
     * is idempotent.
     */
    @Override
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "shutting down camera capture");
        audio.shutdown();
        try (arena) {
            if (swsCtx != null && swsCtx.address() != 0L) {
                Ffmpeg.sws_freeContext(swsCtx);
            }
            if (frame != null && frame.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, frame);
                    Ffmpeg.av_frame_free(pp);
                }
            }
            if (packet != null && packet.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, packet);
                    Ffmpeg.av_packet_free(pp);
                }
            }
            if (codecCtx != null && codecCtx.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, codecCtx);
                    Ffmpeg.avcodec_free_context(pp);
                }
            }
            if (formatCtx != null && formatCtx.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, formatCtx);
                    Ffmpeg.avformat_close_input(pp);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A device backed source fills itself from the capture device inside {@link #takeVideo()} and ignores
     * application writes, so this does nothing.
     *
     * @param frame the frame that would be written; ignored
     */
    @Override
    public void writeVideo(VideoFrame frame) {
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
     * <p>Yields a silence frame from the composed audio companion, since a camera carries no audio.
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
        return bitrate;
    }

    /**
     * Returns the libavdevice input format name conventional for the running platform.
     *
     * <p>Resolves to {@code avfoundation} on macOS, {@code dshow} on Windows, and {@code v4l2}
     * elsewhere.
     *
     * @return the platform's conventional input format name
     */
    static String defaultIndev() {
        var os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "avfoundation";
        if (os.contains("win")) return "dshow";
        return "v4l2";
    }

    /**
     * Returns the device URL conventional for the running platform's default camera.
     *
     * <p>Resolves to {@code "0"} on macOS and {@code "/dev/video0"} on Linux. Windows has no stable
     * default device, so it is unsupported here.
     *
     * @return the platform's conventional default camera URL
     * @throws UnsupportedOperationException on Windows, where there is no stable default device
     */
    private static String defaultUrl() {
        var os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "0";
        if (os.contains("win")) {
            throw new UnsupportedOperationException(
                    "Windows has no default camera URL - pass a device "
                            + "name to CameraVideoOutput(String), e.g. \"Integrated Camera\"");
        }
        return "/dev/video0";
    }

    /**
     * Adjusts a caller supplied device URL into the form the platform's libavdevice input format
     * expects.
     *
     * <p>Wraps a Windows {@code dshow} name with {@code "video="} when it is not already prefixed, and
     * leaves the URL unchanged on other platforms.
     *
     * @param url the caller supplied device URL
     * @return the device URL corrected for the platform
     * @throws NullPointerException if {@code url} is {@code null}
     */
    private static String normalizeUrlForPlatform(String url) {
        Objects.requireNonNull(url, "url cannot be null");
        var os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win") && !url.startsWith("video=")) {
            return "video=" + url;
        }
        return url;
    }

    /**
     * Validates that a requested dimension is even and at least {@code 2}, returning it unchanged.
     *
     * @param value the dimension to validate
     * @param name  the dimension's name, used in the failure message
     * @return {@code value} unchanged
     * @throws IllegalArgumentException if {@code value} is odd or below {@code 2}
     */
    private static int requireEven(int value, String name) {
        if (value < 2 || (value & 1) != 0) {
            throw new IllegalArgumentException(name + " must be even and at least 2, got " + value);
        }
        return value;
    }

    /**
     * Returns the {@code AVInputFormat} matching the given name from libavdevice's registered video
     * devices.
     *
     * <p>Walks the registered input device list and returns the first whose name equals the argument, or
     * {@code null} when none matches.
     *
     * @param name the input format name to find
     * @return the matching input format pointer, or {@code null} if none is registered under that name
     */
    private static MemorySegment findInputDevice(String name) {
        var cursor = MemorySegment.NULL;
        while (true) {
            cursor = Ffmpeg.av_input_video_device_next(cursor);
            if (cursor == null || cursor.address() == 0L) {
                return null;
            }
            var struct = cursor.reinterpret(AVInputFormat.layout().byteSize());
            var namePtr = AVInputFormat.name(struct);
            if (namePtr == null || namePtr.address() == 0L) {
                continue;
            }
            var n = namePtr.reinterpret(Long.MAX_VALUE).getString(0L);
            if (n.equals(name)) {
                return cursor;
            }
        }
    }

    /**
     * Converts the current decoded frame to {@link VideoPixelFormat#I420 I420} at the advertised geometry
     * and wraps it in a {@link VideoFrame}.
     *
     * <p>Rejects captured frames whose dimensions are below {@code 2} or odd, since I420's half resolution
     * chroma planes require even dimensions. When the device already emits I420 at the advertised geometry
     * the three planes are copied straight through with no conversion; otherwise the frame is handed to
     * libswscale, which converts the pixel format and, for a native mode source whose advertised geometry
     * was capped below the captured resolution, rescales in the same pass. Every frame is stamped with a
     * monotonic zero based presentation timestamp.
     *
     * @return the converted frame at the advertised geometry
     * @throws IllegalStateException if the captured frame has unsupported dimensions, the converter
     *                               cannot be built, or the scale fails
     * @implNote This implementation prefers the zero copy I420 fast path so a requested geometry camera that
     * natively delivers I420 never touches libswscale, and reserves the swscale pass for pixel format
     * conversion and the native mode capped rescale. The presentation timestamp is the microseconds elapsed
     * since {@link System#nanoTime()} on the first frame rather than absolute wall clock time, so it is
     * zero based per source and never decreases across a manual clock change or NTP step, matching the
     * monotonic per stream capture clock WhatsApp derives RTP timestamps from.
     */
    private VideoFrame convertCurrentFrame() {
        var w = AVFrame.width(frame);
        var h = AVFrame.height(frame);
        var srcFmt = AVFrame.format(frame);
        if (w < 2 || h < 2 || (w & 1) != 0 || (h & 1) != 0) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "captured frame has unsupported dimensions {0}x{1}", w, h);
            }
            throw new IllegalStateException(
                    "captured frame has unsupported dimensions " + w + "x" + h);
        }
        var pixels = srcFmt == Ffmpeg.AV_PIX_FMT_YUV420P() && w == width && h == height
                ? copyPlanarI420(w, h)
                : scaleToI420(w, h, srcFmt);
        var now = System.nanoTime();
        if (ptsBaseNanos == Long.MIN_VALUE) {
            ptsBaseNanos = now;
        }
        var ptsMicros = (now - ptsBaseNanos) / 1_000L;
        return new VideoFrame(pixels, VideoPixelFormat.I420, width(), height(), ptsMicros);
    }

    /**
     * Copies the current I420 frame's three planes straight into the reusable {@link #pixelBuffer}.
     *
     * <p>Used only when the device already delivers I420 at the advertised geometry, so no pixel format
     * conversion or rescale is needed. Each plane is copied to strip the decoder's linesize padding, since
     * {@link VideoFrame} carries planes with no padding per row; a plane whose stride already equals its
     * width is copied in one bulk transfer, otherwise row by row.
     *
     * @param w the frame width in pixels, equal to {@link #width()}
     * @param h the frame height in pixels, equal to {@link #height()}
     * @return the reused buffer holding the tightly packed I420 pixels
     */
    private byte[] copyPlanarI420(int w, int h) {
        var ySize = w * h;
        var uvSize = (w / 2) * (h / 2);
        var pixels = pixelBuffer;
        var data = AVFrame.data(frame);
        var linesize = AVFrame.linesize(frame);
        copyPlaneRows(planePointer(data, linesize, 0, h), 0L,
                linesize.getAtIndex(ValueLayout.JAVA_INT, 0L), w, h, pixels, 0);
        copyPlaneRows(planePointer(data, linesize, 1, h / 2), 0L,
                linesize.getAtIndex(ValueLayout.JAVA_INT, 1L), w / 2, h / 2, pixels, ySize);
        copyPlaneRows(planePointer(data, linesize, 2, h / 2), 0L,
                linesize.getAtIndex(ValueLayout.JAVA_INT, 2L), w / 2, h / 2, pixels, ySize + uvSize);
        return pixels;
    }

    /**
     * Returns the decoded frame's plane pointer at the given index, reinterpreted to span the plane's rows.
     *
     * @param data     the frame's {@code data[]} pointer array
     * @param linesize the frame's {@code linesize[]} array
     * @param plane    the plane index
     * @param planeH   the plane's height in rows
     * @return the plane pointer sized to hold {@code planeH} rows of its stride
     */
    private static MemorySegment planePointer(MemorySegment data, MemorySegment linesize, int plane,
                                              int planeH) {
        var stride = linesize.getAtIndex(ValueLayout.JAVA_INT, plane);
        return data.getAtIndex(ValueLayout.ADDRESS, plane).reinterpret((long) stride * planeH);
    }

    /**
     * Converts the current decoded frame to a tightly packed I420 buffer at the advertised geometry with
     * libswscale.
     *
     * <p>Builds the converter to scale from the captured {@code (width, height, source pixel format)} triple
     * to the advertised {@link #width()} by {@link #height()} geometry, rebuilding it whenever the captured
     * triple changes. The libswscale destination is the reusable {@link #scaleBuf} scratch (see
     * {@link #ensureScaleScratch(int, int)}) allocated once with each plane stride padded to
     * {@link #SWS_DST_ALIGN} so the SIMD write paths cannot spill past a plane; the scaled planes are then
     * copied into the reusable {@link #pixelBuffer}, stripping the padding.
     *
     * @param w      the captured frame width in pixels
     * @param h      the captured frame height in pixels
     * @param srcFmt the captured frame's libav pixel format
     * @return the reused buffer holding the tightly packed I420 pixels at the advertised geometry
     * @throws IllegalStateException if the converter cannot be built or the scale fails
     */
    private byte[] scaleToI420(int w, int h, int srcFmt) {
        var dstW = width();
        var dstH = height();
        if (swsCtx == null || swsCtx.address() == 0L
                || w != swsW || h != swsH || srcFmt != swsFmt) {
            if (swsCtx != null && swsCtx.address() != 0L) {
                Ffmpeg.sws_freeContext(swsCtx);
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "(re)building sws scaler {0}x{1} -> {2}x{3}", w, h, dstW, dstH);
            }
            swsCtx = FFmpegError.requireNonNull("sws_getContext",
                    Ffmpeg.sws_getContext(w, h, srcFmt, dstW, dstH, Ffmpeg.AV_PIX_FMT_YUV420P(),
                            Ffmpeg.SWS_BILINEAR(),
                            MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL));
            swsW = w;
            swsH = h;
            swsFmt = srcFmt;
        }
        ensureScaleScratch(dstW, dstH);

        var produced = Ffmpeg.sws_scale(swsCtx,
                AVFrame.data(frame), AVFrame.linesize(frame),
                0, h, scaleDstData, scaleDstStride);
        if (produced < 0) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sws_scale failed, code={0}", produced);
            throw new IllegalStateException("sws_scale failed: " + produced);
        }

        var ySize = dstW * dstH;
        var uvSize = (dstW / 2) * (dstH / 2);
        var pixels = pixelBuffer;
        copyPlaneRows(scaleBuf, 0L, scaleYStride, dstW, dstH, pixels, 0);
        copyPlaneRows(scaleBuf, scaleYPlaneBytes, scaleCStride, dstW / 2, dstH / 2, pixels, ySize);
        copyPlaneRows(scaleBuf, scaleYPlaneBytes + scaleCPlaneBytes, scaleCStride,
                dstW / 2, dstH / 2, pixels, ySize + uvSize);
        return pixels;
    }

    /**
     * Allocates the reusable libswscale destination scratch once, on the first scale.
     *
     * <p>The advertised destination geometry is fixed for the source's life, so {@link #scaleBuf} and its
     * companion {@link #scaleDstData} plane pointer and {@link #scaleDstStride} stride arrays are built once
     * from {@link #arena} and reused by every later scale, rather than opening a confined arena per frame and
     * allocating a full destination buffer each time. Each destination plane's stride is padded to
     * {@link #SWS_DST_ALIGN} so libswscale's SIMD write paths cannot spill past a plane. A later call does
     * nothing once the scratch exists.
     *
     * @param dstW the advertised destination width in pixels
     * @param dstH the advertised destination height in pixels
     */
    private void ensureScaleScratch(int dstW, int dstH) {
        if (scaleBuf != null) {
            return;
        }
        var yStride = alignUp(dstW, SWS_DST_ALIGN);
        var cStride = alignUp(dstW / 2, SWS_DST_ALIGN);
        var yPlane = (long) yStride * dstH;
        var cPlane = (long) cStride * (dstH / 2);
        var buf = arena.allocate(yPlane + 2L * cPlane + SWS_DST_ALIGN);
        var dstData = arena.allocate(8L * ValueLayout.ADDRESS.byteSize());
        var dstStride = arena.allocate(8L * Integer.BYTES);
        dstData.setAtIndex(ValueLayout.ADDRESS, 0L, buf);
        dstData.setAtIndex(ValueLayout.ADDRESS, 1L, buf.asSlice(yPlane));
        dstData.setAtIndex(ValueLayout.ADDRESS, 2L, buf.asSlice(yPlane + cPlane));
        dstStride.setAtIndex(ValueLayout.JAVA_INT, 0L, yStride);
        dstStride.setAtIndex(ValueLayout.JAVA_INT, 1L, cStride);
        dstStride.setAtIndex(ValueLayout.JAVA_INT, 2L, cStride);
        this.scaleBuf = buf;
        this.scaleDstData = dstData;
        this.scaleDstStride = dstStride;
        this.scaleYStride = yStride;
        this.scaleCStride = cStride;
        this.scaleYPlaneBytes = yPlane;
        this.scaleCPlaneBytes = cPlane;
    }

    /**
     * Copies one plane from a strided native buffer into a tightly packed region of a byte array.
     *
     * <p>A plane whose row stride already equals its width is contiguous and copied in a single bulk
     * transfer; otherwise each row is copied separately to strip the stride padding, since {@link VideoFrame}
     * carries planes with no padding per row.
     *
     * @param src       the native buffer holding the plane
     * @param srcOffset the byte offset of the plane's first row within {@code src}
     * @param srcStride the plane's row stride in bytes
     * @param planeW    the plane's width in bytes per row
     * @param planeH    the plane's height in rows
     * @param dst       the destination byte array
     * @param dstOffset the byte offset of the plane within {@code dst}
     */
    private static void copyPlaneRows(MemorySegment src, long srcOffset, int srcStride,
                                      int planeW, int planeH, byte[] dst, int dstOffset) {
        if (srcStride == planeW) {
            MemorySegment.copy(src, ValueLayout.JAVA_BYTE, srcOffset, dst, dstOffset, planeW * planeH);
            return;
        }
        for (var row = 0; row < planeH; row++) {
            MemorySegment.copy(src, ValueLayout.JAVA_BYTE, srcOffset + (long) row * srcStride,
                    dst, dstOffset + row * planeW, planeW);
        }
    }

    /**
     * Rounds a value up to the next multiple of the given power of two alignment.
     *
     * @param value the value to round
     * @param align the alignment, a power of two
     * @return the smallest multiple of {@code align} not below {@code value}
     */
    private static int alignUp(int value, int align) {
        return (value + align - 1) & ~(align - 1);
    }

    /**
     * Returns the index of the first video stream in the device's container, or {@code -1} when none
     * exists.
     *
     * @param formatCtx the device input context
     * @return the video stream index, or {@code -1} if the container has no video stream
     */
    private static int pickVideoStream(MemorySegment formatCtx) {
        var n = AVFormatContext.nb_streams(formatCtx);
        var streamsArr = AVFormatContext.streams(formatCtx)
                .reinterpret((long) n * ValueLayout.ADDRESS.byteSize());
        for (var i = 0; i < n; i++) {
            var stream = streamsArr.getAtIndex(ValueLayout.ADDRESS, i)
                    .reinterpret(AVStream.layout().byteSize());
            var params = AVStream.codecpar(stream);
            if (AVCodecParameters.codec_type(params) == Ffmpeg.AVMEDIA_TYPE_VIDEO()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the {@code AVStream} pointer at the given index in the device's container.
     *
     * @param formatCtx the device input context
     * @param index     the stream index
     * @return the stream pointer at that index
     */
    private static MemorySegment streamPointer(MemorySegment formatCtx, int index) {
        var n = AVFormatContext.nb_streams(formatCtx);
        var streamsArr = AVFormatContext.streams(formatCtx)
                .reinterpret((long) n * ValueLayout.ADDRESS.byteSize());
        return streamsArr.getAtIndex(ValueLayout.ADDRESS, index)
                .reinterpret(AVStream.layout().byteSize());
    }

    /**
     * Opens a libavdevice capture device, prepares its decoder, and reports its geometry and native handles.
     *
     * <p>Ensures the FFmpeg libraries are loaded, registers libavdevice, resolves the named input format,
     * and opens the device. When {@code reqWidth} and {@code reqHeight} are positive they are requested from
     * the device as a {@code video_size} capture mode, and a positive {@code reqFps} as a {@code framerate},
     * so the driver produces frames at that resolution; otherwise the device runs in its native mode. The
     * method then probes the container, picks its first video stream, reads that stream's delivered pixel
     * geometry, opens a decoder for its codec, and allocates the reusable packet and frame. The reported
     * geometry is the delivered geometry rounded to even for a requested mode, so no rescale is needed, or
     * the delivered geometry passed through {@link #capGeometry(int, int)} for a native mode, so a device
     * with a high resolution is bounded. If any step fails the arena is closed before the exception
     * propagates, so a failed probe leaks no native resource.
     *
     * <p>This probe runs inside the {@code this(...)} argument of the public constructors, so its result
     * feeds the {@link #CameraVideoOutput(CapturedInput, int, int)} constructor without a flexible
     * constructor body: the device must be fully opened before {@code super} runs because the advertised
     * geometry is only known after the stream is probed.
     *
     * @param indev     the libavdevice input format name (for example {@code "v4l2"})
     * @param url       the device URL
     * @param reqWidth  the requested capture width, or {@code 0} for the device's native mode
     * @param reqHeight the requested capture height, or {@code 0} for the device's native mode
     * @param reqFps    the requested capture frame rate, or {@code 0} to leave the device's own rate
     * @return the opened device's geometry and native handles
     * @throws NullPointerException  if {@code indev} or {@code url} is {@code null}
     * @throws IllegalStateException if the named input format is unavailable in this build, the device
     *                               cannot be opened, or it has no video stream
     */
    private static CapturedInput openDevice(String indev, String url,
                                            int reqWidth, int reqHeight, int reqFps) {
        Objects.requireNonNull(indev, "indev cannot be null");
        Objects.requireNonNull(url, "url cannot be null");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "opening camera device {0} ({1}), requested {2}x{3}@{4}fps",
                    indev, url, reqWidth, reqHeight, reqFps);
        }
        FFmpegLoader.ensureLoaded();
        Ffmpeg.avdevice_register_all();
        var arena = Arena.ofShared();
        try {
            var ifmt = findInputDevice(indev);
            if (ifmt == null || ifmt.address() == 0L) {
                throw new IllegalStateException("libavdevice has no '" + indev
                        + "' input format on this build");
            }

            var formatPtr = arena.allocate(ValueLayout.ADDRESS);
            var urlSeg = arena.allocateFrom(url);
            var options = arena.allocate(ValueLayout.ADDRESS);
            options.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            var requested = reqWidth > 0 && reqHeight > 0;
            try (var opts = Arena.ofConfined()) {
                if (requested) {
                    dictSet(opts, options, "video_size", reqWidth + "x" + reqHeight);
                }
                if (reqFps > 0) {
                    dictSet(opts, options, "framerate", Integer.toString(reqFps));
                }
            }
            FFmpegError.check("avformat_open_input(" + indev + ":" + url + ")",
                    Ffmpeg.avformat_open_input(formatPtr, urlSeg, ifmt, options));
            Ffmpeg.av_dict_free(options);
            var formatCtx = formatPtr.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFormatContext.layout().byteSize());
            FFmpegError.check("avformat_find_stream_info",
                    Ffmpeg.avformat_find_stream_info(formatCtx, MemorySegment.NULL));

            var streamIndex = pickVideoStream(formatCtx);
            if (streamIndex < 0) {
                throw new IllegalStateException("device has no video stream");
            }
            var stream = streamPointer(formatCtx, streamIndex);
            var params = AVStream.codecpar(stream);
            var nativeWidth = AVCodecParameters.width(params);
            var nativeHeight = AVCodecParameters.height(params);
            var codecId = AVCodecParameters.codec_id(params);
            var codec = FFmpegError.requireNonNull(
                    "avcodec_find_decoder(" + codecId + ")",
                    Ffmpeg.avcodec_find_decoder(codecId));
            var codecCtx = FFmpegError.requireNonNull(
                    "avcodec_alloc_context3",
                    Ffmpeg.avcodec_alloc_context3(codec));
            FFmpegError.check("avcodec_parameters_to_context",
                    Ffmpeg.avcodec_parameters_to_context(codecCtx, params));
            FFmpegError.check("avcodec_open2",
                    Ffmpeg.avcodec_open2(codecCtx, codec, MemorySegment.NULL));

            var packet = FFmpegError.requireNonNull("av_packet_alloc", Ffmpeg.av_packet_alloc());
            var frame = FFmpegError.requireNonNull("av_frame_alloc", Ffmpeg.av_frame_alloc());
            var advertised = requested
                    ? new int[]{evenDown(nativeWidth), evenDown(nativeHeight)}
                    : capGeometry(nativeWidth, nativeHeight);
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "camera device opened, advertised {0}x{1}",
                        advertised[0], advertised[1]);
            }
            return new CapturedInput(advertised[0], advertised[1], arena, formatCtx, codecCtx, packet,
                    frame, streamIndex);
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to open camera device " + indev + ":" + url, e);
            arena.close();
            throw e;
        }
    }

    /**
     * Sets one libavdevice open option on the given dictionary.
     *
     * <p>libavformat copies the key and value strings, so they may be allocated in a confined arena the
     * caller closes as soon as every option is set.
     *
     * @param strings  the arena the transient key and value strings are allocated in
     * @param options  the {@code AVDictionary **} the option is added to
     * @param key      the option name (for example {@code "video_size"})
     * @param value    the option value (for example {@code "1280x720"})
     * @throws IllegalStateException if libavutil rejects the option
     */
    private static void dictSet(Arena strings, MemorySegment options, String key, String value) {
        FFmpegError.check("av_dict_set(" + key + ")",
                Ffmpeg.av_dict_set(options, strings.allocateFrom(key), strings.allocateFrom(value), 0));
    }

    /**
     * Caps a native pixel geometry to the engine's maximum encoded resolution, preserving aspect ratio.
     *
     * <p>Returns the geometry rounded down to even when neither dimension exceeds {@code 1280}; otherwise
     * scales the longer dimension down to {@code 1280} and the shorter dimension proportionally, then rounds
     * both down to the nearest even value of at least {@code 2}. H264 requires even dimensions, and capping
     * the longer side bounds the encode cost of a capture with a high resolution while keeping its aspect
     * ratio, so a 16:9 camera is advertised as 16:9 rather than squished to a fixed 4:3 default.
     *
     * @param width  the native pixel width
     * @param height the native pixel height
     * @return a two element array of the capped, even {@code [width, height]}
     */
    private static int[] capGeometry(int width, int height) {
        if (Math.max(width, height) <= 1280) {
            return new int[]{evenDown(width), evenDown(height)};
        }
        int cappedWidth;
        int cappedHeight;
        if (width >= height) {
            cappedWidth = 1280;
            cappedHeight = (int) Math.round((double) height * 1280 / width);
        } else {
            cappedHeight = 1280;
            cappedWidth = (int) Math.round((double) width * 1280 / height);
        }
        return new int[]{evenDown(cappedWidth), evenDown(cappedHeight)};
    }

    /**
     * Rounds a dimension down to the nearest even value of at least {@code 2}.
     *
     * @param value the dimension to round
     * @return the largest even value not exceeding {@code value}, or {@code 2} when that would be below
     *         {@code 2}
     */
    private static int evenDown(int value) {
        return Math.max(2, value & ~1);
    }

    /**
     * Carries the geometry and native handles of a capture device opened by
     * {@link #openDevice(String, String, int, int, int)}.
     *
     * <p>Lets the probe run the full open and probe sequence before the
     * {@link CameraVideoOutput#CameraVideoOutput(CapturedInput, int, int)} constructor runs, so the constructor can adopt the
     * advertised geometry and the native handles without a flexible constructor body.
     *
     * @param width       the advertised frame width in pixels, even
     * @param height      the advertised frame height in pixels, even
     * @param arena       the arena owning every native allocation the open made
     * @param formatCtx   the libavformat input context pointer
     * @param codecCtx    the libavcodec decoder context pointer
     * @param packet      the reusable demuxer packet pointer
     * @param frame       the reusable decoded frame pointer
     * @param streamIndex the index of the chosen video stream
     */
    private record CapturedInput(int width, int height, Arena arena, MemorySegment formatCtx,
                                 MemorySegment codecCtx, MemorySegment packet, MemorySegment frame,
                                 int streamIndex) {
    }
}
