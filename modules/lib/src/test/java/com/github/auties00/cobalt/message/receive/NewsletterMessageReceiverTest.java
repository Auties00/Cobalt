package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the Channels (newsletter) inbound path of {@link NewsletterMessageReceiver}: extraction of
 * the {@code id}, {@code t}, {@code server_id}, and {@code is_sender} attributes, decoding the
 * protobuf carried by the {@code <plaintext>} child, and stamping the resulting
 * {@link com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo} with
 * {@link MessageStatus#DELIVERED}. Synthetic inbound {@code <message>} nodes are driven through
 * {@link NewsletterMessageReceiver#receive(Stanza, Jid)} directly; the
 * receiver reads only the local JID from its
 * {@link LinkedWhatsAppStore}, so no signal-session installation is
 * required.
 */
@DisplayName("NewsletterMessageReceiver")
class NewsletterMessageReceiverTest {

    private static final Jid SELF = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid NEWSLETTER = Jid.of("120363402045452944@newsletter");

    @Test
    @DisplayName("receive: extracts id, t, server_id, status=DELIVERED and decodes the <plaintext> payload")
    void receivePlaintext() {
        var receiver = new NewsletterMessageReceiver(MessageFixtures.temporaryStore(SELF, null));
        var payload = LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("newsletter body"));

        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0NL0001")
                .attribute("from", NEWSLETTER)
                .attribute("t", 1700000000L)
                .attribute("server_id", 42)
                .attribute("type", "text")
                .content(new StanzaBuilder()
                        .description("plaintext")
                        .content(payload)
                        .build())
                .build();

        var info = receiver.receive(inbound, NEWSLETTER);

        assertNotNull(info, "valid <plaintext> input must yield a non-null NewsletterMessageInfo");
        assertEquals("3EB0NL0001", info.key().id().orElseThrow());
        assertEquals(NEWSLETTER, info.key().parentJid().orElseThrow());
        assertEquals(Instant.ofEpochSecond(1700000000L), info.timestamp().orElseThrow());
        assertEquals(42, info.serverId());
        assertEquals(MessageStatus.DELIVERED, info.status().orElseThrow(),
                "newsletter receive always produces DELIVERED status (no E2E ack contract)");

        var content = info.message().content();
        var text = assertInstanceOf(ExtendedTextMessage.class, content,
                "the conversation field decodes to an ExtendedTextMessage on receive");
        assertEquals("newsletter body", text.text().orElseThrow());

        assertFalse(info.key().fromMe(),
                "absent is_sender attribute then fromMe=false");
    }

    @Test
    @DisplayName("receive: is_sender=\"true\" sets the resulting key.fromMe=true")
    void isSenderTrue() {
        var receiver = new NewsletterMessageReceiver(MessageFixtures.temporaryStore(SELF, null));
        var payload = LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("self post"));

        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0SELF001")
                .attribute("from", NEWSLETTER)
                .attribute("t", 1700000000L)
                .attribute("server_id", 100)
                .attribute("type", "text")
                .attribute("is_sender", "true")
                .content(new StanzaBuilder()
                        .description("plaintext")
                        .content(payload)
                        .build())
                .build();

        var info = receiver.receive(inbound, NEWSLETTER);
        assertTrue(info.key().fromMe(),
                "is_sender=\"true\" must propagate to key.fromMe=true");
    }

    @Test
    @DisplayName("receive: missing <plaintext> child returns null (no-op)")
    void missingPlaintextReturnsNull() {
        var receiver = new NewsletterMessageReceiver(MessageFixtures.temporaryStore(SELF, null));
        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0NL0002")
                .attribute("from", NEWSLETTER)
                .attribute("t", 1700000000L)
                .attribute("server_id", 1)
                .attribute("type", "text")
                .build();

        assertNull(receiver.receive(inbound, NEWSLETTER),
                "no <plaintext> child then null (the orchestrator treats this as an unavailable message)");
    }

    @Test
    @DisplayName("receive: empty <plaintext> payload returns null")
    void emptyPlaintextReturnsNull() {
        var receiver = new NewsletterMessageReceiver(MessageFixtures.temporaryStore(SELF, null));
        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0NL0003")
                .attribute("from", NEWSLETTER)
                .attribute("t", 1700000000L)
                .attribute("server_id", 1)
                .attribute("type", "text")
                .content(new StanzaBuilder()
                        .description("plaintext")
                        .content(new byte[0])
                        .build())
                .build();

        assertNull(receiver.receive(inbound, NEWSLETTER));
    }

    @Test
    @DisplayName("receive: missing required server_id throws NoSuchElementException")
    void missingServerIdThrows() {
        var receiver = new NewsletterMessageReceiver(MessageFixtures.temporaryStore(SELF, null));
        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("id", "3EB0NL0004")
                .attribute("from", NEWSLETTER)
                .attribute("t", 1700000000L)
                .attribute("type", "text")
                .content(new StanzaBuilder()
                        .description("plaintext")
                        .content(LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("hi")))
                        .build())
                .build();

        assertThrows(NoSuchElementException.class,
                () -> receiver.receive(inbound, NEWSLETTER));
    }

    @Test
    @DisplayName("receive: missing required id throws NoSuchElementException")
    void missingIdThrows() {
        var receiver = new NewsletterMessageReceiver(MessageFixtures.temporaryStore(SELF, null));
        var inbound = new StanzaBuilder()
                .description("message")
                .attribute("from", NEWSLETTER)
                .attribute("t", 1700000000L)
                .attribute("server_id", 1)
                .attribute("type", "text")
                .content(new StanzaBuilder()
                        .description("plaintext")
                        .content(LinkedMessageContainerSpec.encode(LinkedMessageContainer.of("hi")))
                        .build())
                .build();
        assertThrows(NoSuchElementException.class,
                () -> receiver.receive(inbound, NEWSLETTER));
    }
}
