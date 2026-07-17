package com.github.auties00.cobalt.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link LiveKeepAliveService}: a failed ping triggers the dead-link
 * action exactly once and stops the loop, while successful pings repeat until
 * stopped. Uses the package-private timing constructor with a short interval so
 * the loop runs fast.
 */
@DisplayName("LiveKeepAliveService")
class LiveKeepAliveServiceTest {
    private static final Duration FAST_INTERVAL = Duration.ofMillis(20);
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @Test
    @DisplayName("a failed ping triggers the dead-link action and stops the loop")
    void deadLinkOnPingFailure() throws InterruptedException {
        var pings = new AtomicInteger();
        var dead = new CountDownLatch(1);
        var keepAlive = new LiveKeepAliveService(
                _ -> {
                    pings.incrementAndGet();
                    throw new IllegalStateException("simulated dead link");
                },
                dead::countDown,
                FAST_INTERVAL,
                TIMEOUT);
        keepAlive.start();
        assertTrue(dead.await(2, TimeUnit.SECONDS), "dead-link action should run");
        var afterDead = pings.get();
        Thread.sleep(200);
        assertEquals(afterDead, pings.get(), "no further pings after a dead link");
        keepAlive.stop();
    }

    @Test
    @DisplayName("healthy pings repeat until stop")
    void pingsWhileHealthy() throws InterruptedException {
        var pings = new AtomicInteger();
        var dead = new AtomicBoolean();
        var keepAlive = new LiveKeepAliveService(
                _ -> pings.incrementAndGet(),
                () -> dead.set(true),
                FAST_INTERVAL,
                TIMEOUT);
        keepAlive.start();
        var deadline = System.currentTimeMillis() + 2_000;
        while (pings.get() < 3 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(pings.get() >= 3, "should ping repeatedly while healthy");
        assertFalse(dead.get(), "no dead-link while healthy");
        keepAlive.stop();
        var afterStop = pings.get();
        Thread.sleep(200);
        assertTrue(pings.get() - afterStop <= 1, "pings stop after stop()");
    }
}
