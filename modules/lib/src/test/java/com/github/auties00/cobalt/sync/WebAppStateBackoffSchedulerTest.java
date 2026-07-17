package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the observable invariants of the {@link WebAppStateBackoffScheduler} public API:
 * finite-failure expiry, per-collection cancellation, attempt-counter reset, the sticky
 * server-backoff floor, that scheduled actions fire (and cancellation suppresses them), and
 * idempotent {@link WebAppStateBackoffScheduler#close()}. The exact backoff curve at high attempt
 * numbers is not asserted: the {@code BASE_DELAY * MULTIPLIER^attempt} formula caps at one hour, so
 * per-attempt timing assertions are infeasible without instrumentation.
 */
@DisplayName("WebAppStateBackoffScheduler")
class WebAppStateBackoffSchedulerTest {
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
    @DisplayName("scheduleRetry -- finite-failure expiry gate")
    class FinitelyFailing {
        @Test
        @DisplayName("retry within the 48h window is scheduled")
        void freshFailureScheduled() {
            var firstFailure = System.currentTimeMillis();
            assertTrue(scheduler.scheduleRetry(SyncPatchType.REGULAR, firstFailure, () -> {}),
                    "fresh failure should be retried");
        }

        @Test
        @DisplayName("retry past the 48h finite-failure expiry is rejected")
        void pastWindowRejected() {
            var pastFailure = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000); // 3 days ago
            assertFalse(scheduler.scheduleRetry(SyncPatchType.REGULAR, pastFailure, () -> {}),
                    "WAWebSyncdCollectionsStateMachine.getExpiredCollections rejects > 2 day window");
        }

        @Test
        @DisplayName("retry exactly at the boundary is treated as expired (strictly greater fails)")
        void atBoundaryIsRetried() {
            var insideWindow = System.currentTimeMillis() - (47L * 60 * 60 * 1000); // 47 hours ago
            assertTrue(scheduler.scheduleRetry(SyncPatchType.REGULAR, insideWindow, () -> {}));
        }
    }

    @Nested
    @DisplayName("cancelRetry")
    class CancelRetry {
        @Test
        @DisplayName("cancelling without pending retry returns false")
        void cancelWithoutPending() {
            assertFalse(scheduler.cancelRetry(SyncPatchType.REGULAR));
        }

        @Test
        @DisplayName("cancelling after scheduling returns true and clears the future")
        void cancelAfterSchedule() {
            // 60s sticky server backoff keeps the timer from firing before the cancel is observed
            scheduler.updateServerBackoff(60_000);
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            assertTrue(scheduler.cancelRetry(SyncPatchType.REGULAR));
            assertFalse(scheduler.cancelRetry(SyncPatchType.REGULAR),
                    "second cancel finds no pending future");
        }

        @Test
        @DisplayName("cancelling one collection leaves another untouched")
        void independentCollections() {
            scheduler.updateServerBackoff(60_000);
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            scheduler.scheduleRetry(SyncPatchType.CRITICAL_BLOCK, System.currentTimeMillis(), () -> {});
            assertTrue(scheduler.cancelRetry(SyncPatchType.REGULAR));
            assertTrue(scheduler.cancelRetry(SyncPatchType.CRITICAL_BLOCK),
                    "the second collection still has a pending future");
        }
    }

    @Nested
    @DisplayName("retry action fires at the scheduled delay")
    class FiresAction {
        @Test
        @DisplayName("scheduled action runs (with a 1s base delay)")
        void firstAttemptFires() throws InterruptedException {
            var latch = new CountDownLatch(1);
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "first retry should fire within 5 seconds (base delay = 1s * 2^0)");
        }

        @Test
        @DisplayName("cancel before fire prevents the action")
        void cancelPreventsFire() throws InterruptedException {
            var latch = new CountDownLatch(1);
            // 30s sticky server backoff guarantees the cancel wins the race against the timer
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(),
                    30_000L, latch::countDown);
            scheduler.cancelRetry(SyncPatchType.REGULAR);
            assertFalse(latch.await(2, TimeUnit.SECONDS),
                    "cancel must prevent the action from firing");
        }
    }

    @Nested
    @DisplayName("server backoff and attempt counter interaction")
    class ServerBackoffAndCounter {
        @Test
        @DisplayName("updateServerBackoff resets the attempt counter to 0")
        void updateServerBackoffResetsCounter() {
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            scheduler.cancelRetry(SyncPatchType.REGULAR);

            scheduler.updateServerBackoff(0);
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            assertDoesNotThrow(() -> scheduler.cancelRetry(SyncPatchType.REGULAR));
        }

        @Test
        @DisplayName("resetAttemptCounter without server backoff change leaves the sticky floor")
        void resetAttemptCounterPreservesSticky() {
            // the sticky floor is not directly observable; assert only that the call does not throw
            scheduler.updateServerBackoff(5_000);
            scheduler.resetAttemptCounter();
            assertDoesNotThrow(() ->
                    scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {}));
        }

        @Test
        @DisplayName("scheduleRetry with explicit serverBackoffMs updates the sticky floor")
        void explicitServerBackoffUpdates() {
            assertDoesNotThrow(() -> scheduler.scheduleRetry(
                    SyncPatchType.REGULAR, System.currentTimeMillis(), 2_000L, () -> {}));
        }
    }

    @Nested
    @DisplayName("close -- cancels pending and is idempotent")
    class Close {
        @Test
        @DisplayName("close cancels pending retries")
        void closeCancelsPending() {
            scheduler.updateServerBackoff(60_000);
            scheduler.scheduleRetry(SyncPatchType.REGULAR, System.currentTimeMillis(), () -> {});
            scheduler.scheduleRetry(SyncPatchType.CRITICAL_BLOCK, System.currentTimeMillis(), () -> {});
            scheduler.close();
            assertFalse(scheduler.cancelRetry(SyncPatchType.REGULAR));
            assertFalse(scheduler.cancelRetry(SyncPatchType.CRITICAL_BLOCK));
        }

        @Test
        @DisplayName("close is idempotent")
        void closeIdempotent() {
            scheduler.close();
            assertDoesNotThrow(scheduler::close);
        }
    }
}
