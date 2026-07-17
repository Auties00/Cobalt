package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.util.ScheduledTask;

import java.io.Closeable;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Schedules retries of failed syncd patch round-trips with exponential
 * backoff.
 *
 * <p>Retries are driven by {@link WebAppStateService} after a server
 * round-trip lands a transient error. The scheduler is a separate class so
 * tests and integration cycles can drive its observable invariants
 * (finite-failure expiry, sticky server backoff, attempt-counter reset)
 * without booting the rest of the syncd stack. The delay curve is
 * {@code min(max(BASE_DELAY * MULTIPLIER^attempt, serverBackoff), MAX_DELAY)}
 * backed by two pieces of state: a single global attempt counter
 * ({@link #globalAttemptCounter}) shared across every collection, and a
 * sticky server-suggested floor ({@link #stickyServerBackoffMs}) that
 * survives across retries until an {@code ErrorRetry} response overwrites it.
 * Once {@code firstFailureTimestamp} is more than
 * {@value #FINITE_FAILURE_EXPIRY_MS} ms in the past, the collection is
 * considered fatally broken and {@link #scheduleRetry(SyncPatchType, long, Runnable)}
 * refuses to reschedule, leaving the caller to escalate to a fatal state.
 *
 * @implNote This implementation uses the constants {@code BASE_DELAY=1s},
 * {@code MULTIPLIER=2}, {@code MAX_DELAY=1h}, and a finite-failure expiry of
 * {@value #FINITE_FAILURE_EXPIRY_MS} ms (2 days). The global attempt counter
 * is shared across every collection rather than being tracked per collection.
 */
public final class WebAppStateBackoffScheduler implements Closeable {
    /**
     * The logger for {@link WebAppStateBackoffScheduler}.
     */
    private static final System.Logger LOGGER = Log.get(WebAppStateBackoffScheduler.class);

    /**
     * The base delay applied at attempt zero, in milliseconds.
     */
    private static final long BASE_DELAY_MS = 1000;

    /**
     * The hard ceiling for any backoff delay, in milliseconds (one hour).
     */
    private static final long MAX_DELAY_MS = 3_600_000;

    /**
     * The exponential growth factor applied per attempt.
     */
    private static final int MULTIPLIER = 2;

    /**
     * The cumulative time a collection may stay in the finite-retry state
     * before being rejected as expired, in milliseconds (two days).
     */
    private static final long FINITE_FAILURE_EXPIRY_MS = 2 * 24 * 60 * 60 * 1000L;

    /**
     * The map of in-flight per-collection retry handles, used to cancel
     * pending retries on {@link #cancelRetry(SyncPatchType)} or
     * {@link #close()}.
     */
    private final ConcurrentHashMap<SyncPatchType, ScheduledTask> pendingRetries;

    /**
     * The global attempt counter shared across every collection.
     */
    private final AtomicInteger globalAttemptCounter;

    /**
     * The sticky server-suggested backoff floor in milliseconds.
     */
    private final AtomicLong stickyServerBackoffMs;

    /**
     * Builds a scheduler with both the attempt counter and the sticky server
     * backoff initialised to zero.
     *
     * <p>Constructed once by {@link WebAppStateService}; the scheduler
     * outlives every individual sync round and is closed when the parent
     * service is closed.
     */
    public WebAppStateBackoffScheduler() {
        this.pendingRetries = new ConcurrentHashMap<>();
        this.globalAttemptCounter = new AtomicInteger(0);
        this.stickyServerBackoffMs = new AtomicLong(0);
    }

    /**
     * Schedules a retry for {@code collectionName}, optionally updating the
     * sticky server-suggested backoff before computing the delay.
     *
     * <p>This is the overload used by callers that have just received an
     * {@code ErrorRetry} server response and already know the server's
     * suggested backoff value. When {@code serverBackoffMs} is non-{@code null}
     * it overwrites the sticky floor before delegating to
     * {@link #scheduleRetry(SyncPatchType, long, Runnable)}; passing
     * {@code null} leaves the existing floor untouched.
     *
     * @param collectionName        the collection whose sync round failed
     * @param firstFailureTimestamp the wall-clock millisecond timestamp at
     *                              which the current finite-retry series
     *                              began, used by the expiry gate
     * @param serverBackoffMs       the server-suggested backoff floor in
     *                              milliseconds, or {@code null} to keep the
     *                              existing sticky value
     * @param retryAction           the action invoked on the timer thread
     *                              once the backoff elapses
     * @return {@code true} when the retry was scheduled, {@code false} when
     *         the finite-failure window had already expired
     */
    public boolean scheduleRetry(SyncPatchType collectionName, long firstFailureTimestamp, Long serverBackoffMs, Runnable retryAction) {
        if (serverBackoffMs != null) {
            stickyServerBackoffMs.set(serverBackoffMs);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "sync retry backoff floor updated to {0} ms for {1}", serverBackoffMs, collectionName);
            }
        }
        return scheduleRetry(collectionName, firstFailureTimestamp, retryAction);
    }

    /**
     * Schedules a retry for {@code collectionName} using the current sticky
     * server backoff floor.
     *
     * <p>Called by the syncd retry loop after every transient failure. The
     * action fires asynchronously on a {@link ScheduledTask} virtual thread
     * once the computed backoff elapses, and can be cancelled through
     * {@link #cancelRetry(SyncPatchType)}. Any pending retry on the same
     * collection is cancelled before scheduling, so the second call wins.
     *
     * @implNote This implementation reads-and-increments
     * {@link #globalAttemptCounter} atomically, then computes
     * {@code min(max(BASE * MULTIPLIER^attempt, sticky), MAX)} as the delay.
     *
     * @param collectionName        the collection whose sync round failed
     * @param firstFailureTimestamp the wall-clock millisecond timestamp at
     *                              which the current finite-retry series began
     * @param retryAction           the action invoked on the timer thread
     *                              once the backoff elapses
     * @return {@code true} when the retry was scheduled, {@code false} when
     *         {@code firstFailureTimestamp} is more than
     *         {@value #FINITE_FAILURE_EXPIRY_MS} ms in the past
     */
    public boolean scheduleRetry(SyncPatchType collectionName, long firstFailureTimestamp, Runnable retryAction) {
        var elapsed = System.currentTimeMillis() - firstFailureTimestamp;
        if (elapsed > FINITE_FAILURE_EXPIRY_MS) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "sync retry window expired for {0} after {1} ms, giving up", collectionName, elapsed);
            }
            return false;
        }

        cancelRetry(collectionName);

        var attempt = globalAttemptCounter.getAndIncrement();
        var delayMs = calculateBackoff(attempt, stickyServerBackoffMs.get());
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "scheduling sync retry for {0}: attempt={1} delay={2} ms", collectionName, attempt, delayMs);
        }

        var handle = ScheduledTask.scheduleDelayed(Duration.ofMillis(delayMs), () -> {
            pendingRetries.remove(collectionName);
            retryAction.run();
        });
        pendingRetries.put(collectionName, handle);

        return true;
    }

    /**
     * Stores a new sticky server-backoff floor and resets the global attempt
     * counter to zero.
     *
     * <p>Called by the syncd response handler when the server returns an
     * {@code ErrorRetry} verdict carrying a fresh {@code serverBackoff} value.
     * The new floor stays in effect for every subsequent retry until the next
     * {@code ErrorRetry} response or an explicit {@link #close()}.
     *
     * @param serverBackoffMs the server-suggested backoff floor in
     *                        milliseconds; {@code 0} disables the floor
     */
    public void updateServerBackoff(long serverBackoffMs) {
        stickyServerBackoffMs.set(serverBackoffMs);
        globalAttemptCounter.set(0);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sync server backoff floor reset to {0} ms", serverBackoffMs);
        }
    }

    /**
     * Computes the backoff delay for the supplied attempt and floor.
     *
     * <p>The result is {@code min(max(BASE_DELAY * MULTIPLIER^attempt,
     * serverBackoff), MAX_DELAY)}; the cap at {@link #MAX_DELAY_MS} applies
     * whether the dominant term is the exponential growth or the sticky floor.
     *
     * @param attemptNumber the attempt index (zero-based)
     * @param serverBackoff the sticky server backoff floor in milliseconds
     * @return the resulting delay in milliseconds
     */
    private long calculateBackoff(int attemptNumber, long serverBackoff) {
        var computed = (long) (BASE_DELAY_MS * Math.pow(MULTIPLIER, attemptNumber));
        var delay = Math.max(computed, serverBackoff);
        return Math.min(delay, MAX_DELAY_MS);
    }

    /**
     * Cancels any pending retry for {@code collectionName}.
     *
     * <p>Called by {@link #scheduleRetry(SyncPatchType, long, Runnable)}
     * before scheduling a new retry, and by callers that want to drop a
     * pending retry on demand (for example after a successful sync of the
     * same collection through a different path).
     *
     * @param collectionName the collection whose pending retry, if any,
     *                       should be cancelled
     * @return {@code true} when a pending retry existed and was cancelled,
     *         {@code false} otherwise
     */
    public boolean cancelRetry(SyncPatchType collectionName) {
        var handle = pendingRetries.remove(collectionName);
        if (handle != null) {
            handle.cancel();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "cancelled pending sync retry for {0}", collectionName);
            }
            return true;
        }
        return false;
    }

    /**
     * Resets the global attempt counter to zero without touching the sticky
     * server backoff.
     *
     * <p>Called by {@link WebAppStateService} after a successful sync round;
     * future retries restart from {@code BASE_DELAY * MULTIPLIER^0} but still
     * honour the last sticky server-suggested floor.
     */
    public void resetAttemptCounter() {
        globalAttemptCounter.set(0);
    }

    /**
     * Force-cancels every pending retry, empties the pending-retry map, and
     * resets both the attempt counter and the sticky server backoff to zero.
     *
     * <p>The method is safe to call repeatedly: subsequent invocations simply
     * find an empty map and idempotent counter assignments.
     *
     * @implNote This implementation cancels each pending retry, which interrupts
     * a retry whose backoff is still elapsing or whose action is already running.
     */
    @Override
    public void close() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "closing sync backoff scheduler: {0} pending retries cancelled", pendingRetries.size());
        }
        for (var handle : pendingRetries.values()) {
            handle.cancel();
        }
        pendingRetries.clear();
        globalAttemptCounter.set(0);
        stickyServerBackoffMs.set(0);
    }
}
