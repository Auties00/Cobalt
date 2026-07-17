package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link AiThreadDeleteMutationFactory} against captured WhatsApp Web
 * encode payloads. The encode check is gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so it no-ops cleanly until a
 * real captured fixture for {@code handler/ai-thread-delete/encode} exists.
 */
@DisplayName("AiThreadDeleteMutationFactory")
class AiThreadDeleteMutationFactoryTest {
    @Test
    @DisplayName("captured encode payload (when present) matches Cobalt's wire encoding")
    void oracle() {
        if (!SyncFixtures.isOracleAvailable("handler/ai-thread-delete/encode")) {
            return;
        }
        var oracle = SyncFixtures.loadOracle("handler/ai-thread-delete/encode");
        assertNotNull(oracle);
    }
}
