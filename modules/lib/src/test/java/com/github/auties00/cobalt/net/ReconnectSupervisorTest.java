package com.github.auties00.cobalt.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link ReconnectSupervisor} loop: retry-until-connected, offline
 * gating via the connectivity monitor, immediate retry on connectivity
 * regained, request coalescing, and that the terminated and cancelled gates
 * stop attempts. Uses a {@link TestNetworkConnectivityMonitor} and a fake
 * connect attempt so no real socket is involved; waits are bounded by short
 * polling deadlines.
 */
@DisplayName("ReconnectSupervisor")
class ReconnectSupervisorTest {
    /**
     * A connect attempt that fails a fixed number of times then succeeds (or
     * always fails when {@code failuresBeforeSuccess < 0}), tracking the call
     * count and the resulting connected state.
     */
    private static final class FakeAttempt implements ConnectAttempt {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicBoolean connected = new AtomicBoolean();
        private final int failuresBeforeSuccess;

        FakeAttempt(int failuresBeforeSuccess) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        public void run() throws IOException {
            var n = calls.incrementAndGet();
            if (failuresBeforeSuccess < 0 || n <= failuresBeforeSuccess) {
                throw new IOException("simulated failure " + n);
            }
            connected.set(true);
        }
    }

    private static boolean awaitTrue(java.util.function.BooleanSupplier condition, long timeoutMillis) {
        var deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }

    @Test
    @DisplayName("succeeds on the first attempt when online")
    void connectsImmediately() {
        var attempt = new FakeAttempt(0);
        var monitor = new TestNetworkConnectivityMonitor(true);
        var supervisor = new ReconnectSupervisor(attempt, attempt.connected::get, () -> false, monitor, new Random(1));
        supervisor.requestReconnect();
        assertTrue(awaitTrue(attempt.connected::get, 2_000), "should connect");
        assertEquals(1, attempt.calls.get(), "exactly one attempt");
        supervisor.cancel();
    }

    @Test
    @DisplayName("retries with backoff until a later attempt succeeds")
    void retriesUntilSuccess() {
        var attempt = new FakeAttempt(1);
        var monitor = new TestNetworkConnectivityMonitor(true);
        var supervisor = new ReconnectSupervisor(attempt, attempt.connected::get, () -> false, monitor, new Random(1));
        supervisor.requestReconnect();
        assertTrue(awaitTrue(attempt.connected::get, 5_000), "should connect after retrying");
        assertEquals(2, attempt.calls.get(), "one failure then one success");
        supervisor.cancel();
    }

    @Test
    @DisplayName("does not attempt while the host is offline, then fires when it returns")
    void gatesOnConnectivity() {
        var attempt = new FakeAttempt(0);
        var monitor = new TestNetworkConnectivityMonitor(false);
        var supervisor = new ReconnectSupervisor(attempt, attempt.connected::get, () -> false, monitor, new Random(1));
        supervisor.requestReconnect();
        // While offline, no attempt should be made
        assertTrue(awaitTrue(() -> attempt.calls.get() == 0, 400) && attempt.calls.get() == 0,
                "no attempt while offline");
        monitor.setOnline(true);
        assertTrue(awaitTrue(attempt.connected::get, 2_000), "connects once online");
        assertEquals(1, attempt.calls.get(), "one attempt after coming online");
        supervisor.cancel();
    }

    @Test
    @DisplayName("cancel stops further attempts")
    void cancelStops() throws InterruptedException {
        var attempt = new FakeAttempt(-1);
        var monitor = new TestNetworkConnectivityMonitor(true);
        var supervisor = new ReconnectSupervisor(attempt, attempt.connected::get, () -> false, monitor, new Random(1));
        supervisor.requestReconnect();
        // Let the first attempt run and the supervisor enter its first backoff sleep
        assertTrue(awaitTrue(() -> attempt.calls.get() >= 1, 1_000), "at least one attempt");
        supervisor.cancel();
        var afterCancel = attempt.calls.get();
        Thread.sleep(1_500);
        assertTrue(attempt.calls.get() - afterCancel <= 1, "attempts should stop after cancel");
    }

    @Test
    @DisplayName("terminated gate stops further attempts")
    void terminatedStops() throws InterruptedException {
        var attempt = new FakeAttempt(-1);
        var monitor = new TestNetworkConnectivityMonitor(true);
        var terminated = new AtomicBoolean(false);
        var supervisor = new ReconnectSupervisor(attempt, attempt.connected::get, terminated::get, monitor, new Random(1));
        supervisor.requestReconnect();
        assertTrue(awaitTrue(() -> attempt.calls.get() >= 1, 1_000), "at least one attempt");
        terminated.set(true);
        var afterTerminate = attempt.calls.get();
        Thread.sleep(1_500);
        assertTrue(attempt.calls.get() - afterTerminate <= 1, "attempts should stop once terminated");
        supervisor.cancel();
    }
}
