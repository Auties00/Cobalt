package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.cloud.CloudCallDirection;
import com.github.auties00.cobalt.wire.cloud.CloudCallEvent;
import com.github.auties00.cobalt.wire.cloud.CloudCallPermissionResponse;
import com.github.auties00.cobalt.wire.cloud.CloudCallSession;
import com.github.auties00.cobalt.wire.cloud.CloudCallStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decode cells for the Calling webhook: the {@code calls} change feeds
 * {@link CloudWebhookDecoder#decodeCalls(JSONObject)} (the inbound signaling variants) and
 * {@link CloudWebhookDecoder#decodeCallStatuses(JSONObject)} (the business-initiated status variant),
 * while the interactive permission reply on the {@code messages} change feeds
 * {@link CloudWebhookDecoder#decodeCallPermissionReplies(JSONObject)}. Each variant of the sealed
 * {@link CloudCallEvent} is round-tripped from its raw webhook shape.
 */
@DisplayName("Cloud Calling webhook decode")
class CloudCallWebhookTest {
    private static JSONObject value(String json) {
        return JSON.parseObject(json);
    }

    @Test
    @DisplayName("connect event decodes to Connect with direction, sdp_type=offer, and the sdp")
    void decodeConnect() {
        var value = value("""
                {"calls":[{"id":"call-1","from":"15551234567","to":"12345678900","event":"connect",
                "timestamp":"1762216151","direction":"USER_INITIATED",
                "session":{"sdp_type":"offer","sdp":"the-sdp"}}]}""");
        var events = CloudWebhookDecoder.decodeCalls(value);
        assertEquals(1, events.size());
        var event = assertInstanceOf(CloudCallEvent.Connect.class, events.getFirst());
        assertEquals("call-1", event.callId());
        assertEquals("15551234567", event.from());
        assertEquals("12345678900", event.to().orElseThrow());
        assertEquals(CloudCallDirection.USER_INITIATED, event.direction().orElseThrow());
        assertEquals(CloudCallSession.Type.OFFER, event.session().orElseThrow().sdpType());
        assertEquals("the-sdp", event.session().orElseThrow().sdp());
        assertEquals(Instant.ofEpochSecond(1762216151L), event.timestamp().orElseThrow());
    }

    @Test
    @DisplayName("terminate event decodes to Terminate with status, duration, and start and end times")
    void decodeTerminate() {
        var value = value("""
                {"calls":[{"id":"call-2","from":"15551234567","to":"12345678900","event":"terminate",
                "direction":"USER_INITIATED","timestamp":"1733734771","status":"COMPLETED",
                "start_time":1733734738,"end_time":1733734771,"duration":33}]}""");
        var event = assertInstanceOf(CloudCallEvent.Terminate.class, CloudWebhookDecoder.decodeCalls(value).getFirst());
        assertEquals("call-2", event.callId());
        assertEquals("COMPLETED", event.status().orElseThrow());
        assertEquals(33, event.durationSeconds().orElseThrow());
        assertEquals(Instant.ofEpochSecond(1733734738L), event.startTime().orElseThrow());
        assertEquals(Instant.ofEpochSecond(1733734771L), event.endTime().orElseThrow());
    }

    @Test
    @DisplayName("business-initiated call status decodes to Status with status and direction")
    void decodeCallStatus() {
        var value = value("""
                {"statuses":[{"id":"wacid.AB","timestamp":"1671644824","type":"call","status":"RINGING",
                "recipient_id":"163155536021"}]}""");
        var events = CloudWebhookDecoder.decodeCallStatuses(value);
        assertEquals(1, events.size());
        var event = events.getFirst();
        assertEquals("wacid.AB", event.callId());
        assertEquals("163155536021", event.from());
        assertEquals(CloudCallStatus.RINGING, event.status());
        assertEquals(CloudCallDirection.BUSINESS_INITIATED, event.direction());
        assertEquals(Instant.ofEpochSecond(1671644824L), event.timestamp().orElseThrow());
    }

    @Test
    @DisplayName("accept permission reply decodes to PermissionReply with response, source, and expiration")
    void decodePermissionAccept() {
        var value = value("""
                {"messages":[{"from":"15551234567","id":"wamid.R","timestamp":"1747659443","type":"interactive",
                "interactive":{"type":"call_permission_reply",
                "call_permission_reply":{"response":"accept","response_source":"user_action","expiration_timestamp":1748264243}}}]}""");
        var replies = CloudWebhookDecoder.decodeCallPermissionReplies(value);
        assertEquals(1, replies.size());
        var reply = replies.getFirst();
        assertEquals("wamid.R", reply.callId());
        assertEquals("15551234567", reply.from());
        assertEquals(CloudCallPermissionResponse.ACCEPT, reply.permissionResponse());
        assertEquals("user_action", reply.permissionResponseSource().orElseThrow());
        assertEquals(Instant.ofEpochSecond(1748264243L), reply.permissionExpiration().orElseThrow());
    }

    @Test
    @DisplayName("reject permission reply carries no expiration")
    void decodePermissionReject() {
        var value = value("""
                {"messages":[{"from":"15551234567","id":"wamid.R2","timestamp":"1747659443","type":"interactive",
                "interactive":{"type":"call_permission_reply",
                "call_permission_reply":{"response":"reject","response_source":"user_action"}}}]}""");
        var reply = CloudWebhookDecoder.decodeCallPermissionReplies(value).getFirst();
        assertEquals(CloudCallPermissionResponse.REJECT, reply.permissionResponse());
        assertTrue(reply.permissionExpiration().isEmpty());
    }

    @Test
    @DisplayName("decodeMessages skips a permission reply so it is not surfaced as a chat message")
    void permissionReplyNotAChatMessage() {
        var value = value("""
                {"messages":[{"from":"15551234567","id":"wamid.R3","timestamp":"1747659443","type":"interactive",
                "interactive":{"type":"call_permission_reply",
                "call_permission_reply":{"response":"accept","response_source":"user_action"}}}]}""");
        assertTrue(CloudWebhookDecoder.decodeMessages(value).isEmpty());
        assertFalse(CloudWebhookDecoder.decodeCallPermissionReplies(value).isEmpty());
    }
}
