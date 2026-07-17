package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudPaymentConfiguration;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the WABA-scoped payment-configuration CRUD edges through a {@link RecordingHttpClient},
 * asserting the request path, the {@code configuration_name} query parameter, and the create body.
 */
@DisplayName("Cloud payment configuration")
class CloudPaymentConfigurationTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "9988776655";

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .whatsappBusinessAccountId(WABA_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .httpClient(http)
                .build();
    }

    private static CloudWhatsAppClient clientWithoutWaba(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .httpClient(http)
                .build();
    }

    @Test
    @DisplayName("queryPaymentConfigurations GETs the WABA edge and parses the data array")
    void list() throws Exception {
        var http = http();
        http.respondWith("""
                {"data":[{"configuration_name":"my-gateway","provider_name":"razorpay",
                 "provider_mid":"mid-123","status":"ACTIVE","created_timestamp":1700000000,
                 "updated_timestamp":1700000100}]}""");
        var configurations = client(http).queryPaymentConfigurations();
        assertEquals(1, configurations.size());
        var configuration = configurations.getFirst();
        assertEquals("my-gateway", configuration.configurationName());
        assertEquals("razorpay", configuration.providerName().orElseThrow());
        assertEquals("mid-123", configuration.providerMerchantId().orElseThrow());
        assertEquals("ACTIVE", configuration.status().orElseThrow());
        assertEquals(1700000000L, configuration.createdTimestamp().orElseThrow().getEpochSecond());
        assertEquals(1700000100L, configuration.updatedTimestamp().orElseThrow().getEpochSecond());
        assertEquals("GET", http.lastMethod());
        assertTrue(http.lastUri().toString().contains(WABA_ID + "/payment_configurations"));
    }

    @Test
    @DisplayName("queryPaymentConfigurations returns an empty list when data is absent")
    void listEmpty() throws Exception {
        var http = http();
        http.respondWith("{}");
        assertTrue(client(http).queryPaymentConfigurations().isEmpty());
    }

    @Test
    @DisplayName("queryPaymentConfiguration GETs the edge with the configuration_name query parameter")
    void get() throws Exception {
        var http = http();
        http.respondWith("""
                {"configuration_name":"my-gateway","provider_name":"razorpay","status":"ACTIVE"}""");
        var configuration = client(http).queryPaymentConfiguration("my-gateway").orElseThrow();
        assertEquals("my-gateway", configuration.configurationName());
        assertEquals("razorpay", configuration.providerName().orElseThrow());
        assertEquals("GET", http.lastMethod());
        var uri = http.lastUri().toString();
        assertTrue(uri.contains(WABA_ID + "/payment_configuration"));
        assertTrue(uri.contains("configuration_name=my-gateway"));
    }

    @Test
    @DisplayName("queryPaymentConfiguration returns empty when the response carries no configuration")
    void getEmpty() throws Exception {
        var http = http();
        http.respondWith("{}");
        assertTrue(client(http).queryPaymentConfiguration("missing").isEmpty());
    }

    @Test
    @DisplayName("createPaymentConfiguration POSTs the name, provider, and merchant id")
    void create() throws Exception {
        var http = http();
        http.respondWith("{\"success\":true}");
        client(http).createPaymentConfiguration(
                new CloudPaymentConfiguration("my-gateway", "razorpay", "mid-123"));
        assertEquals("POST", http.lastMethod());
        assertTrue(http.lastUri().toString().contains(WABA_ID + "/payment_configuration"));
        var body = JSON.parseObject(http.lastBody());
        assertEquals("my-gateway", body.getString("configuration_name"));
        assertEquals("razorpay", body.getString("provider_name"));
        assertEquals("mid-123", body.getString("provider_mid"));
    }

    @Test
    @DisplayName("createPaymentConfiguration omits the merchant id when it is absent")
    void createWithoutMid() throws Exception {
        var http = http();
        http.respondWith("{\"success\":true}");
        client(http).createPaymentConfiguration(
                new CloudPaymentConfiguration("my-gateway", "razorpay", null));
        var body = JSON.parseObject(http.lastBody());
        assertEquals("my-gateway", body.getString("configuration_name"));
        assertEquals("razorpay", body.getString("provider_name"));
        assertFalse(body.containsKey("provider_mid"));
    }

    @Test
    @DisplayName("deletePaymentConfiguration DELETEs the edge with the configuration_name query parameter")
    void delete() throws Exception {
        var http = http();
        http.respondWith("{\"success\":true}");
        client(http).deletePaymentConfiguration("my-gateway");
        assertEquals("DELETE", http.lastMethod());
        var uri = http.lastUri().toString();
        assertTrue(uri.contains(WABA_ID + "/payment_configuration"));
        assertTrue(uri.contains("configuration_name=my-gateway"));
    }

    @Test
    @DisplayName("the CRUD edges require a configured WhatsApp Business Account id")
    void requiresWaba() throws Exception {
        var http = http();
        assertThrows(IllegalStateException.class, () -> clientWithoutWaba(http).queryPaymentConfigurations());
        assertThrows(IllegalStateException.class,
                () -> clientWithoutWaba(http).queryPaymentConfiguration("my-gateway"));
        assertThrows(IllegalStateException.class,
                () -> clientWithoutWaba(http).createPaymentConfiguration(
                        new CloudPaymentConfiguration("my-gateway", "razorpay", null)));
        assertThrows(IllegalStateException.class,
                () -> clientWithoutWaba(http).deletePaymentConfiguration("my-gateway"));
    }
}
