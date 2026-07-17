package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DecisionLogic operation selection")
class DecisionLogicTest {
    private static DecisionLogic warmedLogic() {
        return warmedLogic(NetEqConfig.defaults());
    }

    private static DecisionLogic warmedLogic(NetEqConfig config) {
        var logic = new DecisionLogic(config);
        // exhaust the warm-up so adaptive decisions are reachable
        for (var i = 0; i < config.numInitialPackets(); i++) {
            logic.decide(new DecisionLogic.Input(100, 100, true, true, false, true, NetEqOperation.NORMAL));
        }
        return logic;
    }

    // The captured production voip_settings pins neteq_allow_time_stretch_acceleration=false, so the
    // accelerate path is suppressed by NetEqConfig.defaults(); the accelerate-path tests enable it to
    // exercise the time-compression branch the gate would otherwise mask.
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

    @Test
    @DisplayName("forces normal decodes during the warm-up window")
    void warmup() {
        var logic = new DecisionLogic(NetEqConfig.defaults());
        var input = new DecisionLogic.Input(2000, 100, true, true, false, true, NetEqOperation.NORMAL);
        // over-full buffer would accelerate, but warm-up forces normal
        assertEquals(NetEqOperation.NORMAL, logic.decide(input));
    }

    @Test
    @DisplayName("decodes normally when the buffer sits near the target")
    void normal() {
        var logic = warmedLogic();
        var input = new DecisionLogic.Input(100, 100, true, true, false, true, NetEqOperation.NORMAL);
        assertEquals(NetEqOperation.NORMAL, logic.decide(input));
    }

    @Test
    @DisplayName("accelerates when the buffer runs over the target and acceleration is enabled")
    void accelerate() {
        var logic = warmedLogic(accelerateEnabled());
        var input = new DecisionLogic.Input(160, 100, true, true, false, true, NetEqOperation.NORMAL);
        assertEquals(NetEqOperation.ACCELERATE, logic.decide(input));
    }

    @Test
    @DisplayName("fast-accelerates when the buffer runs grossly over the target and acceleration is enabled")
    void fastAccelerate() {
        var logic = warmedLogic(accelerateEnabled());
        var input = new DecisionLogic.Input(300, 100, true, true, false, true, NetEqOperation.NORMAL);
        assertEquals(NetEqOperation.FAST_ACCELERATE, logic.decide(input));
    }

    @Test
    @DisplayName("suppresses acceleration with the captured production default, decoding NORMAL instead")
    void accelerateSuppressedByDefault() {
        var logic = warmedLogic();
        var input = new DecisionLogic.Input(300, 100, true, true, false, true, NetEqOperation.NORMAL);
        // NetEqConfig.defaults() pins neteq_allow_time_stretch_acceleration=false, so even a grossly
        // over-full buffer decodes NORMAL rather than accelerating.
        assertEquals(NetEqOperation.NORMAL, logic.decide(input));
    }

    @Test
    @DisplayName("preemptively expands when the buffer runs under the target")
    void preemptiveExpand() {
        var logic = warmedLogic();
        var input = new DecisionLogic.Input(50, 100, true, true, false, true, NetEqOperation.NORMAL);
        assertEquals(NetEqOperation.PREEMPTIVE_EXPAND, logic.decide(input));
    }

    @Test
    @DisplayName("prefers codec PLC over the built-in expander when a gap appears")
    void codecPlcOnGap() {
        var logic = warmedLogic();
        var input = new DecisionLogic.Input(100, 100, true, false, false, true, NetEqOperation.NORMAL);
        assertEquals(NetEqOperation.CODEC_PLC, logic.decide(input));
    }

    @Test
    @DisplayName("expands when a gap appears and the codec has no concealment")
    void expandWithoutCodecPlc() {
        var logic = warmedLogic();
        var input = new DecisionLogic.Input(100, 100, true, false, false, false, NetEqOperation.NORMAL);
        assertEquals(NetEqOperation.EXPAND, logic.decide(input));
    }

    @Test
    @DisplayName("generates comfort noise when the buffer is empty during a silence gap")
    void comfortNoise() {
        var logic = warmedLogic();
        var input = new DecisionLogic.Input(0, 100, false, false, true, true, NetEqOperation.NORMAL);
        assertEquals(NetEqOperation.RFC3389_CNG, logic.decide(input));
    }

    @Test
    @DisplayName("merges the first real packet after a concealment run")
    void mergeAfterExpand() {
        var logic = warmedLogic();
        var input = new DecisionLogic.Input(100, 100, true, true, false, true, NetEqOperation.EXPAND);
        assertEquals(NetEqOperation.MERGE, logic.decide(input));
    }
}
