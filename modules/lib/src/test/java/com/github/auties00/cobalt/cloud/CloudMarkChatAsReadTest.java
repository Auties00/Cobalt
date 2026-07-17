package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStore;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import com.github.auties00.cobalt.store.cloud.protobuf.ProtobufCloudWhatsAppStore;
import com.github.auties00.cobalt.store.cloud.protobuf.ProtobufCloudWhatsAppStoreBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Cloud markChatAsRead persistence")
class CloudMarkChatAsReadTest {
    private static final String PHONE_ID = "1234567890";
    private static final String VERIFY_TOKEN = "verify-me";
    private static final Jid SENDER = Jid.of("12065550102");

    private static int freePort() throws Exception {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static ProtobufCloudWhatsAppStore restoredStore(String chatJid, String messageId) {
        var map = new ConcurrentHashMap<String, String>();
        map.put(chatJid, messageId);
        return new ProtobufCloudWhatsAppStoreBuilder()
                .accessToken("token")
                .phoneNumberId(PHONE_ID)
                .apiVersion(CloudApiVersion.V21_0.version())
                .webhookPath("/webhook")
                .lastInboundMessageIdByChat(map)
                .build();
    }

    // A factory that always resolves to the given pre-populated store, standing in for a session
    // restored from persistence so the builder's load path returns markers seeded before the test.
    private static CloudWhatsAppStoreFactory factoryReturning(CloudWhatsAppStore store) {
        return new CloudWhatsAppStoreFactory() {
            @Override
            public Optional<CloudWhatsAppStore> load(String phoneNumberId) {
                return Optional.of(store);
            }

            @Override
            public Optional<CloudWhatsAppStore> loadLatest() {
                return Optional.of(store);
            }

            @Override
            public CloudWhatsAppStore create(String accessToken, String phoneNumberId) {
                return store;
            }
        };
    }

    @Test
    @DisplayName("a dispatched inbound message drives markChatAsRead to post a read for that id")
    void recordsThenReadsAfterDispatch() throws Exception {
        var http = new RecordingHttpClient();
        var port = freePort();
        var delivered = new CountDownLatch(1);
        var client = CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V21_0)
                .httpClient(http)
                .webhook(VERIFY_TOKEN, port)
                .build();
        client.addNewMessageListener((c, info) -> delivered.countDown());
        client.connect();
        try {
            var envelope = """
                    {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"messages",
                    "value":{"messaging_product":"whatsapp","messages":[{"from":"12065550102","id":"wamid.INBOUND",
                    "timestamp":"1723506230","type":"text","text":{"body":"hi"}}]}}]}]}""";
            var response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/webhook"))
                            .POST(HttpRequest.BodyPublishers.ofString(envelope))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(delivered.await(5, TimeUnit.SECONDS), "inbound message was not dispatched");

            client.markChatAsRead(SENDER);
            var body = JSON.parseObject(http.lastBody());
            assertEquals("read", body.getString("status"));
            assertEquals("wamid.INBOUND", body.getString("message_id"));
            assertTrue(http.lastUri().toString().endsWith(PHONE_ID + "/messages"));
        } finally {
            client.disconnect();
        }
    }

    @Test
    @DisplayName("a restored store mapping drives markChatAsRead to post a read for the stored id")
    void readsFromRestoredStore() throws Exception {
        var http = new RecordingHttpClient();
        var factory = factoryReturning(restoredStore(SENDER.toString(), "wamid.RESTORED"));
        var client = CloudWhatsAppClient.builder(factory)
                .loadConnection(PHONE_ID)
                .orElseThrow()
                .httpClient(http)
                .build();
        client.markChatAsRead(SENDER);
        var body = JSON.parseObject(http.lastBody());
        assertEquals("read", body.getString("status"));
        assertEquals("wamid.RESTORED", body.getString("message_id"));
    }

    @Test
    @DisplayName("markChatAsRead for an unknown chat posts nothing")
    void unknownChatNoOp() throws Exception {
        var http = new RecordingHttpClient();
        var factory = factoryReturning(restoredStore(SENDER.toString(), "wamid.RESTORED"));
        var client = CloudWhatsAppClient.builder(factory)
                .loadConnection(PHONE_ID)
                .orElseThrow()
                .httpClient(http)
                .build();
        client.markChatAsRead(Jid.of("19998887777"));
        assertNull(http.lastBody());
    }
}
