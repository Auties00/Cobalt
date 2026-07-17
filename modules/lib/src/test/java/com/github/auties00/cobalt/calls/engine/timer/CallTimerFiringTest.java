package com.github.auties00.cobalt.calls.engine.timer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial verification that the real {@link CallTimers} virtual-thread driver actually fires a
 * armed timeout at its deadline and that a self-rescheduling callback walks an interval sequence to a
 * terminal teardown, the firing mechanism the P9 caller-lonely and connected-lonely teardown relies on.
 *
 * <p>The P9 timer teardown is a two-part chain: the {@code LiveCallTimerScheduler} arms a real
 * {@link CallTimers.Timer} with a callback that ends the call with {@link com.github.auties00.cobalt.wire.linked.call.CallEndReason#TIMEOUT},
 * and this driver fires that callback at the deadline. That scheduler is package-private to the engine
 * assembler and not constructible here, so this suite pins the half it can reach directly: that the driver
 * fires a one-shot, that a self-rescheduling callback runs the recovered number of times before stopping
 * (the connected-lonely interval walk), and that {@link CallTimers#stop()} cancels a pending timeout
 * so no teardown runs after the call is gone. The teardown's end-reason mapping is asserted against the
 * production controller in {@code GroupCallPlacementTest}.
 */
@DisplayName("CallTimers driver fires lonely/ringing timeouts")
class CallTimerFiringTest {
    @Nested
    @DisplayName("one-shot firing")
    class OneShot {
        @Test
        @DisplayName("a CALLER_LONELY timeout armed with a short delay fires its teardown callback once")
        void callerLonelyFires() throws InterruptedException {
            var timers = new CallTimers("ringing-call");
            try {
                var fired = new CountDownLatch(1);
                // Stand in for the caller-lonely ring watchdog: a short-delay one-shot whose callback ends
                // the call (here, counts down). The production scheduler arms CALLER_LONELY this way and its
                // body calls controller.endCall(callId, TIMEOUT).
                timers.arm(CallTimers.Timer.CALLER_LONELY, Duration.ofMillis(20), fired::countDown);
                assertTrue(fired.await(2, TimeUnit.SECONDS),
                        "the caller-lonely timeout must fire its teardown callback at the deadline");
            } finally {
                timers.stop();
            }
        }

        @Test
        @DisplayName("a fired one-shot does not re-fire on its own")
        void oneShotFiresExactlyOnce() throws InterruptedException {
            var timers = new CallTimers("ringing-call");
            try {
                var count = new AtomicInteger();
                timers.arm(CallTimers.Timer.CALLER_LONELY, Duration.ofMillis(20), count::incrementAndGet);
                Thread.sleep(300);
                assertEquals(1, count.get(), "a one-shot caller-lonely timeout must fire exactly once");
                assertFalse(timers.isArmed(CallTimers.Timer.CALLER_LONELY),
                        "a fired one-shot must not remain armed");
            } finally {
                timers.stop();
            }
        }
    }

    @Nested
    @DisplayName("connected-lonely interval walk")
    class IntervalWalk {
        @Test
        @DisplayName("a self-rescheduling connected-lonely callback walks every interval then ends the call")
        void walksIntervalsThenEnds() throws InterruptedException {
            var timers = new CallTimers("lonely-call");
            try {
                // Reproduce the connected-lonely interval walk the production scheduler runs: arm at a short
                // delay, re-arm for the next interval on each non-final fire, and end (count down) after the
                // last. The production array has DEFAULT_CONNECTED_LONELY_INTERVALS_MS.length intervals.
                var intervalCount = 3;
                var fires = new AtomicInteger();
                var ended = new CountDownLatch(1);
                timers.arm(CallTimers.Timer.CONNECTED_LONELY, Duration.ofMillis(10),
                        walk(timers, fires, ended, intervalCount));
                assertTrue(ended.await(3, TimeUnit.SECONDS),
                        "the connected-lonely walk must reach its terminal teardown");
                assertEquals(intervalCount, fires.get(),
                        "the callback must fire once per interval before the terminal end");
            } finally {
                timers.stop();
            }
        }

        /**
         * Builds the self-rescheduling connected-lonely callback: each non-final fire re-arms the timer for
         * the next interval, and the final fire ends the call, mirroring
         * {@code LiveCallTimerScheduler.connectedLonelyTimeout}'s interval walk.
         */
        private static Runnable walk(CallTimers timers, AtomicInteger fires, CountDownLatch ended,
                                     int intervalCount) {
            return new Runnable() {
                @Override
                public void run() {
                    var index = fires.incrementAndGet();
                    if (index >= intervalCount) {
                        ended.countDown();
                        return;
                    }
                    timers.armIfRunning(CallTimers.Timer.CONNECTED_LONELY, Duration.ofMillis(10), this);
                }
            };
        }
    }

    @Nested
    @DisplayName("teardown cancels pending timeouts")
    class StopCancels {
        @Test
        @DisplayName("stopping the driver before the deadline cancels a pending caller-lonely timeout")
        void stopCancelsPending() throws InterruptedException {
            var timers = new CallTimers("ringing-call");
            var fired = new AtomicInteger();
            // Arm well into the future, then stop immediately: the teardown must cancel the pending timeout
            // so no callback runs after the call is torn down (the cancelAll the controller performs).
            timers.arm(CallTimers.Timer.CALLER_LONELY, Duration.ofSeconds(30), fired::incrementAndGet);
            timers.stop();
            Thread.sleep(100);
            assertEquals(0, fired.get(), "a cancelled timeout must never fire after stop");
        }
    }
}
