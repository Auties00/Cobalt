package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the outgoing-mutation wire shape of {@link PinChatMutationFactory} by
 * re-encoding a {@link SyncActionValueSpec} and comparing it byte-for-byte against
 * the captured WhatsApp Web oracle. The value is rebuilt from the oracle's
 * {@code pinned} flag and {@code timestampSeconds} field rather than through the
 * production factory so the check focuses on the boolean encoding; each test returns
 * early when its oracle fixture is absent.
 */
@DisplayName("PinChatMutationFactory")
class PinChatMutationFactoryTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/pin-chat/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/pin-chat/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var pinned = oracle.getBoolean("pinned");

        var action = new PinActionBuilder().pinned(pinned).build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .pinAction(action)
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
