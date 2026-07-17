package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Covers {@link ArchiveChatMutationFactory} against the captured WhatsApp Web
 * encode oracle for {@code handler/archive-chat/encode}. The
 * {@link SyncActionValueBuilder} graph is rebuilt inline rather than through
 * the factory so the test pins the exact field shape the oracle captures
 * (archived flag, no message range). The check is gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so it no-ops cleanly until
 * the fixture is present.
 */
@DisplayName("ArchiveChatMutationFactory")
class ArchiveChatMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/archive-chat/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/archive-chat/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var archived = oracle.getBoolean("archived");

        var action = new ArchiveChatActionBuilder().archived(archived).build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .archiveChatAction(action)
                .build();
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
