package com.github.auties00.cobalt.calls.engine.info;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.telemetry.CallResult;
import com.github.auties00.cobalt.telemetry.log.Log;

/**
 * Maintains the periodically updated immutable snapshot of one call's coarse information for listeners,
 * field statistics, and the host {@code getCallInfo} query.
 *
 * <p>The engine keeps a small call info aggregate alongside each call context that carries the call's
 * accumulated durations, its current result, and its call link information, and refreshes it on every
 * event so the host can read a consistent view without touching the live context. This class is that
 * aggregate: it holds one {@link Snapshot} per call and rebuilds it from the engine's running state
 * whenever a transition or a periodic tick calls {@link #update(CallLifecycleState, CallResult, Duration,
 * Duration, Duration, String)} or {@link #updateForEvent(CallEventType, CallLifecycleState, CallResult,
 * Duration, Duration, Duration, String)}. The latter additionally derives the {@link CallResult} the
 * snapshot should carry from the dispatched {@link CallEventType} when the caller has not already
 * resolved one.
 *
 * <p>The snapshot is published through a {@code volatile} field, so {@link #snapshot()} is a lock free
 * read returning the latest immutable {@link Snapshot} (or an empty result before the first update); this
 * is the only read path and it never blocks an update or another read. Updates are serialised behind a
 * single lock so a snapshot is rebuilt atomically from a consistent set of inputs; the publish itself is
 * a volatile write of the immutable record. Because every {@link Snapshot} is immutable, handing one to a
 * listener or the host leaks no mutable state.
 */
public final class CallInfoManager {
    /**
     * The logger for {@link CallInfoManager}.
     */
    private static final System.Logger LOGGER = Log.get(CallInfoManager.class);

    /**
     * Holds the latest published immutable snapshot, or {@code null} before the first update.
     *
     * <p>Published by a volatile write after each update and read lock free by {@link #snapshot()}, so a
     * reader always observes a fully built immutable {@link Snapshot} and never a torn one.
     */
    private volatile Snapshot current;

    /**
     * Serialises updates so a snapshot is rebuilt atomically from a consistent set of inputs.
     *
     * <p>Guards the rebuild and publish sequence, while the read path bypasses it entirely through the
     * {@code volatile} field.
     */
    private final Object lock;

    /**
     * Constructs an info manager with no snapshot yet published.
     *
     * <p>Until the first update {@link #snapshot()} returns an empty result, reflecting a call whose info
     * has not yet been computed.
     */
    public CallInfoManager() {
        this.lock = new Object();
    }

