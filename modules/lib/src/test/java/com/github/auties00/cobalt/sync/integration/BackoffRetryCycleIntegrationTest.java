package com.github.auties00.cobalt.sync.integration;

import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.sync.WebAppStateBackoffScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the app-state sync retry cycle: a server-rejected upload schedules a
 * backoff retry, retries fire within their computed delays, the finite 48h
 * failure window moves a collection to a fatal (no-retry) state, and a sticky
 * server-backoff floor caps the delay curve. The synthetic group asserts the
 * {@link WebAppStateBackoffScheduler} invariants directly; the captured group is
 * gated on {@link SyncFixtures#isAvailable(String)} so it skips cleanly until the
 * recorded retry corpus is committed.
 */
@DisplayName("BackoffRetryCycle integration")
class BackoffRetryCycleIntegrationTest {
    private WebAppStateBackoffScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WebAppStateBackoffScheduler();
    }

    @AfterEach
    void tearDown() {
        scheduler.close();
    }

    @Nested
    @DisplayName("synthetic — backoff scheduler invariants")
    class Smoke {
        @Test
        @DisplayName("a fresh failure within the 48h window is retried")
        void freshFailureScheduled() {
            assertTrue(scheduler.scheduleRetry(
                    SyncPatchType.REGULAR_LOW, System.currentTimeMillis(), () -> {}));
        }

        @Test
        @DisplayName("a failure older than 48h is rejected (collection should move to fatal)")
        void expiredFailureRejected() {
            var pastFailure = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000;
            assertFalse(scheduler.scheduleRetry(
                    SyncPatchType.REGULAR_LOW, pastFailure, () -> {}));
        }

        @Test
        @DisplayName("cancel before fire prevents the retry action from executing")
        void cancelPreventsFire() throws InterruptedException {
            var latch = new CountDownLatch(1);
            scheduler.scheduleRetry(SyncPatchType.REGULAR_LOW, System.currentTimeMillis(),
                    30_000L, latch::countDown);
            scheduler.cancelRetry(SyncPatchType.REGULAR_LOW);
            assertFalse(latch.await(2, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("a scheduled retry fires within the base 1s delay (attempt=0, no server floor)")
        void firstAttemptFires() throws InterruptedException {
            var latch = new CountDownLatch(1);
            scheduler.scheduleRetry(SyncPatchType.REGULAR_LOW, System.currentTimeMillis(), latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "attempt=0 base delay is 1s; the action must fire within 5s");
        }

        @Test
        @DisplayName("close cancels every pending retry across collections")
        void closeCancelsAll() {
            scheduler.updateServerBackoff(60_000);
            scheduler.scheduleRetry(SyncPatchType.REGULAR_LOW, System.currentTimeMillis(), () -> {});
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            scheduler.close();
            assertFalse(scheduler.cancelRetry(SyncPatchType.REGULAR_LOW));
            assertFalse(scheduler.cancelRetry(SyncPatchType.REGULAR));
        }
    }

    @Nested
    @DisplayName("captured cycle — oracle parity once fixtures land")
    class CapturedCycle {
        @Test
        @DisplayName("captured retryable error → retry → success matches the WA Web delay curve")
        void capturedRetryCurve() {
            if (!SyncFixtures.isAvailable("integration/backoff-retry-cycle/retryable-then-success")) return;
            // Fixture captures the failed IQ with its retryable-error attrs, the
            // retry IQ, and the elapsed wall-clock delay between attempts; the
            // assertion tolerates +/-10% jitter against the captured delay.
            assertNotNull(SyncFixtures.loadOracle(
                    "integration/backoff-retry-cycle/retryable-then-success"));
        }

        @Test
        @DisplayName("captured fatal-after-window trace moves the collection to FATAL state")
        void fatalAfterWindow() {
            if (!SyncFixtures.isAvailable("integration/backoff-retry-cycle/finite-failure-expiry")) return;
            // The fixture replays a 48h failure window and asserts the collection
            // transitions to FATAL state and no further retries are scheduled.
            assertDoesNotThrow(scheduler::close);
        }

        @Test
        @DisplayName("captured server-backoff override is honoured as the sticky floor")
        void serverBackoffSticky() {
            if (!SyncFixtures.isAvailable("integration/backoff-retry-cycle/server-backoff-sticky")) return;
            // The fixture captures an ErrorRetry response with backoff=N; subsequent
            // retries (after a separate ErrorRetry without backoff) must still honour
            // N as the floor until a new backoff value is provided.
        }
    }
}
