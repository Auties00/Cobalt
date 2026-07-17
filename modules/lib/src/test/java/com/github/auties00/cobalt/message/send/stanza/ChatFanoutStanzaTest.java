package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural tests for {@link ChatFanoutStanza}, pinning the outer-stanza
 * dimensions {@link ChatFanoutStanza#build} must round-trip without losing
 * information: stanza type, edit marker, addressing mode, peer-recipient
 * attributes, device-count topology, and the absent-when-null attribute
 * discipline. The 479 regression guard verifies that LID self-sends produce
 * LID-form participant JIDs. The tests drive the 27-arg builder directly via
 * the {@link #build} helper that fills every non-essential argument with
 * {@code null} or {@code false}; the byte-equality oracle for the same shape
 * lives in {@link ChatFanoutLiveOracleTest}.
 */
@DisplayName("ChatFanoutStanza")
class ChatFanoutStanzaTest {

    private static final Jid CHAT_PN = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid CHAT_LID = Jid.of("258252122116273@lid");

    private static final Jid SELF_LID = Jid.of("258252122116273@lid");

    private static final Jid DEVICE_PRIMARY_LID = Jid.of("258252122116273:0@lid");

    private static final Jid DEVICE_77_LID = Jid.of("258252122116273:77@lid");

    private static final Jid DEVICE_73_LID = Jid.of("258252122116273:73@lid");

    private static final byte[] CIPHERTEXT = new byte[]{1, 2, 3, 4};

    @Test
    @DisplayName("single primary device -> <enc> as direct child of <message> (no <participants>)")
    void singlePrimaryFlatStructure() {
        var payload = new MessageEncryptedPayload(
                MessageEncryptionType.MSG, CIPHERTEXT, Jid.of("12025550100:0@s.whatsapp.net"));
        var stanza = build(CHAT_PN, "text", List.of(payload), null, null);

        assertEquals("message", stanza.description());
        assertTrue(stanza.getChild("enc").isPresent(),
                "single-primary fanout must place <enc> as a direct child of <message>");
        assertFalse(stanza.getChild("participants").isPresent(),
                "single-primary fanout must NOT wrap in <participants>");
    }

    @Test
    @DisplayName("multi-device fanout -> <enc> wrapped in <participants>, one <to jid=...> per device")
    void multiDeviceParticipantsStructure() {
        var payloads = List.of(
                new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_PRIMARY_LID),
                new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_73_LID),
                new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID));
        var stanza = build(CHAT_LID, "text", payloads, null, "lid");

        assertFalse(stanza.getChild("enc").isPresent(),
                "multi-device fanout must NOT place a top-level <enc>");
        var participants = stanza.getChild("participants").orElseThrow();
        var toNodes = participants.streamChildren("to").toList();
        assertEquals(3, toNodes.size(), "one <to jid=...> per encrypted device");
    }

    // 479 guard: pre-fix the server rejected PN-form participant JIDs on a LID outer chat.
    // Every payload's recipientJid is LID-projected upstream in DeviceService.getUserFanout
    // before ChatFanoutStanza sees it; this test pins that invariant.
    @Test
    @DisplayName("479 regression guard: self-send via LID -> every participant <to jid=...> is @lid")
    void selfSendUsesLidParticipants() {
        var payloads = List.of(
                new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_PRIMARY_LID),
                new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_73_LID),
                new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_77_LID));
        var stanza = build(SELF_LID, "text", payloads, null, "lid");

        assertEquals(SELF_LID.toString(), stanza.getAttributeAsString("to").orElseThrow(),
                "outer <message to=...> must be LID-form for self-send");

        var participants = stanza.getChild("participants").orElseThrow();
        var toNodes = participants.streamChildren("to").toList();
        assertEquals(3, toNodes.size());
        for (var to : toNodes) {
            var jidAttr = to.getAttributeAsString("jid").orElseThrow();
            assertTrue(jidAttr.endsWith("@lid"),
                    "participant <to jid=...> must be LID-form, got: " + jidAttr);
            assertFalse(jidAttr.endsWith("@s.whatsapp.net"),
                    "participant <to jid=...> must NOT be PN-form when outer addressing_mode is LID: " + jidAttr);
        }
    }

    @Test
    @DisplayName("addressing_mode attribute propagates to the <message> attrs")
    void addressingModeAttribute() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID);
        var stanza = build(CHAT_LID, "text", List.of(payload), null, "lid");
        assertEquals("lid", stanza.getAttributeAsString("addressing_mode").orElseThrow());
    }

    @Test
    @DisplayName("edit attribute propagates to the <message> attrs")
    void editAttribute() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID);
        var stanza = build(CHAT_LID, "text", List.of(payload), "7", "lid");
        assertEquals("7", stanza.getAttributeAsString("edit").orElseThrow(),
                "edit=7 is the status-revoke marker; must propagate verbatim");
    }

    @Test
    @DisplayName("stanza type propagates to the <message> type attribute")
    void typeAttribute() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID);
        for (var type : List.of("text", "media", "reaction", "poll", "event")) {
            var stanza = build(CHAT_PN, type, List.of(payload), null, null);
            assertEquals(type, stanza.getAttributeAsString("type").orElseThrow(),
                    "stanza type=" + type + " must propagate to outer <message type=...>");
        }
    }

    @Test
    @DisplayName("message id propagates to the <message> id attribute")
    void idAttribute() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID);
        var stanza = ChatFanoutStanza.build(
                "3EB0CAFEBABE",
                CHAT_PN, "text", List.of(payload),
                null, null, null, null, null, null, null,
                false, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        ).build();

        assertEquals("3EB0CAFEBABE", stanza.getAttributeAsString("id").orElseThrow());
    }

    @Test
    @DisplayName("absent optional attributes are absent on the wire (never empty strings)")
    void optionalAttributesAreNullOnWire() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID);
        var stanza = build(CHAT_PN, "text", List.of(payload), null, null);

        assertTrue(stanza.getAttribute("edit").isEmpty(), "edit attribute must be absent when null");
        assertTrue(stanza.getAttribute("addressing_mode").isEmpty(), "addressing_mode must be absent when null");
        assertTrue(stanza.getAttribute("device_fanout").isEmpty(), "device_fanout must be absent when null");
        assertTrue(stanza.getAttribute("peer_recipient_lid").isEmpty(), "peer_recipient_lid must be absent when null");
        assertTrue(stanza.getAttribute("peer_recipient_pn").isEmpty(), "peer_recipient_pn must be absent when null");
        assertTrue(stanza.getAttribute("recipient_pn").isEmpty(), "recipient_pn must be absent when null");
        assertTrue(stanza.getAttribute("peer_recipient_username").isEmpty(), "peer_recipient_username must be absent when null");
    }

    @Test
    @DisplayName("CIPHERTEXT_VERSION is set on every <enc> via attribute v=...")
    void encVersionAttribute() {
        var payloads = List.of(
                new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_PRIMARY_LID),
                new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_73_LID));
        var multiStanza = build(CHAT_LID, "text", payloads, null, "lid");
        var participants = multiStanza.getChild("participants").orElseThrow();
        for (var to : participants.streamChildren("to").toList()) {
            var enc = to.getChild("enc").orElseThrow();
            assertTrue(enc.getAttribute("v").isPresent(),
                    "every <enc> must carry the v=... (CIPHERTEXT_VERSION) attribute");
        }
    }

    @Test
    @DisplayName("null payloads list throws NullPointerException")
    void nullPayloadsThrows() {
        assertThrows(NullPointerException.class,
                () -> ChatFanoutStanza.build(
                        "3EB0CAFEBABE",
                        CHAT_PN, "text", null,
                        null, null, null, null, null, null, null,
                        false, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null
                ));
    }

    @Test
    @DisplayName("null messageId throws NullPointerException")
    void nullMessageIdThrows() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID);
        assertThrows(NullPointerException.class,
                () -> ChatFanoutStanza.build(
                        null,
                        CHAT_PN, "text", List.of(payload),
                        null, null, null, null, null, null, null,
                        false, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null
                ));
    }

    @Test
    @DisplayName("null chatJid throws NullPointerException")
    void nullChatJidThrows() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_77_LID);
        assertThrows(NullPointerException.class,
                () -> ChatFanoutStanza.build(
                        "3EB0CAFEBABE",
                        null, "text", List.of(payload),
                        null, null, null, null, null, null, null,
                        false, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null
                ));
    }

    // Fills every optional argument with null/false so each test supplies only what it exercises.
    private static Stanza build(
            Jid chatJid,
            String type,
            List<MessageEncryptedPayload> payloads,
            String editAttribute,
            String addressingMode
    ) {
        return ChatFanoutStanza.build(
                "3EB0CAFEBABE",
                chatJid,
                type,
                payloads,
                editAttribute,
                addressingMode,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ).build();
    }
}
