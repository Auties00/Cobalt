package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial verification of {@link DecisionLogic} driven by synthetic jitter traces (SPEC 16, NetEq
 * decision logic).
 *
 * <p>Where {@code DecisionLogicTest} pins each single decision, this suite feeds the decision logic a
 * sequence that models a real buffer's evolution over a call: a fill-and-warm-up phase, a build-up that
 * must drain by acceleration, a loss gap that must conceal and then merge, a silence gap that must emit
 * comfort noise, and an under-run that must pre-emptively expand. It asserts on the emitted operation
 * sequence as a whole rather than one decision at a time, which is the property a jitter buffer must hold
 * across a trace. The decision logic itself carries only a warm-up counter, so the buffer span and target
 * are supplied per step to model the trace.
 */
@DisplayName("NetEq DecisionLogic over synthetic jitter traces")
class NetEqDecisionTraceTest {
    /**
     * One step of a synthetic trace: the buffered span and target at this pull plus packet availability.
     */
    private record Step(int bufferSpanMillis, int targetMillis, boolean available, boolean contiguous,
                        boolean comfortNoise) {
    }

    // The captured production voip_settings pins neteq_allow_time_stretch_acceleration=false, so the
    // accelerate path is suppressed by NetEqConfig.defaults(); the build-up trace enables it to exercise
    // the drain-by-acceleration property the gate would otherwise mask.
    private static NetEqConfig accelerateEnabled() {
        var d = NetEqConfig.defaults();
        return new NetEqConfig(d.minDelayMs(), d.maxDelayMs(), d.delayOffsetMs(), d.targetDelayMs(),
                d.initMinE2eDelayMs(), d.dmHistorySizeMs(), d.dlHistorySizeMs(), d.maxHistoryMs(),
                d.underrunQuantile(), d.underrunForgetFactor(), d.reorderForgetFactor(),
                d.reorderStartForgetWeight(), d.enablePeakDetector(), d.smartBufferFlushEnabled(),
                d.bufferFlushMaxLengthMs(), d.maxPacketsInBuffer(), d.use20msGetPeriod(), d.numInitialPackets(),
                d.audioJitbufBufferLowerLimitScalePercent(), d.audioJitbufBufferLimitsWindowSizeMs(),
                d.highThresholdOffsetMs(), d.useMaxDelayInHighThreshold(), true,
                d.allowTimeStretchForHighLatency(), d.allowTimeStretchThresholdMs(), d.enableCodecPlc(),
                d.preexpandWithFilteredLevelPerc(), d.skipNackWithFec(), d.ladEnabledForNack(),
                d.ladEnabledForFec(), d.ladNackExtraInsertTimeMs(), d.nackRttLimitMs(), d.maxNackListSize(),
                d.audioNackMaxSeqReq(), d.enableSpeakerStatus());
    }

    /**
     * Runs a trace through a fresh decision logic, threading each step's previous operation in, and
     * returns the emitted operations in order.
     */
    private static List<NetEqOperation> run(NetEqConfig config, List<Step> trace) {
        var logic = new DecisionLogic(config);
        var ops = new ArrayList<NetEqOperation>(trace.size());
        var last = NetEqOperation.NORMAL;
        for (var step : trace) {
            var op = logic.decide(new DecisionLogic.Input(
                    step.bufferSpanMillis(), step.targetMillis(), step.available(),
                    step.contiguous(), step.comfortNoise(), true, last));
            ops.add(op);
            last = op;
        }
        return ops;
    }

    @Nested
    @DisplayName("warm-up phase")
    class WarmUp {
        @Test
        @DisplayName("the first numInitialPackets decodes are forced NORMAL even with an over-full buffer")
        void warmUpForcesNormal() {
            var config = NetEqConfig.defaults();
            var trace = new ArrayList<Step>();
            // Buffer is grossly over-full from the start; without warm-up this would fast-accelerate.
            for (var i = 0; i < config.numInitialPackets(); i++) {
                trace.add(new Step(1000, 100, true, true, false));
            }
            var ops = run(config, trace);
            assertTrue(ops.stream().allMatch(op -> op == NetEqOperation.NORMAL),
                    "every warm-up decode must be NORMAL, got " + ops);
        }
    }

    @Nested
    @DisplayName("steady state and build-up")
    class SteadyAndBuildUp {
        @Test
        @DisplayName("a steady trace near target decodes NORMAL after warm-up")
        void steadyIsNormal() {
            var config = NetEqConfig.defaults();
            var trace = new ArrayList<Step>();
            for (var i = 0; i < config.numInitialPackets() + 20; i++) {
                trace.add(new Step(100, 100, true, true, false));
            }
            var ops = run(config, trace);
            var steady = ops.subList(config.numInitialPackets(), ops.size());
            assertTrue(steady.stream().allMatch(op -> op == NetEqOperation.NORMAL),
                    "steady near-target trace must stay NORMAL, got " + steady);
        }

