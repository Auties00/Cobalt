package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.StarActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the wire-shape parity of Cobalt's {@code starAction} encoding against
 * captured WhatsApp Web {@code WAWebStarMessageSync} encode payloads. The
 * handler itself owns no outbound factory, so this test exists as the
 * encode-side companion to
 * {@link com.github.auties00.cobalt.sync.handler.StarMessageHandler}
 * whose incoming-side coverage lives in {@code StarMessageHandlerTest}.
 */
@DisplayName("StarMessage sync encoding")
class StarMessageSyncEncodingTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/star-message/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/star-message/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var starred = oracle.getBoolean("starred");

        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .starAction(new StarActionBuilder().starred(starred).build())
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
