package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudCatalogMessage;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudCommerceSettings;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudProductListMessage;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudProductMessage;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the commerce settings, catalog read, and commerce sends through a {@link RecordingHttpClient}.
 */
@DisplayName("Cloud commerce")
class CloudCommerceTest {
    private static final String PHONE_ID = "1234567890";
    private static final String WABA_ID = "999";
    private static final String CATALOG_ID = "194836987003835";
    private static final Jid RECIPIENT = Jid.of("15551234567");

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .whatsappBusinessAccountId(WABA_ID)
                .httpClient(http)
                .build();
    }

    private static JSONObject body(RecordingHttpClient http) {
        return JSON.parseObject(http.lastBody());
    }

    @Nested
    @DisplayName("commerce settings")
    class CommerceSettings {
        @Test
        @DisplayName("get maps the first data entry toggles")
        void get() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[{\"id\":\"727705352028726\",\"is_cart_enabled\":true,"
                    + "\"is_catalog_visible\":false}]}");
            var settings = client(http).queryCommerceSettings();
            assertEquals("727705352028726", settings.id().orElseThrow());
            assertEquals(true, settings.cartEnabled().orElseThrow());
            assertEquals(false, settings.catalogVisible().orElseThrow());
            assertTrue(http.lastUri().toString().endsWith(PHONE_ID + "/whatsapp_commerce_settings"));
        }

        @Test
        @DisplayName("get returns empty toggles for empty data")
        void getEmpty() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[]}");
            var settings = client(http).queryCommerceSettings();
            assertTrue(settings.id().isEmpty());
            assertTrue(settings.cartEnabled().isEmpty());
            assertTrue(settings.catalogVisible().isEmpty());
        }

        @Test
        @DisplayName("update posts the populated toggles as form parameters")
        void update() throws Exception {
            var http = http();
            http.respondWith("{\"success\":true}");
            client(http).editCommerceSettings(new CloudCommerceSettings(null, true, false));
            assertTrue(http.lastUri().toString().endsWith(PHONE_ID + "/whatsapp_commerce_settings"));
            assertTrue(http.lastBody().contains("is_cart_enabled=true"));
            assertTrue(http.lastBody().contains("is_catalog_visible=false"));
        }

        @Test
        @DisplayName("update with no toggles throws IllegalArgumentException")
        void updateEmpty() throws Exception {
            var http = http();
            assertThrows(IllegalArgumentException.class,
                    () -> client(http).editCommerceSettings(new CloudCommerceSettings(null, null, null)));
        }
    }

    @Nested
    @DisplayName("connected catalog")
    class ConnectedCatalog {
        @Test
        @DisplayName("get returns the first product_catalogs entry")
        void get() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[{\"id\":\"914477888655787\",\"name\":\"My Catalog\"}],"
                    + "\"paging\":{\"cursors\":{\"before\":\"b\",\"after\":\"a\"}}}");
            var catalog = client(http).queryConnectedProductCatalog().orElseThrow();
            assertEquals("914477888655787", catalog.id());
            assertEquals("My Catalog", catalog.name().orElseThrow());
            assertTrue(http.lastUri().toString().contains(WABA_ID + "/product_catalogs"));
        }

        @Test
        @DisplayName("get returns empty for empty data")
        void getEmpty() throws Exception {
            var http = http();
            http.respondWith("{\"data\":[]}");
            assertTrue(client(http).queryConnectedProductCatalog().isEmpty());
        }
    }

    @Nested
    @DisplayName("commerce sends")
    class CommerceSends {
        @Test
        @DisplayName("sendProduct posts the product interactive with captions")
        void sendProduct() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.TESTPRODUCT\"}]}");
            var key = client(http).sendProduct(RECIPIENT,
                    new CloudProductMessage(CATALOG_ID, "r9_4_test", "Check this out", "Limited stock"));
            assertEquals("wamid.TESTPRODUCT", key.id().orElseThrow());
            assertEquals(RECIPIENT.user(), key.parentJid().orElseThrow().user());
            assertTrue(key.fromMe());
            var interactive = body(http).getJSONObject("interactive");
            assertEquals("product", interactive.getString("type"));
            var action = interactive.getJSONObject("action");
            assertEquals(CATALOG_ID, action.getString("catalog_id"));
            assertEquals("r9_4_test", action.getString("product_retailer_id"));
            assertEquals("Check this out", interactive.getJSONObject("body").getString("text"));
            assertEquals("Limited stock", interactive.getJSONObject("footer").getString("text"));
        }

        @Test
        @DisplayName("sendProduct omits body and footer when null")
        void sendProductNoCaptions() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.X\"}]}");
            client(http).sendProduct(RECIPIENT, new CloudProductMessage(CATALOG_ID, "r9_4_test", null, null));
            var interactive = body(http).getJSONObject("interactive");
            assertFalse(interactive.containsKey("body"));
            assertFalse(interactive.containsKey("footer"));
        }

        @Test
        @DisplayName("sendProductList posts the product_list interactive with sections")
        void sendProductList() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.TESTLIST\"}]}");
            var list = new CloudProductListMessage(CATALOG_ID, "New products", "In stock now", null,
                    List.of(new CloudProductListMessage.Section("Mousepads", List.of("r9_4_test", "r9_5_test"))));
            var key = client(http).sendProductList(RECIPIENT, list);
            assertEquals("wamid.TESTLIST", key.id().orElseThrow());
            assertTrue(key.fromMe());
            var interactive = body(http).getJSONObject("interactive");
            assertEquals("product_list", interactive.getString("type"));
            assertEquals("text", interactive.getJSONObject("header").getString("type"));
            assertEquals("New products", interactive.getJSONObject("header").getString("text"));
            assertEquals("In stock now", interactive.getJSONObject("body").getString("text"));
            assertFalse(interactive.containsKey("footer"));
            var section = interactive.getJSONObject("action").getJSONArray("sections").getJSONObject(0);
            assertEquals("Mousepads", section.getString("title"));
            assertEquals("r9_4_test", section.getJSONArray("product_items").getJSONObject(0).getString("product_retailer_id"));
            assertEquals("r9_5_test", section.getJSONArray("product_items").getJSONObject(1).getString("product_retailer_id"));
        }

        @Test
        @DisplayName("sendProductList includes the footer when present")
        void sendProductListFooter() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.X\"}]}");
            var list = new CloudProductListMessage(CATALOG_ID, "h", "b", "Free shipping",
                    List.of(new CloudProductListMessage.Section("s", List.of("sku"))));
            client(http).sendProductList(RECIPIENT, list);
            assertEquals("Free shipping", body(http).getJSONObject("interactive").getJSONObject("footer").getString("text"));
        }

        @Test
        @DisplayName("empty sections throws at model construction")
        void emptySectionsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CloudProductListMessage(CATALOG_ID, "h", "b", null, List.of()));
        }

        @Test
        @DisplayName("sendCatalog posts the catalog interactive with a thumbnail")
        void sendCatalog() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.TESTCATALOG\"}]}");
            var key = client(http).sendCatalog(RECIPIENT,
                    new CloudCatalogMessage("Browse our catalog", "Free shipping", "r9_4_test"));
            assertEquals("wamid.TESTCATALOG", key.id().orElseThrow());
            assertTrue(key.fromMe());
            var interactive = body(http).getJSONObject("interactive");
            assertEquals("catalog_message", interactive.getString("type"));
            var action = interactive.getJSONObject("action");
            assertEquals("catalog_message", action.getString("name"));
            assertEquals("r9_4_test", action.getJSONObject("parameters").getString("thumbnail_product_retailer_id"));
            assertEquals("Browse our catalog", interactive.getJSONObject("body").getString("text"));
            assertEquals("Free shipping", interactive.getJSONObject("footer").getString("text"));
        }

        @Test
        @DisplayName("sendCatalog omits parameters and footer when null")
        void sendCatalogMinimal() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.X\"}]}");
            client(http).sendCatalog(RECIPIENT, new CloudCatalogMessage("Browse", null, null));
            var interactive = body(http).getJSONObject("interactive");
            var action = interactive.getJSONObject("action");
            assertEquals("catalog_message", action.getString("name"));
            assertFalse(action.containsKey("parameters"));
            assertFalse(interactive.containsKey("footer"));
        }
    }
}
