package com.github.auties00.cobalt.calls.stream.ffmpeg;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVIOInterruptCB;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.Objects;

/**
 * Bounds blocking FFmpeg input operations with an {@code AVIOInterruptCB} deadline watchdog.
 *
 * <p>FFmpeg polls the installed callback during every blocking demux or protocol operation, including
 * {@code avformat_open_input}, {@code avformat_find_stream_info}, and each {@code av_read_frame}. The
 * callback returns nonzero to abort the current operation once the armed deadline has elapsed or the
 * owner has {@linkplain #cancel() cancelled} the source. This is the protocol agnostic hard timeout a
 * network backed media source needs so that a stalled connect or a mid stream read does not block the
 * call's decode thread forever; a local file source never installs or arms one.
 *
 * <p>The deadline is written by {@link #arm(Duration)} and {@link #disarm()} on the same thread that
 * drives the blocking FFmpeg call, immediately before and after that call, so arming is lock free. The
 * cancel flag is set from another thread by {@link #cancel()} during teardown and is read by the
 * callback, so it is {@code volatile}; setting it unblocks an in flight read promptly. The native upcall
 * stub is allocated in the supplied {@link Arena}, so the arena must outlive every FFmpeg call that can
 * invoke the callback: the owning source frees that arena only after it has cancelled and joined its
 * decode work.
 */
public final class FfmpegIoWatchdog {
    /**
     * The logger for {@link FfmpegIoWatchdog}.
     */
    private static final System.Logger LOGGER = Log.get(FfmpegIoWatchdog.class);

    /**
     * A custom byte source backing a demuxer through a Java side {@code AVIOContext}, whose blocking reads
     * this watchdog must be able to unblock and free.
     *
     * <p>A network demuxer that reads through {@link FfmpegHttpAvio} blocks inside the {@code read_packet}
     * upcall rather than inside a native protocol, so the interrupt callback is not polled while a read is
     * parked. {@link #cancel()} therefore also {@link #abort() aborts} the attached source so a parked read
     * returns at once, and the owning source {@link #close() closes} it during teardown to release its native
     * {@code AVIOContext} and its Java connection. A local file demuxer attaches none.
     */
    interface Io {
        /**
         * Unblocks a read parked in the {@code read_packet} upcall so the demux call it backs returns at once.
         *
         * <p>Called from {@link #cancel()} on the teardown thread; makes any thread blocked waiting for the
         * next byte chunk wake and report end of input, so the demuxer's {@code av_read_frame} (or probe)
         * returns and the owner can close the demuxer without racing an in flight read.
         */
        void abort();

        /**
         * Releases the source's native {@code AVIOContext}, its read buffer, and its Java connection.
         *
         * <p>Called once by the owner during teardown, after {@code avformat_close_input} has closed the
         * demuxer, so no callback can fire while the native context is freed.
         */
        void close();
    }

    /**
     * The disarmed deadline sentinel.
     *
     * <p>A disarmed watchdog leaves {@link #deadlineNanos} at this value, and the callback short circuits on
     * it explicitly rather than relying on the elapsed time subtraction: {@link System#nanoTime()} may be
     * negative, and a negative reading minus {@link Long#MAX_VALUE} underflows and wraps positive, which
     * would spuriously trip a disarmed watchdog and abort a network source. The callback returns {@code 0}
     * (continue) while disarmed until the source is armed or cancelled.
     */
    private static final long NO_DEADLINE = Long.MAX_VALUE;

    /**
     * The native {@code int (*)(void*)} upcall stub FFmpeg invokes during blocking input, bound to this
     * watchdog's deadline check and owned by the source's arena.
     */
    private final MemorySegment callbackStub;

    /**
     * The {@link System#nanoTime()} instant at which the current operation is due to abort, or
     * {@link #NO_DEADLINE} while disarmed.
     *
     * <p>Written on the decode thread by {@link #arm(Duration)} and {@link #disarm()} just around a
     * blocking call and read by the callback on that same thread.
     */
    private volatile long deadlineNanos = NO_DEADLINE;

    /**
     * Caches the last {@link #arm(Duration)} timeout so a repeated arm with the same duration skips
     * revalidating it and reconverting it to nanoseconds; {@code null} until the first arm.
     *
     * <p>Read and written only on the decode thread that drives {@link #arm(Duration)}/{@link #disarm()}, so
     * it needs no synchronization.
     */
    private Duration armTimeout;

