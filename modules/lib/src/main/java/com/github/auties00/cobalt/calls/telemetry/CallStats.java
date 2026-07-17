package com.github.auties00.cobalt.calls.telemetry;

import com.github.auties00.cobalt.calls.telemetry.CallResult;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.CallState;
import com.github.auties00.cobalt.wire.wam.type.CallSide;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.CallRuntime;

/**
 * Accumulates the per call telemetry dimensions drained into a WAM Call event when a call ends.
 *
 * <p>One instance is created with each {@link CallRuntime} and owned by it. It captures the dimensions
 * known when the call begins ({@link #callId()}, {@link #side()}, {@link #videoEnabled()},
 * {@link #startedAt()}) and is stamped at the two lifecycle transitions the runtime drives:
 * {@link #markConnected()} the first time the call reaches {@link CallState#ACTIVE} and
 * {@link #markEnded()} when it reaches {@link CallState#ENDED}. The telemetry emitter reads the
 * accumulated dimensions through the accessors when the call is unregistered and folds them into a single
 * WAM Call event, so an in progress call never publishes telemetry.
 *
 * @implNote This implementation is event driven: the connected and ended instants are stamped directly
 * from the runtime's lifecycle transitions rather than by a per call thread polling the call state, so no
 * background ticker exists.
 */
public final class CallStats {
    /**
     * The logger for {@link CallStats}.
     */
    private static final System.Logger LOGGER = Log.get(CallStats.class);

    /**
     * The call identifier these dimensions belong to.
     */
    private final String callId;

    /**
     * Which side initiated the call.
     */
    private final CallSide side;

    /**
     * Whether video was enabled at call setup.
     */
    private final boolean videoEnabled;

    /**
     * When the call was placed or accepted.
     */
    private final Instant startedAt;

    /**
     * When the call first reached {@link CallState#ACTIVE}, or {@code null} if it never connected.
     */
    private volatile Instant connectedAt;

    /**
     * When the call reached {@link CallState#ENDED}, or {@code null} until it ends.
     */
    private volatile Instant endedAt;

    /**
     * The engine call result the service resolved for this call, or {@code null} when none was resolved
     * and the end reason alone determines the telemetry result type.
     *
     * <p>Set by the service for the outcomes whose distinct result the lifecycle controller's terminal end
     * reason cannot recover, in particular the accept ack NACK ({@link CallResult#CALL_IS_FULL} and
     * {@link CallResult#CALL_DOES_NOT_EXIST_FOR_REJOIN}), so the WAM Call event reports the engine's
     * own result code rather than the lossy end reason projection.
     */
    private volatile CallResult result;

    /**
     * Constructs a telemetry accumulator for one call.
     *
     * @param callId       the call identifier
     * @param side         which side initiated the call
     * @param videoEnabled whether video was enabled at call setup
     * @param startedAt    when the call was placed or accepted
     * @throws NullPointerException if {@code callId}, {@code side}, or {@code startedAt} is {@code null}
     */
    public CallStats(String callId, CallSide side, boolean videoEnabled, Instant startedAt) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.side = Objects.requireNonNull(side, "side cannot be null");
        this.videoEnabled = videoEnabled;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
        // TODO: wire the "last minute" call telemetry (the last_min_* WAM fields: avg_rtt, jitter buffer
        //  delay, jitter buffer lost, video render freeze). Concrete steps, in order; every one is a
        //  prerequisite that does not exist in Cobalt yet, which is why this is unwired:
        //   1. Add a per-call 1Hz field-stats sampler: a periodic task (one per active call) that fires once
        //      per second while the call runs and snapshots each metric below. CallStats today stamps only
        //      discrete lifecycle instants, so this sampler is new (mirror WhatsApp's per-second field stats
        //      tick). Cobalt's virtual-thread model can host it as a scheduled per-call task.
        //   2. Expose each metric as a per-second value the sampler can read; none of these surfaces exists:
        //      a. avg_rtt: fold the per-unit RTT estimators (scattered across the transport/BWE) into one
        //         aggregated per-second RTT reading.
        //      b. jitter buffer delay/lost: expose a per-second delta from NetEq, which today only drains its
        //         statistics once at end-of-call rather than as a running per-second delta.
        //      c. video render freeze: build a freeze detector on the video render path; there is no freeze
        //         source at all today.
        //   3. Accumulate each per-second sample into a per-metric rolling 60s buffer.
        //   4. Emit the buffers via their sum()/count() aggregates, gated by the engine's
        //      report-last-minute-metrics flag.
        //  DO NOT wire steps 3-4 until 2a-2c produce real values: these fields ship to Meta's WAM telemetry,
        //  so feeding fabricated or partial numbers would send invented telemetry upstream. Until then
        //  CallStats stays lifecycle-instant-only and the last_min_* fields are left unset.
    }

    /**
     * Returns the call identifier these dimensions belong to.
     *
     * @return the call identifier
     */
    public String callId() {
        return callId;
    }

    /**
     * Returns which side initiated the call.
     *
     * @return the call side
     */
    public CallSide side() {
        return side;
    }

    /**
     * Returns whether video was enabled at call setup.
     *
     * @return {@code true} if video was enabled
     */
    public boolean videoEnabled() {
        return videoEnabled;
    }

    /**
     * Returns when the call was placed or accepted.
     *
     * @return the start instant
     */
    public Instant startedAt() {
        return startedAt;
    }

    /**
     * Stamps the connected instant the first time the call reaches {@link CallState#ACTIVE}.
     *
     * <p>Idempotent: a later invocation after the first leaves the recorded instant unchanged, so a call
     * that briefly reconnects does not reset its connected timestamp.
     */
    public void markConnected() {
        if (connectedAt == null) {
            connectedAt = Instant.now();
            if (Log.INFO) LOGGER.log(Level.INFO, "call {0} connected", callId);
        }
    }

    /**
     * Stamps the ended instant the first time the call reaches {@link CallState#ENDED}.
     *
     * <p>Idempotent: a later invocation after the first leaves the recorded instant unchanged, so a
     * double teardown does not move the ended timestamp.
     */
    public void markEnded() {
        if (endedAt == null) {
            endedAt = Instant.now();
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "call {0} ended, connected duration {1}s", callId,
                        connectedDurationSeconds());
            }
        }
    }

    /**
     * Returns the connected duration in seconds, or zero when the call never reached
     * {@link CallState#ACTIVE}.
     *
     * <p>When the call connected but {@link #markEnded()} has not yet run, the duration is measured to
     * the current instant, so a still active call reports its running duration. The result is clamped so it
     * is never negative.
     *
     * @return the connected duration in seconds
     */
    public long connectedDurationSeconds() {
        var connected = connectedAt;
        if (connected == null) {
            return 0;
        }
        var end = endedAt != null ? endedAt : Instant.now();
        return Math.max(0, end.getEpochSecond() - connected.getEpochSecond());
    }

    /**
     * Records the engine call result the service resolved for this call.
     *
     * <p>Stamped before the lifecycle controller tears the call down, so the recorded result survives the
     * controller's release of the engine context and is available when the telemetry is drained at
     * unregister. A {@code null} clears it.
     *
     * @param result the resolved call result, or {@code null} to clear
     */
    public void result(CallResult result) {
        this.result = result;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} result set to {1}", callId, result);
    }

    /**
     * Returns the engine call result the service resolved for this call, when one was resolved.
     *
     * @return an {@link Optional} holding the resolved result, or empty when none was resolved
     */
    public Optional<CallResult> result() {
        return Optional.ofNullable(result);
    }
}
