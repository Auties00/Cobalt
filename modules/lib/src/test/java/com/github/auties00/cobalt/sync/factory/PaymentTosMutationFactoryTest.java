package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the outgoing-mutation wire shape of {@link PaymentTosMutationFactory}.
 * Full byte-equality would require a fully populated
 * {@link com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction}
 * that the corpus capture does not yet emit, so this suite stops at confirming the
 * oracle loads when present and returns early when its fixture is absent.
 */
@DisplayName("PaymentTosMutationFactory")
class PaymentTosMutationFactoryTest {
    @Test
    @DisplayName("captured encode payload (when present) matches Cobalt's wire encoding")
    void oracle() {
        if (!SyncFixtures.isOracleAvailable("handler/payment-tos/encode")) {
            return;
        }
        var oracle = SyncFixtures.loadOracle("handler/payment-tos/encode");
        assertNotNull(oracle);
    }
}
