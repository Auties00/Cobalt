package com.github.auties00.cobalt.util;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class DelayedSchedulerUtils {
    private DelayedSchedulerUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static CompletableFuture<Void> scheduleDelayed(Duration delay, Runnable task) {
        var delayedExecutor = CompletableFuture.delayedExecutor(delay.toNanos(), TimeUnit.MILLISECONDS, Thread::startVirtualThread);
        return CompletableFuture.runAsync(task, delayedExecutor);
    }
}
