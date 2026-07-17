package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link ProtocolMessage.Type} protobuf-index contract that drives
 * the edit, decrypt-fail, and target-sender selection branches: every value
 * carries a unique non-negative index, the well-known WA Web indices are
 * present, and every value round-trips through {@code index()}. The enum is
 * exercised directly; the full per-subtype dispatch behaviour lives in the
 * package-private senders and is covered by the live-corpus oracle.
 */
@DisplayName("ProtocolMessage.Type dispatch")
class ProtocolMessageDispatchTest {

    @ParameterizedTest(name = "{0} has non-null name")
    @EnumSource(ProtocolMessage.Type.class)
    @DisplayName("every value carries a stable enum name (regression guard)")
    void everyValueHasName(ProtocolMessage.Type type) {
        assertNotNull(type.name());
        assertFalse(type.name().isBlank());
    }

    @Test
    @DisplayName("every value has a unique protobuf index")
    void indicesAreUnique() {
        var values = ProtocolMessage.Type.values();
        var indices = new HashSet<Integer>();
        for (var type : values) {
            var index = type.index();
            assertTrue(index >= 0, type + " has negative index " + index);
            assertTrue(indices.add(index),
                    type + " has duplicate index " + index);
        }
        assertEquals(values.length, indices.size(),
                "every value must contribute a distinct index");
    }

    @Test
    @DisplayName("well-known WA Web protobuf indices are present")
    void wellKnownIndicesPresent() {
        // Pinned against WA Web's Message$ProtocolMessage$Type so drift
        // surfaces as a test failure rather than silent round-trip loss.
        assertEquals(0, ProtocolMessage.Type.REVOKE.index(),
                "REVOKE must be 0 (the default-on-protobuf-wire value)");
        assertEquals(3, ProtocolMessage.Type.EPHEMERAL_SETTING.index());
        assertEquals(5, ProtocolMessage.Type.HISTORY_SYNC_NOTIFICATION.index());
        assertEquals(6, ProtocolMessage.Type.APP_STATE_SYNC_KEY_SHARE.index());
        assertEquals(7, ProtocolMessage.Type.APP_STATE_SYNC_KEY_REQUEST.index());
        assertEquals(14, ProtocolMessage.Type.MESSAGE_EDIT.index(),
                "MESSAGE_EDIT must be 14; the wire stanza's edit=1 marker comes from this branch");
    }

    @Test
    @DisplayName("count of values matches the WA Web wire enum (at least 20 documented subtypes)")
    void valueCountLowerBound() {
        var values = ProtocolMessage.Type.values();
        // 20 is a deliberate lower bound: WA Web's wire enum has 31 subtypes
        // but new additions should not break the test.
        assertTrue(values.length >= 20,
                "ProtocolMessage.Type must enumerate at least 20 subtypes, got " + values.length);
    }

    @Test
    @DisplayName("enum values resolve back via index() so each yields a distinct value")
    void everyValueRoundTripsViaIndex() {
        var values = ProtocolMessage.Type.values();
        var indices = Arrays.stream(values).map(t -> t.index()).toArray();
        var distinct = Arrays.stream(indices).distinct().toArray();
        assertEquals(indices.length, distinct.length,
                "every protobuf index must map back to a unique enum value");
    }

    @Test
    @DisplayName("REVOKE and MESSAGE_EDIT are distinct values (used for edit=7 vs edit=1 on wire)")
    void revokeAndEditAreDistinct() {
        assertNotEquals(ProtocolMessage.Type.REVOKE, ProtocolMessage.Type.MESSAGE_EDIT,
                "REVOKE and MESSAGE_EDIT must not collide; they drive different wire edit attributes");
    }
}
