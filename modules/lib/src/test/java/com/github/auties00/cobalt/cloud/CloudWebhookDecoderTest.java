package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.cloud.content.CloudContactsArrayContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudInteractiveResponseContent;
import com.github.auties00.cobalt.wire.cloud.content.CloudOrderContent;
import com.github.auties00.cobalt.wire.core.message.EmptyContent;
import com.github.auties00.cobalt.wire.core.message.MessageContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Inbound mapping cells for {@link CloudWebhookDecoder}: each cell wraps a single webhook message in a
 * change value, decodes it, and asserts the resulting Cloud-native content body.
 */
@DisplayName("CloudWebhookDecoder")
class CloudWebhookDecoderTest {
    private static MessageContainer decode(String messageJson) {
        var value = new JSONObject();
        value.put("messages", JSON.parseArray("[" + messageJson + "]"));
        var messages = CloudWebhookDecoder.decodeMessages(value);
        assertEquals(1, messages.size());
        return messages.getFirst().message();
    }

    private static Object content(String messageJson) {
        return decode(messageJson).content();
    }

    @Test
    @DisplayName("nfm_reply decodes into a native flow response body")
    void nfmReply() {
        var content = content("""
                {"from":"16505551234","id":"wamid.X","timestamp":"1700000000","type":"interactive",
                 "interactive":{"type":"nfm_reply","nfm_reply":{"name":"flow","body":"Sent","response_json":"{\\"a\\":1}"}}}""");
        var response = assertInstanceOf(CloudInteractiveResponseContent.class, content);
        assertEquals("flow", response.name().orElseThrow());
        assertEquals("{\"a\":1}", response.responseJson().orElseThrow());
        assertEquals("Sent", response.body().orElseThrow());
    }

    @Test
    @DisplayName("contacts decodes into a contacts-array body with a reconstructed vCard")
    void contacts() {
        var content = content("""
                {"from":"16505551234","id":"wamid.X","timestamp":"1700000000","type":"contacts",
                 "contacts":[{"name":{"formatted_name":"John Smith"},
                 "phones":[{"phone":"+16505551234","wa_id":"16505551234","type":"CELL"}],
                 "org":{"company":"Acme"}}]}""");
        var array = assertInstanceOf(CloudContactsArrayContent.class, content);
        assertEquals(1, array.contacts().size());
        var first = array.contacts().getFirst();
        assertEquals("John Smith", first.displayName().orElseThrow());
        var vcard = first.vcard().orElseThrow();
        assertTrue(vcard.contains("FN:John Smith"), vcard);
        assertTrue(vcard.contains("TEL;type=CELL;waid=16505551234:+16505551234"), vcard);
        assertTrue(vcard.contains("X-WA-BIZ-NAME:Acme"), vcard);
    }

    @Test
    @DisplayName("order decodes into an order body with a derived total")
    void order() {
        var content = content("""
                {"from":"16505551234","id":"wamid.X","timestamp":"1700000000","type":"order",
                 "order":{"catalog_id":"4928452840550143","text":"please deliver fast",
                 "product_items":[{"product_retailer_id":"03-Pack","quantity":1,"item_price":210,"currency":"HKD"}]},
                 "context":{"from":"16315551234","id":"wamid.PRODUCT_MSG_ID"}}""");
        var order = assertInstanceOf(CloudOrderContent.class, content);
        assertEquals("please deliver fast", order.message().orElseThrow());
        assertEquals(1, order.itemCount().orElseThrow());
        assertEquals("HKD", order.totalCurrencyCode().orElseThrow());
        assertEquals(210000L, order.totalAmount1000().orElseThrow());
        assertEquals("wamid.PRODUCT_MSG_ID", order.orderRequestMessageId().orElseThrow());
    }

    @Test
    @DisplayName("system and unknown types fall back to an empty container")
    void fallback() {
        var system = decode("""
                {"from":"16505551234","id":"wamid.X","timestamp":"1700000000","type":"system",
                 "system":{"body":"changed","type":"customer_changed_number","new_wa_id":"16505559999"}}""");
        assertSame(EmptyContent.of(), system.content());
        var unknown = decode("""
                {"from":"16505551234","id":"wamid.X","timestamp":"1700000000","type":"unknown",
                 "errors":[{"code":131051,"title":"Message type is not currently supported."}]}""");
        assertSame(EmptyContent.of(), unknown.content());
    }
}
