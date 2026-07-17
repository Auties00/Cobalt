package com.github.auties00.cobalt.calls.platform.video;

import com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver.State;
import com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver.VideoCaptureCapability;
import com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver.VideoSink;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Production {@link DesktopCaptureDriver} that captures a host screen surface and forwards its frames
 * to the engine sink.
 *
 * <p>This driver owns the four state lifecycle and the frame forwarding contract of
 * {@link DesktopCaptureDriver}, pumping frames from a host {@link VideoOutput} that acts as the screen
 * surface. {@link #initDriver(String, VideoSink)} binds a source for the named surface through the
 * installed {@link ScreenSourceFactory} and latches the engine sink;
 * {@link #start(VideoCaptureCapability)} spins a virtual thread capture pump that repeatedly
 * {@linkplain VideoOutput#takeVideo() pulls} frames and forwards each through
 * {@link #sendVideoToSink(VideoFrame)}; {@link #setConfig(VideoCaptureCapability)} updates the capture
 * geometry in place and reseeds the frame size baseline so the next differently sized frame is accepted
 * rather than dropped; {@link #stop()} ends the pump and closes the bound source. The frame drop and
 * sink error handling match {@link LiveVideoCaptureDriver}.
 *
 * <p>The host surface is supplied rather than opened here: an embedder or the engine installs a
 * {@link ScreenSourceFactory} that turns a surface identifier into a {@link VideoOutput}, typically a
 * screen backed source obtained from the public stream factories.
 *
 * @implNote This implementation drives capture from a single virtual thread pump rather than a
 * dedicated native reader thread, consistent with the Cobalt threading model.
 */
public final class LiveDesktopCaptureDriver implements DesktopCaptureDriver {
    /**
     * The logger for {@link LiveDesktopCaptureDriver}.
     */
    private static final System.Logger LOGGER = Log.get(LiveDesktopCaptureDriver.class);

    /**
     * Success code returned when a {@link VideoSink} accepts a forwarded frame.
     *
     * <p>A {@link VideoSink} returns {@code 0} when it consumed the frame; any other value is an error
     * code the sink reports.
     */
    private static final int SINK_OK = 0;

    /**
     * Sentinel returned when a frame never reaches the sink.
     *
     * <p>Distinguishes a drop taken by this driver (wrong state, missing sink, or a changed frame size)
     * from a sink reported error, which carries the sink's own nonzero code.
     */
    private static final int SINK_DROPPED = -1;

    /**
     * Turns a surface identifier into the host {@link VideoOutput} a {@link LiveDesktopCaptureDriver}
     * captures from.
     *
     * <p>This is the seam between the driver's state machine and the concrete screen capture backend:
     * the engine or embedder installs a factory that opens the named display, window, or capture target
     * and returns it as a {@link VideoOutput} whose {@link VideoOutput#takeVideo()} yields raw frames. It is a
     * single abstract method type, so a lambda or a method reference to a stream factory satisfies it.
     */
    @FunctionalInterface
    public interface ScreenSourceFactory {
        /**
         * Opens the named screen surface and returns it as a frame source.
         *
         * <p>Binds the operating system display or window identified by {@code surfaceId} and returns a
         * {@link VideoOutput} whose {@link VideoOutput#takeVideo()} delivers its captured frames. The returned
         * source is owned by the driver and {@linkplain VideoOutput#shutdown() shut down} when the driver
         * stops.
         *
         * @param surfaceId the implementation defined surface identifier; never {@code null}
         * @return the opened surface as a frame source; never {@code null}
         * @throws IllegalStateException if the surface cannot be opened
         */
        VideoOutput open(String surfaceId);
    }

    /**
     * Builds the host {@link VideoOutput} for a bound surface identifier.
     */
    private final ScreenSourceFactory sourceFactory;

    /**
     * Guards every state transition and the {@code state}, {@code sink}, {@code source},
     * {@code lastWidth}, and {@code lastHeight} fields against the caller thread and the capture pump
     * racing.
     */
    private final ReentrantLock lock;

    /**
     * Holds the current lifecycle state, only ever mutated under {@link #lock}.
     */
    private State state;

    /**
     * Holds the latched engine sink, or {@code null} before {@link #initDriver(String, VideoSink)} and
     * after the source is released.
     */
    private VideoSink sink;

    /**
     * Holds the bound host frame source, or {@code null} while {@link State#VOID}.
     */
    private VideoOutput source;

    /**
     * Holds the capture pump thread spun by {@link #start(VideoCaptureCapability)}, or {@code null} while
     * not {@link State#RUNNING}.
     */
    private Thread pump;

    /**
     * Holds the width of the most recently forwarded frame, or {@code -1} before any frame is forwarded
     * and right after {@link #setConfig(VideoCaptureCapability)} reseeds the baseline.
     */
    private int lastWidth;

    /**
     * Holds the height of the most recently forwarded frame, or {@code -1} before any frame is forwarded
     * and right after {@link #setConfig(VideoCaptureCapability)} reseeds the baseline.
     */
    private int lastHeight;

    /**
     * Creates a screen share capture driver that binds its host source through the given factory.
     *
     * <p>The driver starts in {@link State#VOID} holding no surface and no sink.
     *
     * @param sourceFactory the factory that opens a named surface as a {@link VideoOutput}; never
     *                      {@code null}
     * @throws NullPointerException if {@code sourceFactory} is {@code null}
     */
    public LiveDesktopCaptureDriver(ScreenSourceFactory sourceFactory) {
        this.sourceFactory = Objects.requireNonNull(sourceFactory, "sourceFactory cannot be null");
        this.lock = new ReentrantLock();
        this.state = State.VOID;
        this.lastWidth = -1;
        this.lastHeight = -1;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public State state() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param surfaceId {@inheritDoc}
     * @param sink      {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void initDriver(String surfaceId, VideoSink sink) {
        Objects.requireNonNull(surfaceId, "surfaceId cannot be null");
        Objects.requireNonNull(sink, "sink cannot be null");
        lock.lock();
        try {
            if (state != State.VOID) {
                throw new IllegalStateException("init_driver requires VOID, was " + state);
            }
            this.source = Objects.requireNonNull(sourceFactory.open(surfaceId),
                    "screen source factory returned null");
            this.sink = sink;
            this.lastWidth = -1;
            this.lastHeight = -1;
            this.state = State.INITIALIZED;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "desktop capture driver initialized");
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param capability {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void start(VideoCaptureCapability capability) {
        Objects.requireNonNull(capability, "capability cannot be null");
        lock.lock();
        try {
            if (state != State.INITIALIZED) {
                throw new IllegalStateException("start requires INITIALIZED, was " + state);
            }
            this.state = State.RUNNING;
            this.pump = Thread.ofVirtual()
                    .name("calls-desktop-capture-" + capability.width() + "x" + capability.height())
                    .start(this::pumpLoop);
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "desktop capture started: {0}x{1} maxFps={2}",
                        capability.width(), capability.height(), capability.maxFps());
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param capability {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void setConfig(VideoCaptureCapability capability) {
        Objects.requireNonNull(capability, "capability cannot be null");
        lock.lock();
        try {
            if (state == State.VOID) {
                throw new IllegalStateException("set_config requires a bound surface, was VOID");
            }
            this.lastWidth = -1;
            this.lastHeight = -1;
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "desktop capture reconfigured: {0}x{1} maxFps={2}",
                        capability.width(), capability.height(), capability.maxFps());
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param frame {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public int sendVideoToSink(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        VideoSink target;
        lock.lock();
        try {
            if (state != State.RUNNING) {
                if (Log.DEBUG)
                    LOGGER.log(Level.DEBUG, "send_video_data_to_sink dropped: driver not running, state {0}",
                            state);
                return SINK_DROPPED;
            }
            if (sink == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "send_video_data_to_sink dropped: sink is null");
                return SINK_DROPPED;
            }
            if (lastWidth != -1 && (frame.width() != lastWidth || frame.height() != lastHeight)) {
                if (Log.DEBUG)
                    LOGGER.log(Level.DEBUG,
                            "send_video_data_to_sink dropped: frame size changed from {0}x{1} to {2}x{3}",
                            lastWidth, lastHeight, frame.width(), frame.height());
                return SINK_DROPPED;
            }
            this.lastWidth = frame.width();
            this.lastHeight = frame.height();
            target = sink;
        } finally {
            lock.unlock();
        }
        try {
            var code = target.accept(frame);
            if (code != SINK_OK) {
                if (Log.DEBUG)
                    LOGGER.log(Level.DEBUG, "send_video_data_to_sink: sink returned error code {0}", code);
            }
            return code;
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "send_video_data_to_sink: exception calling sink", e);
            return SINK_DROPPED;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void stop() {
        Thread toJoin;
        VideoOutput toClose;
        lock.lock();
        try {
            if (state == State.VOID) {
                throw new IllegalStateException("stop requires INITIALIZED, RUNNING, or INTERRUPTED");
            }
            if (state == State.INITIALIZED) {
                return;
            }
            this.state = State.INITIALIZED;
            toJoin = pump;
            this.pump = null;
            this.lastWidth = -1;
            this.lastHeight = -1;
            toClose = source;
            this.source = null;
        } finally {
            lock.unlock();
        }
        if (toJoin != null) {
            toJoin.interrupt();
        }
        if (toClose != null) {
            toClose.shutdown();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "desktop capture stopped");
    }

    /**
     * Pulls frames from the bound source and forwards them until the driver leaves {@link State#RUNNING}.
     *
     * <p>Runs on the capture pump virtual thread spun by {@link #start(VideoCaptureCapability)}. Each
     * iteration {@linkplain VideoOutput#takeVideo() takes} the next frame from the source and forwards it
     * through {@link #sendVideoToSink(VideoFrame)}; an interrupt from {@link #stop()} exits the loop cleanly.
     * A shared surface never cleanly drains on its own, so a {@code null} frame or a capture fault while the
     * driver is still {@link State#RUNNING} is an operating system surface revocation (the user stopped
     * sharing or the shared window closed), which enters {@link State#INTERRUPTED} through
     * {@link #enterInterrupted(RuntimeException)}. The loop reads {@code state} and {@code source} under
     * {@link #lock} so a concurrent {@link #stop()} is observed cleanly.
     */
    private void pumpLoop() {
        while (true) {
            VideoOutput current;
            lock.lock();
            try {
                if (state != State.RUNNING) {
                    return;
                }
                current = source;
            } finally {
                lock.unlock();
            }
            if (current == null) {
                return;
            }
            VideoFrame frame;
            try {
                frame = current.takeVideo();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                enterInterrupted(e);
                return;
            }
            if (frame == null) {
                enterInterrupted(null);
                return;
            }
            sendVideoToSink(frame);
        }
    }

    /**
     * Transitions a running driver into {@link State#INTERRUPTED} on an operating system surface revocation
     * and notifies the latched sink.
     *
     * <p>Called by {@link #pumpLoop()} when the capture source ended or faulted without a {@link #stop()}.
     * The state is flipped under {@link #lock} guarded by a {@link State#RUNNING} check so a concurrent
     * {@link #stop()}, which sets {@link State#INITIALIZED} under the same lock before it unblocks the pump,
     * is not mistaken for a revocation and the transition is made at most once. When the transition is made
     * the latched {@link VideoSink} is notified through {@link VideoSink#onInterrupted()} outside the lock so
     * the engine can react.
     *
     * @param cause the capture fault that ended the surface, or {@code null} when the source returned end of
     *              stream
     */
    private void enterInterrupted(RuntimeException cause) {
        VideoSink latched;
        lock.lock();
        try {
            if (state != State.RUNNING) {
                return;
            }
            this.state = State.INTERRUPTED;
            latched = sink;
        } finally {
            lock.unlock();
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "desktop capture surface revoked, entering INTERRUPTED", cause);
        }
        if (latched != null) {
            latched.onInterrupted();
        }
    }
}
