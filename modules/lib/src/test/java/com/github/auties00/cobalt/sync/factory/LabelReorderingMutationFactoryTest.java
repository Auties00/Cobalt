package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link LabelReorderingMutationFactory} encodes its {@link SyncActionValueSpec} bytes
 * byte-for-byte against the captured WhatsApp Web encode oracle under
 * {@code handler/label-reordering/encode}. The test skips when the oracle fixture is absent.
 */
@DisplayName("LabelReorderingMutationFactory")
class LabelReorderingMutationFactoryTest {
    private LabelReorderingMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LabelReorderingMutationFactory();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/label-reordering/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/label-reordering/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.getReorderLabelsMutation(List.of(1, 2, 3), Instant.ofEpochSecond(1_700_000_000L));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
