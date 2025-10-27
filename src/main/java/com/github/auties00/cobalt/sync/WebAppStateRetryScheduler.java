package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.model.proto.sync.PatchType;

import java.io.Closeable;
import java.util.concurrent.*;

/**
 * Schedules sync retry operations with exponential backoff.
 *
 * <p>Features:
 * <ul>
 *   <li>Exponential backoff with jitter</li>
 *   <li>Automatic retry cancellation when max retries reached</li>
 *   <li>Per-collection retry tracking</li>
 *   <li>Thread-safe retry management</li>
 * </ul>
 */
public final class WebAppStateRetryScheduler implements Closeable {
    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60_000;
    private static final int MULTIPLIER = 2;
    private static final long JITTER_MS = 1000;

    private final ConcurrentHashMap<PatchType, CompletableFuture<?>> pendingRetries;

    /**
     * Creates a new SyncRetryScheduler.
     */
    public WebAppStateRetryScheduler() {
        this.pendingRetries = new ConcurrentHashMap<>();
    }

    /**
     * Schedules a retry for a collection.
     *
     * <p>If a retry is already scheduled for this collection, it will be cancelled
     * and replaced with the new retry.
     *
     * @param collectionName the collection to retry
     * @param attemptNumber the retry attempt number (0-based)
     * @param retryAction the action to execute on retry
     * @return true if retry was scheduled, false if max retries exceeded
     */
    public boolean scheduleRetry(PatchType collectionName, int attemptNumber, Runnable retryAction) {
        // Check if we should retry
        if (attemptNumber >= MAX_RETRIES) {
            return false;
        }

        // Cancel any existing retry for this collection
        cancelRetry(collectionName);

        // Calculate backoff delay
        var delayMs = calculateBackoff(attemptNumber);

        // Schedule the retry
        var delayedExecutor = CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS, Thread::startVirtualThread);
        var future = CompletableFuture.runAsync(() -> {
            pendingRetries.remove(collectionName);
            retryAction.run();
        }, delayedExecutor);

        pendingRetries.put(collectionName, future);
        return true;
    }

    private long calculateBackoff(int attemptNumber) {
        if (attemptNumber < 0) {
            throw new IllegalArgumentException("Attempt number cannot be negative");
        }

        // Calculate exponential delay
        var delay = (long) (BASE_DELAY_MS * Math.pow(MULTIPLIER, attemptNumber));

        // Cap at maximum delay
        delay = Math.min(delay, MAX_DELAY_MS);

        // Add random jitter to prevent thundering herd
        delay += (long) (Math.random() * JITTER_MS);

        return delay;
    }

    /**
     * Cancels a pending retry for a collection.
     *
     * @param collectionName the collection name
     * @return true if a retry was cancelled, false if no retry was pending
     */
    public boolean cancelRetry(PatchType collectionName) {
        var future = pendingRetries.remove(collectionName);
        if (future != null) {
            future.cancel(false);
            return true;
        }
        return false;
    }

    /**
     * Shuts down the scheduler and cancels all pending retries.
     */
    @Override
    public void close() {
        for (var future : pendingRetries.values()) {
            future.cancel(true);
        }
        pendingRetries.clear();
    }
}
