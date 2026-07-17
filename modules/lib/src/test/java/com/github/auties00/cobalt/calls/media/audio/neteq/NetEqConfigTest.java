package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The defaults pin the neteq_* field-trial values decoded from the captured offer-ack voip_settings JSON
// (re/calls2-spec/captures/group-stanzas-bizcaller.jsonl, voip_settings_version 151561); a change here means
// the recovered ground truth changed.
@DisplayName("NetEqConfig defaults from the captured voip_settings blob")
class NetEqConfigTest {
    @Test
    @DisplayName("carries the captured neteq_* field-trial values verbatim")
    void capturedValues() {
        var config = NetEqConfig.defaults();
        assertEquals(-50, config.delayOffsetMs());
        assertEquals(500, config.maxDelayMs());
        assertEquals(500, config.dmHistorySizeMs());
        assertEquals(500, config.dlHistorySizeMs());
        assertEquals(0.98, config.underrunQuantile());
        assertEquals(0.99, config.underrunForgetFactor());
        assertFalse(config.enablePeakDetector());
        assertTrue(config.enableCodecPlc());
        assertTrue(config.use20msGetPeriod());
        assertEquals(50, config.preexpandWithFilteredLevelPerc());
        assertFalse(config.skipNackWithFec());
        assertTrue(config.ladEnabledForNack());
        assertTrue(config.ladEnabledForFec());
        assertEquals(20, config.ladNackExtraInsertTimeMs());
        assertEquals(500, config.nackRttLimitMs());
        assertEquals(10, config.audioNackMaxSeqReq());
        assertTrue(config.enableSpeakerStatus());
    }

    @Test
    @DisplayName("caps the NACK list at the native kNackListSizeLimitLocal")
    void nackCap() {
        assertEquals(500, NetEqConfig.NACK_LIST_SIZE_LIMIT);
        assertEquals(500, NetEqConfig.defaults().maxNackListSize());
    }

    @Test
    @DisplayName("derives a 20 ms get period from the captured neteq_use_20ms_get_period")
    void getPeriod() {
        assertEquals(20, NetEqConfig.defaults().getPeriodMs());
    }

    @Test
    @DisplayName("rejects an inverted delay range")
    void invertedRange() {
        assertThrows(IllegalArgumentException.class, () -> rebuild(100, 50));
    }

    @Test
    @DisplayName("rejects an out-of-range underrun quantile")
    void badQuantile() {
        var defaults = NetEqConfig.defaults();
        assertThrows(IllegalArgumentException.class, () -> new NetEqConfig(
                defaults.minDelayMs(), defaults.maxDelayMs(), defaults.delayOffsetMs(), defaults.targetDelayMs(),
                defaults.initMinE2eDelayMs(), defaults.dmHistorySizeMs(), defaults.dlHistorySizeMs(),
                defaults.maxHistoryMs(), 1.5, defaults.underrunForgetFactor(), defaults.reorderForgetFactor(),
                defaults.reorderStartForgetWeight(), defaults.enablePeakDetector(),
                defaults.smartBufferFlushEnabled(), defaults.bufferFlushMaxLengthMs(),
                defaults.maxPacketsInBuffer(), defaults.use20msGetPeriod(), defaults.numInitialPackets(),
                defaults.audioJitbufBufferLowerLimitScalePercent(), defaults.audioJitbufBufferLimitsWindowSizeMs(),
                defaults.highThresholdOffsetMs(), defaults.useMaxDelayInHighThreshold(),
                defaults.allowTimeStretchAcceleration(), defaults.allowTimeStretchForHighLatency(),
                defaults.allowTimeStretchThresholdMs(),
                defaults.enableCodecPlc(), defaults.preexpandWithFilteredLevelPerc(), defaults.skipNackWithFec(),
                defaults.ladEnabledForNack(), defaults.ladEnabledForFec(), defaults.ladNackExtraInsertTimeMs(),
                defaults.nackRttLimitMs(), defaults.maxNackListSize(), defaults.audioNackMaxSeqReq(),
                defaults.enableSpeakerStatus()));
    }

    private static NetEqConfig rebuild(int min, int max) {
        var d = NetEqConfig.defaults();
        return new NetEqConfig(min, max, d.delayOffsetMs(), d.targetDelayMs(), d.initMinE2eDelayMs(),
                d.dmHistorySizeMs(), d.dlHistorySizeMs(), d.maxHistoryMs(), d.underrunQuantile(),
                d.underrunForgetFactor(), d.reorderForgetFactor(), d.reorderStartForgetWeight(),
                d.enablePeakDetector(), d.smartBufferFlushEnabled(), d.bufferFlushMaxLengthMs(),
                d.maxPacketsInBuffer(), d.use20msGetPeriod(), d.numInitialPackets(),
                d.audioJitbufBufferLowerLimitScalePercent(), d.audioJitbufBufferLimitsWindowSizeMs(),
                d.highThresholdOffsetMs(), d.useMaxDelayInHighThreshold(), d.allowTimeStretchAcceleration(),
                d.allowTimeStretchForHighLatency(), d.allowTimeStretchThresholdMs(), d.enableCodecPlc(),
                d.preexpandWithFilteredLevelPerc(), d.skipNackWithFec(), d.ladEnabledForNack(),
                d.ladEnabledForFec(), d.ladNackExtraInsertTimeMs(), d.nackRttLimitMs(), d.maxNackListSize(),
                d.audioNackMaxSeqReq(), d.enableSpeakerStatus());
    }
}
