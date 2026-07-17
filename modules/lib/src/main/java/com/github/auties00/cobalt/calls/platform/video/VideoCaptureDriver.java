package com.github.auties00.cobalt.calls.platform.video;

import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;

/**
 * Defines the raw camera capture endpoint of a call, the host side driver the call engine drives to
 * obtain uncompressed video frames from a physical or virtual camera.
 *
 * <p>The engine layer never touches a camera directly; it owns a driver implementing this contract
 * and feeds the frames the driver captures into the video encode pipeline through the engine supplied
 * {@link VideoSink}. The driver is a four state machine. {@link State#VOID} is the freshly constructed
 * driver that holds no device; {@link #initDriver(String, VideoSink)} acquires the named device,
 * latches the sink, and moves to {@link State#INITIALIZED}; {@link #start(VideoCaptureCapability)}
 * begins capture at the requested geometry and moves to {@link State#RUNNING}; {@link #stop()} releases
 * capture back to {@link State#INITIALIZED}. {@link State#INTERRUPTED} is the host loss state a running
 * driver enters when the operating system revokes the device (a hot unplugged or preempted camera)
 * without the engine having asked it to stop.
 *
 * <p>While {@link State#RUNNING}, every frame the device produces is forwarded to the latched sink by
 * {@link #sendVideoToSink(VideoFrame)}. A frame whose dimensions differ from the dimensions of the
 * frame most recently forwarded is dropped rather than forwarded, because the downstream encoder is
 * sized once per capability and a mid stream resolution change is the engine's job to drive through a
 * fresh {@link #start(VideoCaptureCapability)}, not the driver's job to smuggle through the sink. A
 * frame offered while the driver is not {@link State#RUNNING} is dropped, and a frame offered while no
 * sink is latched is dropped.
 *
 * <p>This interface is sealed and permits only {@link LiveVideoCaptureDriver}, the production
 * implementation. A {@link DesktopCaptureDriver} is the separate sealed contract for screen share
 * capture, which adds a runtime reconfiguration entry point this camera contract does not need.
 *
 * <p>Embedders do not implement this interface; it is the seam between the engine and its host, and the
 * engine owns the driver and the {@link VideoSink}. An embedder that wants to supply its own pictures
 * uses the public {@link com.github.auties00.cobalt.calls.stream.VideoOutput} source instead, which is
 * the application facing write side of a call's video; this driver is the internal capture side the
 * engine wires onto an operating system device.
 */
public sealed interface VideoCaptureDriver permits LiveVideoCaptureDriver {
    /**
     * Enumerates the lifecycle states of a {@link VideoCaptureDriver}.
     *
     * <p>The states form the linear capture lifecycle the engine drives plus the one host loss branch:
     * {@link #VOID} (no device held) advances to {@link #INITIALIZED} (device held, not capturing) on
     * {@link #initDriver(String, VideoSink)}, to {@link #RUNNING} (capturing, forwarding frames) on
     * {@link #start(VideoCaptureCapability)}, and back to {@link #INITIALIZED} on {@link #stop()}.
     * {@link #INTERRUPTED} is entered from {@link #RUNNING} when the operating system revokes the device
     * underneath a running capture.
     *
     * <p>The transition guards are asymmetric: {@link #initDriver(String, VideoSink)} is accepted only
     * from {@link #VOID}, {@link #start(VideoCaptureCapability)} only from {@link #INITIALIZED}, and
     * {@link #stop()} from {@link #RUNNING}, {@link #INTERRUPTED}, or {@link #INITIALIZED}.
     */
    enum State {
        /**
         * Denotes the freshly constructed driver that holds no operating system device and forwards no
         * frames.
         *
         * <p>This is the only state from which {@link #initDriver(String, VideoSink)} is accepted.
         */
        VOID,

        /**
         * Denotes a driver that has acquired its device and latched its sink but is not yet capturing.
         *
         * <p>Entered from {@link #VOID} by {@link #initDriver(String, VideoSink)} and entered again from
         * {@link #RUNNING} by {@link #stop()}; it is the only state from which
         * {@link #start(VideoCaptureCapability)} is accepted.
         */
        INITIALIZED,

