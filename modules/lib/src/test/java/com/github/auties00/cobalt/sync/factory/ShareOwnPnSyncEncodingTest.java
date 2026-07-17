package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the wire-shape parity of Cobalt's {@code shareOwnPn} encoding against
 * captured WhatsApp Web {@code WAWebShareOwnPnSync} encode payloads. The
 * handler itself owns no outbound factory, so this test exists as the
 * encode-side companion to
 * {@link com.github.auties00.cobalt.sync.handler.ShareOwnPnHandler}
 * whose incoming-side coverage lives in {@code ShareOwnPnHandlerTest}.
 */
@DisplayName("ShareOwnPn sync encoding")
class ShareOwnPnSyncEncodingTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/share-own-pn/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/share-own-pn/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        // shareOwnPn carries no value payload; only the timestamp field is encoded.
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(1_700_000_000L))
                .build();
        var actual = SyncActionValueSpec.encode(value);

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
