package com.github.auties00.cobalt.calls.platform.video;

import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Production {@link VideoCaptureDriver} backing the camera capture onto a host video source.
 *
 * <p>This driver owns the four state lifecycle and the frame forwarding contract of
 * {@link VideoCaptureDriver} and pumps frames from a host {@link VideoOutput} acting as the operating
 * system camera. {@link #initDriver(String, VideoSink)} binds a source for the named device through the
 * installed {@link CameraSourceFactory} and latches the sink; {@link #start(VideoCaptureCapability)}
 * spins a virtual thread capture pump that repeatedly {@linkplain VideoOutput#takeVideo() pulls} frames from
 * the source and forwards each through {@link #sendVideoToSink(VideoFrame)}; {@link #stop()} ends the
 * pump and the bound source. A frame whose dimensions differ from the previously forwarded frame is
 * dropped, a frame offered while not {@link State#RUNNING} is dropped, and a sink that throws is logged
 * and treated as a transient per frame error rather than a fatal condition.
 *
 * <p>The host camera is supplied rather than opened here: an embedder installs a
 * {@link CameraSourceFactory} that turns a device identifier into a {@link VideoOutput}, typically
 * fulfilled with a camera backed source from the public stream factories. Keeping the device binding
 * behind that factory lets this driver be the host side state machine without pinning a particular
 * capture backend.
 *
 * @implNote This implementation drives the capture with a single virtual thread pump under the Cobalt
 * threading model, in place of a dedicated reader thread.
 */
public final class LiveVideoCaptureDriver implements VideoCaptureDriver {
    /**
     * The logger for {@link LiveVideoCaptureDriver}.
     */
    private static final System.Logger LOGGER = Log.get(LiveVideoCaptureDriver.class);

    /**
     * Names the result code a forwarded frame returns when the sink accepts it.
     *
     * <p>A sink that accepts a frame reports {@code 0}.
     */
    private static final int SINK_OK = 0;

    /**
     * Names the sentinel a dropped frame returns when it never reaches the sink.
     *
     * <p>Distinguishes a host side drop (wrong state, no sink, or changed size) from a sink reported
     * error, which carries the sink's own nonzero code.
     */
    private static final int SINK_DROPPED = -1;

    /**
     * Turns a device identifier into the host {@link VideoOutput} a {@link LiveVideoCaptureDriver}
     * captures from.
     *
     * <p>This is the seam between the driver's state machine and the concrete camera backend: an embedder
     * installs a factory that opens the named operating system camera and returns it as a
     * {@link VideoOutput} whose {@link VideoOutput#takeVideo()} yields raw frames. It is a single abstract
     * method type so a lambda or a method reference to a stream factory satisfies it.
     */
    @FunctionalInterface
    public interface CameraSourceFactory {
        /**
         * Opens the named camera and returns it as a frame source.
         *
         * <p>Binds the operating system camera identified by {@code deviceId} and returns a
         * {@link VideoOutput} whose {@link VideoOutput#takeVideo()} delivers its captured frames. The returned
         * source is owned by the driver and {@linkplain VideoOutput#shutdown() shut down} when the driver
         * stops.
         *
         * @param deviceId the implementation defined camera identifier; never {@code null}
         * @return the opened camera as a frame source; never {@code null}
         * @throws IllegalStateException if the camera cannot be opened
         */
        VideoOutput open(String deviceId);
    }

    /**
     * Builds the host {@link VideoOutput} for a bound device identifier.
     */
    private final CameraSourceFactory sourceFactory;

    /**
     * Guards every state transition and the {@code state}/{@code sink}/{@code source}/{@code lastWidth}/
     * {@code lastHeight} fields against caller threads and the capture pump racing.
     */
    private final ReentrantLock lock;

    /**
     * Holds the current lifecycle state.
     *
     * <p>Mutated only under {@link #lock}, and declared {@code volatile} so {@link #selectCamera(String)}
     * can fast-fail its {@link State#VOID} guard with a lock-free read before opening the replacement
     * source, then re-validate authoritatively under the lock.
     */
    private volatile State state;

    /**
     * Holds the latched sink, or {@code null} before {@link #initDriver(String, VideoSink)} and after a
     * discard.
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
     * Holds the width of the most recently forwarded frame, or {@code -1} before any frame is forwarded.
     */
    private int lastWidth;

    /**
     * Holds the height of the most recently forwarded frame, or {@code -1} before any frame is forwarded.
     */
    private int lastHeight;

    /**
     * Creates a camera capture driver that binds its host source through the given factory.
     *
     * <p>The driver starts in {@link State#VOID} holding no device and no sink.
     *
     * @param sourceFactory the factory that opens a named camera as a {@link VideoOutput}; never
     *                      {@code null}
     * @throws NullPointerException if {@code sourceFactory} is {@code null}
     */
    public LiveVideoCaptureDriver(CameraSourceFactory sourceFactory) {
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
     * @param deviceId {@inheritDoc}
     * @param sink     {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void initDriver(String deviceId, VideoSink sink) {
        Objects.requireNonNull(deviceId, "deviceId cannot be null");
        Objects.requireNonNull(sink, "sink cannot be null");
        lock.lock();
        try {
            if (state != State.VOID) {
                throw new IllegalStateException("init_driver requires VOID, was " + state);
            }
            this.source = Objects.requireNonNull(sourceFactory.open(deviceId),
                    "camera source factory returned null");
            this.sink = sink;
            this.lastWidth = -1;
            this.lastHeight = -1;
            this.state = State.INITIALIZED;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video capture driver initialized");
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
                    .name("calls-video-capture-" + capability.width() + "x" + capability.height())
                    .start(this::pumpLoop);
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "video capture started: {0}x{1} maxFps={2}",
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
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video capture stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceId {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void selectCamera(String deviceId) {
        Objects.requireNonNull(deviceId, "deviceId cannot be null");
        // Fast-fail the VOID guard with a lock-free read so no camera is opened on the rejection path, then
        // open the (potentially slow) replacement outside the lock. The lock is held only to swap the field,
        // and re-checks the guard authoritatively: if the driver raced to VOID the speculatively opened
        // source is closed so the rejection path still leaves no device open.
        if (state == State.VOID) {
            throw new IllegalStateException("select_camera requires a bound device, was VOID");
        }
        var next = Objects.requireNonNull(sourceFactory.open(deviceId),
                "camera source factory returned null");
        VideoOutput previous;
        lock.lock();
        try {
            if (state == State.VOID) {
                next.shutdown();
                throw new IllegalStateException("select_camera requires a bound device, was VOID");
            }
            previous = this.source;
            this.source = next;
            this.lastWidth = -1;
            this.lastHeight = -1;
        } finally {
            lock.unlock();
        }
        if (previous != null) {
            previous.shutdown();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video capture camera selected");
    }

    /**
     * Pulls frames from the bound source and forwards them until the driver leaves {@link State#RUNNING}.
     *
     * <p>Runs on the capture pump virtual thread spun by {@link #start(VideoCaptureCapability)}. Each
     * iteration {@linkplain VideoOutput#takeVideo() takes} the next frame from the source and forwards it
     * through {@link #sendVideoToSink(VideoFrame)}; an interrupt from {@link #stop()} exits the loop cleanly.
     * A camera never cleanly drains on its own, so a {@code null} frame or a device read fault while the
     * driver is still {@link State#RUNNING} is an operating system device revocation, which enters
     * {@link State#INTERRUPTED} through {@link #enterInterrupted(RuntimeException)}. The loop reads
     * {@code state} and {@code source} under {@link #lock} so a concurrent {@link #stop()} or
     * {@link #selectCamera(String)} is observed cleanly.
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
     * Transitions a running driver into {@link State#INTERRUPTED} on an operating system device revocation
     * and notifies the latched sink.
     *
     * <p>Called by {@link #pumpLoop()} when the capture source ended or faulted without a {@link #stop()}.
     * The state is flipped under {@link #lock} guarded by a {@link State#RUNNING} check so a concurrent
     * {@link #stop()}, which sets {@link State#INITIALIZED} under the same lock before it unblocks the pump,
     * is not mistaken for a revocation and the transition is made at most once. When the transition is made
     * the latched {@link VideoSink} is notified through {@link VideoSink#onInterrupted()} outside the lock so
     * the engine can react.
     *
     * @param cause the device fault that ended the capture, or {@code null} when the source returned end of
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
            LOGGER.log(Level.WARNING, "video capture device revoked, entering INTERRUPTED", cause);
        }
        if (latched != null) {
            latched.onInterrupted();
        }
    }
}
