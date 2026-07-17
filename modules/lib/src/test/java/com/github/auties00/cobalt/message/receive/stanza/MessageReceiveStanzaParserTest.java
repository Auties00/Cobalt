package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link MessageReceiveStanzaParser#parse} against synthetic stanzas
 * built via {@link StanzaBuilder} (so each invariant is isolated) and one
 * captured live bot stanza pulled through end to end. Covers the required
 * attributes ({@code id}, {@code t}, {@code from}, {@code type}), optional
 * metadata ({@code notify}, {@code edit}, {@code count}), the absent-child
 * default for the HSM flag, and the missing-required-attribute error path. The
 * live-fixture test is guarded on {@link MessageFixtures#isAvailable} so the
 * suite stays green when the captured corpus is not present.
 */
@DisplayName("MessageReceiveStanzaParser")
class MessageReceiveStanzaParserTest {

    private static final Jid SELF_PN = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("258252122116273@lid");

    // Matches the captured business session so LID/PN derivations align between synthetic and captured input.
    private static final Jid PEER_PN = Jid.of("19254863482@s.whatsapp.net");

    @Test
    @DisplayName("parse: required attributes (id, t, from, type) populate the result")
    void parseRequiredAttrs() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0CAFEBABE")
                .attribute("t", 1700000000L)
                .attribute("from", PEER_PN)
                .attribute("type", "text")
                .build();

        var parsed = MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID);
        assertNotNull(parsed);
        assertEquals("3EB0CAFEBABE", parsed.id());
        assertEquals(Instant.ofEpochSecond(1700000000L), parsed.timestamp());
        assertEquals(PEER_PN, parsed.chatJid());
        Assertions.assertNotNull(parsed.messageType());
    }

    @Test
    @DisplayName("parse: optional notify (pushName) propagates")
    void parseOptionalNotify() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0")
                .attribute("t", 1700000000L)
                .attribute("from", PEER_PN)
                .attribute("type", "text")
                .attribute("notify", "Alice")
                .build();
        var parsed = MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID);
        assertEquals("Alice", parsed.pushName().orElseThrow());
    }

    @Test
    @DisplayName("parse: edit attribute defaults to 0 when absent")
    void parseEditDefaultsToZero() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0")
                .attribute("t", 1700000000L)
                .attribute("from", PEER_PN)
                .attribute("type", "text")
                .build();
        var parsed = MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID);
        assertEquals(0, parsed.editAttribute());
    }

    @Test
    @DisplayName("parse: missing required id throws IllegalArgumentException")
    void parseMissingIdThrows() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("t", 1700000000L)
                .attribute("from", PEER_PN)
                .attribute("type", "text")
                .build();
        assertThrows(NoSuchElementException.class,
                () -> MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID));
    }

    @Test
    @DisplayName("parse: missing required from throws IllegalArgumentException")
    void parseMissingFromThrows() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0")
                .attribute("t", 1700000000L)
                .attribute("type", "text")
                .build();
        assertThrows(NoSuchElementException.class,
                () -> MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID));
    }

    @Test
    @DisplayName("parse: null stanza throws NullPointerException")
    void parseNullNodeThrows() {
        assertThrows(NullPointerException.class,
                () -> MessageReceiveStanzaParser.parse(null, SELF_PN, SELF_LID));
    }

    // Validates the parser against real wire input; pairs with BotMsmsgReceiveLiveOracleTest.
    @Test
    @DisplayName("parse: live captured bot MSMSG stanza parses end-to-end with non-null fields")
    void parseLiveBotMsmsg() {
        var topic = "receive/template-or-buttons-from-biz";
        if (!MessageFixtures.isAvailable(topic)) return;
        var events = MessageFixtures.loadEvents(topic);
        var incoming = events.stream()
                .filter(e -> "in".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .findFirst()
                .orElseThrow();
        var node = MessageFixtures.buildNodeFromEvent(incoming);
        var parsed = MessageReceiveStanzaParser.parse(node, SELF_PN, SELF_LID);

        assertNotNull(parsed.id());
        assertTrue(parsed.chatJid().toString().endsWith("@bot"),
                "bot stream stanza must parse with @bot from JID");
        Assertions.assertNotNull(parsed.messageType());
        assertNotNull(parsed.timestamp());
        assertTrue(parsed.pushName().isPresent(),
                "captured bot stanza carries a notify (push name) attribute");
    }

    @Test
    @DisplayName("parse: count attribute (group ack-like) is preserved as Optional<Integer>")
    void parseCountAttribute() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0")
                .attribute("t", 1700000000L)
                .attribute("from", PEER_PN)
                .attribute("type", "text")
                .attribute("count", 5)
                .build();
        var parsed = MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID);
        assertEquals(5, parsed.count().orElseThrow(),
                "count attribute must propagate to the parsed stanza");
    }

    @Test
    @DisplayName("parse: stanza with no <hsm> child has isHsm=false")
    void parseHsmAbsent() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0")
                .attribute("t", 1700000000L)
                .attribute("from", PEER_PN)
                .attribute("type", "text")
                .build();
        var parsed = MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID);
        assertFalse(parsed.isHsm(),
                "no <hsm> child means isHsm=false");
    }

    @Test
    @DisplayName("parse: plain recipient attribute on a self-echo names the peer, not from")
    void parseSelfEchoRecipient() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0")
                .attribute("t", 1700000000L)
                .attribute("from", SELF_PN)
                .attribute("recipient", PEER_PN)
                .attribute("type", "text")
                .build();
        var parsed = MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID);
        assertEquals(PEER_PN, parsed.recipient().orElseThrow(),
                "plain recipient attribute must carry the peer the echo was addressed to");
        assertEquals(SELF_PN, parsed.chatJid(),
                "chatJid stays the raw from JID; the recipient is tracked separately");
    }

    @Test
    @DisplayName("parse: recipient is empty when the attribute is absent")
    void parseRecipientAbsent() {
        var msg = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0")
                .attribute("t", 1700000000L)
                .attribute("from", PEER_PN)
                .attribute("type", "text")
                .build();
        var parsed = MessageReceiveStanzaParser.parse(msg, SELF_PN, SELF_LID);
        assertTrue(parsed.recipient().isEmpty(),
                "no recipient attribute means recipient() is empty");
    }
}
