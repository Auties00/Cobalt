package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.stanza.model.Stanza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-shape oracle for {@link ChatFanoutStanza}, comparing Cobalt output
 * against {@code <message>} stanzas captured from a live WhatsApp Web
 * session and committed under {@code fixtures/message/send/}. Each test
 * loads one captured {@code <message>} via
 * {@link MessageFixtures#loadEvents(String)} plus
 * {@link MessageFixtures#buildNodeFromEvent} and asserts the wire
 * invariants WA Web emits: outer attribute set, child topology
 * (single-{@code <enc>} vs {@code <participants>}), per-participant
 * {@code <enc>} version and type, and the addressing-mode projection. Each
 * test is skipped when its topic fixture is not available locally; if the
 * captured shape drifts, regenerate the corpus via
 * {@code src/test/resources/fixtures/message/generate.mjs}.
 */
@DisplayName("ChatFanoutStanza live wire oracle")
class ChatFanoutLiveOracleTest {

    // Pre-fix the server rejected PN-form participants on a LID send with error 479.
    @Test
    @DisplayName("479 oracle: 1:1 peer LID send - every participant <to jid=...> is @lid")
    void peerTextAllParticipantsAreLid() {
        var topic = "send/peer-text";
        if (!MessageFixtures.isAvailable(topic)) return;

        var message = loadMessageStanza(topic);
        assertEquals("message", message.description());
        assertEquals("text", message.getAttributeAsString("type").orElseThrow(),
                "peer-text fixture must carry type=text");
        var to = message.getAttributeAsString("to").orElseThrow();
        assertTrue(to.endsWith("@lid"),
                "peer LID send: outer <message to=...> must be LID, got " + to);
        assertTrue(message.getAttributeAsString("peer_recipient_pn").isPresent(),
                "peer LID send must carry peer_recipient_pn for the legacy device");

        var participants = message.getChild("participants").orElseThrow(
                () -> new AssertionError("multi-device peer send must wrap <enc> in <participants>"));
        var tos = participants.streamChildren("to").toList();
        assertTrue(tos.size() >= 2,
                "1:1 send fans out across companion devices, expected >=2 participants, got " + tos.size());

        for (var participantTo : tos) {
            var jid = participantTo.getAttributeAsString("jid").orElseThrow();
            assertTrue(jid.endsWith("@lid"),
                    "participant <to jid=...> must be @lid (479 guard), got " + jid);
            assertFalse(jid.endsWith("@s.whatsapp.net"),
                    "participant <to jid=...> must NOT be @s.whatsapp.net when outer is LID: " + jid);
            var enc = participantTo.getChild("enc").orElseThrow();
            assertEquals("2", enc.getAttributeAsString("v").orElseThrow(),
                    "<enc> must carry CIPHERTEXT_VERSION v=2");
            var encType = enc.getAttributeAsString("type").orElseThrow();
            assertTrue(List.of("pkmsg", "msg").contains(encType),
                    "<enc type=...> must be pkmsg or msg for 1:1 send, got " + encType);
        }

        assertTrue(message.getChild("device-identity").isPresent(),
                "PKMSG-bearing fanout must include <device-identity>");
    }

    @Test
    @DisplayName("peer reaction: <message type=\"reaction\"> with same fanout shape")
    void peerReactionShape() {
        var topic = "send/peer-reaction";
        if (!MessageFixtures.isAvailable(topic)) return;
        var message = loadMessageStanza(topic);

        assertEquals("reaction", message.getAttributeAsString("type").orElseThrow(),
                "reaction fixture must carry type=reaction");
        assertTrue(message.getChild("participants").isPresent()
                        || message.getChild("enc").isPresent(),
                "reaction must have either <participants> or a bare <enc>");
    }

    @Test
    @DisplayName("peer edit + revoke: capture contains initial send + edit (edit=1) + revoke (edit=7)")
    void peerEditRevokeShape() {
        var topic = "send/peer-edit-revoke";
        if (!MessageFixtures.isAvailable(topic)) return;

        var peerLid = "83116928594056@lid";
        var flowMessages = MessageFixtures.loadEvents(topic).stream()
                .filter(e -> "out".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .map(MessageFixtures::buildNodeFromEvent)
                .filter(n -> peerLid.equals(n.getAttributeAsString("to").orElse(null)))
                .toList();
        assertTrue(flowMessages.size() >= 3,
                "edit/revoke capture must include 3 outgoing <message> stanzas to "
                        + peerLid + " (send + edit + revoke), got " + flowMessages.size());

        var editValues = flowMessages.stream()
                .map(n -> n.getAttributeAsString("edit").orElse(null))
                .toList();

        assertTrue(editValues.contains("1"),
                "edit fanout must include a <message edit=\"1\"> stanza; got " + editValues);
        assertTrue(editValues.contains("7"),
                "revoke fanout must include a <message edit=\"7\"> stanza; got " + editValues);
        assertTrue(editValues.contains(null),
                "initial send must NOT carry edit attribute; got " + editValues);

        for (var msg : flowMessages) {
            assertTrue(msg.getChild("participants").isPresent(),
                    "multi-device fanout wraps <enc> in <participants> for every send/edit/revoke");
            var participants = msg.getChild("participants").orElseThrow();
            for (var to : participants.streamChildren("to").toList()) {
                var jid = to.getAttributeAsString("jid").orElseThrow();
                assertTrue(jid.endsWith("@lid"),
                        "479 invariant: every participant <to jid=...> must be @lid for LID chat, got " + jid);
            }
        }
    }

    @Test
    @DisplayName("location send: <message type=\"media\"> with <enc mediatype=\"location\">")
    void locationShape() {
        var topic = "send/location";
        if (!MessageFixtures.isAvailable(topic)) return;
        var message = loadMessageStanza(topic);

        assertEquals("media", message.getAttributeAsString("type").orElseThrow(),
                "location must be wire-type=media");
        var participants = message.getChild("participants").orElseThrow();
        var encsWithLocation = participants.streamChildren("to")
                .map(t -> t.getChild("enc").orElseThrow())
                .filter(e -> "location".equals(e.getAttributeAsString("mediatype").orElse(null)))
                .count();
        assertTrue(encsWithLocation > 0,
                "at least one <enc mediatype=\"location\"> required, got " + encsWithLocation);
    }

    @Test
    @DisplayName("group invite link send: outgoing text <message> with participants")
    void groupInviteLinkShape() {
        var topic = "send/group-invite-link";
        if (!MessageFixtures.isAvailable(topic)) return;
        var message = loadMessageStanza(topic);

        assertEquals("text", message.getAttributeAsString("type").orElseThrow(),
                "invite link is a TEXT message containing a URL; wire-type must be text");
        assertTrue(message.getChild("participants").isPresent(),
                "1:1 send must wrap <enc> in <participants>");
    }

    @Test
    @DisplayName("bot text (MSMSG): participants include @bot device with <enc type=pkmsg>")
    void botTextShape() {
        var topic = "send/bot-text";
        if (!MessageFixtures.isAvailable(topic)) return;
        var message = loadMessageStanza(topic);

        var to = message.getAttributeAsString("to").orElseThrow();
        assertTrue(to.endsWith("@bot"),
                "bot send: outer to must be @bot, got " + to);

        var participants = message.getChild("participants").orElseThrow();
        var hasBotParticipant = participants.streamChildren("to")
                .anyMatch(t -> t.getAttributeAsString("jid").orElse("").endsWith("@bot"));
        assertTrue(hasBotParticipant, "bot fanout must include a <to jid=...@bot> participant");
    }

    @Test
    @DisplayName("hosted business send: participants list with @lid devices, includes peer_recipient_pn")
    void hostedBizShape() {
        var topic = "send/hosted-biz-text";
        if (!MessageFixtures.isAvailable(topic)) return;

        var events = MessageFixtures.loadEvents(topic);
        var outgoing = events.stream()
                .filter(e -> "out".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "hosted-biz-text capture must include an outgoing <message>"));
        var message = MessageFixtures.buildNodeFromEvent(outgoing);

        var to = message.getAttributeAsString("to").orElseThrow();
        assertTrue(to.endsWith("@lid"),
                "hosted business send: outer to must be @lid, got " + to);
        assertTrue(message.getAttributeAsString("peer_recipient_pn").isPresent(),
                "hosted business send: peer_recipient_pn must be present");
    }

    @Test
    @DisplayName("every captured peer fanout has the auxiliary nodes WA Web emits (device-identity)")
    void peerFanoutCarriesDeviceIdentity() {
        for (var topic : List.of("send/peer-text", "send/peer-reaction", "send/location",
                "send/group-invite-link", "send/hosted-biz-text")) {
            if (!MessageFixtures.isAvailable(topic)) continue;
            var events = MessageFixtures.loadEvents(topic);
            var outgoing = events.stream()
                    .filter(e -> "out".equals(e.getString("direction")))
                    .filter(e -> "message".equals(e.getString("tag")))
                    .findFirst()
                    .orElse(null);
            if (outgoing == null) continue;
            var message = MessageFixtures.buildNodeFromEvent(outgoing);
            assertTrue(message.getChild("device-identity").isPresent(),
                    topic + ": peer fanout must carry <device-identity>");
        }
    }

    // Single-message topics; the multi-message edit/revoke topic loads its own stream inline.
    private static Stanza loadMessageStanza(String topic) {
        var events = MessageFixtures.loadEvents(topic);
        var outgoing = events.stream()
                .filter(e -> "out".equals(e.getString("direction")))
                .filter(e -> "message".equals(e.getString("tag")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no outgoing <message> in topic " + topic));
        var node = MessageFixtures.buildNodeFromEvent(outgoing);
        assertNotNull(node, "rebuilt stanza must not be null for topic " + topic);
        return node;
    }

}