    /**
     * Rebuilds and publishes the call info snapshot from the engine's current running state.
     *
     * <p>Builds a fresh immutable {@link Snapshot} from the supplied state, result, and accumulated
     * durations, marks it valid, and publishes it for lock free reads. The total duration is the sum of
     * the active and lonely durations the engine accumulates as the call leaves the {@link
     * CallLifecycleState#CALL_ACTIVE} and {@link CallLifecycleState#CONNECTED_LONELY} states. A {@code null}
     * link token denotes a call that is not a group call link join.
     *
     * @param state           the call's current internal state; must not be {@code null}
     * @param result          the call's current result; must not be {@code null}
     * @param activeDuration  the accumulated time the call has spent with peer media flowing; must not be
     *                        {@code null}
     * @param lonelyDuration  the accumulated time the call has spent connected without a peer; must not be
     *                        {@code null}
     * @param setupDuration   the time elapsed from call start to the first connected state; must not be
     *                        {@code null}
     * @param linkToken       the call link token, or {@code null} when the call is not a link join
     * @throws NullPointerException if {@code state}, {@code result}, {@code activeDuration},
     *                              {@code lonelyDuration}, or {@code setupDuration} is {@code null}
     */
    public void update(CallLifecycleState state, CallResult result, Duration activeDuration,
                       Duration lonelyDuration, Duration setupDuration, String linkToken) {
        Objects.requireNonNull(state, "state cannot be null");
        Objects.requireNonNull(result, "result cannot be null");
        Objects.requireNonNull(activeDuration, "activeDuration cannot be null");
        Objects.requireNonNull(lonelyDuration, "lonelyDuration cannot be null");
        Objects.requireNonNull(setupDuration, "setupDuration cannot be null");
        synchronized (lock) {
            current = new Snapshot(true, state, result, activeDuration.plus(lonelyDuration),
                    activeDuration, lonelyDuration, setupDuration, linkToken);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "call info snapshot updated, state {0} result {1}", state, result);
        }
    }

    /**
     * Rebuilds and publishes the call info snapshot for a dispatched event, deriving the result from the
     * event when one is implied.
     *
     * <p>Derives the result from the event: a dispatched {@link CallEventType} that resolves to a fresh
     * call result (the relay bind failure, the audio init error, and the video preview failure, each
     * resolving to {@link CallResult#SETUP_ERROR}) overrides the supplied {@code result} with that event
     * derived result; every other event leaves the supplied result intact, including the call concluding
     * events whose result is already recorded on the context before they are dispatched. The remaining
     * inputs are aggregated exactly as in {@link #update(CallLifecycleState, CallResult, Duration,
     * Duration, Duration, String)}.
     *
     * @param event           the dispatched event whose implied result the snapshot reflects; must not be
     *                        {@code null}
     * @param state           the call's current internal state; must not be {@code null}
     * @param result          the call's current result; must not be {@code null}
     * @param activeDuration  the accumulated time the call has spent with peer media flowing; must not be
     *                        {@code null}
     * @param lonelyDuration  the accumulated time the call has spent connected without a peer; must not be
     *                        {@code null}
     * @param setupDuration   the time elapsed from call start to the first connected state; must not be
     *                        {@code null}
     * @param linkToken       the call link token, or {@code null} when the call is not a link join
     * @throws NullPointerException if {@code event}, {@code state}, {@code result}, {@code activeDuration},
     *                              {@code lonelyDuration}, or {@code setupDuration} is {@code null}
     */
    public void updateForEvent(CallEventType event, CallLifecycleState state, CallResult result,
                               Duration activeDuration, Duration lonelyDuration, Duration setupDuration,
                               String linkToken) {
        Objects.requireNonNull(event, "event cannot be null");
        Objects.requireNonNull(result, "result cannot be null");
        update(state, deriveResult(event, result), activeDuration, lonelyDuration, setupDuration, linkToken);
    }

    /**
     * Returns the latest published call info snapshot, if one has been computed.
     *
     * <p>This is a lock free read of the immutable {@link Snapshot} published by the most recent update; it
     * returns an empty result before the first update. The returned snapshot is safe to hand directly to a
     * listener or the host because it is immutable.
     *
     * @return the latest snapshot, or an empty result when none has been computed yet
     */
    public Optional<Snapshot> snapshot() {
        return Optional.ofNullable(current);
    }

    /**
     * Resets the manager so it holds no snapshot.
     *
     * <p>Clears the published snapshot so {@link #snapshot()} reports an empty result again, as when a call
     * context is freed. A later update publishes a fresh snapshot.
     */
    public void reset() {
        synchronized (lock) {
            current = null;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call info snapshot reset");
    }

    /**
     * Derives the snapshot result from a dispatched event, falling back to the supplied result.
     *
     * <p>Three events resolve to an unconditional fresh result: the relay bind failure
     * ({@link CallEventType#RELAY_BINDS_FAILED}), the audio init error ({@link CallEventType#AUDIO_INIT_ERROR}),
     * and the video preview failure ({@link CallEventType#VIDEO_PREVIEW_FAILED}) each map to
     * {@link CallResult#SETUP_ERROR}, so the snapshot carries that failure rather than a stale in progress
     * result. Every other event returns the supplied {@code result} unchanged. The state change events
     * ({@link CallEventType#CALL_STATE_CHANGED}, {@link CallEventType#CALL_OFFER_NACK_RECEIVED}) resolve
     * their result only from context fields this two argument derivation is not given, so their result is
     * already recorded on the supplied {@code result} by the time the event reaches this manager; the call
     * concluding events ({@link CallEventType#CALL_FATAL}, {@link CallEventType#UPDATE_1ON1_CALL_LOG},
     * {@link CallEventType#UPDATE_JOINABLE_CALL_LOG}) likewise have their result recorded on the context by
     * the terminate, reject, accept, and fatal emitter paths before they are dispatched, so they too surface
     * the supplied result unchanged.
     *
     * @param event  the dispatched event
     * @param result the caller supplied current result
     * @return the result the snapshot should carry, never {@code null}
     */
    private static CallResult deriveResult(CallEventType event, CallResult result) {
        return switch (event) {
            case RELAY_BINDS_FAILED, AUDIO_INIT_ERROR, VIDEO_PREVIEW_FAILED -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "call info result overridden to {0} by event {1}",
                            CallResult.SETUP_ERROR, event);
                }
                yield CallResult.SETUP_ERROR;
            }
            default -> result;
        };
    }

    /**
     * Holds one immutable, point in time view of a call's coarse information for listeners and the host.
     *
     * <p>A snapshot aggregates the call's current state and result, the durations the engine accumulates
     * as the call moves through its connected states, the time the call took to set up, and the call link
     * token when the call is a link join. It is the lock free read result of
     * {@link CallInfoManager#snapshot()} and is immutable, so it may be retained and read by any thread
     * after publication.
     *
     * @param valid          whether the snapshot holds computed information, always {@code true} for a
     *                       published snapshot
     * @param state          the call's internal state at snapshot time; never {@code null}
     * @param result         the call's result at snapshot time; never {@code null}
     * @param totalDuration  the sum of {@code activeDuration} and {@code lonelyDuration}; never
     *                       {@code null}
     * @param activeDuration the accumulated time spent with peer media flowing; never {@code null}
     * @param lonelyDuration the accumulated time spent connected without a peer; never {@code null}
     * @param setupDuration  the time from call start to the first connected state; never {@code null}
     * @param linkToken      the call link token, or {@code null} when the call is not a link join
     */
    public record Snapshot(boolean valid, CallLifecycleState state, CallResult result,
                           Duration totalDuration, Duration activeDuration, Duration lonelyDuration,
                           Duration setupDuration, String linkToken) {
        /**
         * Canonicalizes the snapshot, rejecting a null state, result, or duration.
         *
         * @throws NullPointerException if {@code state}, {@code result}, {@code totalDuration},
         *                              {@code activeDuration}, {@code lonelyDuration}, or
         *                              {@code setupDuration} is {@code null}
         */
        public Snapshot {
            Objects.requireNonNull(state, "state cannot be null");
            Objects.requireNonNull(result, "result cannot be null");
            Objects.requireNonNull(totalDuration, "totalDuration cannot be null");
            Objects.requireNonNull(activeDuration, "activeDuration cannot be null");
            Objects.requireNonNull(lonelyDuration, "lonelyDuration cannot be null");
            Objects.requireNonNull(setupDuration, "setupDuration cannot be null");
        }

        /**
         * Returns the call link token as an optional, empty when the call is not a link join.
         *
         * @return the call link token, or an empty result when absent
         */
        public Optional<String> linkTokenOptional() {
            return Optional.ofNullable(linkToken);
        }
    }
}
