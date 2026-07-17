package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validates the single-channel SyncBuffer port: the left-shifting push, the read cursor, and the history
 * window the time-stretch and concealment operations splice over.
 */
@DisplayName("NetEqSyncBuffer decoded-PCM output history")
class NetEqSyncBufferTest {
    @Test
    @DisplayName("starts full of silence with the read cursor at the end")
    void emptyOnConstruction() {
        var buffer = new NetEqSyncBuffer(100);
        assertEquals(100, buffer.capacity());
        assertEquals(100, buffer.nextIndex());
        assertEquals(0, buffer.futureLength());
        assertEquals((short) 0, buffer.at(50));
    }

    @Test
    @DisplayName("rejects a non-positive capacity")
    void rejectsBadCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new NetEqSyncBuffer(0));
        assertThrows(IllegalArgumentException.class, () -> new NetEqSyncBuffer(-1));
    }

    @Test
    @DisplayName("pushes a frame into the tail and moves the cursor a frame back")
    void pushBackPlacesFrameAtTail() {
        var buffer = new NetEqSyncBuffer(10);
        buffer.pushBack(new short[]{1, 2, 3});
        // last three samples are the pushed frame
        assertEquals((short) 1, buffer.at(7));
        assertEquals((short) 2, buffer.at(8));
        assertEquals((short) 3, buffer.at(9));
        // the cursor moved back by the frame length, exposing the frame as unplayed
        assertEquals(7, buffer.nextIndex());
        assertEquals(3, buffer.futureLength());
    }

    @Test
    @DisplayName("shifts the window left so the oldest samples drop out on successive pushes")
    void pushBackShiftsWindowLeft() {
        var buffer = new NetEqSyncBuffer(6);
        buffer.pushBack(new short[]{1, 2, 3});
        buffer.pushBack(new short[]{4, 5, 6});
        // window now holds the six most recent samples in order
        for (var i = 0; i < 6; i++) {
            assertEquals((short) (i + 1), buffer.at(i), "index " + i);
        }
        buffer.pushBack(new short[]{7, 8});
        // the two oldest (1,2) dropped out; window is 3,4,5,6,7,8
        short[] expected = {3, 4, 5, 6, 7, 8};
        for (var i = 0; i < 6; i++) {
            assertEquals(expected[i], buffer.at(i), "index " + i);
        }
    }

    @Test
    @DisplayName("reads the unplayed run out and advances the cursor")
    void getNextAudioAdvancesCursor() {
        var buffer = new NetEqSyncBuffer(10);
        buffer.pushBack(new short[]{10, 20, 30, 40});
        var out = new short[4];
        var copied = buffer.getNextAudioInterleaved(out, 4);
        assertEquals(4, copied);
        assertEquals(10, out[0]);
        assertEquals(40, out[3]);
        assertEquals(10, buffer.nextIndex());
        assertEquals(0, buffer.futureLength());
        // a further read returns nothing
        assertEquals(0, buffer.getNextAudioInterleaved(out, 4));
    }

    @Test
    @DisplayName("clamps a read to the unplayed span")
    void getNextAudioClampsToAvailable() {
        var buffer = new NetEqSyncBuffer(10);
        buffer.pushBack(new short[]{1, 2});
        var out = new short[8];
        assertEquals(2, buffer.getNextAudioInterleaved(out, 8));
    }

    @Test
    @DisplayName("overwrites the tail in place without moving the cursor")
    void replaceTailLeavesCursor() {
        var buffer = new NetEqSyncBuffer(8);
        buffer.pushBack(new short[]{1, 2, 3, 4});
        var cursorBefore = buffer.nextIndex();
        buffer.replaceTail(new short[]{9, 9, 9, 9});
        assertEquals(cursorBefore, buffer.nextIndex());
        assertEquals((short) 9, buffer.at(4));
        assertEquals((short) 9, buffer.at(7));
    }

    @Test
    @DisplayName("drops the oldest samples when a frame exceeds the capacity")
    void pushBackLongerThanCapacity() {
        var buffer = new NetEqSyncBuffer(4);
        buffer.pushBack(new short[]{1, 2, 3, 4, 5, 6});
        // only the last four samples survive
        short[] expected = {3, 4, 5, 6};
        for (var i = 0; i < 4; i++) {
            assertEquals(expected[i], buffer.at(i), "index " + i);
        }
        assertEquals(0, buffer.nextIndex());
    }

    @Test
    @DisplayName("reset clears the history back to silence with the cursor at the end")
    void resetClears() {
        var buffer = new NetEqSyncBuffer(10);
        buffer.pushBack(new short[]{1, 2, 3});
        buffer.reset();
        assertEquals(10, buffer.nextIndex());
        assertEquals(0, buffer.futureLength());
        assertEquals((short) 0, buffer.at(9));
    }
}