        @Test
        @DisplayName("a growing backlog drains via ACCELERATE / FAST_ACCELERATE when acceleration is enabled")
        void backlogAccelerates() {
            var config = accelerateEnabled();
            var trace = new ArrayList<Step>();
            for (var i = 0; i < config.numInitialPackets(); i++) {
                trace.add(new Step(100, 100, true, true, false));
            }
            // Backlog climbs: 1.5x, 2.5x of target -> accelerate then fast-accelerate.
            trace.add(new Step(150, 100, true, true, false));
            trace.add(new Step(260, 100, true, true, false));
            var ops = run(config, trace);
            assertEquals(NetEqOperation.ACCELERATE, ops.get(ops.size() - 2),
                    "a backlog above 1.25x target must accelerate");
            assertEquals(NetEqOperation.FAST_ACCELERATE, ops.get(ops.size() - 1),
                    "a backlog above 2x target must fast-accelerate");
        }

        @Test
        @DisplayName("an under-run below target pre-emptively expands")
        void underRunExpands() {
            var config = NetEqConfig.defaults();
            var trace = new ArrayList<Step>();
            for (var i = 0; i < config.numInitialPackets(); i++) {
                trace.add(new Step(100, 100, true, true, false));
            }
            trace.add(new Step(50, 100, true, true, false)); // 0.5x target
            var ops = run(config, trace);
            assertEquals(NetEqOperation.PREEMPTIVE_EXPAND, ops.get(ops.size() - 1),
                    "a buffer below 0.75x target must pre-emptively expand");
        }
    }

    @Nested
    @DisplayName("loss and silence gaps")
    class Gaps {
        @Test
        @DisplayName("a loss gap conceals then merges the first recovered packet")
        void gapConcealsThenMerges() {
            var config = NetEqConfig.defaults();
            var trace = new ArrayList<Step>();
            for (var i = 0; i < config.numInitialPackets(); i++) {
                trace.add(new Step(100, 100, true, true, false));
            }
            // Two pulls with no contiguous packet (loss), then the packet returns.
            trace.add(new Step(100, 100, true, false, false));
            trace.add(new Step(100, 100, true, false, false));
            trace.add(new Step(100, 100, true, true, false));
            var ops = run(config, trace);
            // Codec PLC is preferred (defaults enable it and codecHasPlc=true here).
            assertEquals(NetEqOperation.CODEC_PLC, ops.get(ops.size() - 3), "first gap pull conceals");
            assertEquals(NetEqOperation.CODEC_PLC, ops.get(ops.size() - 2), "second gap pull conceals");
            assertEquals(NetEqOperation.MERGE, ops.get(ops.size() - 1),
                    "the first packet after concealment must MERGE");
        }

        @Test
        @DisplayName("an empty buffer during a DTX silence gap emits comfort noise")
        void silenceEmitsComfortNoise() {
            var config = NetEqConfig.defaults();
            var trace = new ArrayList<Step>();
            for (var i = 0; i < config.numInitialPackets(); i++) {
                trace.add(new Step(100, 100, true, true, false));
            }
            trace.add(new Step(0, 100, false, false, true)); // empty + comfort-noise gap
            var ops = run(config, trace);
            assertEquals(NetEqOperation.RFC3389_CNG, ops.get(ops.size() - 1),
                    "an empty buffer in a silence gap must emit comfort noise");
        }

        @Test
        @DisplayName("a decision trace never emits the UNDEFINED operation")
        void neverUndefined() {
            var config = NetEqConfig.defaults();
            var trace = new ArrayList<Step>();
            // A varied trace touching every branch.
            for (var i = 0; i < config.numInitialPackets(); i++) {
                trace.add(new Step(100, 100, true, true, false));
            }
            trace.add(new Step(300, 100, true, true, false));
            trace.add(new Step(40, 100, true, true, false));
            trace.add(new Step(100, 100, true, false, false));
            trace.add(new Step(0, 100, false, false, true));
            trace.add(new Step(100, 100, true, true, false));
            var ops = run(config, trace);
            assertFalse(ops.contains(NetEqOperation.UNDEFINED), "no decision may be UNDEFINED, got " + ops);
        }
    }

    @Test
    @DisplayName("reset re-arms the warm-up so the next decodes refill before adapting")
    void resetReArmsWarmUp() {
        var config = NetEqConfig.defaults();
        var logic = new DecisionLogic(config);
        for (var i = 0; i < config.numInitialPackets() + 5; i++) {
            logic.decide(new DecisionLogic.Input(100, 100, true, true, false, true, NetEqOperation.NORMAL));
        }
        logic.reset();
        // After reset an over-full buffer is again forced NORMAL for the warm-up window.
        var op = logic.decide(new DecisionLogic.Input(1000, 100, true, true, false, true, NetEqOperation.NORMAL));
        assertEquals(NetEqOperation.NORMAL, op, "reset must re-arm the warm-up");
    }
}
