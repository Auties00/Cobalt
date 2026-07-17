package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link DisableLinkPreviewsMutationFactory} encodes byte-for-byte against the captured
 * WhatsApp Web encode oracle under {@code handler/disable-link-previews/encode}. The timestamp is
 * pinned to the seed ({@code 1_700_000_000L}) used when capturing the oracle and the disabled flag is
 * set to {@code true} to match the capture. The test skips when the oracle fixture is absent.
 */
@DisplayName("DisableLinkPreviewsMutationFactory")
class DisableLinkPreviewsMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/disable-link-previews/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/disable-link-previews/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = new DisableLinkPreviewsMutationFactory().getDisableLinkPreviewsMutation(Instant.ofEpochSecond(1_700_000_000L), true);
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