        /**
         * Denotes a driver actively capturing and forwarding frames to its sink.
         *
         * <p>Entered from {@link #INITIALIZED} by {@link #start(VideoCaptureCapability)}; while in this
         * state {@link #sendVideoToSink(VideoFrame)} forwards frames.
         */
        RUNNING,

        /**
         * Denotes a driver whose device was revoked by the operating system while it was capturing.
         *
         * <p>Entered from {@link #RUNNING} when the host loses the device without an engine
         * {@link #stop()}; the driver forwards no further frames until it is {@link #stop() stopped} and
         * {@link #start(VideoCaptureCapability) started} again.
         */
        INTERRUPTED
    }

    /**
     * Receives one raw captured frame from a {@link VideoCaptureDriver} on its way into the engine's
     * video encode pipeline.
     *
     * <p>This is the engine supplied callback a driver latches at {@link #initDriver(String, VideoSink)}
     * and invokes from {@link #sendVideoToSink(VideoFrame)} while {@link State#RUNNING}. The return value
     * follows the engine convention of a zero or error result code: {@code 0} means the engine accepted
     * the frame, and any nonzero value is an engine internal error the driver logs but does not act on
     * beyond not counting the frame as forwarded.
     */
    @FunctionalInterface
    interface VideoSink {
        /**
         * Accepts one raw captured frame and returns an engine result code.
         *
         * <p>Invoked by {@link #sendVideoToSink(VideoFrame)} for each frame a {@link State#RUNNING}
         * driver forwards. An implementation hands the frame to the encoder and returns {@code 0} on
         * success or a nonzero engine error code otherwise. The frame is never {@code null}.
         *
         * @param frame the raw captured frame; never {@code null}
         * @return {@code 0} if the engine accepted the frame, or a nonzero engine error code
         */
        int accept(VideoFrame frame);

        /**
         * Notifies the engine that the driver lost its capture device mid capture and entered
         * {@link State#INTERRUPTED}.
         *
         * <p>Invoked once by the driver's capture pump when the operating system revokes the device under a
         * running capture (a hot unplugged, preempted, or privacy revoked camera, or a screen share surface
         * the user stopped) rather than the engine calling {@link #stop()}. The default does nothing, so a
         * sink that only consumes frames is unaffected; an engine sink overrides it to react, for example by
         * ending its outbound video and broadcasting the local video state to the peer. It is called off the
         * driver's lock on the capture pump thread, so an implementation may block.
         */
        default void onInterrupted() {
        }
    }

    /**
     * Holds the capture geometry a {@link VideoCaptureDriver} is started at.
     *
     * <p>The triple is the descriptor the engine passes to {@link #start(VideoCaptureCapability)} to
     * size the capture: the {@code width} by {@code height} the device is asked to deliver and the
     * {@code maxFps} ceiling on its delivery rate. Both dimensions must be even and at least {@code 2}
     * so the captured {@link VideoPixelFormat#I420} or {@link VideoPixelFormat#NV12} chroma planes have
     * integral sizes, and {@code maxFps} must be at least {@code 1}.
     *
     * @param width  the requested capture width in pixels; even and at least {@code 2}
     * @param height the requested capture height in pixels; even and at least {@code 2}
     * @param maxFps the ceiling on the capture frame rate in frames per second; at least {@code 1}
     */
    record VideoCaptureCapability(int width, int height, int maxFps) {
        /**
         * Validates the capture geometry.
         *
         * <p>Rejects a {@code width} or {@code height} that is odd or less than {@code 2}, and a
         * {@code maxFps} below {@code 1}.
         *
         * @throws IllegalArgumentException if {@code width} or {@code height} is odd or less than
         *                                  {@code 2}, or {@code maxFps} is less than {@code 1}
         */
        public VideoCaptureCapability {
            if (width < 2 || width % 2 != 0) {
                throw new IllegalArgumentException("width must be even and >= 2, got " + width);
            }
            if (height < 2 || height % 2 != 0) {
                throw new IllegalArgumentException("height must be even and >= 2, got " + height);
            }
            if (maxFps < 1) {
                throw new IllegalArgumentException("maxFps must be >= 1, got " + maxFps);
            }
        }
    }

