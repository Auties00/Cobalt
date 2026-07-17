package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowAssetType;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowEndpointAvailability;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowMetadataEditBuilder;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowStatus;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises Cloud Flow management (rich read, metadata update, deletion, asset upload and listing,
 * preview, and cross-WABA migration) through a {@link RecordingHttpClient}, asserting the request
 * shape and the parsed response model.
 */
@DisplayName("Cloud Flow management")
class CloudFlowManagementTest {
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
    @DisplayName("queryFlow")
    class GetFlow {
        @Test
        @DisplayName("parses the rich flow read view")
        void parsesDetails() throws Exception {
            var http = http();
            http.respondWith("""
                    {
                      "id": "1234567890",
                      "name": "My Flow",
                      "status": "PUBLISHED",
                      "categories": ["SIGN_UP"],
                      "validation_errors": [
                        {"error": "INVALID_PROPERTY", "error_type": "FLOW_JSON_ERROR", "message": "bad",
                         "line_start": 10, "line_end": 10, "column_start": 5, "column_end": 20}
                      ],
                      "json_version": "3.0",
                      "data_api_version": "3.0",
                      "endpoint_uri": "https://example.com/flow",
                      "preview": {"preview_url": "https://business.facebook.com/preview", "expires_at": "2026-06-18T00:00:00+0000"},
                      "application": {"id": "111", "name": "My App", "link": "https://app"},
                      "health_status": {"can_send_message": "AVAILABLE"},
                      "whatsapp_business_account": {"id": "999"}
                    }""");
            var flow = client(http).queryFlow("1234567890");
            assertEquals("1234567890", flow.id());
            assertEquals(CloudFlowStatus.PUBLISHED, flow.status().orElseThrow());
            assertEquals(List.of("SIGN_UP"), flow.categories());
            assertEquals(1, flow.validationErrors().size());
            assertEquals(10, flow.validationErrors().getFirst().span().orElseThrow().lineStart());
            assertEquals("https://business.facebook.com/preview", flow.preview().orElseThrow().previewUrl().orElseThrow());
            assertEquals("111", flow.application().orElseThrow().id().orElseThrow());
            assertEquals(CloudFlowEndpointAvailability.AVAILABLE, flow.healthStatus().orElseThrow());
            assertEquals("999", flow.whatsappBusinessAccountId().orElseThrow());
            assertTrue(http.lastUri().toString().contains("/1234567890"));
            assertTrue(http.lastUri().toString().contains("fields="));
        }
    }

