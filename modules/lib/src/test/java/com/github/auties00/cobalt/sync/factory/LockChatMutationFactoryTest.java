package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.LockChatActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the {@code lockChatAction} value encodes byte-for-byte against the captured WhatsApp
 * Web encode oracle under {@code handler/lock-chat/encode}. The value is rebuilt from the oracle's
 * {@code locked} flag and {@code timestampSeconds} field rather than via
 * {@link LockChatMutationFactory} directly, so the parity check exercises the protobuf encoding
 * rather than index-string formatting; the companion archive and pin mutations emitted by
 * {@link LockChatMutationFactory#getMutationsForLock(Instant, boolean, com.github.auties00.cobalt.wire.core.jid.Jid, SyncActionMessageRange)}
 * are covered by their own factory tests. The test skips when the oracle fixture is absent.
 */
@DisplayName("LockChatMutationFactory")
class LockChatMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/lock-chat/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/lock-chat/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var locked = oracle.getBoolean("locked");

        var action = new LockChatActionBuilder().locked(locked).build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .lockChatAction(action)
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
