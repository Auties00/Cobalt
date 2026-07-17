package com.github.auties00.cobalt.calls.engine.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CallEventType event table")
class CallEventTypeTest {
    @Test
    @DisplayName("declares exactly 172 constants")
    void cardinality() {
        assertEquals(172, CallEventType.values().length);
    }

    @Test
    @DisplayName("has 172 distinct indices spanning 0x00..0xab contiguously")
    void distinctContiguousIndices() {
        var seen = new HashSet<Integer>();
        for (var type : CallEventType.values()) {
            assertTrue(seen.add(type.index()), () -> "duplicate index " + type.index() + " on " + type);
        }
        assertEquals(172, seen.size());
        for (var id = 0x00; id <= 0xab; id++) {
            assertTrue(seen.contains(id), "missing event id " + id);
        }
    }

    @ParameterizedTest
    @EnumSource(CallEventType.class)
    @DisplayName("each constant round-trips through its native index")
    void indexRoundTrip(CallEventType type) {
        assertSame(type, CallEventType.ofIndex(type.index()).orElseThrow());
    }

    @Test
    @DisplayName("an out-of-range id resolves to empty")
    void outOfRangeIsEmpty() {
        assertTrue(CallEventType.ofIndex(-1).isEmpty());
        assertTrue(CallEventType.ofIndex(0xac).isEmpty());
    }

    @Test
    @DisplayName("only the reserved 0x76 slot has an empty display name")
    void onlyReservedSlotIsBlank() {
        for (var type : CallEventType.values()) {
            if (type == CallEventType.RESERVED_0X76) {
                assertTrue(type.displayName().isEmpty());
            } else {
                assertTrue(!type.displayName().isEmpty(), () -> type + " must carry a display name");
            }
        }
    }

    @Test
    @DisplayName("carries the recovered engine typo verbatim at 0x8b")
    void verbatimTypoPreserved() {
        assertEquals("Call Link Lobby Self Sate Changed",
                CallEventType.CALL_LINK_LOBBY_SELF_STATE_CHANGED.displayName());
    }
}
