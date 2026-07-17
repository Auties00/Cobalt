package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplatePauseUpdate;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code message_template_pause} and {@code message_template_unpause} webhook fields: the
 * decode round-trip through {@link CloudWebhookDecoder#decodeTemplatePause(JSONObject)} and the dispatch
 * to {@code CloudTemplatePauseListener} for both a pause (with a pause date) and an unpause.
 */
@DisplayName("Cloud template pause webhook")
class CloudTemplatePauseTest {
    private static final String PHONE_ID = "1234567890";
    private static final String VERIFY_TOKEN = "verify-me";

    private static JSONObject value(String json) {
        return JSON.parseObject(json);
    }

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
    @DisplayName("decodes id, name, language, reason and the pause date")
    void decodePauseWithDate() {
        var value = value("""
                {"message_template_id":1234567890,"message_template_name":"order_update",
                "message_template_language":"en_US","reason":"PAIRWISE_RATE_LIMIT","pause_date":1671644824}""");
        var update = CloudWebhookDecoder.decodeTemplatePause(value);
        assertEquals("1234567890", update.messageTemplateId());
        assertEquals("order_update", update.messageTemplateName());
        assertEquals("en_US", update.messageTemplateLanguage());
        assertEquals("PAIRWISE_RATE_LIMIT", update.reason());
        assertEquals(Instant.ofEpochSecond(1671644824L), update.pauseDate().orElseThrow());
    }

    @Test
    @DisplayName("an unpause without a pause date omits it")
    void decodeUnpauseNoDate() {
        var value = value("""
                {"message_template_id":"42","message_template_name":"welcome","message_template_language":"en",
                "reason":"NONE"}""");
        var update = CloudWebhookDecoder.decodeTemplatePause(value);
        assertEquals("42", update.messageTemplateId());
        assertEquals("NONE", update.reason());
        assertTrue(update.pauseDate().isEmpty());
    }

    @Test
    @DisplayName("message_template_pause field reaches onTemplatePause")
    void pauseReachesListener() throws Exception {
        var port = freePort();
        var fired = new CountDownLatch(1);
        var seen = new AtomicReference<CloudTemplatePauseUpdate>();
        var client = client(port);
        client.addTemplatePauseListener((c, update) -> {
            seen.set(update);
            fired.countDown();
        });
        client.connect();
        try {
            post(port, """
                    {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"message_template_pause",
                    "value":{"message_template_id":1234567890,"message_template_name":"order_update",
                    "message_template_language":"en_US","reason":"PAIRWISE_RATE_LIMIT","pause_date":1671644824}}]}]}""");
            assertTrue(fired.await(5, TimeUnit.SECONDS), "pause was not dispatched to onTemplatePause");
            assertEquals("order_update", seen.get().messageTemplateName());
            assertEquals(Instant.ofEpochSecond(1671644824L), seen.get().pauseDate().orElseThrow());
        } finally {
            client.disconnect();
        }
    }

    @Test
    @DisplayName("message_template_unpause field reaches onTemplatePause")
    void unpauseReachesListener() throws Exception {
        var port = freePort();
        var fired = new CountDownLatch(1);
        var seen = new AtomicReference<CloudTemplatePauseUpdate>();
        var client = client(port);
        client.addTemplatePauseListener((c, update) -> {
            seen.set(update);
            fired.countDown();
        });
        client.connect();
        try {
            post(port, """
                    {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"message_template_unpause",
                    "value":{"message_template_id":"42","message_template_name":"welcome",
                    "message_template_language":"en","reason":"NONE"}}]}]}""");
            assertTrue(fired.await(5, TimeUnit.SECONDS), "unpause was not dispatched to onTemplatePause");
            assertEquals("welcome", seen.get().messageTemplateName());
            assertTrue(seen.get().pauseDate().isEmpty());
        } finally {
            client.disconnect();
        }
    }
}
