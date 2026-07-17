package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MuteActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the outgoing-mutation wire shape of {@link MuteChatMutationFactory} by
 * re-encoding a {@link SyncActionValueSpec} and comparing it byte-for-byte against
 * the captured WhatsApp Web oracle. The value is rebuilt from the oracle's
 * {@code muted} flag and {@code muteEndMillis} field rather than through the
 * production factory so the test runs without the AB-props dependency; each test
 * returns early when its oracle fixture is absent.
 */
@DisplayName("MuteChatMutationFactory")
class MuteChatMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/mute-chat/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/mute-chat/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var muted = oracle.getBoolean("muted");
        var muteEndMillis = oracle.getLong("muteEndMillis");

        var builder = new MuteActionBuilder().muted(muted);
        if (muteEndMillis != null) builder.muteEndTimestamp(Instant.ofEpochMilli(muteEndMillis));
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .muteAction(builder.build())
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
