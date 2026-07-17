package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-shape oracle for newsletter plaintext (SMAX) sends, comparing Cobalt
 * output against the WA Web stanza captured in
 * {@code fixtures/message/send/newsletter-text.jsonl}. The captured shape is
 * the distinct form newsletter messages take from WA Web's SMAX publish
 * pipeline: a single {@code <plaintext>} payload child under
 * {@code <message to="...@newsletter">} with no Signal envelope and no
 * {@code <participants>} list. Cross-references {@link NewsletterStanza}.
 * The assertion block is skipped when the fixture is not available locally;
 * the capture script under {@code src/test/resources/fixtures/message/}
 * regenerates the file against a live WA Web session when needed.
 */
@DisplayName("Newsletter stanza live wire oracle")
class NewsletterStanzaLiveOracleTest {

    @Test
    @DisplayName("newsletter text send: <message to=\"...@newsletter\" type=\"text\"> with <plaintext> body")
    void newsletterTextPlaintext() {
        var topic = "send/newsletter-text";
        if (!MessageFixtures.isAvailable(topic)) return;

        var events = MessageFixtures.loadEvents(topic);
        var outgoing = events.stream()
                .filter(e -> "out".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no outgoing newsletter <message> in fixture"));
        var message = MessageFixtures.buildNodeFromEvent(outgoing);

        assertEquals("text", message.getAttributeAsString("type").orElseThrow(),
                "newsletter text send must carry type=text");
        var to = message.getAttributeAsString("to").orElseThrow();
        assertTrue(to.endsWith("@newsletter"),
                "outer to must be @newsletter, got " + to);

        var plaintext = message.getChild("plaintext").orElseThrow(
                () -> new AssertionError("newsletter send must include <plaintext> child"));
        assertTrue(plaintext.hasContent(),
                "<plaintext> must carry payload bytes (the SMAX message body)");
        assertTrue(plaintext.toContentBytes().orElseThrow().length > 0,
                "<plaintext> payload must be non-empty");

        assertFalse(message.getChild("participants").isPresent(),
                "newsletter sends must not carry <participants>");
        assertFalse(message.getChild("enc").isPresent(),
                "newsletter sends must not carry <enc>");
    }
}
