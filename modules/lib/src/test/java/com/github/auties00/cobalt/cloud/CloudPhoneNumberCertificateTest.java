package com.github.auties00.cobalt.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Cloud phone-number new_certificate")
class CloudPhoneNumberCertificateTest {
    private static final String PHONE_ID = "1234567890";

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V21_0)
                .httpClient(http)
                .build();
    }

    @Test
    @DisplayName("queryPhoneNumber maps new_certificate when present")
    void parsesNewCertificate() throws Exception {
        var http = new RecordingHttpClient();
        http.respondWith("""
                {"id":"123456789","certificate":"Y2VydA==","new_certificate":"bmV3Y2VydA=="}""");
        var number = client(http).queryPhoneNumber();
        assertEquals("Y2VydA==", number.certificate().orElseThrow());
        assertEquals("bmV3Y2VydA==", number.newCertificate().orElseThrow());
    }

    @Test
    @DisplayName("queryPhoneNumber requests the new_certificate field")
    void requestsNewCertificate() throws Exception {
        var http = new RecordingHttpClient();
        http.respondWith("{}");
        client(http).queryPhoneNumber();
        assertTrue(http.lastUri().getQuery().contains("new_certificate"), http.lastUri().toString());
    }

    @Test
    @DisplayName("queryPhoneNumber leaves new_certificate empty when absent")
    void absentNewCertificate() throws Exception {
        var http = new RecordingHttpClient();
        http.respondWith("{\"id\":\"123456789\"}");
        var number = client(http).queryPhoneNumber();
        assertTrue(number.newCertificate().isEmpty());
    }
}
