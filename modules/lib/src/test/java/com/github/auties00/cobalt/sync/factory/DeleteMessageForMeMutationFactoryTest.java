package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteMessageForMeActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that Cobalt's outgoing delete-message-for-me mutation encodes byte-for-byte against the
 * captured WhatsApp Web encode oracle under {@code handler/delete-message-for-me/encode}. The action
 * graph is rebuilt inline so the captured minimal shape (only {@code deleteMedia} and timestamp) is
 * pinned independently of the full per-message index the factory would normally produce. Each test
 * skips when the oracle fixture is absent.
 */
@DisplayName("DeleteMessageForMeMutationFactory")
class DeleteMessageForMeMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/delete-message-for-me/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/delete-message-for-me/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var deleteMedia = oracle.getBoolean("deleteMedia");

        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .deleteMessageForMeAction(new DeleteMessageForMeActionBuilder().deleteMedia(deleteMedia).build())
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
