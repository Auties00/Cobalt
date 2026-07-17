package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.cloud.CloudMessageInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decode cells covering the business-scoped user id (BSUID) fallback identity, where a hidden-number
 * sender omits {@code from}/{@code wa_id} and carries only {@code from_user_id}/{@code user_id}, plus the
 * regression guard that an outbound status without {@code conversation}/{@code pricing} still decodes.
 */
@DisplayName("Cloud webhook identity decode")
class CloudWebhookIdentityTest {
    private static JSONObject value(String json) {
        return JSON.parseObject(json);
    }

    // A numeric BSUID is used so the fallback identity also resolves a user JID; an opaque non-numeric
    // BSUID is covered separately by bsuidOpaqueIdentityNoJid.
    @Test
    @DisplayName("hidden-number inbound message falls back to the BSUID identity")
    void bsuidFallback() {
        var value = value("""
                {"messaging_product":"whatsapp","metadata":{"display_phone_number":"14255550123","phone_number_id":"46271669"},
                "contacts":[{"profile":{"name":"Hidden User"},"user_id":"99887766554433"}],
                "messages":[{"from_user_id":"99887766554433","id":"wamid.HBgLhidden","timestamp":"1723506230",
                "type":"text","text":{"body":"hi from a hidden number"}}]}""");
        var messages = CloudWebhookDecoder.decodeMessages(value);
        assertEquals(1, messages.size());
        var info = (CloudMessageInfo) messages.getFirst();
        assertEquals("wamid.HBgLhidden", info.key().id().orElseThrow());
        assertEquals(Jid.of("99887766554433"), info.key().parentJid().orElseThrow());
        assertEquals("Hidden User", info.pushName().orElseThrow());
    }

    @Test
    @DisplayName("opaque non-numeric BSUID decodes without a JID and without crashing")
    void bsuidOpaqueIdentityNoJid() {
        var value = value("""
                {"contacts":[{"profile":{"name":"Hidden User"},"user_id":"bsuid_ABC123"}],
                "messages":[{"from_user_id":"bsuid_ABC123","id":"wamid.H","timestamp":"1723506230",
                "type":"text","text":{"body":"hi"}}]}""");
        var messages = CloudWebhookDecoder.decodeMessages(value);
        assertEquals(1, messages.size());
        var info = (CloudMessageInfo) messages.getFirst();
        assertEquals("wamid.H", info.key().id().orElseThrow());
        assertTrue(info.key().parentJid().isEmpty());
        assertEquals("Hidden User", info.pushName().orElseThrow());
    }

    @Test
    @DisplayName("ordinary inbound message keeps the phone-scoped wa_id identity")
    void phoneScopedIdentity() {
        var value = value("""
                {"contacts":[{"profile":{"name":"Diego"},"wa_id":"12065550102"}],
                "messages":[{"from":"12065550102","id":"wamid.A","timestamp":"1723506230",
                "type":"text","text":{"body":"hi"}}]}""");
        var info = (CloudMessageInfo) CloudWebhookDecoder.decodeMessages(value).getFirst();
        assertEquals(Jid.of("12065550102"), info.key().parentJid().orElseThrow());
        assertEquals("Diego", info.pushName().orElseThrow());
    }

    @Test
    @DisplayName("status without conversation or pricing still decodes")
    void statusWithoutConversation() {
        var value = value("""
                {"statuses":[{"id":"wamid.X","status":"read","timestamp":"1736379100","recipient_id":"01234567890"}]}""");
        var statuses = CloudWebhookDecoder.decodeStatuses(value);
        assertEquals(1, statuses.size());
        var update = statuses.getFirst();
        assertFalse(update.deleted());
        assertEquals(MessageStatus.READ, update.info().status().orElseThrow());
    }
}
