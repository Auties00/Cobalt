package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Cloud message pricing decode")
class CloudMessagePricingTest {
    private static JSONObject value(String json) {
        return JSON.parseObject(json);
    }

    @Test
    @DisplayName("status pricing object with type is captured")
    void pricingWithType() {
        var value = value("""
                {"statuses":[{"id":"wamid.X","status":"sent","timestamp":"1736379100","recipient_id":"01234567890",
                "pricing":{"billable":true,"pricing_model":"CBP","category":"marketing","type":"regular"}}]}""");
        var statuses = CloudWebhookDecoder.decodeStatuses(value);
        assertEquals(1, statuses.size());
        var pricing = statuses.getFirst().pricing();
        assertNotNull(pricing);
        assertTrue(pricing.billable());
        assertEquals("CBP", pricing.pricingModel());
        assertEquals("regular", pricing.pricingType().orElseThrow());
        assertEquals("marketing", pricing.category());
    }

    @Test
    @DisplayName("status pricing object with legacy pricing_type is captured")
    void pricingWithLegacyPricingType() {
        var value = value("""
                {"statuses":[{"id":"wamid.Y","status":"delivered","timestamp":"1736379200","recipient_id":"01234567890",
                "pricing":{"billable":false,"pricing_model":"PMP","category":"utility","pricing_type":"free_customer_service"}}]}""");
        var pricing = CloudWebhookDecoder.decodeStatuses(value).getFirst().pricing();
        assertNotNull(pricing);
        assertFalse(pricing.billable());
        assertEquals("PMP", pricing.pricingModel());
        assertEquals("free_customer_service", pricing.pricingType().orElseThrow());
        assertEquals("utility", pricing.category());
    }

    @Test
    @DisplayName("status pricing object without billable defaults to billable")
    void pricingDefaultsBillable() {
        var value = value("""
                {"statuses":[{"id":"wamid.Z","status":"sent","timestamp":"1736379300","recipient_id":"01234567890",
                "pricing":{"pricing_model":"CBP","category":"authentication"}}]}""");
        var pricing = CloudWebhookDecoder.decodeStatuses(value).getFirst().pricing();
        assertNotNull(pricing);
        assertTrue(pricing.billable());
        assertTrue(pricing.pricingType().isEmpty());
        assertEquals("authentication", pricing.category());
    }

    @Test
    @DisplayName("status without a pricing object yields null pricing")
    void noPricingObject() {
        var value = value("""
                {"statuses":[{"id":"wamid.N","status":"read","timestamp":"1736379400","recipient_id":"01234567890"}]}""");
        var statuses = CloudWebhookDecoder.decodeStatuses(value);
        assertEquals(1, statuses.size());
        assertNull(statuses.getFirst().pricing());
    }
}
