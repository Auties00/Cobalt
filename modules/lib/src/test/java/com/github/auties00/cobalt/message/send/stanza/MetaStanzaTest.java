package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Structural tests for {@link MetaStanza}, pinning the constructor null
 * guard and the two attribute pass-through paths driven by build inputs
 * rather than chat state: {@code status_setting} for status broadcasts and
 * {@code conversation_thread_id} for AI threads. The fixtures use
 * {@link MessageFixtures#temporaryStore(Jid, Jid)} with no seeded contact
 * records, so the LID-origin and hosted-business branches are not exercised;
 * the richer message-type branches (polls, events, view-once, hosted
 * business) are covered by the upstream send-pipeline tests with seeded
 * message containers.
 */
@DisplayName("MetaStanza")
class MetaStanzaTest {

    @Test
    @DisplayName("constructor: null store throws NullPointerException")
    void nullStoreThrows() {
        assertThrows(NullPointerException.class, () -> new MetaStanza(null));
    }

    @Test
    @DisplayName("buildChat: status_setting propagates to the meta attribute when supplied")
    void statusSettingPropagates() {
        var store = MessageFixtures.temporaryStore(
                Jid.of("12025550100@s.whatsapp.net"),
                Jid.of("258252122116273@lid"));
        var stanza = new MetaStanza(store);
        var node = stanza.buildChat(Jid.statusBroadcastAccount(),
                LinkedMessageContainer.of("status body"), "contacts", null);
        assertNotNull(node, "status messages with status_setting must emit <meta>");
        assertEquals("meta", node.description());
        assertEquals("contacts", node.getAttributeAsString("status_setting").orElseThrow(),
                "status_setting must propagate verbatim onto <meta status_setting=...>");
    }

    @Test
    @DisplayName("buildChat: AI thread id propagates as conversation_thread_id")
    void aiThreadIdPropagates() {
        var store = MessageFixtures.temporaryStore(
                Jid.of("12025550100@s.whatsapp.net"), null);
        var stanza = new MetaStanza(store);
        var hashedThreadId = "abcdef0123456789";
        var node = stanza.buildChat(
                Jid.of("12025550200@s.whatsapp.net"),
                LinkedMessageContainer.of("hi"),
                null, hashedThreadId);
        assertNotNull(node, "messages in an AI thread must emit <meta conversation_thread_id=...>");
        assertEquals(hashedThreadId, node.getAttributeAsString("conversation_thread_id").orElseThrow());
    }
}
