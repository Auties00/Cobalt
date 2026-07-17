package com.github.auties00.cobalt.calls;

import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.telemetry.CallStats;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallState;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Holds the per client, service level live state of a single call.
 *
 * <p>{@link LiveCallsService} keeps one {@code CallRuntime} per active call in its registry and discards
 * it when the call ends. This runtime owns the slice the service layer is responsible for: the public
 * {@link Call} data view the application observes, the four media streams that bridge the application and
 * the codecs, and the {@link CallStats} telemetry accumulator drained at the ended transition. The
 * transport stack and the media session itself are built and owned by the engine behind its lifecycle
 * controller, so the heavy media plane machinery does not live here at the host boundary.
 *
 * <p>{@link #end(CallEndReason)} is the single service level teardown path: it flips the {@link Call} to
 * {@link CallState#ENDED}, stamps the telemetry accumulator, and shuts the four streams so blocked
 * application reads and writes unblock. It does not unregister the call from the service or emit telemetry;
 * the service owns those steps because they touch the service's registry and the WAM emitter.
 */
public final class CallRuntime {
    /**
     * The logger for {@link CallRuntime}.
     */
    private static final System.Logger LOGGER = Log.get(CallRuntime.class);

    /**
     * Holds the public data view this runtime drives.
     */
    private final Call call;

    /**
     * Holds the per call telemetry accumulator owned by this runtime.
     *
     * <p>Stamped at the connected and ended lifecycle transitions and drained into a WAM Call event by
     * the service when the call is unregistered.
     */
    private final CallStats stats;

    /**
     * Holds the source the application writes local audio into and the encoder drains.
     */
    private final AudioOutput audioOut;

    /**
     * Holds the sink the decoder fills with remote audio and the application reads.
     */
    private final AudioInput audioIn;

    /**
     * Holds the source the application writes local video into and the encoder drains.
     */
    private final VideoOutput videoOut;

    /**
     * Holds the sink the decoder fills with remote video and the application reads.
     */
    private final VideoInput videoIn;

    /**
     * Guards {@link #end(CallEndReason)} so the service level teardown runs its stream shutdown exactly
     * once, keyed independently of the public {@link Call} state.
     *
     * <p>The engine lifecycle controller flips the shared {@link Call} view to {@link CallState#ENDED}
     * before it fires the service teardown sink that drives {@link #end(CallEndReason)}, so guarding on the
     * call view's {@link CallState#ENDED} would suppress the stream shutdown and leave blocked application
     * reads and writes hanging. This flag records whether this runtime already ran its teardown, so
     * {@link #end(CallEndReason)} shuts the four streams once regardless of the shared call view's state.
     */
    private boolean ended;

    /**
     * Constructs a runtime bound to a call, its four media streams, and its telemetry accumulator.
     *
     * @param call     the public data view
     * @param stats    the per call telemetry accumulator
     * @param audioOut the local audio source
     * @param audioIn  the remote audio sink
     * @param videoOut the local video source
     * @param videoIn  the remote video sink
     * @throws NullPointerException if any argument is {@code null}
     */
    public CallRuntime(Call call, CallStats stats,
                         AudioOutput audioOut, AudioInput audioIn,
                         VideoOutput videoOut, VideoInput videoIn) {
        this.call = Objects.requireNonNull(call, "call cannot be null");
        this.stats = Objects.requireNonNull(stats, "stats cannot be null");
        this.audioOut = Objects.requireNonNull(audioOut, "audioOut cannot be null");
        this.audioIn = Objects.requireNonNull(audioIn, "audioIn cannot be null");
        this.videoOut = Objects.requireNonNull(videoOut, "videoOut cannot be null");
        this.videoIn = Objects.requireNonNull(videoIn, "videoIn cannot be null");
    }

    /**
     * Returns the public data view this runtime drives.
     *
     * @return the call
     */
    public Call call() {
        return call;
    }

    /**
     * Returns this call's identifier.
     *
     * @return the call id
     */
    public String callId() {
        return call.callId();
    }

    /**
     * Returns the per call telemetry accumulator owned by this runtime.
     *
     * @return the telemetry accumulator
     */
    public CallStats stats() {
        return stats;
    }

    /**
     * Returns the source the application writes local audio into.
     *
     * @return the local audio source
     */
    public AudioOutput audioOut() {
        return audioOut;
    }

    /**
     * Returns the sink the application reads remote audio from.
     *
     * @return the remote audio sink
     */
    public AudioInput audioIn() {
        return audioIn;
    }

    /**
     * Returns the source the application writes local video into.
     *
     * @return the local video source
     */
    public VideoOutput videoOut() {
        return videoOut;
    }

    /**
     * Returns the sink the application reads remote video from.
     *
     * @return the remote video sink
     */
    public VideoInput videoIn() {
        return videoIn;
    }

    /**
     * Drives the call's service level state to {@link CallState#ENDED} and releases its application facing
     * resources.
     *
     * <p>Records the end reason on the {@link Call}, stamps the telemetry accumulator's ended instant, and
     * shuts down the four media streams so blocked application reads and writes return. Idempotent: a second
     * invocation after the call has ended returns without effect. This does not remove the call from the
     * service registry or commit telemetry; the service performs those steps so the registry and the WAM
     * emitter stay owned by the service.
     *
     * @param reason the canonical end reason to record on the call view
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    public void end(CallEndReason reason) {
        Objects.requireNonNull(reason, "reason cannot be null");
        synchronized (this) {
            if (ended) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "call {0} end ignored, already ended", call.callId());
                return;
            }
            ended = true;
            call.setState(CallState.ENDED);
            call.setEndReason(reason);
            stats.markEnded();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} ended, reason={1}", call.callId(), reason);
        audioOut.shutdown();
        audioIn.shutdown();
        videoOut.shutdown();
        videoIn.shutdown();
    }
}
