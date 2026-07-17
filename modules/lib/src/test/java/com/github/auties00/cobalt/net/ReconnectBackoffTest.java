package com.github.auties00.cobalt.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the jitter window, growth, cap, and reset of {@link ReconnectBackoff},
 * using a fixed-seed generator so the exact delays are deterministic.
 */
@DisplayName("ReconnectBackoff")
class ReconnectBackoffTest {
    @Test
    @DisplayName("every delay stays within its equal-jitter window")
    void delaysWithinWindow() {
        var backoff = new ReconnectBackoff(new Random(1));
        var capped = ReconnectBackoff.BASE_MILLIS;
        for (var attempt = 0; attempt < 12; attempt++) {
            var expectedCapped = Math.min(ReconnectBackoff.CAP_MILLIS,
                    (long) (ReconnectBackoff.BASE_MILLIS * Math.pow(ReconnectBackoff.FACTOR, attempt)));
            var delay = backoff.nextDelayMillis();
            assertTrue(delay >= expectedCapped / 2, "delay " + delay + " below floor for attempt " + attempt);
            assertTrue(delay <= expectedCapped, "delay " + delay + " above cap for attempt " + attempt);
            capped = expectedCapped;
        }
        assertEquals(ReconnectBackoff.CAP_MILLIS, capped, "window should reach the cap");
    }

    @Test
    @DisplayName("the window never exceeds the cap once saturated")
    void capIsRespected() {
        var backoff = new ReconnectBackoff(new Random(7));
        for (var i = 0; i < 50; i++) {
            backoff.nextDelayMillis();
        }
        var delay = backoff.nextDelayMillis();
        assertTrue(delay >= ReconnectBackoff.CAP_MILLIS / 2 && delay <= ReconnectBackoff.CAP_MILLIS,
                "saturated delay " + delay + " outside [cap/2, cap]");
    }

    @Test
    @DisplayName("reset returns the window to the base attempt")
    void resetRestartsWindow() {
        var backoff = new ReconnectBackoff(new Random(3));
        for (var i = 0; i < 10; i++) {
            backoff.nextDelayMillis();
        }
        backoff.reset();
        var delay = backoff.nextDelayMillis();
        assertTrue(delay >= ReconnectBackoff.BASE_MILLIS / 2 && delay <= ReconnectBackoff.BASE_MILLIS,
                "post-reset delay " + delay + " not in the base window");
    }

    @Test
    @DisplayName("the same seed yields the same sequence")
    void deterministicWithSeed() {
        var a = new ReconnectBackoff(new Random(42));
        var b = new ReconnectBackoff(new Random(42));
        for (var i = 0; i < 8; i++) {
            assertEquals(a.nextDelayMillis(), b.nextDelayMillis(), "seeded sequences should match at " + i);
        }
    }
}
