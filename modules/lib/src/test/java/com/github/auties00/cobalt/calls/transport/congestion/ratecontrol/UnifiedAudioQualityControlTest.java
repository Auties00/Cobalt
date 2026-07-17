package com.github.auties00.cobalt.calls.transport.congestion.ratecontrol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial verification of {@link UnifiedAudioQualityControl} signal mapping and the six-state ladder
 * against SPEC 15 (UAQC) and the recovered {@code uaqc_states.cc} handlers.
 *
 * <p>The three hysteresis signals (packet-loss ratio, round-trip time, receiver-estimated-maximum
 * bitrate) each map to a {@link UaqcSignal}, and the aggregate moves the {@link UaqcState} one rung better
 * or worse along the ladder {@link UaqcState#HIGH_QUALITY} (best) to {@link UaqcState#ULTRA_LOW_BANDWIDTH}
 * (worst), with {@link UaqcState#PROBING} as the entry state. The control exponentially smooths each input
 * (alpha defaults to {@code 0.1}, with the first non-zero sample seeding the average directly), so these
 * tests either rely on that direct seed or drive a value hard for several rounds to move the smoothed
 * signal past a threshold. The probing entry state only exits to {@link UaqcState#BANDWIDTH_MANAGED} once
 * the receiver estimate clears the exit threshold; an all-healthy round from {@link UaqcState#BANDWIDTH_MANAGED}
 * then promotes to {@link UaqcState#HIGH_QUALITY}, and the signal-to-state ladder is exercised from there.
 */
@DisplayName("UnifiedAudioQualityControl signal mapping and state ladder")
class UnifiedAudioQualityControlTest {
    private static final UnifiedAudioQualityControl.Config CONFIG =
            UnifiedAudioQualityControl.Config.defaults();

    /**
     * Constructs a control moved out of {@link UaqcState#PROBING} and settled in
     * {@link UaqcState#BANDWIDTH_MANAGED}, with all three smoothed signals seeded healthy.
     *
     * <p>The probing-exit update lands in {@link UaqcState#BANDWIDTH_MANAGED}; the helper stops there so a
     * test can drive a congesting signal before the next all-healthy round would promote to
     * {@link UaqcState#HIGH_QUALITY}.
     */
    private static UnifiedAudioQualityControl bandwidthManaged() {
        var uaqc = new UnifiedAudioQualityControl(CONFIG);
        // First update seeds the EMAs directly; a high remb crosses the probing-exit threshold and the
        // PROBING branch lands the machine in BANDWIDTH_MANAGED for exactly this round.
        var state = uaqc.update(0.0, 20.0, 5_000_000, 0);
        assertSame(UaqcState.BANDWIDTH_MANAGED, state, "precondition: out of probing");
        return uaqc;
    }

    @Nested
    @DisplayName("probing entry/exit")
    class Probing {
        @Test
        @DisplayName("starts in PROBING")
        void startsProbing() {
            var uaqc = new UnifiedAudioQualityControl(CONFIG);
            assertSame(UaqcState.PROBING, uaqc.state());
        }

        @Test
        @DisplayName("stays in PROBING while the receiver estimate is below the exit threshold")
        void staysProbingBelowExit() {
            var uaqc = new UnifiedAudioQualityControl(CONFIG);
            var state = uaqc.update(0.0, 20.0, CONFIG.probingExitRembBps() - 1, 0);
            assertSame(UaqcState.PROBING, state);
        }

        @Test
        @DisplayName("exits PROBING to BANDWIDTH_MANAGED when the receiver estimate clears the exit threshold")
        void exitsProbingAboveExit() {
            var uaqc = new UnifiedAudioQualityControl(CONFIG);
            var state = uaqc.update(0.0, 20.0, CONFIG.probingExitRembBps() + 1, 0);
            assertSame(UaqcState.BANDWIDTH_MANAGED, state);
        }

        @Test
        @DisplayName("reports a non-zero FEC overhead while probing under loss and clears it once managed")
        void probingFecOverhead() {
            var uaqc = new UnifiedAudioQualityControl(CONFIG);
            // Probe with loss present: the first sample seeds plrEma directly to 0.20.
            uaqc.update(0.20, 20.0, 10_000, 0);
            assertTrue(uaqc.fecOverheadFraction() > 0.0, "probing under loss must fund FEC overhead");
            assertTrue(uaqc.fecOverheadFraction() <= CONFIG.probingFecOverheadPct() / 100.0,
                    "FEC overhead is capped at the configured probing percentage");
            // Now clear probing; managed states carry no probing FEC overhead.
            uaqc.update(0.0, 20.0, CONFIG.probingExitRembBps() + 1, 1_000);
            assertSame(UaqcState.BANDWIDTH_MANAGED, uaqc.state());
            assertEquals(0.0, uaqc.fecOverheadFraction(), 1e-12);
        }
    }

    @Nested
    @DisplayName("high-quality promotion")
    class HighQuality {
        @Test
        @DisplayName("a sustained healthy link promotes BANDWIDTH_MANAGED -> HIGH_QUALITY")
        void healthyPromotesToHighQuality() {
            var uaqc = bandwidthManaged();
            var nowMs = 1_000L;
            UaqcState state = uaqc.state();
            // All three signals non-congested: the BANDWIDTH_MANAGED handler promotes to the better state.
            for (var i = 0; i < 40 && state != UaqcState.HIGH_QUALITY; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, 20.0, 5_000_000, nowMs);
            }
            assertSame(UaqcState.HIGH_QUALITY, state,
                    "an all-healthy link must promote from BANDWIDTH_MANAGED to HIGH_QUALITY");
        }

        @Test
        @DisplayName("congestion from HIGH_QUALITY steps back down to BANDWIDTH_MANAGED")
        void congestionLeavesHighQuality() {
            var uaqc = bandwidthManaged();
            var nowMs = 1_000L;
            UaqcState state = uaqc.state();
            for (var i = 0; i < 40 && state != UaqcState.HIGH_QUALITY; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, 20.0, 5_000_000, nowMs);
            }
            assertSame(UaqcState.HIGH_QUALITY, state);
            // Drive RTT hard: any congested signal steps HIGH_QUALITY down to BANDWIDTH_MANAGED.
            for (var i = 0; i < 80 && state != UaqcState.BANDWIDTH_MANAGED; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, CONFIG.highQuality().rttAbsoluteThresholdMs() * 3, 5_000_000, nowMs);
            }
            assertSame(UaqcState.BANDWIDTH_MANAGED, state,
                    "congestion from HIGH_QUALITY must step down to BANDWIDTH_MANAGED");
        }
    }

    @Nested
    @DisplayName("packet-loss-ratio signal")
    class PlrSignal {
        @Test
        @DisplayName("high sustained loss with healthy RTT moves BANDWIDTH_MANAGED -> LOSSY")
        void highLossEntersLossy() {
            var uaqc = bandwidthManaged();
            // Push loss above the upper threshold while RTT stays low; plr congested, rtt not congested.
            UaqcState state = uaqc.state();
            var nowMs = 1_000L;
            for (var i = 0; i < 40 && state != UaqcState.LOSSY; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.5, 20.0, 5_000_000, nowMs);
            }
            assertSame(UaqcState.LOSSY, state,
                    "loss congested while RTT healthy must enter LOSSY (random-loss regime)");
        }

        @Test
        @DisplayName("loss recovery moves LOSSY -> BANDWIDTH_MANAGED")
        void lossRecoveryLeavesLossy() {
            var uaqc = bandwidthManaged();
            var nowMs = 1_000L;
            UaqcState state = uaqc.state();
            for (var i = 0; i < 40 && state != UaqcState.LOSSY; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.5, 20.0, 5_000_000, nowMs);
            }
            assertSame(UaqcState.LOSSY, state);
            // Now drop loss to zero: plr becomes non-congested and the LOSSY handler recovers to managed.
            for (var i = 0; i < 60 && state != UaqcState.BANDWIDTH_MANAGED; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, 20.0, 5_000_000, nowMs);
            }
            assertSame(UaqcState.BANDWIDTH_MANAGED, state, "loss recovery must leave LOSSY");
        }
    }

    @Nested
    @DisplayName("round-trip-time signal")
    class RttSignal {
        @Test
        @DisplayName("RTT above the absolute threshold drives congestion into DRAIN")
        void highRttDrains() {
            var uaqc = bandwidthManaged();
            var nowMs = 1_000L;
            UaqcState state = uaqc.state();
            // Drive RTT hard above the absolute threshold; rtt congested with loss healthy => DRAIN.
            for (var i = 0; i < 80 && state != UaqcState.DRAIN; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, CONFIG.bandwidthManaged().rttAbsoluteThresholdMs() * 3, 5_000_000, nowMs);
            }
            assertSame(UaqcState.DRAIN, state,
                    "RTT congestion (not loss) from BANDWIDTH_MANAGED must enter DRAIN");
        }
    }

    @Nested
    @DisplayName("receiver-estimate signal")
    class RembSignal {
        @Test
        @DisplayName("a collapsing receiver estimate drives congestion into DRAIN")
        void lowRembDrains() {
            var uaqc = bandwidthManaged();
            var nowMs = 1_000L;
            UaqcState state = uaqc.state();
            // Collapse remb below the lower threshold; remb congested, loss healthy => DRAIN.
            for (var i = 0; i < 120 && state != UaqcState.DRAIN; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, 20.0, CONFIG.bandwidthManaged().rembLowerBps() / 2, nowMs);
            }
            assertSame(UaqcState.DRAIN, state,
                    "a receiver estimate below the lower threshold must enter DRAIN");
        }

        @Test
        @DisplayName("DRAIN recovers to BANDWIDTH_MANAGED once all signals clear")
        void drainRecovers() {
            var uaqc = bandwidthManaged();
            var nowMs = 1_000L;
            UaqcState state = uaqc.state();
            for (var i = 0; i < 120 && state != UaqcState.DRAIN; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, 20.0, CONFIG.bandwidthManaged().rembLowerBps() / 2, nowMs);
            }
            assertSame(UaqcState.DRAIN, state);
            // Restore a healthy estimate and let the EMA climb back so remb reads non-congested.
            for (var i = 0; i < 200 && state != UaqcState.BANDWIDTH_MANAGED; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, 20.0, 5_000_000, nowMs);
            }
            assertSame(UaqcState.BANDWIDTH_MANAGED, state, "DRAIN must recover when congestion clears");
        }

        @Test
        @DisplayName("sustained congestion from DRAIN steps down to ULTRA_LOW_BANDWIDTH")
        void drainWorsensToUltraLow() {
            var uaqc = bandwidthManaged();
            var nowMs = 1_000L;
            UaqcState state = uaqc.state();
            // Collapse remb hard and hold it: BANDWIDTH_MANAGED -> DRAIN, then DRAIN -> ULTRA_LOW_BANDWIDTH.
            for (var i = 0; i < 200 && state != UaqcState.ULTRA_LOW_BANDWIDTH; i++) {
                nowMs += 1_000;
                state = uaqc.update(0.0, 20.0, CONFIG.ultraLowBandwidth().rembLowerBps() / 2, nowMs);
            }
            assertSame(UaqcState.ULTRA_LOW_BANDWIDTH, state,
                    "sustained congestion from DRAIN must step down to ULTRA_LOW_BANDWIDTH");
        }
    }

    @Nested
    @DisplayName("per-state target bitrate")
    class TargetBitrate {
        @Test
        @DisplayName("each state reports its configured target bitrate")
        void targetBitratePerState() {
            var uaqc = new UnifiedAudioQualityControl(CONFIG);
            assertEquals(CONFIG.probingTargetBps(), uaqc.targetBitrateBps(),
                    "probing reports the probing target");
            uaqc.update(0.0, 20.0, CONFIG.probingExitRembBps() + 1, 0);
            assertEquals(CONFIG.bandwidthManagedTargetBps(), uaqc.targetBitrateBps(),
                    "managed reports the managed target");
        }
    }

    @Test
    @DisplayName("a non-positive RTT sample is ignored by the estimator and does not force congestion")
    void nonPositiveRttIgnored() {
        var uaqc = bandwidthManaged();
        // Feeding a zero RTT must not throw and must not force a worse state; with the other signals
        // healthy the machine promotes toward HIGH_QUALITY rather than congesting on the ignored sample.
        var state = uaqc.update(0.0, 0.0, 5_000_000, 4_000);
        assertTrue(state == UaqcState.BANDWIDTH_MANAGED || state == UaqcState.HIGH_QUALITY,
                "a zero RTT must be ignored, not drive the state worse");
    }
}
