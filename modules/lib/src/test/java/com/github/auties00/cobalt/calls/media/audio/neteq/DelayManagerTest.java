package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DelayManager target-level estimator")
class DelayManagerTest {
    @Test
    @DisplayName("keeps the target within the configured delay bounds")
    void boundedTarget() {
        var config = NetEqConfig.defaults();
        var manager = new DelayManager(config);
        var time = 0L;
        for (var i = 0; i < 200; i++) {
            // a wildly jittery stream that would push the target high
            time += (i % 5 == 0) ? 200 : 5;
            manager.update(time, config.getPeriodMs());
        }
        assertTrue(manager.targetLevelMillis() <= config.maxDelayMs());
        assertTrue(manager.targetLevelMillis() >= config.minDelayMs());
    }

    @Test
    @DisplayName("settles to a low target for a perfectly paced stream")
    void pacedStreamLowTarget() {
        var config = NetEqConfig.defaults();
        var manager = new DelayManager(config);
        var time = 0L;
        for (var i = 0; i < 500; i++) {
            time += config.getPeriodMs();
            manager.update(time, config.getPeriodMs());
        }
        // a stream with no inter-arrival deviation keeps the quantile bucket at zero, so the offset and
        // bounds dominate; the captured offset is negative, so the target floors at minDelay
        assertEquals(config.minDelayMs(), manager.targetLevelMillis());
    }

    @Test
    @DisplayName("clears the estimate on reset")
    void reset() {
        var config = NetEqConfig.defaults();
        var manager = new DelayManager(config);
        var time = 0L;
        for (var i = 0; i < 50; i++) {
            time += (i % 3 == 0) ? 80 : 10;
            manager.update(time, config.getPeriodMs());
        }
        manager.reset();
        assertEquals(Math.max(config.initMinE2eDelayMs(), config.minDelayMs()), manager.targetLevelMillis());
        assertEquals(0, manager.peakFloorPackets());
    }
}
