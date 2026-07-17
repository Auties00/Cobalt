package com.github.auties00.cobalt.ack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the {@link NackReason} constant set, a closed set of stable wire codes, against
 * {@code WAWebCreateNackFromStanza.NackReason}: the tests assert the canonical values, reject
 * accidental collisions, and assert that every code falls within the 4xx/5xx server-error range.
 *
 * <p>The canonical values are hard-coded as literals so drift from the WA Web side is visible on the
 * diff rather than buried in a shared constant.
 */
@DisplayName("NackReason")
class NackReasonTest {

    @Test
    @DisplayName("known nack codes match the WA Web canonical values")
    void canonicalCodes() {
        assertEquals(421, NackReason.STALE_GROUP_ADDRESSING_MODE.code());
        assertEquals(475, NackReason.NEW_CHAT_MESSAGES_CAPPED.code());
        assertEquals(487, NackReason.PARSING_ERROR.code());
        assertEquals(488, NackReason.UNRECOGNIZED_STANZA.code());
        assertEquals(489, NackReason.UNRECOGNIZED_STANZA_CLASS.code());
        assertEquals(490, NackReason.UNRECOGNIZED_STANZA_TYPE.code());
        assertEquals(491, NackReason.INVALID_PROTOBUF.code());
        assertEquals(493, NackReason.INVALID_HOSTED_COMPANION_STANZA.code());
        assertEquals(495, NackReason.MISSING_MESSAGE_SECRET.code());
        assertEquals(496, NackReason.SIGNAL_ERROR_OLD_COUNTER.code());
        assertEquals(499, NackReason.MESSAGE_DELETED_ON_PEER.code());
        assertEquals(500, NackReason.UNHANDLED_ERROR.code());
        assertEquals(550, NackReason.UNSUPPORTED_ADMIN_REVOKE.code());
        assertEquals(551, NackReason.UNSUPPORTED_LID_GROUP.code());
        assertEquals(552, NackReason.DB_OPERATION_FAILED.code());
    }

    @Test
    @DisplayName("every declared nack reason is distinct (no value collision)")
    void distinctValues() {
        var codes = Arrays.stream(NackReason.values())
                .map(NackReason::code)
                .toList();
        var distinct = new HashSet<>(codes);
        assertEquals(codes.size(), distinct.size(),
                "every NackReason constant must hold a unique code: " + codes);
    }

    @Test
    @DisplayName("all codes fall within the HTTP-style 4xx/5xx server-error range")
    void codesAreInServerErrorRange() {
        for (var reason : NackReason.values()) {
            var value = reason.code();
            assertTrue(value >= 400 && value < 600,
                    reason.name() + " = " + value + " is outside the 4xx/5xx range");
        }
    }
}
