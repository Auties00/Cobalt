package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the {@code markChatAsReadAction} value encodes byte-for-byte against the captured
 * WhatsApp Web encode oracle under {@code handler/mark-chat-as-read/encode}. The value is rebuilt
 * from the oracle's {@code read} flag and {@code timestampSeconds} field with a null message range so
 * the parity check focuses on the read-boolean encoding; range-bearing parity is covered by the
 * handler tests. The test skips when the oracle fixture is absent.
 */
@DisplayName("MarkChatAsReadMutationFactory")
class MarkChatAsReadMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/mark-chat-as-read/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/mark-chat-as-read/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var read = oracle.getBoolean("read");

        var action = new MarkChatAsReadActionBuilder().read(read).build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .markChatAsReadAction(action)
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
