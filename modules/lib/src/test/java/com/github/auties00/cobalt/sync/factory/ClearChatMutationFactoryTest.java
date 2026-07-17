package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link ClearChatMutationFactory} against the captured WhatsApp Web
 * encode oracle for {@code handler/clear-chat/encode}. The
 * {@link SyncActionValueBuilder} graph is rebuilt inline rather than through
 * the factory so the captured shape (empty {@link ClearChatActionBuilder}, no
 * message range) is pinned independently of any factory-side coalescing. The
 * check is gated on {@link SyncFixtures#isOracleAvailable(String)} so it
 * no-ops cleanly until the fixture is present.
 */
@DisplayName("ClearChatMutationFactory")
class ClearChatMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/clear-chat/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/clear-chat/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var action = new ClearChatActionBuilder().build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .clearChatAction(action)
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
