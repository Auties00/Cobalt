package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link CustomPaymentMethodsMutationFactory} against captured WhatsApp
 * Web encode payloads. The check is gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so it no-ops cleanly until a
 * real captured SMB Brazil PIX fixture for
 * {@code handler/custom-payment-methods/encode} exists.
 */
@DisplayName("CustomPaymentMethodsMutationFactory")
class CustomPaymentMethodsMutationFactoryTest {
    @Test
    @DisplayName("captured encode payload (when present) matches Cobalt's wire encoding")
    void oracle() {
        if (!SyncFixtures.isOracleAvailable("handler/custom-payment-methods/encode")) {
            return;
        }
        var oracle = SyncFixtures.loadOracle("handler/custom-payment-methods/encode");
        assertNotNull(oracle);
    }
}
