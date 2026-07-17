package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the outgoing-mutation wire shape of {@link PushNameSettingMutationFactory}
 * by re-encoding a {@link SyncActionValueSpec} and comparing it byte-for-byte against
 * the captured WhatsApp Web oracle. The factory is driven with the canonical
 * {@code "Maria"} pushname at a pinned timestamp so the oracle reproduces across
 * builds; the test returns early when its fixture is absent.
 */
@DisplayName("PushNameSettingMutationFactory")
class PushNameSettingMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/push-name-setting/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/push-name-setting/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = new PushNameSettingMutationFactory().getPushnameMutation(Instant.ofEpochSecond(1_700_000_000L), "Maria");
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
