package com.github.auties00.cobalt.calls.stream.ffmpeg;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVIOContext;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegError;
import com.github.auties00.cobalt.util.ffmpeg.Ffmpeg;
import com.github.auties00.cobalt.util.ffmpeg.avio_alloc_context$read_packet;
import com.github.auties00.cobalt.util.ffmpeg.avio_alloc_context$seek;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Opens a media input for the demuxed media sources of a call and, for {@code http}/{@code https}, streams
 * it through a custom {@code AVIOContext} so FFmpeg pulls its bytes from a JDK {@link HttpURLConnection}
 * instead of opening a socket.
 *
 * <p>The static {@link #openFile(Arena, Path)} and {@link #openUri(Arena, FfmpegIoWatchdog, URI, Duration)}
 * entry points are the demuxer openers that the sources backed by a file or a URI call: a local file and a
 * {@code file} URI open through libavformat directly, while an {@code http}/{@code https} URI is served by
 * an instance of this type acting as the byte source.
 */
public final class FfmpegHttpAvio implements FfmpegIoWatchdog.Io {
    /**
     * The logger for {@link FfmpegHttpAvio}.
     */
    private static final System.Logger LOGGER = Log.get(FfmpegHttpAvio.class);

    /**
     * The read buffer size in bytes, used for both the native buffer FFmpeg reads from and the array each
     * upcall reads into; larger than the 8 KiB default to cut the upcall count on a fast stream.
     */
    private static final int READ_BUFFER_SIZE = 1 << 16;

    /**
     * The maximum number of redirect hops followed before an open gives up, bounding a redirect loop.
     */
    private static final int MAX_REDIRECTS = 8;

    /**
     * The user agent sent to identify the client to a server.
     */
    private static final String USER_AGENT = "Cobalt";

    /**
     * The {@code AVFMT_FLAG_CUSTOM_IO} flag, set on a demuxer context so {@code avformat_close_input} leaves
     * the {@code AVIOContext} this class owns for it to free.
     *
     * @implNote This implementation hardcodes the value from {@code libavformat/avformat.h} because the
     * binding does not expose it as a constant.
     */
    private static final int AVFMT_FLAG_CUSTOM_IO = 0x0080;

    /**
     * The {@code AVSEEK_SIZE} whence value, a query for the total resource length rather than a reposition.
     *
     * @implNote This implementation hardcodes the value from {@code libavutil/avio.h} because the binding
     * does not expose it as a constant.
     */
    private static final int AVSEEK_SIZE = 0x10000;

    /**
     * The {@code AVSEEK_FORCE} whence modifier, masked off before the base {@code whence} is interpreted.
     *
     * @implNote This implementation hardcodes the value from {@code libavutil/avio.h} because the binding
     * does not expose it as a constant.
     */
    private static final int AVSEEK_FORCE = 0x20000;

    /**
     * The {@code whence} value requesting an absolute position.
     */
    private static final int SEEK_SET = 0;

    /**
     * The {@code whence} value requesting a position relative to the current one.
     */
    private static final int SEEK_CUR = 1;

    /**
     * The {@code whence} value requesting a position relative to the end.
     */
    private static final int SEEK_END = 2;

    /**
     * Holds the URI schemes an embedder may open, rejecting every other so a URI supplied by an application
     * cannot reach an unintended protocol.
     *
     * @implNote This implementation allows the local file scheme and the two HTTP schemes this type serves;
     * streaming session protocols such as RTSP, RTMP, and SRT are excluded because they are not byte streams
     * the connection can back and the native build carries no protocol for them.
     */
    private static final Set<String> PERMITTED_SCHEMES = Set.of("file", "http", "https");

    /**
     * Opens and probes a local media file, returning its demuxer context.
     *
     * <p>Opens the file directly through libavformat's file protocol with no watchdog, since a local read
     * does not stall on the network.
     *
     * @param arena the source's lifetime arena that owns the native allocations the open makes
     * @param path  the media file to open
     * @return the opened and probed {@code AVFormatContext} pointer
     * @throws NullPointerException  if {@code path} is {@code null}
     * @throws IllegalStateException if the file cannot be opened or probed
     */
    public static MemorySegment openFile(Arena arena, Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        return nativeOpen(arena, null, path.toAbsolutePath().toString(), null, path.toString());
    }

    /**
     * Opens and probes the given URI under the watchdog, returning its demuxer context.
     *
     * <p>Validates the scheme and timeout, then dispatches on the scheme: a {@code file} URI opens through
     * libavformat directly under the watchdog, and an {@code http}/{@code https} URI streams through a
     * custom {@code AVIOContext} this type hosts. When the watchdog aborts a connect or probe the failure
     * message names the timeout.
     *
     * @param arena     the source's lifetime arena that owns the native allocations the open makes
     * @param watchdog  the timeout watchdog installed on the context and armed around each blocking call
     * @param uri       the media stream to open
     * @param ioTimeout the maximum time the connect, probe, or a later read may block; must be positive
     * @return the opened and probed {@code AVFormatContext} pointer
     * @throws NullPointerException     if {@code uri} or {@code ioTimeout} is {@code null}
     * @throws IllegalArgumentException if {@code ioTimeout} is not positive, or the scheme is not permitted
     * @throws IllegalStateException    if the stream cannot be opened or probed
     */
    public static MemorySegment openUri(Arena arena, FfmpegIoWatchdog watchdog, URI uri, Duration ioTimeout) {
        var scheme = requirePermittedScheme(uri);
        Objects.requireNonNull(ioTimeout, "ioTimeout cannot be null");
        if (ioTimeout.isZero() || ioTimeout.isNegative()) {
            throw new IllegalArgumentException("ioTimeout must be positive, got " + ioTimeout);
        }
        return scheme.equals("file")
                ? nativeOpen(arena, watchdog, uri.toString(), ioTimeout, uri.toString())
                : openBridged(arena, watchdog, uri, ioTimeout);
    }

    /**
     * Opens and probes a URL through libavformat's own protocol, optionally under a watchdog.
     *
     * @param arena     the source's lifetime arena that owns the native allocations
     * @param watchdog  the timeout watchdog installed and armed, or {@code null} for an input that does not block
     * @param url       the URL passed to {@code avformat_open_input}
     * @param ioTimeout the timeout armed around the blocking calls, or {@code null} when {@code watchdog} is
     * @param label     the input named in a failure message
     * @return the opened and probed {@code AVFormatContext} pointer
     * @throws IllegalStateException if the input cannot be opened or probed
     */
    private static MemorySegment nativeOpen(Arena arena, FfmpegIoWatchdog watchdog, String url,
                                            Duration ioTimeout, String label) {
        var formatCtx = FFmpegError.requireNonNull("avformat_alloc_context", Ffmpeg.avformat_alloc_context());
        if (watchdog != null) {
            watchdog.installOn(formatCtx);
        }
        var formatPtr = arena.allocate(ValueLayout.ADDRESS);
        formatPtr.set(ValueLayout.ADDRESS, 0L, formatCtx);
        var urlSeg = arena.allocateFrom(url);
        if (watchdog != null) {
            watchdog.arm(ioTimeout);
        }
        var opened = Ffmpeg.avformat_open_input(formatPtr, urlSeg, MemorySegment.NULL, MemorySegment.NULL);
        if (opened < 0) {
            var description = describeFailure(watchdog, opened, ioTimeout);
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "avformat_open_input({0}) failed: {1}", label, description);
            }
            throw new IllegalStateException("avformat_open_input(" + label + ") failed: " + description);
        }
        var result = probe(watchdog, formatPtr, label, ioTimeout);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "native media input opened: {0}", label);
        return result;
    }

    /**
     * Opens an {@code http}/{@code https} URI through a custom {@code AVIOContext} this type hosts.
     *
     * <p>Builds the bridge, which connects the stream, attaches it to the watchdog so a teardown can abort a
     * parked read and release the native context, then opens the demuxer against the bridge with a null URL
     * so libavformat reads only through it. Frees the bridge and any open context if the open or probe
     * fails.
     *
     * @param arena     the source's lifetime arena that owns the upcall stubs
     * @param watchdog  the timeout watchdog installed, armed, and given the bridge to abort and release
     * @param uri       the {@code http}/{@code https} URI to stream
     * @param ioTimeout the maximum time the connect, probe, or a later read may block; must be positive
     * @return the opened and probed {@code AVFormatContext} pointer
     * @throws IllegalStateException if the stream cannot be opened or probed
     */
    private static MemorySegment openBridged(Arena arena, FfmpegIoWatchdog watchdog, URI uri, Duration ioTimeout) {
        var bridge = new FfmpegHttpAvio(arena, uri, ioTimeout, watchdog);
        watchdog.attach(bridge);
        var formatCtx = FFmpegError.requireNonNull("avformat_alloc_context", Ffmpeg.avformat_alloc_context());
        watchdog.installOn(formatCtx);
        bridge.install(formatCtx);
        var formatPtr = arena.allocate(ValueLayout.ADDRESS);
        formatPtr.set(ValueLayout.ADDRESS, 0L, formatCtx);
        var opened = false;
        try {
            watchdog.arm(ioTimeout);
            var code = Ffmpeg.avformat_open_input(formatPtr, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
            if (code < 0) {
                throw new IllegalStateException("avformat_open_input(" + uri + ") failed: "
                        + describeFailure(watchdog, code, ioTimeout));
            }
            opened = true;
            var result = probe(watchdog, formatPtr, uri.toString(), ioTimeout);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "http media bridge opened: host={0}", uri.getHost());
            return result;
        } catch (RuntimeException e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "http media bridge open failed for host " + uri.getHost(), e);
            }
            if (opened) {
                Ffmpeg.avformat_close_input(formatPtr);
            }
            bridge.close();
            throw e;
        }
    }

    /**
     * Probes the opened demuxer for its stream layout, optionally under the watchdog.
     *
     * @param watchdog  the timeout watchdog armed around the probe, or {@code null} for an input that does not block
     * @param formatPtr the double pointer holding the opened context
     * @param label     the input named in a failure message
     * @param ioTimeout the maximum time the probe may block, or {@code null} when {@code watchdog} is
     * @return the probed {@code AVFormatContext} pointer, reinterpreted to its layout size
     * @throws IllegalStateException if the probe fails
     */
    private static MemorySegment probe(FfmpegIoWatchdog watchdog, MemorySegment formatPtr, String label,
                                       Duration ioTimeout) {
        var openedCtx = formatPtr.get(ValueLayout.ADDRESS, 0L)
                .reinterpret(AVFormatContext.layout().byteSize());
        if (watchdog != null) {
            watchdog.arm(ioTimeout);
        }
        var probed = Ffmpeg.avformat_find_stream_info(openedCtx, MemorySegment.NULL);
        if (probed < 0) {
            var description = describeFailure(watchdog, probed, ioTimeout);
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "avformat_find_stream_info({0}) failed: {1}", label, description);
            }
            throw new IllegalStateException("avformat_find_stream_info(" + label + ") failed: " + description);
        }
        if (watchdog != null) {
            watchdog.disarm();
        }
        return openedCtx;
    }

    /**
     * Describes a failed open or probe, naming a watchdog timeout where one occurred.
     *
     * @param watchdog  the watchdog whose {@linkplain FfmpegIoWatchdog#fired() fired} state distinguishes a
     *                  timeout from a protocol error, or {@code null} for an input that does not block
     * @param code      the negative libavformat return code
     * @param ioTimeout the configured timeout, named when the watchdog fired
     * @return a human readable failure description
     */
    private static String describeFailure(FfmpegIoWatchdog watchdog, int code, Duration ioTimeout) {
        return watchdog != null && watchdog.fired() ? "timed out after " + ioTimeout : FFmpegError.describe(code);
    }

    /**
     * Validates that the URI carries a scheme on the {@link #PERMITTED_SCHEMES} allowlist.
     *
     * @param uri the URI to validate
     * @return the URI's lowercased scheme
     * @throws NullPointerException     if {@code uri} is {@code null}
     * @throws IllegalArgumentException if the URI has no scheme or its scheme is not permitted
     */
    private static String requirePermittedScheme(URI uri) {
        Objects.requireNonNull(uri, "uri cannot be null");
        var scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("uri has no scheme: " + uri);
        }
        var normalized = scheme.toLowerCase(Locale.ROOT);
        if (!PERMITTED_SCHEMES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "uri scheme '" + scheme + "' is not permitted; allowed schemes are " + PERMITTED_SCHEMES);
        }
        return normalized;
    }

    /**
     * The resource being streamed, reused as the base for the initial request and every range reopen so
     * redirects are evaluated fresh each time.
     */
    private final URI uri;

    /**
     * The watchdog whose cancel state the read upcall checks and whose timeout flag it sets on a read stall.
     */
    private final FfmpegIoWatchdog watchdog;

    /**
     * The connect and read timeout in milliseconds, derived from the caller's timeout.
     */
    private final int timeoutMillis;

    /**
     * The array each read fills before its bytes are copied into the native buffer.
     */
    private final byte[] readBuffer;

    /**
     * A reusable heap segment over {@link #readBuffer}, so each copy into native memory needs no wrapper
     * per read.
     */
    private final MemorySegment readSegment;

    /**
     * The native {@code AVIOContext} handed to the demuxer, reinterpreted to its layout so its buffer field
     * can be read at teardown.
     */
    private final MemorySegment avioContext;

    /**
     * The current connection, replaced on every range reopen (demux thread) and disconnected on teardown
     * (another thread), so it is {@code volatile} for visibility across those threads.
     */
    private volatile HttpURLConnection connection;

    /**
     * The current connection's body stream, read by each upcall on the demux thread and closed on teardown
     * from another thread, so it is {@code volatile} for visibility across those threads.
     */
    private volatile InputStream stream;

    /**
     * The resource's total length in bytes, or {@code -1} when the server does not report it.
     */
    private long totalLength = -1;

    /**
     * Whether the server advertised byte range support, gating whether {@link #seek} can reposition.
     */
    private boolean seekable;

    /**
     * The absolute byte position of the next read, advanced by each read and reset by each seek.
     */
    private long position;

    /**
     * Whether the bridge has been aborted or closed, after which a read ends at once.
     */
    private volatile boolean closed;

    /**
     * Opens the resource, then allocates the native read buffer, the read and (when seekable) seek upcalls,
     * and the {@code AVIOContext} wired to them.
     *
     * @param arena     the demuxer's lifetime arena that owns the upcall stubs
     * @param uri       the {@code http}/{@code https} resource to stream
     * @param ioTimeout the connect and read timeout; must be positive
     * @param watchdog  the timeout watchdog the read upcall consults for cancellation and stall reporting
     * @throws IllegalStateException if the connection cannot be established, the server rejects the request,
     *                               or the native buffer or context cannot be allocated
     */
    public FfmpegHttpAvio(Arena arena, URI uri, Duration ioTimeout, FfmpegIoWatchdog watchdog) {
        this.uri = uri;
        this.watchdog = watchdog;
        this.timeoutMillis = (int) Math.max(1L, Math.min(ioTimeout.toMillis(), Integer.MAX_VALUE));
        this.readBuffer = new byte[READ_BUFFER_SIZE];
        this.readSegment = MemorySegment.ofArray(readBuffer);
        try {
            openStream(0);
        } catch (IOException e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "http media open failed for host " + uri.getHost(), e);
            }
            throw new IllegalStateException("open(" + uri + ") failed", e);
        }
        var buffer = FFmpegError.requireNonNull("av_malloc", Ffmpeg.av_malloc(READ_BUFFER_SIZE));
        var readStub = avio_alloc_context$read_packet.allocate(this::readPacket, arena);
        var seekStub = seekable
                ? avio_alloc_context$seek.allocate(this::seek, arena)
                : MemorySegment.NULL;
        this.avioContext = FFmpegError.requireNonNull("avio_alloc_context",
                        Ffmpeg.avio_alloc_context(buffer, READ_BUFFER_SIZE, 0, MemorySegment.NULL,
                                readStub, MemorySegment.NULL, seekStub))
                .reinterpret(AVIOContext.layout().byteSize());
    }

    /**
     * Points a demuxer context at this bridge's {@code AVIOContext} and marks its IO custom.
     *
     * <p>Must run on a context obtained from {@code avformat_alloc_context}, before
     * {@code avformat_open_input}, so open reads through the bridge. Setting {@code AVFMT_FLAG_CUSTOM_IO}
     * keeps {@code avformat_close_input} from freeing the context the caller owns, which {@link #close()}
     * frees instead.
     *
     * @param formatCtx the demuxer context to attach this bridge to
     */
    void install(MemorySegment formatCtx) {
        AVFormatContext.pb(formatCtx, avioContext);
        AVFormatContext.flags(formatCtx, AVFormatContext.flags(formatCtx) | AVFMT_FLAG_CUSTOM_IO);
    }

    /**
     * Opens the transfer at the given byte offset, following redirects and replacing any current connection.
     *
     * <p>Sends a request carrying a {@code Range} header when the offset is nonzero, follows up to
     * {@link #MAX_REDIRECTS} redirect hops (rejecting any that leave {@code http}/{@code https}), and adopts
     * the body only once the response is accepted, so a rejected range leaves the current connection intact.
     * Records the resource length and range support on the initial open.
     *
     * @param offset the byte offset to begin streaming from; {@code 0} for the initial open
     * @throws IOException if the request fails or is rejected, a redirect cannot be followed, or a nonzero
     *                     range is not honored
     */
    private void openStream(long offset) throws IOException {
        var target = uri;
        var redirects = 0;
        HttpURLConnection connected;
        int status;
        while (true) {
            connected = (HttpURLConnection) target.toURL().openConnection();
            connected.setInstanceFollowRedirects(false);
            connected.setConnectTimeout(timeoutMillis);
            connected.setReadTimeout(timeoutMillis);
            connected.setRequestProperty("User-Agent", USER_AGENT);
            if (offset > 0) {
                connected.setRequestProperty("Range", "bytes=" + offset + "-");
            }
            status = connected.getResponseCode();
            if (!isRedirect(status)) {
                break;
            }
            var location = connected.getHeaderField("Location");
            connected.disconnect();
            if (location == null || ++redirects > MAX_REDIRECTS) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "unfollowable redirect (HTTP {0}) after {1} hops", status, redirects);
                }
                throw new IOException("unfollowable redirect (HTTP " + status + ") from " + target);
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "following redirect: status={0} hop={1}", status, redirects);
            target = resolveRedirect(target, location);
        }
        var accepted = offset > 0 ? status == 206 : status / 100 == 2;
        if (!accepted) {
            connected.disconnect();
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "http media open rejected: host={0} status={1} offset={2}",
                        uri.getHost(), status, offset);
            }
            throw new IOException("open(" + uri + ") returned HTTP " + status);
        }
        if (offset == 0) {
            totalLength = connected.getContentLengthLong();
            seekable = "bytes".equalsIgnoreCase(connected.getHeaderField("Accept-Ranges"));
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "http media stream opened: host={0} offset={1} length={2} seekable={3}",
                    uri.getHost(), offset, totalLength, seekable);
        }
        var body = connected.getInputStream();
        var previousStream = stream;
        var previousConnection = connection;
        stream = body;
        connection = connected;
        closeStream(previousStream);
        disconnect(previousConnection);
    }

    /**
     * Fills the native buffer with the next bytes from the connection, blocking until some arrive or the
     * read times out.
     *
     * <p>Reads once into the array and copies the bytes into {@code buf}. Returns the byte count on success
     * and {@code AVERROR_EOF} at end of input, once the bridge is closed, or on a transport error. A read
     * that exceeds the connection's read timeout marks the watchdog timed out before returning end, and a
     * transport error during the transfer (a connection reset or TLS drop) records a read failure on the
     * watchdog, so the read loop reports a stall or a truncated transfer rather than a clean end.
     *
     * @param opaque the callback opaque pointer, unused because the connection is captured directly
     * @param buf    the native buffer to fill, valid for {@code size} bytes
     * @param size   the buffer capacity in bytes
     * @return the number of bytes written, or {@code AVERROR_EOF}
     */
    private int readPacket(MemorySegment opaque, MemorySegment buf, int size) {
        if (closed || watchdog.cancelled()) {
            return Ffmpeg.AVERROR_EOF();
        }
        try {
            var n = stream.read(readBuffer, 0, Math.min(size, readBuffer.length));
            if (n < 0) {
                return Ffmpeg.AVERROR_EOF();
            }
            MemorySegment.copy(readSegment, 0L, buf.reinterpret(size), 0L, n);
            position += n;
            return n;
        } catch (SocketTimeoutException e) {
            if (!watchdog.cancelled()) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "http media read timed out: host={0}", uri.getHost());
                }
                watchdog.markTimedOut();
            }
            return Ffmpeg.AVERROR_EOF();
        } catch (IOException e) {
            if (!watchdog.cancelled()) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "http media read failed for host " + uri.getHost(), e);
                }
                watchdog.markReadFailed(e.getMessage() != null ? e.getMessage() : e.toString());
            }
            return Ffmpeg.AVERROR_EOF();
        }
    }

    /**
     * Repositions the stream, reopening the transfer at the requested byte offset.
     *
     * <p>Answers an {@code AVSEEK_SIZE} query with the resource's total length, and otherwise resolves the
     * {@code whence}-relative target, reopens there, and returns the new absolute position. Returns
     * {@code -1} when the resource is not seekable, the size is unknown, the target is negative, or the
     * server does not honor the range, in which case the current connection and position are left untouched.
     *
     * @param opaque the callback opaque pointer, unused because the connection is captured directly
     * @param offset the requested offset, interpreted per {@code whence}
     * @param whence {@code SEEK_SET}, {@code SEEK_CUR}, {@code SEEK_END}, or {@code AVSEEK_SIZE}, possibly
     *               with {@code AVSEEK_FORCE} set
     * @return the new absolute position, the total length for a size query, or {@code -1} on failure
     */
    private long seek(MemorySegment opaque, long offset, int whence) {
        var base = whence & ~AVSEEK_FORCE;
        if (base == AVSEEK_SIZE) {
            return totalLength;
        }
        var target = switch (base) {
            case SEEK_SET -> offset;
            case SEEK_CUR -> position + offset;
            case SEEK_END -> totalLength < 0 ? -1 : totalLength + offset;
            default -> -1L;
        };
        if (target < 0 || !seekable || closed) {
            return -1;
        }
        try {
            openStream(target);
            position = target;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "http media seek to {0}", target);
            return target;
        } catch (IOException e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "http media seek to " + target + " failed for host " + uri.getHost(), e);
            }
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Disconnects so a read parked in {@code read_packet} returns at once and the demux call it backs
     * unwinds.
     */
    @Override
    public void abort() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "http media source aborted: host={0}", uri.getHost());
        closed = true;
        closeStream(stream);
        disconnect(connection);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Frees the native {@code AVIOContext} and its current buffer (which FFmpeg may have grown from the
     * one allocated here) and releases the connection. Runs after {@code avformat_close_input}, so no
     * callback can fire while the context is torn down.
     */
    @Override
    public void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "http media source closed: host={0}", uri.getHost());
        closed = true;
        try (var local = Arena.ofConfined()) {
            var buffer = AVIOContext.buffer(avioContext);
            if (buffer != null && buffer.address() != 0L) {
                Ffmpeg.av_free(buffer);
            }
            var pp = local.allocate(ValueLayout.ADDRESS);
            pp.set(ValueLayout.ADDRESS, 0L, avioContext);
            Ffmpeg.avio_context_free(pp);
        }
        closeStream(stream);
        disconnect(connection);
    }

    /**
     * Returns whether the status code is a redirect this bridge follows.
     *
     * @param status the HTTP status code
     * @return {@code true} for the moved, found, see other, temporary, and permanent redirect codes
     */
    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    /**
     * Resolves a redirect {@code Location} against the request it answered, rejecting a target that leaves
     * the {@code http}/{@code https} schemes.
     *
     * @param base     the URI the redirect responded to, the base for a relative location
     * @param location the {@code Location} header value
     * @return the resolved redirect target
     * @throws IOException if the location is malformed or resolves to a disallowed scheme
     */
    private static URI resolveRedirect(URI base, String location) throws IOException {
        URI resolved;
        try {
            resolved = base.resolve(location);
        } catch (IllegalArgumentException e) {
            throw new IOException("malformed redirect Location: " + location, e);
        }
        var scheme = resolved.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IOException("redirect to disallowed scheme: " + resolved);
        }
        return resolved;
    }

    /**
     * Closes a body stream, ignoring a failure since teardown proceeds regardless.
     *
     * @param toClose the stream to close, or {@code null}
     */
    private static void closeStream(InputStream toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException ignored) {
                // Teardown proceeds regardless of a failure to close the transport.
            }
        }
    }

    /**
     * Disconnects a connection, tolerating a {@code null}.
     *
     * @param toDisconnect the connection to disconnect, or {@code null}
     */
    private static void disconnect(HttpURLConnection toDisconnect) {
        if (toDisconnect != null) {
            toDisconnect.disconnect();
        }
    }
}
