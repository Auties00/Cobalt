package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.PnForLidChatActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the wire-shape parity of Cobalt's {@code pnForLidChatAction} encoding
 * against captured WhatsApp Web {@code WAWebPnForLidChatSync} encode
 * payloads. The handler itself owns no outbound factory, so this test exists
 * as the encode-side companion to
 * {@link com.github.auties00.cobalt.sync.handler.PnForLidChatHandler}
 * whose incoming-side coverage lives in {@code PnForLidChatHandlerTest}.
 */
@DisplayName("PnForLidChat sync encoding")
class PnForLidChatSyncEncodingTest {
    private static final Jid CONTACT_PN = Jid.of("33330000@s.whatsapp.net");

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/pn-for-lid-chat/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/pn-for-lid-chat/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(1_700_000_000L))
                .pnForLidChatAction(action)
                .build();
        var actual = SyncActionValueSpec.encode(value);

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
