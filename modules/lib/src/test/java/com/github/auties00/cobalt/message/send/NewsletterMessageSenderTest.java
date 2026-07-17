package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfoBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link NewsletterMessageSender} plaintext publish shape:
 * newsletters bypass the Signal envelope, so the payload travels in clear
 * inside a {@code <plaintext>} child of the outer {@code <message>} with no
 * {@code <enc>} or {@code <participants>} children. The sender runs against a
 * {@link TestWhatsAppClient} that captures the first emitted stanza into an
 * {@link AtomicReference} and returns a synthetic success ack.
 */
@DisplayName("NewsletterMessageSender")
class NewsletterMessageSenderTest {

    private static final Jid SELF = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid NEWSLETTER = Jid.of("120363402045452944@newsletter");

    @Test
    @DisplayName("send: ExtendedTextMessage -> <message type=\"text\"> with <plaintext> child")
    void textMessage() {
        var capturedStanza = new AtomicReference<Stanza>();
        var props = TestABPropsService.builder().build();
        var client = TestWhatsAppClient.create()
                .withStore(MessageFixtures.temporaryStore(SELF, null))
                .withAbPropsService(props)
                .withSendNodeHandler(node -> {
                    capturedStanza.set(node.build());
                    return new StanzaBuilder()
                            .description("ack")
                            .attribute("t", 1700000000L)
                            .attribute("server_id", "42")
                            .build();
                });
        var sender = new NewsletterMessageSender(client, props, new LiveWamService(client, props));

        var info = new NewsletterMessageInfoBuilder()
                .key(new MessageKeyBuilder()
                        .id("3EB0NEWS0001")
                        .parentJid(NEWSLETTER)
                        .fromMe(true)
                        .build())
                .serverId(1)
                .message(LinkedMessageContainer.of("hello newsletter"))
                .build();

        var ack = sender.send(NEWSLETTER, info);

        var stanza = capturedStanza.get();
        assertNotNull(stanza, "exactly one <message> must be sent");
        assertEquals("message", stanza.description());
        assertEquals("3EB0NEWS0001", stanza.getAttributeAsString("id").orElseThrow());
        assertEquals(NEWSLETTER.toString(), stanza.getAttributeAsString("to").orElseThrow(),
                "outer to must be the newsletter JID");
        assertEquals("text", stanza.getAttributeAsString("type").orElseThrow());

        // <plaintext> payload, NO <enc>, NO <participants>.
        assertTrue(stanza.getChild("plaintext").isPresent(),
                "newsletter sends carry the payload in <plaintext>, not <enc>");
        assertFalse(stanza.getChild("enc").isPresent(),
                "no Signal envelope on the newsletter wire path");
        assertFalse(stanza.getChild("participants").isPresent(),
                "no <participants> wrapper on newsletter sends");

        // The <plaintext> contains non-empty bytes (the serialised LinkedMessageContainer).
        var plaintext = stanza.getChild("plaintext").orElseThrow();
        assertTrue(plaintext.toContentBytes().orElseThrow().length > 0,
                "plaintext payload must be non-empty");

        assertTrue(ack.isSuccess());
    }
}
