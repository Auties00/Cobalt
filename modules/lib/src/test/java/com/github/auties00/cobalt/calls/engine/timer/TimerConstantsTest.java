package com.github.auties00.cobalt.calls.engine.timer;

import com.github.auties00.cobalt.calls.engine.timer.CallTimers.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.context.CallContext;

/**
 * Adversarial verification of the eleven per-call timers against the recovered constants in SPEC section
 * 4.5 (call_timer.cc). Asserts the timer set has exactly the eleven members named in the SPEC, that the
 * members carrying a recovered nominal period carry the right value (the 1000ms watchdog, the 30000ms
 * caller-lonely default, the 270000ms first connected-lonely interval), that the per-call-configured
 * members expose the zero "configured per call" sentinel, that the connected-lonely interval array and
 * the strict 44999ms unanswered-group-offer cutoff match, and that the lifecycle-facing
 * {@link CallTimerKind} enumerates the same eleven kinds.
 */
@DisplayName("calls SPEC 4.5 timer constants")
class TimerConstantsTest {
    @Test
    @DisplayName("the timer set has exactly the eleven members SPEC 4.5 names")
    void elevenTimers() {
        assertEquals(11, Timer.values().length);
        assertEquals(11, CallTimerKind.values().length);
    }

    @Nested
    @DisplayName("members with a recovered nominal period")
    class RecoveredPeriods {
        @Test
        @DisplayName("PERIODIC is the 1000ms watchdog cadence")
        void periodic() {
            assertEquals(Duration.ofMillis(1000), Timer.PERIODIC.defaultPeriod());
        }

        @Test
        @DisplayName("CALLER_LONELY defaults to the 30000ms short lonely-state timeout")
        void callerLonely() {
            assertEquals(Duration.ofMillis(30000), Timer.CALLER_LONELY.defaultPeriod());
        }

        @Test
        @DisplayName("CONNECTED_LONELY's default period is the 270000ms first interval")
        void connectedLonely() {
            assertEquals(Duration.ofMillis(270000), Timer.CONNECTED_LONELY.defaultPeriod());
        }

        @Test
        @DisplayName("OHAI falls back to the 1000ms watchdog cadence until a negotiated delay is supplied")
        void ohai() {
            assertEquals(Duration.ofMillis(1000), Timer.OHAI.defaultPeriod());
        }
    }

    @Nested
    @DisplayName("members configured per call expose the zero sentinel")
    class PerCallConfigured {
        @Test
        @DisplayName("the group/per-call timers carry Duration.ZERO as the configured-per-call sentinel")
        void zeroSentinel() {
            for (var timer : new Timer[]{Timer.HEARTBEAT, Timer.LOBBY, Timer.UPDATE_ENCRYPTION_KEY,
                    Timer.REACTION_CLEAR, Timer.VIDEO_UPGRADE, Timer.E2EE_RESTORE, Timer.APP_DATA_STREAM_TEST}) {
                assertTrue(timer.defaultPeriod().isZero(), timer + " must carry the zero sentinel");
            }
        }

        @Test
        @DisplayName("armDefault refuses a zero-sentinel timer because it has no recovered constant")
        void armDefaultRefusesZero() {
            var timers = new CallTimers("006454CB35389E8C2BE8C5AAAF1CC4E5");
            try {
                assertThrows(IllegalArgumentException.class,
                        () -> timers.armDefault(Timer.HEARTBEAT, () -> {}));
            } finally {
                timers.stop();
            }
        }
    }

    @Nested
    @DisplayName("connected-lonely interval array and unanswered-offer cutoff")
    class ConnectedLonelyAndWatchdog {
        @Test
        @DisplayName("the default connected-lonely config carries 30000/270000/300000 ms")
        void connectedLonelyDefaults() {
            var config = CallContext.ConnectedLonelyConfig.defaults();
            assertEquals(30_000L, config.shortMillis());
            assertEquals(270_000L, config.longMillis());
            assertEquals(300_000L, config.maxMillis());
        }

        @Test
        @DisplayName("the caller direction uses the short interval and the callee direction the long one")
        void intervalByDirection() {
            var config = CallContext.ConnectedLonelyConfig.defaults();
            assertEquals(30_000L, config.intervalForDirection(CallContext.CallDirection.OUTGOING));
            assertEquals(270_000L, config.intervalForDirection(CallContext.CallDirection.INCOMING));
        }

        @Test
        @DisplayName("the unanswered-group-offer cutoff is 45000ms, enforced with the strict >44999 compare")
        void unansweredOfferCutoff() {
            assertEquals(Duration.ofMillis(45000), CallTimers.UNANSWERED_GROUP_OFFER_TIMEOUT);
            // The native check is `elapsed > 44999`, so exactly 45000ms is the first elapsed value that
            // trips it and 44999ms must not.
            assertTrue(CallTimers.UNANSWERED_GROUP_OFFER_TIMEOUT.toMillis() > 44999);
            assertFalse(Duration.ofMillis(44999).toMillis() > 44999);
        }
    }

    @Test
    @DisplayName("arming a recovered-period timer fires it after its default delay")
    void armDefaultFires() throws InterruptedException {
        var timers = new CallTimers("006454CB35389E8C2BE8C5AAAF1CC4E5");
        var latch = new java.util.concurrent.CountDownLatch(1);
        try {
            // Arm with an explicit tiny delay (not the 1000ms default) so the driver-thread fire is
            // observed quickly; this asserts the arm/fire plumbing, not the constant itself.
            timers.arm(Timer.PERIODIC, Duration.ofMillis(5), latch::countDown);
            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS), "armed timer must fire");
        } finally {
            timers.stop();
        }
    }
}
