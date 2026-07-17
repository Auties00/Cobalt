package com.github.auties00.cobalt.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.CloudCallEvent;
import com.github.auties00.cobalt.wire.cloud.CloudCallPermissionResponse;
import com.github.auties00.cobalt.wire.cloud.CloudCallStatus;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives a webhook envelope through the running {@link CloudWhatsAppClient} and asserts that each call
 * event reaches the listener the dispatcher routes it to: signaling connect and terminate to the call
 * listener, the business-initiated status to the call-status listener, and the consumer permission reply
 * to the call-permission listener.
 */
@DisplayName("Cloud call listener dispatch")
class CloudCallListenerDispatchTest {
    private static final String PHONE_ID = "1234567890";
    private static final String VERIFY_TOKEN = "verify-me";

    private static int freePort() throws Exception {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static CloudWhatsAppClient client(int port) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .httpClient(new RecordingHttpClient())
                .webhook(VERIFY_TOKEN, port)
                .build();
    }

    private static void post(int port, String envelope) throws Exception {
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/webhook"))
                        .POST(HttpRequest.BodyPublishers.ofString(envelope))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    @DisplayName("connect signaling reaches onCall as a Connect")
    void connectReachesOnCall() throws Exception {
        var port = freePort();
        var fired = new CountDownLatch(1);
        var seen = new AtomicReference<CloudCallEvent.Signaling>();
        var client = client(port);
        client.addCallListener((c, event) -> {
            seen.set(event);
            fired.countDown();
        });
        client.connect();
        try {
            post(port, """
                    {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"calls",
                    "value":{"calls":[{"id":"call-1","from":"15551234567","to":"12345678900","event":"connect",
                    "timestamp":"1762216151","direction":"USER_INITIATED",
                    "session":{"sdp_type":"offer","sdp":"the-sdp"}}]}}]}]}""");
            assertTrue(fired.await(5, TimeUnit.SECONDS), "connect was not dispatched to onCall");
            var connect = assertInstanceOf(CloudCallEvent.Connect.class, seen.get());
            assertEquals("call-1", connect.callId());
            assertEquals("the-sdp", connect.session().orElseThrow().sdp());
        } finally {
            client.disconnect();
        }
    }

    @Test
    @DisplayName("terminate signaling reaches onCall as a Terminate")
    void terminateReachesOnCall() throws Exception {
        var port = freePort();
        var fired = new CountDownLatch(1);
        var seen = new AtomicReference<CloudCallEvent.Signaling>();
        var client = client(port);
        client.addCallListener((c, event) -> {
            seen.set(event);
            fired.countDown();
        });
        client.connect();
        try {
            post(port, """
                    {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"calls",
                    "value":{"calls":[{"id":"call-2","from":"15551234567","to":"12345678900","event":"terminate",
                    "direction":"USER_INITIATED","timestamp":"1733734771","status":"COMPLETED",
                    "start_time":1733734738,"end_time":1733734771,"duration":33}]}}]}]}""");
            assertTrue(fired.await(5, TimeUnit.SECONDS), "terminate was not dispatched to onCall");
            var terminate = assertInstanceOf(CloudCallEvent.Terminate.class, seen.get());
            assertEquals("COMPLETED", terminate.status().orElseThrow());
            assertEquals(33, terminate.durationSeconds().orElseThrow());
        } finally {
            client.disconnect();
        }
    }

    @Test
    @DisplayName("business-initiated call status reaches onCallStatus")
    void statusReachesOnCallStatus() throws Exception {
        var port = freePort();
        var fired = new CountDownLatch(1);
        var seen = new AtomicReference<CloudCallEvent.Status>();
        var client = client(port);
        client.addCallStatusListener((c, event) -> {
            seen.set(event);
            fired.countDown();
        });
        client.connect();
        try {
            post(port, """
                    {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"calls",
                    "value":{"statuses":[{"id":"wacid.AB","timestamp":"1671644824","type":"call","status":"RINGING",
                    "recipient_id":"163155536021"}]}}]}]}""");
            assertTrue(fired.await(5, TimeUnit.SECONDS), "status was not dispatched to onCallStatus");
            assertEquals(CloudCallStatus.RINGING, seen.get().status());
            assertEquals("163155536021", seen.get().from());
        } finally {
            client.disconnect();
        }
    }

    @Test
    @DisplayName("consumer permission reply reaches onCallPermission")
    void permissionReachesOnCallPermission() throws Exception {
        var port = freePort();
        var fired = new CountDownLatch(1);
        var seen = new AtomicReference<CloudCallEvent.PermissionReply>();
        var client = client(port);
        client.addCallPermissionListener((c, event) -> {
            seen.set(event);
            fired.countDown();
        });
        client.connect();
        try {
            post(port, """
                    {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"messages",
                    "value":{"messaging_product":"whatsapp","messages":[{"from":"15551234567","id":"wamid.R",
                    "timestamp":"1747659443","type":"interactive","interactive":{"type":"call_permission_reply",
                    "call_permission_reply":{"response":"accept","response_source":"user_action",
                    "expiration_timestamp":1748264243}}}]}}]}]}""");
            assertTrue(fired.await(5, TimeUnit.SECONDS), "permission reply was not dispatched to onCallPermission");
            assertEquals(CloudCallPermissionResponse.ACCEPT, seen.get().permissionResponse());
            assertEquals("15551234567", seen.get().from());
        } finally {
            client.disconnect();
        }
    }
}
