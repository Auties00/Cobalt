package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NackTracker audio NACK list")
class NackTrackerTest {
    @Nested
    @DisplayName("16-bit IsNewerSequenceNumber wrap-around comparison")
    class IsNewer {
        @ParameterizedTest(name = "{0} newer than {1} -> {2}")
        @CsvSource({
                "1, 0, true",
                "0, 1, false",
                "0, 65535, true",
                "65535, 0, false",
                "100, 100, false",
                "32768, 0, false",
                "32767, 0, true"
        })
        @DisplayName("treats the shorter forward distance as newer, handling rollover")
        void comparison(int sequence, int previous, boolean expected) {
            assertEquals(expected, NackTracker.isNewerSequenceNumber(sequence, previous));
        }
    }

    @Test
    @DisplayName("records the sequence numbers skipped between two received packets")
    void recordsGap() {
        var tracker = new NackTracker(NetEqConfig.defaults());
        tracker.updateLastReceived(10, 0);
        tracker.updateLastReceived(14, 0);
        // 11, 12, 13 are missing
        assertEquals(3, tracker.size());
        var due = tracker.nackList(1000, 0);
        assertEquals(java.util.List.of(11, 12, 13), due);
    }

    @Test
    @DisplayName("clears a missing entry when its packet arrives late")
    void lateArrivalClears() {
        var tracker = new NackTracker(NetEqConfig.defaults());
        tracker.updateLastReceived(10, 0);
        tracker.updateLastReceived(13, 0);
        assertEquals(2, tracker.size());
        tracker.updateLastReceived(11, 0);
        assertEquals(1, tracker.size());
    }

    @Test
    @DisplayName("prunes entries at or before the last decoded packet")
    void prunesPastDecoded() {
        var tracker = new NackTracker(NetEqConfig.defaults());
        tracker.updateLastReceived(10, 0);
        tracker.updateLastReceived(20, 0);
        assertEquals(9, tracker.size());
        tracker.updateLastDecoded(15);
        // 11..15 are now in the past; 16..19 remain
        assertEquals(java.util.List.of(16, 17, 18, 19), tracker.nackList(1000, 0));
    }

    @Test
    @DisplayName("suppresses the whole list when the round-trip time exceeds the ceiling")
    void suppressedOnHighRtt() {
        var config = NetEqConfig.defaults();
        var tracker = new NackTracker(config);
        tracker.updateLastReceived(10, 0);
        tracker.updateLastReceived(14, 0);
        assertTrue(tracker.nackList(1000, config.nackRttLimitMs() + 1).isEmpty());
        assertFalse(tracker.nackList(1000, 0).isEmpty());
    }

    @Test
    @DisplayName("caps the request to the configured maximum sequence count")
    void capsRequestCount() {
        var config = NetEqConfig.defaults();
        var tracker = new NackTracker(config);
        tracker.updateLastReceived(0, 0);
        tracker.updateLastReceived(100, 0);
        assertEquals(config.audioNackMaxSeqReq(), tracker.nackList(1000, 0).size());
    }

    @Test
    @DisplayName("bounds the missing list to the configured maximum size")
    void boundsListSize() {
        var config = NetEqConfig.defaults();
        var tracker = new NackTracker(config);
        tracker.updateLastReceived(0, 0);
        // a forward gap of 600 (newer under the 16-bit comparison) would create 599 missing entries
        tracker.updateLastReceived(600, 0);
        assertEquals(config.maxNackListSize(), tracker.size());
    }
}
