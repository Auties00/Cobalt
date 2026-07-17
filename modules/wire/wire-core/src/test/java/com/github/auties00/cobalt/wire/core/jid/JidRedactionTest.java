package com.github.auties00.cobalt.wire.core.jid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link Jid#toLogRedactable()}, the structural redaction a {@link Jid} supplies to the logging
 * pipeline: the user component is replaced by a keyed fingerprint while the routing structure (agent, device,
 * server) survives, and a server-only JID is rendered verbatim. A fixed sample phone number
 * ({@code 393331234567}) stands in for the canonical secret; the salt is process-random, so the tests assert
 * within-run determinism rather than pinning exact token bytes.
 */
@DisplayName("Jid.toLogRedactable")
class JidRedactionTest {
    private static final String PHONE = "393331234567";

    private static String redact(String jid) {
        return Jid.of(jid).toLogRedactable().toRedactedLog();
    }

    @Test
    @DisplayName("hides the user but keeps server and device structure")
    void keepsRoutingHidesUser() {
        var redacted = redact(PHONE + ":5@lid");
        assertFalse(redacted.contains(PHONE), "phone/user must not appear");
        assertTrue(redacted.startsWith("#"), "user is replaced by a fingerprint token");
        assertTrue(redacted.contains("@lid"), "server is preserved");
        assertTrue(redacted.contains(":5"), "device is preserved");
    }

    @Test
    @DisplayName("renders server-only JIDs verbatim")
    void serverOnlyVerbatim() {
        assertEquals("s.whatsapp.net", Jid.userServer().toLogRedactable().toRedactedLog());
    }

    @Test
    @DisplayName("maps distinct users to distinct tokens and the same user to the same token")
    void correlatable() {
        var a = redact(PHONE + "@s.whatsapp.net");
        var aAgain = redact(PHONE + "@s.whatsapp.net");
        var b = redact("393339999999@s.whatsapp.net");
        assertEquals(a, aAgain);
        assertNotEquals(a, b);
    }
}
