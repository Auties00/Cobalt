package com.github.auties00.cobalt.calls.engine.state;

import com.github.auties00.cobalt.wire.linked.call.CallState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CallLifecycleState machine")
class CallStateTest {
    @Test
    @DisplayName("declares the fifteen engine states")
    void cardinality() {
        assertEquals(15, CallLifecycleState.values().length);
    }

    @ParameterizedTest
    @EnumSource(CallLifecycleState.class)
    @DisplayName("each state round-trips through its wire ordinal")
    void wireOrdinalRoundTrip(CallLifecycleState state) {
        assertSame(state, CallLifecycleState.ofWireOrdinal(state.wireOrdinal()).orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(CallLifecycleState.class)
    @DisplayName("projects every internal state onto a public phase")
    void toPublicIsTotal(CallLifecycleState state) {
        assertNotNull(state.toPublic());
    }

    @Test
    @DisplayName("collapses the internal states onto the expected public phases")
    void projectionSpotChecks() {
        assertEquals(CallState.RINGING, CallLifecycleState.CALLING.toPublic());
        assertEquals(CallState.RINGING, CallLifecycleState.RECEIVED_CALL.toPublic());
        assertEquals(CallState.CONNECTING, CallLifecycleState.ACCEPT_SENT.toPublic());
        assertEquals(CallState.CONNECTING, CallLifecycleState.LINK.toPublic());
        assertEquals(CallState.ACTIVE, CallLifecycleState.CALL_ACTIVE.toPublic());
        assertEquals(CallState.ACTIVE, CallLifecycleState.CONNECTED_LONELY.toPublic());
        assertEquals(CallState.RECONNECTING, CallLifecycleState.REJOINING.toPublic());
        assertEquals(CallState.ENDED, CallLifecycleState.NONE.toPublic());
        assertEquals(CallState.ENDED, CallLifecycleState.ENDING.toPublic());
        assertEquals(CallState.ENDED, CallLifecycleState.CALL_ACTIVE_ELSEWHERE.toPublic());
    }

    @Test
    @DisplayName("an out-of-range wire ordinal resolves to empty")
    void outOfRangeIsEmpty() {
        assertTrue(CallLifecycleState.ofWireOrdinal(-1).isEmpty());
        assertTrue(CallLifecycleState.ofWireOrdinal(15).isEmpty());
    }
}
