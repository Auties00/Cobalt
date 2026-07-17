package com.github.auties00.cobalt.calls.transport.rtcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the inbound video loss detector: that a confirmed sequence gap eventually yields the sequence
 * numbers the transport packs into a sixteen-byte generic NACK with the correct packet identifier and
 * bitmask, that a packet reordered within the tolerance window is not requested, that a freshly requested
 * gap is not re-requested inside the suppression window, and that a key frame clears the pending set. The
 * NACK byte layout is asserted by packing the tracker's output through the same {@link RtcpReportBuilder}
 * the production send path uses.
 */
@DisplayName("VideoNackTracker inbound video NACK list")
class VideoNackTrackerTest {
    private static final int REMOTE_VIDEO_SSRC = 0x12345678;

    @Test
    @DisplayName("a gap eventually packs into a single 16-byte NACK with the correct PID and BLP")
    void gapProducesNack() {
        var tracker = new VideoNackTracker();
        tracker.recordReceived(100);
        tracker.recordReceived(110);
        // 101..107 are far enough past the reorder window (highest 110) to be due; 108 and 109 are still
        // inside it.
        var due = tracker.nackList(1000, 0);
        assertEquals(List.of(101, 102, 103, 104, 105, 106, 107), due);

        var records = packGenericNack(REMOTE_VIDEO_SSRC, due);
        assertEquals(1, records.size());
        var record = records.getFirst();
        assertEquals(16, record.length);
        assertEquals((byte) 0x81, record[0]);
        assertEquals((byte) 205, record[1]);
        assertEquals(0x00000001, readU32(record, 4));
        assertEquals(REMOTE_VIDEO_SSRC, readU32(record, 8));
        assertEquals(101, readU16(record, 12));
        // 102..107 follow 101 contiguously, so bits 0..5 of the bitmask are set.
        assertEquals(0b0000000000111111, readU16(record, 14));
    }

    @Test
    @DisplayName("does not NACK a packet reordered within the tolerance window, and clears it on arrival")
    void reorderWithinWindowNotNacked() {
        var tracker = new VideoNackTracker();
        tracker.recordReceived(10);
        tracker.recordReceived(12);
        // 11 is one short of the highest, inside the reorder window, so it is not yet due.
        assertTrue(tracker.nackList(1000, 0).isEmpty());
        // The reordered packet then arrives and is removed from the missing set without ever being requested.
        tracker.recordReceived(11);
        assertEquals(0, tracker.size());
        assertTrue(tracker.nackList(1000, 0).isEmpty());
    }

    @Test
    @DisplayName("does not re-NACK within the suppression window, then re-NACKs once past it")
    void suppressesReNackWithinWindow() {
        var tracker = new VideoNackTracker();
        tracker.recordReceived(100);
        tracker.recordReceived(110);
        var first = tracker.nackList(1000, 0);
        assertEquals(List.of(101, 102, 103, 104, 105, 106, 107), first);
        // Five milliseconds later, inside the re-NACK interval, nothing is due again.
        assertTrue(tracker.nackList(1005, 0).isEmpty());
        // Past the re-NACK interval the same run is requested once more.
        assertEquals(first, tracker.nackList(1025, 0));
        // The single re-NACK is the cap; a later poll requests nothing further.
        assertTrue(tracker.nackList(2000, 0).isEmpty());
    }

    @Test
    @DisplayName("clears all pending state on a key frame reset")
    void keyFrameClearsPending() {
        var tracker = new VideoNackTracker();
        tracker.recordReceived(100);
        tracker.recordReceived(110);
        assertEquals(9, tracker.size());
        tracker.reset();
        assertEquals(0, tracker.size());
        assertTrue(tracker.nackList(1000, 0).isEmpty());
    }

    @Test
    @DisplayName("detects a gap across the 16-bit sequence-number wrap")
    void detectsGapAcrossWrap() {
        var tracker = new VideoNackTracker();
        tracker.recordReceived(65534);
        tracker.recordReceived(1);
        // 65535 and 0 were skipped across the rollover; advance past the reorder window so both are due.
        tracker.recordReceived(2);
        tracker.recordReceived(3);
        assertEquals(List.of(65535, 0), tracker.nackList(1000, 0));
    }

    @Test
    @DisplayName("suppresses the whole list when the round-trip time exceeds the ceiling")
    void suppressedOnHighRtt() {
        var tracker = new VideoNackTracker();
        tracker.recordReceived(100);
        tracker.recordReceived(110);
        assertTrue(tracker.nackList(1000, 501).isEmpty());
        assertTrue(tracker.size() > 0);
    }

    /**
     * Packs a due-sequence list into generic NACK records the same way the production transport send path
     * does: a run of up to sixteen sequence numbers past a packet identifier collapses into one record's
     * bitmask, and a wider gap starts a new record.
     */
    private static List<byte[]> packGenericNack(int mediaSsrc, List<Integer> due) {
        var sorted = new ArrayList<>(new TreeSet<>(due));
        var records = new ArrayList<byte[]>();
        var index = 0;
        while (index < sorted.size()) {
            var pid = sorted.get(index) & 0xFFFF;
            var blp = 0;
            var next = index + 1;
            while (next < sorted.size()) {
                var offset = (sorted.get(next) - pid) & 0xFFFF;
                if (offset < 1 || offset > 16) {
                    break;
                }
                blp |= 1 << (offset - 1);
                next++;
            }
            records.add(RtcpReportBuilder.buildNack(mediaSsrc, pid, blp));
            index = next;
        }
        return records;
    }

    private static int readU16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int readU32(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }
}
