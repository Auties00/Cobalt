package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteChatActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that Cobalt's outgoing delete-chat mutation encodes byte-for-byte against the captured
 * WhatsApp Web encode oracle under {@code handler/delete-chat/encode}. The action value is rebuilt
 * inline (empty {@link DeleteChatActionBuilder}, no message range) rather than via
 * {@link DeleteChatMutationFactory} so the captured shape is pinned independently of any
 * factory-side coalescing. Each test skips when the oracle fixture is absent.
 */
@DisplayName("DeleteChatMutationFactory")
class DeleteChatMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/delete-chat/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/delete-chat/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var action = new DeleteChatActionBuilder().build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .deleteChatAction(action)
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
