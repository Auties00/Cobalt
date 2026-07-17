package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelEditAction;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link LabelEditMutationFactory} encodes byte-for-byte against the captured WhatsApp
 * Web encode oracle under {@code handler/label-edit/encode}. Every label field (id, name, colour,
 * deleted flag, predefined id, active flag, list type, timestamp) is pinned to the values the oracle
 * was captured with so byte parity holds. The test skips when the oracle fixture is absent.
 */
@DisplayName("LabelEditMutationFactory")
class LabelEditMutationFactoryTest {
    private LabelEditMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LabelEditMutationFactory();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/label-edit/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/label-edit/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.getLabelMutation(
                "42", "Customers", 5, false, 0, true,
                LabelEditAction.ListType.NONE, Instant.ofEpochSecond(1_700_000_000L));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
