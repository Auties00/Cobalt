package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessageBodyBuilder;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessageFooterBuilder;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessageInteractiveHeaderBuilder;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessageNativeFlowMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.interactive.NativeFlowMessageInteractiveMessageNativeFlowButtonBuilder;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessageBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Outbound mapping cells for {@link CloudMessageEncoder}: each cell builds a {@link LinkedMessageContainer}
 * and asserts the produced Cloud {@code interactive} stanza against the Meta documented shape.
 */
@DisplayName("CloudMessageEncoder")
class CloudMessageEncoderTest {
    private static final Jid RECIPIENT = Jid.of("15551234567");

    private static JSONObject interactive(LinkedMessageContainer container) {
        return CloudMessageEncoder.encode(RECIPIENT, container).getJSONObject("interactive");
    }

    @Test
    @DisplayName("cta_url native flow button yields a cta_url interactive stanza")
    void ctaUrl() {
        var btn = new NativeFlowMessageInteractiveMessageNativeFlowButtonBuilder()
                .name("cta_url")
                .buttonParamsJson("{\"display_text\":\"View receipt\",\"url\":\"https://example.com/receipt/123\"}")
                .build();
        var flow = new InteractiveMessageNativeFlowMessageBuilder().buttons(List.of(btn)).build();
        var interactive = new InteractiveMessageBuilder()
                .body(new InteractiveMessageBodyBuilder().text("Tap below to view your receipt").build())
                .footer(new InteractiveMessageFooterBuilder().text("Acme Inc").build())
                .header(new InteractiveMessageInteractiveHeaderBuilder().title("Order confirmed").build())
                .nativeFlowMessage(flow)
                .build();

        var node = interactive(LinkedMessageContainer.of(interactive));
        assertEquals("cta_url", node.getString("type"));
        assertEquals("Tap below to view your receipt", node.getJSONObject("body").getString("text"));
        assertEquals("Acme Inc", node.getJSONObject("footer").getString("text"));
        assertEquals("text", node.getJSONObject("header").getString("type"));
        assertEquals("Order confirmed", node.getJSONObject("header").getString("text"));
        var action = node.getJSONObject("action");
        assertEquals("cta_url", action.getString("name"));
        assertEquals("View receipt", action.getJSONObject("parameters").getString("display_text"));
        assertEquals("https://example.com/receipt/123", action.getJSONObject("parameters").getString("url"));
    }

    @Test
    @DisplayName("flow native flow button echoes the button parameters verbatim")
    void flow() {
        var paramsJson = "{\"flow_token\":\"AQAAAA.\",\"flow_id\":\"1234567890\",\"flow_cta\":\"Book!\",\"flow_action\":\"navigate\"}";
        var btn = new NativeFlowMessageInteractiveMessageNativeFlowButtonBuilder()
                .name("flow")
                .buttonParamsJson(paramsJson)
                .build();
        var flow = new InteractiveMessageNativeFlowMessageBuilder().buttons(List.of(btn)).build();
        var interactive = new InteractiveMessageBuilder()
                .body(new InteractiveMessageBodyBuilder().text("Pick a time").build())
                .nativeFlowMessage(flow)
                .build();

        var node = interactive(LinkedMessageContainer.of(interactive));
        assertEquals("flow", node.getString("type"));
        var parameters = node.getJSONObject("action").getJSONObject("parameters");
        assertEquals("flow", node.getJSONObject("action").getString("name"));
        assertEquals("1234567890", parameters.getString("flow_id"));
        assertEquals("3", parameters.getString("flow_message_version"));
    }

    @Test
    @DisplayName("media image header overrides a text title on a cta_url stanza")
    void mediaHeader() {
        var btn = new NativeFlowMessageInteractiveMessageNativeFlowButtonBuilder()
                .name("cta_url")
                .buttonParamsJson("{\"display_text\":\"Open\",\"url\":\"https://example.com\"}")
                .build();
        var flow = new InteractiveMessageNativeFlowMessageBuilder().buttons(List.of(btn)).build();
        var header = new InteractiveMessageInteractiveHeaderBuilder()
                .title("ignored")
                .imageMessage(new ImageMessageBuilder().mediaUrl("https://example.com/hero.jpg").build())
                .build();
        var interactive = new InteractiveMessageBuilder()
                .body(new InteractiveMessageBodyBuilder().text("...").build())
                .header(header)
                .nativeFlowMessage(flow)
                .build();

        var node = interactive(LinkedMessageContainer.of(interactive));
        assertEquals("image", node.getJSONObject("header").getString("type"));
        assertEquals("https://example.com/hero.jpg", node.getJSONObject("header").getJSONObject("image").getString("link"));
    }

    @Test
    @DisplayName("send_location native flow button yields a location_request_message stanza")
    void locationRequest() {
        var flow = new InteractiveMessageNativeFlowMessageBuilder()
                .buttons(List.of(new NativeFlowMessageInteractiveMessageNativeFlowButtonBuilder().name("send_location").build()))
                .build();
        var interactive = new InteractiveMessageBuilder()
                .body(new InteractiveMessageBodyBuilder().text("Please share your delivery location.").build())
                .footer(new InteractiveMessageFooterBuilder().text("dropped").build())
                .nativeFlowMessage(flow)
                .build();

        var node = interactive(LinkedMessageContainer.of(interactive));
        assertEquals("location_request_message", node.getString("type"));
        assertEquals("send_location", node.getJSONObject("action").getString("name"));
        assertEquals("Please share your delivery location.", node.getJSONObject("body").getString("text"));
        assertNull(node.get("footer"));
    }

    @Test
    @DisplayName("address_message native flow button passes its parameters through")
    void addressMessage() {
        var params = "{\"country\":\"IN\",\"values\":{\"name\":\"CUSTOMER_NAME\",\"city\":\"Mumbai\"}}";
        var flow = new InteractiveMessageNativeFlowMessageBuilder()
                .buttons(List.of(new NativeFlowMessageInteractiveMessageNativeFlowButtonBuilder().name("address_message").buttonParamsJson(params).build()))
                .build();
        var interactive = new InteractiveMessageBuilder()
                .body(new InteractiveMessageBodyBuilder().text("Tell us your address.").build())
                .nativeFlowMessage(flow)
                .build();

        var node = interactive(LinkedMessageContainer.of(interactive));
        assertEquals("address_message", node.getString("type"));
        assertEquals("address_message", node.getJSONObject("action").getString("name"));
        var parameters = node.getJSONObject("action").getJSONObject("parameters");
        assertEquals("IN", parameters.getString("country"));
        assertEquals("Mumbai", parameters.getJSONObject("values").getString("city"));
    }

}
