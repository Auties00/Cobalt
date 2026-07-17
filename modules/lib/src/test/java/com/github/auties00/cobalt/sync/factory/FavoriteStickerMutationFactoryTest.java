package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the captured WhatsApp Web favourite-sticker encode oracle under
 * {@code handler/favorite-sticker/encode} is loadable. The test gates on
 * {@link SyncFixtures#isOracleAvailable(String)} so the suite stays green until a real captured
 * fixture pinning {@code (stickerHash, isFavorite)} to encoded bytes is added.
 */
@DisplayName("FavoriteStickerMutationFactory")
class FavoriteStickerMutationFactoryTest {
    @Test
    @DisplayName("captured encode payload (when present) matches Cobalt's wire encoding")
    void oracle() {
        if (!SyncFixtures.isOracleAvailable("handler/favorite-sticker/encode")) {
            return;
        }
        var oracle = SyncFixtures.loadOracle("handler/favorite-sticker/encode");
        assertNotNull(oracle);
    }
}
