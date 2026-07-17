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
 * Covers the outgoing-mutation wire shape of {@link QuickReplyMutationFactory} by
 * re-encoding a {@link SyncActionValueSpec} and comparing it byte-for-byte against
 * the captured WhatsApp Web oracle. Only the add-or-edit path is exercised; the
 * delete path differs solely by a {@code deleted} flag flip over the same value
 * shape. Each test returns early when its oracle fixture is absent.
 */
@DisplayName("QuickReplyMutationFactory")
class QuickReplyMutationFactoryTest {
    private QuickReplyMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new QuickReplyMutationFactory();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/quick-reply/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/quick-reply/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.getQuickReplyAddOrEditMutation(
                "qr-oracle", "/hello", "Hi", 1, List.of("k1"),
                Instant.ofEpochSecond(1_700_000_000L));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
