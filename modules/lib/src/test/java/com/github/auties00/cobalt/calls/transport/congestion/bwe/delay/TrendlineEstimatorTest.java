package com.github.auties00.cobalt.calls.transport.congestion.bwe.delay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial verification of {@link TrendlineEstimator} against SPEC 15 (GoogCC trendline + over-use
 * detector).
 *
 * <p>The estimator accumulates one-way-delay growth ({@code arrivalDelta - sendDelta}) per packet group,
 * smooths it, and fits a least-squares line over a {@link TrendlineEstimator#WINDOW_SIZE}-sample window.
 * The slope sign is the contract under test: a queue that is filling (arrival spacing wider than send
 * spacing) yields a positive slope and ultimately {@link BandwidthUsage#OVERUSING}; a queue that is
 * draining yields a negative slope and {@link BandwidthUsage#UNDERUSING}; a queue in equilibrium (equal
 * spacing) yields a near-zero slope and {@link BandwidthUsage#NORMAL}. The over-use classification is
 * additionally gated by the duration filter, so these tests drive enough groups past the window fill to
 * exercise both the slope and the detector.
 */
@DisplayName("TrendlineEstimator slope and over-use detection")
class TrendlineEstimatorTest {
    /**
     * Feeds {@code count} packet groups with the given constant send and arrival spacing, advancing the
     * clock by the arrival spacing each group, and returns the last classification.
     */
    private static BandwidthUsage feed(TrendlineEstimator estimator, int count, double sendDeltaMs,
                                       double arrivalDeltaMs) {
        var nowMs = 0L;
        BandwidthUsage usage = BandwidthUsage.NORMAL;
        for (var i = 0; i < count; i++) {
            nowMs += (long) arrivalDeltaMs;
            usage = estimator.update(sendDeltaMs, arrivalDeltaMs, 1000, nowMs);
        }
        return usage;
    }

    @Nested
    @DisplayName("slope sign tracks queue direction")
    class SlopeSign {
        @Test
        @DisplayName("a filling queue (arrival spacing > send spacing) gives a positive slope")
        void positiveSlopeWhenFilling() {
            var estimator = new TrendlineEstimator();
            // Each group arrives 5 ms later than it was sent: monotonically growing one-way delay.
            feed(estimator, TrendlineEstimator.WINDOW_SIZE + 5, 20.0, 25.0);
            assertTrue(estimator.trendSlope() > 0.0,
                    "filling queue must yield a positive trend slope, got " + estimator.trendSlope());
        }

        @Test
        @DisplayName("a draining queue (arrival spacing < send spacing) gives a negative slope")
        void negativeSlopeWhenDraining() {
            var estimator = new TrendlineEstimator();
            // Build a backlog first so the delay can fall, then drain it.
            feed(estimator, TrendlineEstimator.WINDOW_SIZE + 5, 20.0, 25.0);
            feed(estimator, TrendlineEstimator.WINDOW_SIZE + 5, 20.0, 15.0);
            assertTrue(estimator.trendSlope() < 0.0,
                    "draining queue must yield a negative trend slope, got " + estimator.trendSlope());
        }

        @Test
        @DisplayName("an equilibrium queue (equal spacing) gives a near-zero slope and NORMAL")
        void zeroSlopeWhenSteady() {
            var estimator = new TrendlineEstimator();
            var usage = feed(estimator, TrendlineEstimator.WINDOW_SIZE + 10, 20.0, 20.0);
            assertEquals(0.0, estimator.trendSlope(), 1e-9,
                    "steady spacing must yield a zero slope");
            assertEquals(BandwidthUsage.NORMAL, usage);
        }

        @Test
        @DisplayName("the slope stays zero until the regression window is full")
        void slopeZeroBeforeWindowFills() {
            var estimator = new TrendlineEstimator();
            // One sample short of a full window: no regression has run yet.
            feed(estimator, TrendlineEstimator.WINDOW_SIZE - 1, 20.0, 30.0);
            assertEquals(0.0, estimator.trendSlope(), 1e-12,
                    "slope must be zero before the window fills to WINDOW_SIZE");
        }
    }

    @Nested
    @DisplayName("over-use detector")
    class OverUse {
        @Test
        @DisplayName("sustained delay growth eventually classifies OVERUSING")
        void sustainedGrowthOverUses() {
            var estimator = new TrendlineEstimator();
            // A strong, sustained ramp drives the modified trend well past the threshold for long enough
            // to satisfy the duration filter.
            BandwidthUsage last = BandwidthUsage.NORMAL;
            var nowMs = 0L;
            for (var i = 0; i < 200; i++) {
                nowMs += 30;
                last = estimator.update(20.0, 30.0, 1000, nowMs);
                if (last == BandwidthUsage.OVERUSING) {
                    break;
                }
            }
            assertEquals(BandwidthUsage.OVERUSING, last,
                    "a sustained delay ramp must classify OVERUSING");
        }

        @Test
        @DisplayName("a single isolated spike does not trip OVERUSING (duration filter)")
        void singleSpikeDoesNotOverUse() {
            var estimator = new TrendlineEstimator();
            // Steady state to fill the window.
            feed(estimator, TrendlineEstimator.WINDOW_SIZE + 5, 20.0, 20.0);
            // One outlier group, then back to steady: the duration filter must absorb it.
            var nowMs = 10_000L;
            var usage = estimator.update(20.0, 200.0, 1000, nowMs);
            assertTrue(usage != BandwidthUsage.OVERUSING,
                    "a single spike must not immediately classify OVERUSING; got " + usage);
        }

        @Test
        @DisplayName("the threshold stays clamped within [MIN_THRESHOLD, MAX_THRESHOLD]")
        void thresholdClamped() {
            var estimator = new TrendlineEstimator();
            feed(estimator, 300, 20.0, 60.0);
            var t = estimator.threshold();
            assertTrue(t >= TrendlineEstimator.MIN_THRESHOLD && t <= TrendlineEstimator.MAX_THRESHOLD,
                    "threshold " + t + " must stay within ["
                            + TrendlineEstimator.MIN_THRESHOLD + ", " + TrendlineEstimator.MAX_THRESHOLD + "]");
        }
    }

    @Test
    @DisplayName("reset clears the slope, threshold, and state")
    void resetClears() {
        var estimator = new TrendlineEstimator();
        feed(estimator, TrendlineEstimator.WINDOW_SIZE + 5, 20.0, 30.0);
        estimator.reset();
        assertEquals(0.0, estimator.trendSlope(), 1e-12);
        assertEquals(TrendlineEstimator.INITIAL_THRESHOLD, estimator.threshold(), 1e-12);
        assertEquals(BandwidthUsage.NORMAL, estimator.state());
    }
}