    @Nested
    @DisplayName("editFlowMetadata")
    class UpdateMetadata {
        @Test
        @DisplayName("posts only the supplied fields")
        void postsSuppliedFields() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).editFlowMetadata(new CloudFlowMetadataEditBuilder()
                    .flowId("123")
                    .name("New name")
                    .categories(List.of("SIGN_UP"))
                    .endpointUri("https://e")
                    .build());
            assertEquals("POST", http.lastMethod());
            var body = JSON.parseObject(http.lastBody());
            assertEquals("New name", body.getString("name"));
            assertEquals("https://e", body.getString("endpoint_uri"));
            assertEquals(List.of("SIGN_UP"), body.getJSONArray("categories").toList(String.class));
            assertFalse(body.containsKey("application_id"));
        }

        @Test
        @DisplayName("omits null and empty fields")
        void omitsNullFields() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).editFlowMetadata(new CloudFlowMetadataEditBuilder()
                    .flowId("123")
                    .applicationId("777")
                    .build());
            var body = JSON.parseObject(http.lastBody());
            assertFalse(body.containsKey("name"));
            assertFalse(body.containsKey("categories"));
            assertFalse(body.containsKey("endpoint_uri"));
            assertEquals("777", body.getString("application_id"));
        }
    }

    @Nested
    @DisplayName("deleteFlow")
    class DeleteFlow {
        @Test
        @DisplayName("issues a DELETE on the flow id")
        void issuesDelete() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).deleteFlow("123");
            assertEquals("DELETE", http.lastMethod());
            assertTrue(http.lastUri().toString().contains("/123"));
        }
    }

    @Nested
    @DisplayName("uploadFlowJson")
    class UploadAsset {
        @Test
        @DisplayName("returns a successful result with no errors when the document validates cleanly")
        void cleanUpload() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true,\"validation_errors\":[]}");
            var result = client(http).uploadFlowJson("123", "{}".getBytes(StandardCharsets.UTF_8));
            assertTrue(result.success());
            assertTrue(result.validationErrors().isEmpty());
            assertEquals("POST", http.lastMethod());
            assertTrue(http.lastUri().toString().contains("/123/assets"));
        }

        @Test
        @DisplayName("parses the structured validation error entries")
        void uploadWithErrors() throws Exception {
            var http = http();
            http.respondWith("""
                    {"success":true,"validation_errors":[{"error":"INVALID_PROPERTY","error_type":"FLOW_JSON_ERROR",
                     "message":"x","line_start":3,"line_end":3,"column_start":1,"column_end":9}]}""");
            var result = client(http).uploadFlowJson("123", "{}".getBytes(StandardCharsets.UTF_8));
            assertTrue(result.success());
            assertEquals(1, result.validationErrors().size());
            var error = result.validationErrors().getFirst();
            assertEquals("INVALID_PROPERTY", error.code());
            assertEquals("FLOW_JSON_ERROR", error.errorType().orElseThrow());
            assertEquals(3, error.span().orElseThrow().lineStart());
        }
    }

    @Nested
    @DisplayName("queryFlowAssets")
    class QueryAssets {
        @Test
        @DisplayName("parses the asset list")
        void parsesAssets() throws Exception {
            var http = http();
            http.respondWith("""
                    {"data":[{"name":"flow.json","asset_type":"FLOW_JSON","download_url":"https://cdn/flow"}]}""");
            var assets = client(http).queryFlowAssets("123");
            assertEquals(1, assets.size());
            assertEquals("flow.json", assets.getFirst().name());
            assertEquals(CloudFlowAssetType.FLOW_JSON, assets.getFirst().assetType());
            assertEquals("https://cdn/flow", assets.getFirst().downloadUrl());
        }

        @Test
        @DisplayName("returns empty for an empty data array")
        void emptyAssets() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[]}");
            assertTrue(client(http).queryFlowAssets("123").isEmpty());
        }
    }

    @Nested
    @DisplayName("queryFlowPreview")
    class Preview {
        @Test
        @DisplayName("reads the nested preview object")
        void nestedPreview() throws Exception {
            var http = http();
            http.respondWith("""
                    {"preview":{"preview_url":"https://business.facebook.com/p","expires_at":"2026-05-07T19:56:10+0000"},"id":"123"}""");
            var preview = client(http).queryFlowPreview("123");
            assertEquals("https://business.facebook.com/p", preview.previewUrl().orElseThrow());
            assertEquals(java.time.Instant.parse("2026-05-07T19:56:10Z"), preview.expiresAt().orElseThrow());
        }

        @Test
        @DisplayName("falls back to top-level preview fields")
        void flatPreview() throws Exception {
            var http = http();
            http.respondWith("""
                    {"preview_url":"https://p","expires_at":"2026-05-07T19:56:10+0000"}""");
            var preview = client(http).queryFlowPreview("123");
            assertEquals("https://p", preview.previewUrl().orElseThrow());
        }
    }

    @Nested
    @DisplayName("migrateFlows")
    class Migrate {
        @Test
        @DisplayName("posts the source waba and parses both result buckets")
        void migrates() throws Exception {
            var http = http();
            http.respondWith("""
                    {"migrated_flows":[{"source_name":"flow_a","source_id":"111","migrated_id":"222"}],
                     "failed_flows":[{"source_name":"flow_b","error_code":"E1","error_message":"nope"}]}""");
            var result = client(http).migrateFlows("source-waba", List.of("flow_a", "flow_b"));
            assertEquals("POST", http.lastMethod());
            assertTrue(http.lastUri().toString().contains(WABA_ID + "/migrate_flows"));
            var body = JSON.parseObject(http.lastBody());
            assertEquals("source-waba", body.getString("source_waba_id"));
            assertEquals(List.of("flow_a", "flow_b"), body.getJSONArray("source_flow_names").toList(String.class));
            assertEquals(1, result.migratedFlows().size());
            assertEquals("111", result.migratedFlows().getFirst().sourceId().orElseThrow());
            assertEquals("222", result.migratedFlows().getFirst().migratedId().orElseThrow());
            assertEquals(1, result.failedFlows().size());
            assertEquals("E1", result.failedFlows().getFirst().errorCode().orElseThrow());
        }

        @Test
        @DisplayName("omits source_flow_names when an empty list is given")
        void migratesAll() throws Exception {
            var http = http();
            http.respondWith("{\"migrated_flows\":[],\"failed_flows\":[]}");
            client(http).migrateFlows("source-waba", List.of());
            var body = JSON.parseObject(http.lastBody());
            assertFalse(body.containsKey("source_flow_names"));
        }
    }
}
