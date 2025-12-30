package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.util.DelayedSchedulerUtils;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.*;

public final class WebAppStateBackoffScheduler implements Closeable {
    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60_000;
    private static final int MULTIPLIER = 2;
    private static final long JITTER_MS = 1000;

    private final ConcurrentHashMap<PatchType, CompletableFuture<?>> pendingRetries;

    public WebAppStateBackoffScheduler() {
        this.pendingRetries = new ConcurrentHashMap<>();
    }

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
        var future = DelayedSchedulerUtils.scheduleDelayed(Duration.ofMillis(delayMs), () -> {
            pendingRetries.remove(collectionName);
            retryAction.run();
        });
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

    public boolean cancelRetry(PatchType collectionName) {
        var future = pendingRetries.remove(collectionName);
        if (future != null) {
            future.cancel(false);
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        for (var future : pendingRetries.values()) {
            future.cancel(true);
        }
        pendingRetries.clear();
    }
}
