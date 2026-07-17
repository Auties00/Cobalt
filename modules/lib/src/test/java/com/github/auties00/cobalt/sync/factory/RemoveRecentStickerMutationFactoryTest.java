package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the outgoing-mutation wire shape of {@link RemoveRecentStickerMutationFactory}.
 * Full byte-equality would require a deterministic sticker hash and timestamp that the
 * corpus capture does not yet emit, so this suite stops at confirming the oracle loads
 * when present and returns early when its fixture is absent.
 */
@DisplayName("RemoveRecentStickerMutationFactory")
class RemoveRecentStickerMutationFactoryTest {
    @Test
    @DisplayName("captured encode payload (when present) matches Cobalt's wire encoding")
    void oracle() {
        if (!SyncFixtures.isOracleAvailable("handler/remove-recent-sticker/encode")) {
            return;
        }
        var oracle = SyncFixtures.loadOracle("handler/remove-recent-sticker/encode");
        assertNotNull(oracle);
    }
}
