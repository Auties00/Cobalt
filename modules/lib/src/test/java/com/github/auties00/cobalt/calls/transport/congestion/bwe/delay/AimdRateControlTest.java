package com.github.auties00.cobalt.calls.transport.congestion.bwe.delay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial verification of {@link AimdRateControl} against SPEC 15.1, with the decrease factor as the
 * headline assertion.
 *
 * <p>The shipped {@code concerto::AimdRateControlWA} stores {@code 0.95} at struct offset {@code 0xf8};
 * the upstream WebRTC default is {@code 0.85}. The decrease-step test isolates one
 * {@link BandwidthUsage#OVERUSING} update with the throughput-headroom clamp disabled (non-positive
 * acked throughput) and asserts the estimate is multiplied by exactly {@code 0.95}, NOT {@code 0.85}.
 * The remaining tests pin the additive-vs-multiplicative increase split, the hold semantics, and the
 * range and headroom clamps.
 */
@DisplayName("AimdRateControl additive-increase / multiplicative-decrease")
class AimdRateControlTest {
    /**
     * The decrease factor SPEC 15.1 pins to the shipped concerto build.
     */
    private static final double EXPECTED_BETA = 0.95;

    /**
     * The upstream WebRTC default the shipped build deliberately does NOT use; asserted-against so a
     * regression to it fails loudly.
     */
    private static final double WEBRTC_DEFAULT_BETA = 0.85;

    @Nested
    @DisplayName("decrease step (the beta == 0.95 invariant)")
    class Decrease {
        @Test
        @DisplayName("multiplies the estimate by 0.95 on over-use, not the WebRTC 0.85 default")
        void decreaseBetaIs095() {
            var control = new AimdRateControl(10_000, 10_000_000);
            // Raise the estimate first with a normal increase so the decrease has room to bite.
            control.update(BandwidthUsage.NORMAL, -1, 1_000);
            control.update(BandwidthUsage.NORMAL, -1, 2_000);
            var before = control.currentBitrateBps();
            assertTrue(before > 10_000, "increase must lift the estimate above the floor first");

            // Over-use with acked throughput <= 0 disables the headroom clamp and the capacity re-seed,
            // so the only thing acting on the estimate is the multiplicative decrease.
            var after = control.update(BandwidthUsage.OVERUSING, -1, 3_000);

            var expected = (long) (before * EXPECTED_BETA);
            assertEquals(expected, after, "decrease must apply beta == 0.95");

            var webrtcDefault = (long) (before * WEBRTC_DEFAULT_BETA);
            assertNotEquals(webrtcDefault, after,
                    "decrease must NOT apply the WebRTC default beta == 0.85 (SPEC 15.1)");
        }

        @Test
        @DisplayName("the named decrease-factor constant is exactly 0.95")
        void betaConstant() {
            assertEquals(EXPECTED_BETA, AimdRateControl.DECREASE_BETA);
        }

        @Test
        @DisplayName("repeated over-use compounds the 0.95 factor geometrically")
        void compoundingDecrease() {
            var control = new AimdRateControl(1_000, 100_000_000);
            for (var i = 0; i < 5; i++) {
                control.update(BandwidthUsage.NORMAL, -1, (i + 1) * 1_000L);
            }
            var before = control.currentBitrateBps();
            var b1 = control.update(BandwidthUsage.OVERUSING, -1, 10_000);
            var b2 = control.update(BandwidthUsage.OVERUSING, -1, 11_000);
            assertEquals((long) (before * EXPECTED_BETA), b1);
            assertEquals((long) (b1 * EXPECTED_BETA), b2);
        }
    }

    @Nested
    @DisplayName("increase step")
    class Increase {
        @Test
        @DisplayName("ramps multiplicatively while far below link capacity")
        void multiplicativeWhenFarBelow() {
            var control = new AimdRateControl(100_000, 100_000_000);
            // No link capacity seeded yet => the increase is multiplicative (8%/s capped at one second).
            var start = control.currentBitrateBps();
            var after = control.update(BandwidthUsage.NORMAL, -1, 1_000);
            // First increase has zero elapsed time (lastIncreaseMs seeded this step), so it holds; the
            // second carries a full second of elapsed time and grows by ~8%.
            after = control.update(BandwidthUsage.NORMAL, -1, 2_000);
            assertTrue(after > start, "estimate must increase");
            assertTrue(after <= (long) (start * 1.10), "multiplicative ramp must be bounded near 8%/s, got " + after);
        }

        @Test
        @DisplayName("never decreases the estimate while increasing")
        void increaseIsMonotone() {
            var control = new AimdRateControl(50_000, 100_000_000);
            var prev = control.currentBitrateBps();
            for (var t = 1; t <= 10; t++) {
                var now = control.update(BandwidthUsage.NORMAL, -1, t * 1_000L);
                assertTrue(now >= prev, "increase must be monotone non-decreasing");
                prev = now;
            }
        }
    }

    @Nested
    @DisplayName("hold and clamps")
    class HoldAndClamps {
        @Test
        @DisplayName("under-use holds the estimate unchanged")
        void underUseHolds() {
            var control = new AimdRateControl(100_000, 100_000_000);
            control.update(BandwidthUsage.NORMAL, -1, 1_000);
            control.update(BandwidthUsage.NORMAL, -1, 2_000);
            var held = control.currentBitrateBps();
            var after = control.update(BandwidthUsage.UNDERUSING, -1, 3_000);
            assertEquals(held, after, "under-use must hold the estimate");
        }

        @Test
        @DisplayName("clamps the estimate to the headroom over acknowledged throughput")
        void headroomClamp() {
            var control = new AimdRateControl(100_000, 100_000_000);
            // Drive several increases, then supply a small acked throughput: the estimate is capped at
            // 1.5x that throughput regardless of how high the ramp had climbed.
            for (var t = 1; t <= 20; t++) {
                control.update(BandwidthUsage.NORMAL, -1, t * 1_000L);
            }
            var ackedThroughput = 200_000L;
            var after = control.update(BandwidthUsage.NORMAL, ackedThroughput, 50_000);
            assertTrue(after <= (long) (ackedThroughput * AimdRateControl.MAX_HEADROOM_FACTOR),
                    "estimate must be clamped to 1.5x acked throughput, got " + after);
        }

        @Test
        @DisplayName("never drops below the configured minimum on decrease")
        void minClamp() {
            var control = new AimdRateControl(500_000, 100_000_000);
            for (var i = 0; i < 50; i++) {
                control.update(BandwidthUsage.OVERUSING, -1, i * 1_000L);
            }
            assertTrue(control.currentBitrateBps() >= 500_000, "must not drop below the minimum");
        }

        @Test
        @DisplayName("reset returns the control to the minimum in the hold state")
        void resetRestoresFloor() {
            var control = new AimdRateControl(123_000, 100_000_000);
            control.update(BandwidthUsage.NORMAL, -1, 1_000);
            control.update(BandwidthUsage.NORMAL, -1, 2_000);
            control.reset();
            assertEquals(123_000, control.currentBitrateBps());
            assertTrue(control.linkCapacityEstimateBps() < 0, "capacity tracker must be cleared");
        }
    }
}
