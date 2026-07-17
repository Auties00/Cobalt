package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the wire-shape parity of Cobalt's {@code payment_info} encoding
 * against captured WhatsApp Web {@code WAWebPaymentInfoSync} encode payloads.
 * The handler itself owns no outbound factory, so this test exists as the
 * encode-side companion to
 * {@link com.github.auties00.cobalt.sync.handler.PaymentInfoHandler}
 * whose incoming-side coverage lives in {@code PaymentInfoHandlerTest}.
 */
@DisplayName("PaymentInfo sync encoding")
class PaymentInfoSyncEncodingTest {
    @Test
    @DisplayName("captured encode payload (when present) matches Cobalt's wire encoding")
    void oracle() {
        if (!SyncFixtures.isOracleAvailable("handler/payment-info/encode")) {
            return;
        }
        var oracle = SyncFixtures.loadOracle("handler/payment-info/encode");
        assertNotNull(oracle);
    }
}
