package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link VoipRelayAllCallsMutationFactory} encodes the privacy-calls relay toggle
 * to the same bytes WA Web emits, by diffing against a captured oracle fixture. The factory is
 * invoked directly with the enabled branch at a pinned timestamp; the assertion is gated on the
 * oracle fixture being present so the suite skips cleanly when it is absent.
 */
@DisplayName("VoipRelayAllCallsMutationFactory")
class VoipRelayAllCallsMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/voip-relay-all-calls/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/voip-relay-all-calls/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = new VoipRelayAllCallsMutationFactory().getVoipRelayAllCallsMutation(Instant.ofEpochSecond(1_700_000_000L), true);
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
