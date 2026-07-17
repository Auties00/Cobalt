package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the in-flight admission, re-trigger bookkeeping, and monitor semantics of
 * {@link SyncdCoordinator} that the syncd serialization relies on: a collection already mid-round
 * is excluded and remembered for re-trigger, the monitor is reentrant, and a blocking step runs
 * with the monitor released and the hold count restored. These are the mechanism behind the
 * lost-wakeup and recovery-deadlock fixes, so they are tested directly rather than through a live
 * sync round.
 */
@DisplayName("SyncdCoordinator")
class SyncdCoordinatorTest {
    @Nested
    @DisplayName("admitForRound / clearInFlight")
    class InFlightSet {
        @Test
        @DisplayName("admits collections that are not in flight")
        void admitsFreeCollections() {
            var coordinator = new SyncdCoordinator();
            var admitted = coordinator.admitForRound(Set.of(SyncPatchType.CRITICAL_BLOCK, SyncPatchType.REGULAR));
            assertEquals(Set.of(SyncPatchType.CRITICAL_BLOCK, SyncPatchType.REGULAR), admitted);
            assertTrue(coordinator.isInFlight(SyncPatchType.CRITICAL_BLOCK));
            assertTrue(coordinator.isInFlight(SyncPatchType.REGULAR));
        }

        @Test
        @DisplayName("excludes a collection already in flight")
        void excludesInFlight() {
            var coordinator = new SyncdCoordinator();
            coordinator.admitForRound(Set.of(SyncPatchType.REGULAR));
            var second = coordinator.admitForRound(Set.of(SyncPatchType.REGULAR, SyncPatchType.REGULAR_HIGH));
            assertEquals(Set.of(SyncPatchType.REGULAR_HIGH), second);
        }

        @Test
        @DisplayName("clearInFlight reports re-trigger only for a deferred collection")
        void clearReportsRetrigger() {
            var coordinator = new SyncdCoordinator();
            coordinator.admitForRound(Set.of(SyncPatchType.REGULAR));
            // No second request arrived while in flight: no re-trigger.
            assertFalse(coordinator.clearInFlight(SyncPatchType.CRITICAL_BLOCK));

            coordinator.admitForRound(Set.of(SyncPatchType.REGULAR));
            // A second request for REGULAR arrived while it was in flight (excluded): re-trigger.
            assertTrue(coordinator.clearInFlight(SyncPatchType.REGULAR));
            assertFalse(coordinator.isInFlight(SyncPatchType.REGULAR));
            // The deferral is consumed once.
            assertFalse(coordinator.clearInFlight(SyncPatchType.REGULAR));
        }
    }

    @Nested
    @DisplayName("runLocked")
    class RunLocked {
        @Test
        @DisplayName("returns the body value and is held during the body")
        void returnsValueWhileHeld() {
            var coordinator = new SyncdCoordinator();
            var held = coordinator.runLocked(coordinator::isHeldByCurrentThread);
            assertTrue(held);
            assertFalse(coordinator.isHeldByCurrentThread());
        }

        @Test
        @DisplayName("is reentrant for nested segments")
        void reentrant() {
            var coordinator = new SyncdCoordinator();
            var depth = coordinator.runLocked(() -> coordinator.runLocked(() -> {
                assertTrue(coordinator.isHeldByCurrentThread());
                return 2;
            }));
            assertEquals(2, depth);
        }
    }

    @Nested
    @DisplayName("runWithMonitorReleased")
    class MonitorReleased {
        @Test
        @DisplayName("rejects a call made without the monitor held")
        void rejectsWhenNotHeld() {
            var coordinator = new SyncdCoordinator();
            assertThrows(IllegalStateException.class, () -> coordinator.runWithMonitorReleased(() -> null));
        }

        @Test
        @DisplayName("releases the monitor for the step and restores the hold count")
        void releasesAndRestores() {
            var coordinator = new SyncdCoordinator();
            coordinator.runLocked(() -> {
                assertTrue(coordinator.isHeldByCurrentThread());
                coordinator.runWithMonitorReleased(() -> assertFalse(coordinator.isHeldByCurrentThread()));
                assertTrue(coordinator.isHeldByCurrentThread());
            });
        }

        @Test
        @DisplayName("lets another thread take the monitor during the step")
        void otherThreadProgresses() throws InterruptedException {
            var coordinator = new SyncdCoordinator();
            var acquired = new CountDownLatch(1);
            var counter = new AtomicInteger();
            coordinator.runLocked(() -> {
                var ok = coordinator.runWithMonitorReleased(() -> {
                    Thread.ofVirtual().start(() -> coordinator.runLocked(() -> {
                        counter.incrementAndGet();
                        acquired.countDown();
                    }));
                    try {
                        return acquired.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                });
                assertTrue(ok, "background thread should acquire the monitor while it is released");
            });
            assertEquals(1, counter.get());
        }
    }
}
