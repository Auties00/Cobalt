package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LidContactActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the wire-shape parity of Cobalt's {@code lidContactAction} encoding
 * against captured WhatsApp Web {@code WAWebLidContactSync} encode payloads.
 * The handler itself owns no outbound factory ({@code lid_contact} is
 * read-only in Cobalt), so this test exists as the encode-side companion to
 * {@link com.github.auties00.cobalt.sync.handler.LidContactHandler}
 * whose incoming-side coverage lives in {@code LidContactHandlerTest}.
 */
@DisplayName("LidContact sync encoding")
class LidContactSyncEncodingTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/lid-contact/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/lid-contact/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var action = new LidContactActionBuilder()
                .fullName("Maria Garcia")
                .firstName("Maria")
                .username("maria")
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(1_700_000_000L))
                .lidContactAction(action)
                .build();
        var actual = SyncActionValueSpec.encode(value);

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