    /**
     * Caches {@link #armTimeout} converted to nanoseconds, the per arm offset added to
     * {@link System#nanoTime()} to form the deadline.
     */
    private long armTimeoutNanos;

    /**
     * Whether the source has been permanently cancelled, after which the callback aborts every operation.
     */
    private volatile boolean cancelled;

    /**
     * Whether the most recently armed window aborted a blocking call, distinguishing a timeout abort from
     * a clean end of stream.
     *
     * <p>Reset by {@link #arm(Duration)} and set by the callback when it trips, so the decode loop can
     * tell an {@code av_read_frame} failure induced by the watchdog apart from a genuine end of input, both of
     * which surface as a negative return code.
     */
    private volatile boolean fired;

    /**
     * The transport failure reason from a read that failed mid stream with an {@link java.io.IOException}
     * other than a timeout, or {@code null} when the current armed window saw no such failure.
     *
     * <p>Set by {@link #markReadFailed(String)} from the {@code read_packet} upcall and reset by
     * {@link #arm(Duration)}, so the read loop can tell a mid stream transport error (a connection reset or
     * TLS drop) apart from a clean end of input, both of which surface {@code av_read_frame} as a negative
     * return.
     */
    private volatile String readFailure;

    /**
     * The custom Java side byte source backing the demuxer, aborted on {@link #cancel()} and released by
     * {@link #closeIo()}, or {@code null} for a native protocol or local file demuxer.
     *
     * <p>Set once by the opener when it builds a {@link FfmpegHttpAvio} bridge; written before the source is
     * handed to the read loop and read on the teardown thread, so it is {@code volatile}.
     */
    private volatile Io io;

