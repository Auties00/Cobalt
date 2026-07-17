package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link LocaleSettingMutationFactory} encodes its {@link SyncActionValueSpec} bytes
 * byte-for-byte against the captured WhatsApp Web encode oracle under
 * {@code handler/locale-setting/encode}, for the {@code "en_US"} locale at a pinned timestamp. The
 * test skips when the oracle fixture is absent.
 */
@DisplayName("LocaleSettingMutationFactory")
class LocaleSettingMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/locale-setting/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/locale-setting/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = new LocaleSettingMutationFactory().getLocaleMutation(Instant.ofEpochSecond(1_700_000_000L), "en_US");
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
