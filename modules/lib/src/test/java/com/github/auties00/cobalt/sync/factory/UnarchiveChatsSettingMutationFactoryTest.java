package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the outgoing-mutation wire shape of {@link UnarchiveChatsSettingMutationFactory}
 * by re-encoding a {@link SyncActionValueSpec} and comparing it byte-for-byte against
 * the captured WhatsApp Web oracle. The factory is driven with the
 * {@code unarchiveChats == true} branch at a pinned timestamp; the disabled branch
 * differs only by the inner boolean. The test returns early when its fixture is absent.
 */
@DisplayName("UnarchiveChatsSettingMutationFactory")
class UnarchiveChatsSettingMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/unarchive-chats-setting/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/unarchive-chats-setting/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = new UnarchiveChatsSettingMutationFactory().getUnarchiveChatsMutation(Instant.ofEpochSecond(1_700_000_000L), true);
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
