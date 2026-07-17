package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudGoodsType;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrder;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrderAmount;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrderDetailsMessage;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrderItem;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrderPayment;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrderPaymentSetting;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrderStatus;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudOrderStatusMessage;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@code order_details} and {@code order_status} interactive payment sends through a
 * {@link RecordingHttpClient}, asserting the built {@code /messages} body.
 */
@DisplayName("Cloud payments")
class CloudPaymentsTest {
    private static final String PHONE_ID = "1234567890";
    private static final Jid RECIPIENT = Jid.of("15551234567");

    private static RecordingHttpClient http() {
        return new RecordingHttpClient();
    }

    private static CloudWhatsAppClient client(RecordingHttpClient http) throws Exception {
        return CloudWhatsAppClient.builder(CloudWhatsAppStoreFactory.temporary())
                .loadConnection("token", PHONE_ID)
                .apiVersion(CloudApiVersion.V23_0)
                .httpClient(http)
                .build();
    }

    private static JSONObject body(RecordingHttpClient http) {
        return JSON.parseObject(http.lastBody());
    }

    private static CloudOrderDetailsMessage sampleOrder() {
        var items = List.of(new CloudOrderItem("retail-1", "Widget", new CloudOrderAmount(20000, 100), 1, null));
        var order = new CloudOrder(CloudOrderStatus.PENDING, "cat-1", items, new CloudOrderAmount(20000, 100),
                new CloudOrder.CloudOrderAdjustment(new CloudOrderAmount(1000, 100), "VAT"), null, null);
        return new CloudOrderDetailsMessage("Please pay", "Thanks", null, "ref-123",
                CloudGoodsType.PHYSICAL, "INR", new CloudOrderAmount(21000, 100),
                new CloudOrderPayment.Gateway(
                        List.of(new CloudOrderPaymentSetting("payment_gateway", "razorpay", "my-config"))),
                order);
    }

    @Nested
    @DisplayName("order_details")
    class OrderDetails {
        @Test
        @DisplayName("builds the review_and_pay interactive with order and total")
        void send() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.ORDER\"}]}");
            var key = client(http).sendOrderDetails(RECIPIENT, sampleOrder());
            assertEquals("wamid.ORDER", key.id().orElseThrow());
            assertTrue(key.fromMe());
            assertTrue(http.lastUri().toString().endsWith(PHONE_ID + "/messages"));
            var interactive = body(http).getJSONObject("interactive");
            assertEquals("order_details", interactive.getString("type"));
            assertEquals("Please pay", interactive.getJSONObject("body").getString("text"));
            assertEquals("Thanks", interactive.getJSONObject("footer").getString("text"));
            var action = interactive.getJSONObject("action");
            assertEquals("review_and_pay", action.getString("name"));
            var parameters = action.getJSONObject("parameters");
            assertEquals("ref-123", parameters.getString("reference_id"));
            assertEquals("physical-goods", parameters.getString("type"));
            assertEquals("INR", parameters.getString("currency"));
            assertEquals(21000L, parameters.getJSONObject("total_amount").getLongValue("value"));
            assertEquals(100, parameters.getJSONObject("total_amount").getIntValue("offset"));
        }

        @Test
        @DisplayName("serializes items, subtotal, and a described tax")
        void orderBreakdown() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.X\"}]}");
            client(http).sendOrderDetails(RECIPIENT, sampleOrder());
            var order = body(http).getJSONObject("interactive").getJSONObject("action")
                    .getJSONObject("parameters").getJSONObject("order");
            assertEquals("pending", order.getString("status"));
            assertEquals("cat-1", order.getString("catalog_id"));
            var item = order.getJSONArray("items").getJSONObject(0);
            assertEquals("retail-1", item.getString("retailer_id"));
            assertEquals("Widget", item.getString("name"));
            assertEquals(20000L, item.getJSONObject("amount").getLongValue("value"));
            assertEquals(1, item.getIntValue("quantity"));
            assertEquals(20000L, order.getJSONObject("subtotal").getLongValue("value"));
            assertEquals(1000L, order.getJSONObject("tax").getLongValue("value"));
            assertEquals("VAT", order.getJSONObject("tax").getString("description"));
            assertFalse(order.containsKey("shipping"));
            assertFalse(order.containsKey("discount"));
        }

        @Test
        @DisplayName("serializes the payment gateway setting under its type")
        void paymentSettings() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.X\"}]}");
            client(http).sendOrderDetails(RECIPIENT, sampleOrder());
            var settings = body(http).getJSONObject("interactive").getJSONObject("action")
                    .getJSONObject("parameters").getJSONArray("payment_settings").getJSONObject(0);
            assertEquals("payment_gateway", settings.getString("type"));
            var gateway = settings.getJSONObject("payment_gateway");
            assertEquals("razorpay", gateway.getString("type"));
            assertEquals("my-config", gateway.getString("configuration_name"));
        }

        @Test
        @DisplayName("the India variant carries payment_type and payment_configuration without settings")
        void indiaVariant() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.X\"}]}");
            var items = List.of(new CloudOrderItem("retail-1", "Widget", new CloudOrderAmount(20000, 100), 1, null));
            var order = new CloudOrder(CloudOrderStatus.PENDING, null, items, new CloudOrderAmount(20000, 100),
                    null, null, null);
            var message = new CloudOrderDetailsMessage("Pay", null, null, "ref-9", CloudGoodsType.DIGITAL, "INR",
                    new CloudOrderAmount(20000, 100), new CloudOrderPayment.India("india-config", "upi"), order);
            client(http).sendOrderDetails(RECIPIENT, message);
            var parameters = body(http).getJSONObject("interactive").getJSONObject("action")
                    .getJSONObject("parameters");
            assertEquals("upi", parameters.getString("payment_type"));
            assertEquals("india-config", parameters.getString("payment_configuration"));
            assertFalse(parameters.containsKey("payment_settings"));
        }
    }

    @Nested
    @DisplayName("order_status")
    class OrderStatus {
        @Test
        @DisplayName("builds the review_order interactive with status and reference")
        void send() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.STATUS\"}]}");
            var key = client(http).sendOrderStatus(RECIPIENT,
                    new CloudOrderStatusMessage("Your order shipped", null, "ref-123", CloudOrderStatus.SHIPPED,
                            "Tracking: ABC"));
            assertEquals("wamid.STATUS", key.id().orElseThrow());
            assertTrue(key.fromMe());
            var interactive = body(http).getJSONObject("interactive");
            assertEquals("order_status", interactive.getString("type"));
            var action = interactive.getJSONObject("action");
            assertEquals("review_order", action.getString("name"));
            var parameters = action.getJSONObject("parameters");
            assertEquals("ref-123", parameters.getString("reference_id"));
            var order = parameters.getJSONObject("order");
            assertEquals("shipped", order.getString("status"));
            assertEquals("Tracking: ABC", order.getString("description"));
            assertFalse(interactive.containsKey("footer"));
        }

        @Test
        @DisplayName("omits the description when absent and includes the footer when present")
        void minimal() throws Exception {
            var http = http();
            http.respondWith("{\"messages\":[{\"id\":\"wamid.X\"}]}");
            client(http).sendOrderStatus(RECIPIENT,
                    new CloudOrderStatusMessage("Processing", "See you soon", "ref-1", CloudOrderStatus.PROCESSING,
                            null));
            var interactive = body(http).getJSONObject("interactive");
            assertEquals("See you soon", interactive.getJSONObject("footer").getString("text"));
            var order = interactive.getJSONObject("action").getJSONObject("parameters").getJSONObject("order");
            assertFalse(order.containsKey("description"));
        }
    }
}
