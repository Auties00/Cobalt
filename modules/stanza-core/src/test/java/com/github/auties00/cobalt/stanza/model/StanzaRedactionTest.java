package com.github.auties00.cobalt.stanza.model;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link Stanza#toRedactedLog()}, the structural summary the logging pipeline renders in place of a
 * stanza's raw bytes. The assertions check that protocol structure (description, attribute keys, child count,
 * content presence) survives while attribute values, textual content, and a carried {@link Jid} never appear.
 * A fixed sample phone number ({@code 393331234567}) stands in for the canonical secret.
 */
@DisplayName("Stanza.toRedactedLog")
class StanzaRedactionTest {
    private static final String PHONE = "393331234567";

    @Test
    @DisplayName("summarises shape without leaking attribute values or content")
    void shapeOnly() {
        var stanza = new StanzaBuilder()
                .description("message")
                .attribute("id", "STANZA123")
                .attribute("from", Jid.of(PHONE + "@s.whatsapp.net"))
                .attribute("type", "text")
                .build();
        var redacted = stanza.toRedactedLog();
        assertTrue(redacted.contains("stanza(message"));
        assertTrue(redacted.contains("attrs=[id, from, type]"), "attribute keys are structure");
        assertFalse(redacted.contains("STANZA123"), "attribute values must not appear");
        assertFalse(redacted.contains(PHONE), "the from JID must not appear");
    }

    @Test
    @DisplayName("marks textual content as redacted without printing it")
    void textContentRedacted() {
        var stanza = new StanzaBuilder()
                .description("body")
                .content("super secret body")
                .build();
        var redacted = stanza.toRedactedLog();
        assertTrue(redacted.contains("content=<redacted>"));
        assertFalse(redacted.contains("secret body"));
    }

    @Test
    @DisplayName("reports the child count for container stanzas")
    void childCount() {
        var child = new StanzaBuilder().description("item").build();
        var stanza = new StanzaBuilder().description("iq").content(child).build();
        assertTrue(stanza.toRedactedLog().contains("children=1"));
    }
}
