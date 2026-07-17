package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the static stanza builders on {@link ParticipantsStanza}: the
 * {@link ParticipantsStanza#buildSenderKeyDistribution(List, Map, String)} sender-key distribution wrapper, the
 * {@link ParticipantsStanza#buildContentBindingOnly(List, Map)} content-binding-only wrapper, and the
 * {@link ParticipantsStanza#requiresIdentityNode(List)} pre-key probe. The builders are driven directly with raw
 * payload and binding inputs, so no store seeding is required.
 */
@DisplayName("ParticipantsStanza")
class ParticipantsStanzaTest {

    private static final Jid DEVICE_A = Jid.of("12025550100:0@s.whatsapp.net");

    // Companion device of the same user as DEVICE_A; both resolve to USER_A.
    private static final Jid DEVICE_B = Jid.of("12025550100:73@s.whatsapp.net");

    private static final Jid USER_A = Jid.of("12025550100@s.whatsapp.net");

    private static final byte[] CIPHERTEXT = new byte[]{1, 2, 3};

    private static final byte[] RCAT_TAG = new byte[]{4, 5, 6};

    @Test
    @DisplayName("buildSenderKeyDistribution: empty payload list returns null")
    void emptyPayloadsReturnsNull() {
        assertNull(ParticipantsStanza.buildSenderKeyDistribution(List.of(), null, null));
    }

    @Test
    @DisplayName("buildSenderKeyDistribution: null payload list returns null")
    void nullPayloadsReturnsNull() {
        assertNull(ParticipantsStanza.buildSenderKeyDistribution(null, null, null));
    }

    @Test
    @DisplayName("buildSenderKeyDistribution: decrypt-fail defaults to \"hide\" on every <enc>")
    void defaultDecryptFailHide() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_A);
        var participants = ParticipantsStanza.buildSenderKeyDistribution(
                List.of(payload), null, null);

        var enc = participants.getChild("to").orElseThrow()
                .getChild("enc").orElseThrow();
        assertEquals("hide", enc.getAttributeAsString("decrypt-fail").orElseThrow(),
                "SK distribution must never produce a visible decrypt-fail placeholder");
    }

    @Test
    @DisplayName("buildSenderKeyDistribution: explicit decrypt-fail overrides the default")
    void overrideDecryptFail() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_A);
        var participants = ParticipantsStanza.buildSenderKeyDistribution(
                List.of(payload), null, "custom-fail");

        var enc = participants.getChild("to").orElseThrow()
                .getChild("enc").orElseThrow();
        assertEquals("custom-fail", enc.getAttributeAsString("decrypt-fail").orElseThrow());
    }

    @Test
    @DisplayName("buildSenderKeyDistribution: content binding is keyed by user JID, not device JID")
    void contentBindingByUserJid() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_A);
        var bindings = Map.of(USER_A, RCAT_TAG);
        var participants = ParticipantsStanza.buildSenderKeyDistribution(
                List.of(payload), bindings, null);

        var to = participants.getChild("to").orElseThrow();
        assertTrue(to.getChild("content_binding").isPresent(),
                "RCAT keyed by user JID must attach <content_binding> under <to> for any matching device");
    }

    @Test
    @DisplayName("buildSenderKeyDistribution: no binding for unmapped user JID")
    void noBindingForUnmappedUser() {
        var payload = new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_A);
        var otherUser = Jid.of("19255550000@s.whatsapp.net");
        var bindings = Map.of(otherUser, RCAT_TAG);
        var participants = ParticipantsStanza.buildSenderKeyDistribution(
                List.of(payload), bindings, null);

        var to = participants.getChild("to").orElseThrow();
        assertFalse(to.getChild("content_binding").isPresent(),
                "RCAT for a different user must not bleed onto the <to> stanza");
    }

    @Test
    @DisplayName("buildSenderKeyDistribution: payloads with null recipientJid are skipped")
    void nullRecipientSkipped() {
        var withRecipient = new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_A);
        var withoutRecipient = new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, null);
        var participants = ParticipantsStanza.buildSenderKeyDistribution(
                List.of(withRecipient, withoutRecipient), null, null);

        var toCount = participants.streamChildren("to").count();
        assertEquals(1, toCount, "payloads with null recipientJid (sender-key group cipher) must be skipped");
    }

    @Test
    @DisplayName("buildContentBindingOnly: returns null when no bindings match the device list")
    void contentBindingOnlyNullWhenNoMatch() {
        var bindings = Map.of(Jid.of("19255550000@s.whatsapp.net"), RCAT_TAG);
        var result = ParticipantsStanza.buildContentBindingOnly(List.of(DEVICE_A), bindings);
        assertNull(result, "no matching user JID -> entire <participants> stanza is null");
    }

    @Test
    @DisplayName("buildContentBindingOnly: builds <to><content_binding/></to> only for matched users")
    void contentBindingOnlyEmitsForMatch() {
        var bindings = Map.of(USER_A, RCAT_TAG);
        var participants = ParticipantsStanza.buildContentBindingOnly(List.of(DEVICE_A, DEVICE_B), bindings);

        var toNodes = participants.streamChildren("to").toList();
        assertEquals(2, toNodes.size(),
                "every device JID whose user matches the RCAT map gets a <to>; here both DEVICE_A and DEVICE_B share USER_A");
        for (var to : toNodes) {
            assertTrue(to.getChild("content_binding").isPresent(),
                    "every emitted <to> in content-binding-only mode must carry <content_binding>");
        }
    }

    @Test
    @DisplayName("requiresIdentityNode: true iff at least one payload is a PreKey message")
    void requiresIdentityNode() {
        var pkmsg = new MessageEncryptedPayload(MessageEncryptionType.PKMSG, CIPHERTEXT, DEVICE_A);
        var msg = new MessageEncryptedPayload(MessageEncryptionType.MSG, CIPHERTEXT, DEVICE_B);

        assertFalse(ParticipantsStanza.requiresIdentityNode(List.of(msg)),
                "all-MSG fanout does not need a <device-identity>");
        assertTrue(ParticipantsStanza.requiresIdentityNode(List.of(pkmsg)),
                "any PKMSG triggers the <device-identity>");
        assertTrue(ParticipantsStanza.requiresIdentityNode(List.of(msg, pkmsg)),
                "even a single PKMSG mixed in triggers the <device-identity>");
        assertFalse(ParticipantsStanza.requiresIdentityNode(null),
                "null payload list must return false, not throw");
    }
}
