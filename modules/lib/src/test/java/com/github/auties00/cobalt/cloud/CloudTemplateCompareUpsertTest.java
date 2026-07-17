package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.template.CloudAuthenticationTemplateUpsert;
import com.github.auties00.cobalt.wire.cloud.template.CloudMessageTemplateComparisonRequestBuilder;
import com.github.auties00.cobalt.wire.cloud.template.CloudOtpButton;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateBlockReason;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateCategory;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateStatus;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises Cloud template comparison and authentication-template upsert through a
 * {@link RecordingHttpClient}.
 */
@DisplayName("Cloud template compare and upsert")
class CloudTemplateCompareUpsertTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "9988776655";

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .whatsappBusinessAccountId(WABA_ID)
                .apiVersion(CloudApiVersion.V25_0)
                .httpClient(http)
                .build();
    }

    @Nested
    @DisplayName("compareMessageTemplates")
    class Compare {
        @Test
        @DisplayName("targets the base template compare edge with template_ids, start, and end")
        void requestShape() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[]}");
            client(http).compareMessageTemplates(new CloudMessageTemplateComparisonRequestBuilder()
                    .templateId("100")
                    .comparisonTemplateIds(List.of("200"))
                    .start(Instant.ofEpochSecond(1700000000L))
                    .end(Instant.ofEpochSecond(1700600000L))
                    .build());
            var uri = java.net.URLDecoder.decode(http.lastUri().toString(),
                    java.nio.charset.StandardCharsets.UTF_8);
            assertEquals("GET", http.lastMethod());
            assertTrue(uri.contains("/100/compare"), uri);
            assertTrue(uri.contains("template_ids=200"), uri);
            assertTrue(uri.contains("start=1700000000"), uri);
            assertTrue(uri.contains("end=1700600000"), uri);
        }

        @Test
        @DisplayName("flattens the metric array into the three views")
        void parseMetrics() throws Exception {
            var http = http();
            http.respondWith("""
                    {"data":[
                      {"metric":"BLOCK_RATE","order_by_relative_metric":["100","200"]},
                      {"metric":"MESSAGE_SENDS","number_values":[{"key":"100","value":1500},
                                                                 {"key":"200","value":2400}]},
                      {"metric":"TOP_BLOCK_REASON","string_values":[{"key":"100","value":"SPAM"},
                                                                    {"key":"200","value":"NO_SIGN_UP"}]}]}""");
            var comparison = client(http).compareMessageTemplates(new CloudMessageTemplateComparisonRequestBuilder()
                    .templateId("100")
                    .comparisonTemplateIds(List.of("200"))
                    .start(Instant.ofEpochSecond(1L))
                    .end(Instant.ofEpochSecond(2L))
                    .build());
            assertEquals(List.of("100", "200"), comparison.blockRateOrder());
            assertEquals(1500L, comparison.perTemplate().get("100").timesSent());
            assertEquals(2400L, comparison.perTemplate().get("200").timesSent());
            assertEquals(CloudTemplateBlockReason.SPAM, comparison.perTemplate().get("100").topBlockReason().orElseThrow());
            assertEquals(CloudTemplateBlockReason.NO_SIGN_UP, comparison.perTemplate().get("200").topBlockReason().orElseThrow());
        }

        @Test
        @DisplayName("leaves absent metrics empty and maps unknown block reasons to UNKNOWN")
        void parsePartial() throws Exception {
            var http = http();
            http.respondWith("""
                    {"data":[{"metric":"TOP_BLOCK_REASON","string_values":[{"key":"100","value":"WAT"}]}]}""");
            var comparison = client(http).compareMessageTemplates(new CloudMessageTemplateComparisonRequestBuilder()
                    .templateId("100")
                    .comparisonTemplateIds(List.of("200"))
                    .start(Instant.ofEpochSecond(1L))
                    .end(Instant.ofEpochSecond(2L))
                    .build());
            assertTrue(comparison.blockRateOrder().isEmpty());
            assertEquals(0L, comparison.perTemplate().get("100").timesSent());
            assertEquals(CloudTemplateBlockReason.UNKNOWN, comparison.perTemplate().get("100").topBlockReason().orElseThrow());
        }
    }

    @Nested
    @DisplayName("upsertMessageTemplates")
    class Upsert {
        @Test
        @DisplayName("posts the authentication body with languages and OTP buttons, omitting button text")
        void requestShape() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[]}");
            var apps = List.of(new CloudOtpButton.App("com.mycompany.myapp", "K8a/AINcGX7"));
            var button = new CloudOtpButton.OneTap("Copy code", "Autofill", apps);
            var template = new CloudAuthenticationTemplateUpsert("auth_otp",
                    List.of("en_US", "fr"), button, true, 5, 600);
            client(http).upsertMessageTemplates(template);
            assertEquals("POST", http.lastMethod());
            assertTrue(http.lastUri().toString().contains(WABA_ID + "/upsert_message_templates"));
            var body = JSON.parseObject(http.lastBody());
            assertEquals("auth_otp", body.getString("name"));
            assertEquals("AUTHENTICATION", body.getString("category"));
            assertEquals(List.of("en_US", "fr"), body.getJSONArray("languages").toList(String.class));
            assertEquals(600, body.getIntValue("message_send_ttl_seconds"));
            var components = body.getJSONArray("components");
            assertTrue(components.getJSONObject(0).getBooleanValue("add_security_recommendation"));
            assertEquals(5, components.getJSONObject(1).getIntValue("code_expiration_minutes"));
            var otp = components.getJSONObject(2).getJSONArray("buttons").getJSONObject(0);
            assertEquals("ONE_TAP", otp.getString("otp_type"));
            assertFalse(otp.containsKey("text"));
            assertFalse(otp.containsKey("autofill_text"));
            assertEquals("com.mycompany.myapp",
                    otp.getJSONArray("supported_apps").getJSONObject(0).getString("package_name"));
        }

        @Test
        @DisplayName("parses the per-language data entries")
        void parseResponse() throws Exception {
            var http = http();
            http.respondWith("""
                    {"data":[{"id":"111","language":"en_US","status":"APPROVED","category":"AUTHENTICATION"},
                             {"id":"222","language":"fr","status":"PENDING","category":"AUTHENTICATION"}]}""");
            var button = new CloudOtpButton.CopyCode(null);
            var template = new CloudAuthenticationTemplateUpsert("auth_otp",
                    List.of("en_US", "fr"), button, null, null, null);
            var result = client(http).upsertMessageTemplates(template);
            assertEquals(2, result.size());
            assertEquals("111", result.getFirst().id());
            assertEquals(CloudTemplateStatus.APPROVED, result.getFirst().status());
            assertEquals("en_US", result.getFirst().language().orElseThrow());
            assertEquals(CloudTemplateCategory.AUTHENTICATION, result.getFirst().category().orElseThrow());
            assertEquals(CloudTemplateStatus.PENDING, result.get(1).status());
        }
    }
}
