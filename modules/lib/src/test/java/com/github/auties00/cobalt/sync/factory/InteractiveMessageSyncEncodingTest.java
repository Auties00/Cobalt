package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageAction.InteractiveMessageActionMode;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the wire-shape parity of Cobalt's {@code interactiveMessageAction}
 * encoding against captured WhatsApp Web {@code WAWebInteractiveMessageSync}
 * encode payloads. The handler itself owns no outbound factory, so this test
 * exists as the encode-side companion to
 * {@link com.github.auties00.cobalt.sync.handler.InteractiveMessageHandler}
 * whose incoming-side coverage lives in {@code InteractiveMessageHandlerTest}.
 */
@DisplayName("InteractiveMessage sync encoding")
class InteractiveMessageSyncEncodingTest {
    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encode output when the oracle is present")
    void byteParityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/interactive-message/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/interactive-message/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");
        var agmId = oracle.getString("agmId");

        var builder = new InteractiveMessageActionBuilder().type(InteractiveMessageActionMode.DISABLE_CTA);
        if (agmId != null) builder.agmId(agmId);
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(oracle.getLong("timestampSeconds")))
                .interactiveMessageAction(builder.build())
                .build();
        assertNotNull(expected);
        assertArrayEquals(expected, SyncActionValueSpec.encode(value));
    }
}
