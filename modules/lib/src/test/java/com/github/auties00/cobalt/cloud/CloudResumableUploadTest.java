package com.github.auties00.cobalt.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Resumable Upload API transport (session create, byte upload, offset query) through a
 * {@link RecordingHttpClient}, asserting the request shape, the OAuth and file_offset headers, and the
 * returned session locator, handle, and offset.
 */
@DisplayName("Cloud resumable upload")
class CloudResumableUploadTest {
    private static final String PHONE_ID = "1234567890";
    private static final String APP_ID = "555000111";

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .appId(APP_ID)
                .apiVersion(CloudApiVersion.V25_0)
                .httpClient(http)
                .build();
    }

    private static String header(RecordingHttpClient http, String name) {
        return http.lastHeaders().firstValue(name).orElse(null);
    }

    @Nested
    @DisplayName("createResumableUploadSession")
    class CreateSession {
        @Test
        @DisplayName("posts to the app uploads edge with file_length and file_type and returns the locator")
        void createsSession() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"upload:MTphdHRhY2htZW50Ojhi\"}");
            var id = client(http).createResumableUploadSession(2048, "image/jpeg", "pic.jpg");
            assertEquals("upload:MTphdHRhY2htZW50Ojhi", id);
            assertEquals("POST", http.lastMethod());
            var uri = http.lastUri().toString();
            assertTrue(uri.contains(APP_ID + "/uploads"));
            assertTrue(uri.contains("file_length=2048"));
            assertTrue(uri.contains("file_type=image%2Fjpeg"));
            assertTrue(uri.contains("file_name=pic.jpg"));
            assertEquals("Bearer token", header(http, "Authorization"));
        }

        @Test
        @DisplayName("omits file_name when null and sends no appsecret_proof")
        void omitsFileNameAndProof() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"upload:abc\"}");
            client(http).createResumableUploadSession(10, "application/pdf", null);
            var uri = http.lastUri().toString();
            assertTrue(!uri.contains("file_name="));
            assertTrue(!uri.contains("appsecret_proof"));
        }
    }

    @Nested
    @DisplayName("uploadToResumableSession")
    class UploadBytes {
        @Test
        @DisplayName("uses Authorization OAuth and file_offset and returns the handle")
        void uploadsBytes() throws Exception {
            var http = http();
            http.respondWith("{\"h\":\"4::aW1hZ2UvanBlZw==:ARZ:e:1700000000\"}");
            var data = "hello".getBytes(StandardCharsets.UTF_8);
            var handle = client(http).uploadToResumableSession("upload:abc", 0, data);
            assertEquals("4::aW1hZ2UvanBlZw==:ARZ:e:1700000000", handle);
            assertEquals("POST", http.lastMethod());
            assertTrue(http.lastUri().toString().contains("/upload:abc"));
            assertEquals("OAuth token", header(http, "Authorization"));
            assertEquals("0", header(http, "file_offset"));
            assertArrayEquals(data, http.lastBodyBytes());
        }

        @Test
        @DisplayName("resumes from the supplied offset")
        void resumesFromOffset() throws Exception {
            var http = http();
            http.respondWith("{\"h\":\"handle\"}");
            client(http).uploadToResumableSession("upload:abc", 1048576, new byte[]{1, 2, 3});
            assertEquals("1048576", header(http, "file_offset"));
        }
    }

    @Nested
    @DisplayName("queryResumableUploadOffset")
    class QueryOffset {
        @Test
        @DisplayName("coerces a string file_offset to a long")
        void coercesStringOffset() throws Exception {
            var http = http();
            http.respondWith("{\"id\":\"upload:abc\",\"file_offset\":\"1048576\"}");
            var offset = client(http).queryResumableUploadOffset("upload:abc");
            assertEquals(1048576L, offset);
            assertEquals("GET", http.lastMethod());
            assertEquals("OAuth token", header(http, "Authorization"));
        }

        @Test
        @DisplayName("reads a numeric file_offset")
        void numericOffset() throws Exception {
            var http = http();
            http.respondWith("{\"file_offset\":42}");
            assertEquals(42L, client(http).queryResumableUploadOffset("upload:abc"));
        }
    }

    @Nested
    @DisplayName("uploadTemplateHeaderMedia")
    class HighLevel {
        @Test
        @DisplayName("creates a session then uploads the bytes and returns the handle")
        void endToEnd() throws Exception {
            var http = http();
            // RecordingHttpClient returns the same canned body for both the create and upload calls;
            // an envelope carrying both id and h satisfies the two-step flow.
            http.respondWith("{\"id\":\"upload:xyz\",\"h\":\"final-handle\"}");
            var handle = client(http).uploadTemplateHeaderMedia("pic".getBytes(StandardCharsets.UTF_8), "image/png", "pic.png");
            assertEquals("final-handle", handle);
            assertEquals("OAuth token", header(http, "Authorization"));
            assertEquals("0", header(http, "file_offset"));
        }
    }

    @Nested
    @DisplayName("missing app id")
    class MissingAppId {
        @Test
        @DisplayName("rejects session create when no app id is configured")
        void requiresAppId() throws Exception {
            var http = http();
            var client = CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                    .loadConnection("token", PHONE_ID)
                    .httpClient(http)
                    .build();
            assertThrows(IllegalStateException.class,
                    () -> client.createResumableUploadSession(1, "image/jpeg", null));
        }
    }
}
