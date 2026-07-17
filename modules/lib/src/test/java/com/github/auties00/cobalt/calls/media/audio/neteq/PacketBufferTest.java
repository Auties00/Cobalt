package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PacketBuffer ordered audio packet store")
class PacketBufferTest {
    private static RtpAudioPacket packet(int seq) {
        return new RtpAudioPacket(seq, seq * 320L, LiveNetEq.SPEECH_PAYLOAD_TYPE, new byte[]{1, 2, 3}, seq);
    }

    @Test
    @DisplayName("extracts packets in ascending sequence order regardless of insert order")
    void ordering() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        buffer.insert(packet(3));
        buffer.insert(packet(1));
        buffer.insert(packet(2));
        assertEquals(1, buffer.extractNext().sequenceNumber());
        assertEquals(2, buffer.extractNext().sequenceNumber());
        assertEquals(3, buffer.extractNext().sequenceNumber());
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("discards a duplicate sequence number")
    void duplicate() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        assertTrue(buffer.insert(packet(5)));
        assertFalse(buffer.insert(packet(5)));
        assertEquals(1, buffer.size());
        assertEquals(1, buffer.packetsDiscarded());
    }

    @Test
    @DisplayName("discards a packet that precedes the playout cursor")
    void latePacket() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        buffer.insert(packet(10));
        buffer.extractNext();
        assertFalse(buffer.insert(packet(9)));
        assertEquals(1, buffer.packetsDiscarded());
    }

    @Test
    @DisplayName("reports a contiguous next packet when no gap follows the cursor")
    void contiguity() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        buffer.insert(packet(10));
        assertTrue(buffer.nextSequenceContiguous());
        buffer.extractNext();
        buffer.insert(packet(12));
        assertFalse(buffer.nextSequenceContiguous());
        buffer.insert(packet(11));
        assertTrue(buffer.nextSequenceContiguous());
    }

    @Test
    @DisplayName("returns null from an empty buffer rather than throwing")
    void emptyBuffer() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        assertNull(buffer.peekNext());
        assertNull(buffer.extractNext());
        assertFalse(buffer.nextSequenceContiguous());
    }

    private static RtpAudioPacket packetStamped(int seq, long timestamp) {
        return new RtpAudioPacket(seq, timestamp, LiveNetEq.SPEECH_PAYLOAD_TYPE, new byte[]{1}, seq);
    }

    @Test
    @DisplayName("falls back to the 320-sample frame before two adjacent packets are seen")
    void perPacketFallback() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        assertEquals(320, buffer.approximateSamplesPerPacket());
        buffer.insert(packetStamped(1, 0));
        assertEquals(320, buffer.approximateSamplesPerPacket());
    }

    @Test
    @DisplayName("measures 320 samples per packet from a 20 ms Opus timestamp delta")
    void perPacketOpus() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        buffer.insert(packetStamped(1, 0));
        buffer.insert(packetStamped(2, 320));
        assertEquals(320, buffer.approximateSamplesPerPacket());
        // identity with the 20 ms get-period frame size keeps the Opus span accounting unchanged
        assertEquals(320, NetEqConfig.defaults().getPeriodMs() * LiveNetEq.SAMPLE_RATE_HZ / 1000);
    }

    @Test
    @DisplayName("measures 960 samples per packet from a 60 ms MLow timestamp delta")
    void perPacketMlow() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        buffer.insert(packetStamped(1, 0));
        buffer.insert(packetStamped(2, 960));
        assertEquals(960, buffer.approximateSamplesPerPacket());
    }

    @Test
    @DisplayName("counts a 60 ms stream's buffered span at full duration, not a third of it")
    void spanCountsFullMlowDuration() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        buffer.insert(packetStamped(1, 0));
        buffer.insert(packetStamped(2, 960));
        buffer.insert(packetStamped(3, 1920));
        // three 60 ms packets span 180 ms, not 60 ms
        assertEquals(180, buffer.spanMillis(buffer.approximateSamplesPerPacket()));
    }

    @Test
    @DisplayName("clear resets the measured per-packet spacing to the fallback")
    void clearResetsSpacing() {
        var buffer = new PacketBuffer(NetEqConfig.defaults());
        buffer.insert(packetStamped(1, 0));
        buffer.insert(packetStamped(2, 960));
        assertEquals(960, buffer.approximateSamplesPerPacket());
        buffer.clear();
        assertEquals(320, buffer.approximateSamplesPerPacket());
    }
}