    /**
     * Allocates the interrupt callback upcall stub bound to this watchdog's deadline check.
     *
     * <p>The callback returns {@code 1} to abort when the source has been cancelled or the armed deadline
     * has elapsed, and {@code 0} to continue otherwise. The stub lives for the lifetime of {@code arena},
     * which the owning source closes only after it stops issuing FFmpeg calls.
     *
     * @param arena the source's lifetime arena that owns the upcall stub
     * @throws NullPointerException if {@code arena} is {@code null}
     */
    public FfmpegIoWatchdog(Arena arena) {
        Objects.requireNonNull(arena, "arena cannot be null");
        AVIOInterruptCB.callback.Function function = opaque -> {
            if (cancelled
                    || (deadlineNanos != NO_DEADLINE && System.nanoTime() - deadlineNanos >= 0)) {
                if (!fired && Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "interrupt callback aborting operation, cancelled={0}", cancelled);
                }
                fired = true;
                return 1;
            }
            return 0;
        };
        this.callbackStub = AVIOInterruptCB.callback.allocate(function, arena);
    }

    /**
     * Installs this watchdog's callback into a freshly allocated demuxer context before it is opened.
     *
     * <p>Copies the upcall stub into the context's embedded {@code interrupt_callback} struct so the
     * subsequent {@code avformat_open_input}, {@code avformat_find_stream_info}, and {@code av_read_frame}
     * calls on that context poll this watchdog. Must be called on a context obtained from
     * {@code avformat_alloc_context}, before {@code avformat_open_input}, since open honors a callback only
     * when it is already present on the context passed in.
     *
     * @param formatCtx the demuxer context to install the callback on
     * @throws NullPointerException if {@code formatCtx} is {@code null}
     */
    void installOn(MemorySegment formatCtx) {
        Objects.requireNonNull(formatCtx, "formatCtx cannot be null");
        var callback = AVFormatContext.interrupt_callback(formatCtx);
        AVIOInterruptCB.callback(callback, callbackStub);
        AVIOInterruptCB.opaque(callback, MemorySegment.NULL);
    }

    /**
     * Starts a fresh timeout window for the blocking call about to be issued.
     *
     * <p>Clears the {@linkplain #fired() fired} flag and sets the deadline to the given duration from now.
     * Call it immediately before a blocking FFmpeg input call and pair it with {@link #disarm()} once the
     * call returns.
     *
     * @param timeout the maximum time the upcoming call may block; must be positive
     * @throws NullPointerException     if {@code timeout} is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     * @implNote This implementation caches the validated duration and its nanosecond conversion, so a
     * steady stream of arms with the same timeout (the common case, one per {@code av_read_frame}) validates
     * and converts only on the first arm.
     */
    public void arm(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout cannot be null");
        if (!timeout.equals(armTimeout)) {
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("timeout must be positive, got " + timeout);
            }
            armTimeout = timeout;
            armTimeoutNanos = timeout.toNanos();
        }
        fired = false;
        readFailure = null;
        deadlineNanos = System.nanoTime() + armTimeoutNanos;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "watchdog armed, timeout={0}ms", timeout.toMillis());
    }

    /**
     * Clears the timeout window after a blocking call has returned, so a later pause that is not an FFmpeg
     * call does not trip the callback.
     */
    public void disarm() {
        deadlineNanos = NO_DEADLINE;
    }

    /**
     * Returns whether the most recently armed window aborted its blocking call.
     *
     * <p>Read after a blocking call returns a failure to tell a timeout abort apart from a clean
     * end of input.
     *
     * @return {@code true} if the callback tripped the deadline during the last armed window
     */
    public boolean fired() {
        return fired;
    }

    /**
     * Permanently aborts any current and future blocking call on the watched context.
     *
     * <p>Called during source teardown so a decode thread parked inside {@code av_read_frame} returns at
     * once instead of waiting out its full timeout, letting the owner release the native context promptly.
     * When a custom {@link Io} source is attached, also {@linkplain Io#abort() aborts} it, since a read
     * parked in the {@code read_packet} upcall does not poll this callback and would otherwise stall
     * {@code av_read_frame} until its own deadline.
     */
    public void cancel() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cancelling io watchdog");
        cancelled = true;
        var attached = io;
        if (attached != null) {
            attached.abort();
        }
    }

    /**
     * Returns whether the source has been permanently cancelled, so a read parked in the {@code read_packet}
     * upcall can abort itself rather than wait out its deadline.
     *
     * @return {@code true} once {@link #cancel()} has run
     */
    boolean cancelled() {
        return cancelled;
    }

    /**
     * Records that a blocking read hit its deadline inside the {@code read_packet} upcall, so the read loop
     * reports a timeout rather than a clean end of input.
     *
     * <p>The interrupt callback is not polled while a {@link FfmpegHttpAvio} read is parked, so a read that
     * exceeds the connection's read timeout sets this flag directly; {@link #fired()} then reports the
     * timeout the same way a native side abort would.
     */
    void markTimedOut() {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "blocking read timed out inside read_packet upcall");
        fired = true;
    }

    /**
     * Records that a read failed mid stream with a transport error, so the read loop reports the failure
     * rather than a clean end of input.
     *
     * <p>Called from the {@code read_packet} upcall when a read throws an {@link java.io.IOException} that is
     * not a timeout; {@link #readFailure()} then carries the reason the read loop surfaces as an
     * {@link IllegalStateException} rather than treating the negative {@code av_read_frame} return as a clean
     * end of input.
     *
     * @param message the transport failure reason
     */
    void markReadFailed(String message) {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "read failed mid stream: {0}", message);
        readFailure = message;
    }

    /**
     * Returns the transport failure reason recorded during the current armed window, or {@code null} when the
     * read saw no mid stream transport error.
     *
     * <p>Read after a blocking demux read returns a negative code to tell a mid stream transport error apart
     * from a genuine end of input.
     *
     * @return the transport failure reason, or {@code null} if none was recorded
     */
    public String readFailure() {
        return readFailure;
    }

    /**
     * Attaches the custom byte source this watchdog aborts on {@link #cancel()} and releases on
     * {@link #closeIo()}.
     *
     * @param source the demuxer's custom byte source, or {@code null} to detach
     */
    void attach(Io source) {
        this.io = source;
    }

    /**
     * Releases the attached custom byte source, if any, during teardown.
     *
     * <p>Called by the owning source after {@code avformat_close_input} has closed the demuxer, so the
     * source's native {@code AVIOContext} and its Java connection are freed only once no callback can fire.
     */
    public void closeIo() {
        var attached = io;
        if (attached != null) {
            attached.close();
        }
    }
}