    /**
     * Returns the driver's current lifecycle state.
     *
     * <p>The returned value reflects the most recent successful transition driven by
     * {@link #initDriver(String, VideoSink)}, {@link #start(VideoCaptureCapability)}, or
     * {@link #stop()}, or a host loss transition into {@link State#INTERRUPTED}. Never {@code null}.
     *
     * @return the current {@link State}; never {@code null}
     */
    State state();

    /**
     * Acquires the named capture device, latches the engine sink, and moves the driver to
     * {@link State#INITIALIZED}.
     *
     * <p>Accepted only while the driver is {@link State#VOID}. The {@code deviceId} names the operating
     * system camera to bind in an implementation defined form (a device URL, friendly name, or index as
     * the platform requires), and {@code sink} is the engine callback every subsequently captured frame
     * is forwarded to. On success the driver holds the device but is not yet capturing;
     * {@link #start(VideoCaptureCapability)} begins capture.
     *
     * @param deviceId the implementation defined identifier of the camera to bind; never {@code null}
     * @param sink     the engine sink captured frames are forwarded to; never {@code null}
     * @throws NullPointerException  if {@code deviceId} or {@code sink} is {@code null}
     * @throws IllegalStateException if the driver is not {@link State#VOID}, or the device cannot be
     *                               acquired
     */
    void initDriver(String deviceId, VideoSink sink);

    /**
     * Begins capturing at the given geometry and moves the driver to {@link State#RUNNING}.
     *
     * <p>Accepted only while the driver is {@link State#INITIALIZED}. The {@code capability} sizes the
     * capture and bounds its frame rate. After this returns, the driver forwards captured frames to the
     * latched sink through {@link #sendVideoToSink(VideoFrame)} until {@link #stop()} or a host loss
     * transition to {@link State#INTERRUPTED}.
     *
     * @param capability the capture geometry and frame rate ceiling; never {@code null}
     * @throws NullPointerException  if {@code capability} is {@code null}
     * @throws IllegalStateException if the driver is not {@link State#INITIALIZED}, or capture cannot be
     *                               started
     */
    void start(VideoCaptureCapability capability);

    /**
     * Forwards one captured frame to the latched engine sink while the driver is running.
     *
     * <p>Forwards {@code frame} to the sink only while the driver is {@link State#RUNNING} and a sink is
     * latched. A frame whose {@link VideoFrame#width()} by {@link VideoFrame#height()} differs from the
     * dimensions of the frame most recently forwarded is dropped, because a mid stream resolution change
     * is driven by a fresh {@link #start(VideoCaptureCapability)} and not by the sink. A frame offered
     * while the driver is not {@link State#RUNNING}, or while no sink is latched, is dropped. The
     * returned value is the latched sink's result code for a forwarded frame ({@code 0} on success), or
     * a nonzero sentinel when the frame was dropped without reaching the sink.
     *
     * @param frame the captured frame to forward; never {@code null}
     * @return the sink's result code if the frame was forwarded, or a nonzero sentinel if it was
     * dropped
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    int sendVideoToSink(VideoFrame frame);

    /**
     * Stops capturing and releases the driver back to {@link State#INITIALIZED}.
     *
     * <p>Accepted while the driver is {@link State#RUNNING}, {@link State#INTERRUPTED}, or
     * {@link State#INITIALIZED}; in the last case it does nothing. After this returns the driver still
     * holds its device and sink and may be {@link #start(VideoCaptureCapability) started} again. This
     * does not release the device; only discarding the driver after a {@link #stop()} relinquishes it.
     *
     * @throws IllegalStateException if the driver is {@link State#VOID}
     */
    void stop();

    /**
     * Selects the active camera by its implementation defined identifier.
     *
     * <p>Switches which operating system camera the driver captures from. The accepted identifier form
     * matches {@link #initDriver(String, VideoSink)}. Whether a switch is permitted while
     * {@link State#RUNNING} is implementation defined; a running switch that the platform cannot honor
     * in place restarts capture on the new device.
     *
     * @param deviceId the implementation defined identifier of the camera to switch to; never
     *                 {@code null}
     * @throws NullPointerException  if {@code deviceId} is {@code null}
     * @throws IllegalStateException if the driver holds no device, or the device cannot be selected
     */
    void selectCamera(String deviceId);
}
