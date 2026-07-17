package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link ChatAssignmentOpenedStatusMutationFactory} against the captured
 * WhatsApp Web encode oracle for
 * {@code handler/chat-assignment-opened-status/encode}. The chat JID, agent id,
 * open flag, and timestamp are pinned to the values the oracle was captured
 * under so byte parity holds. The check is gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so it no-ops cleanly until
 * the fixture is present.
 */
@DisplayName("ChatAssignmentOpenedStatusMutationFactory")
class ChatAssignmentOpenedStatusMutationFactoryTest {
    private static final Jid CHAT_JID = Jid.of("12345@s.whatsapp.net");

    private static final String AGENT_ID = "agent-1";

    private ChatAssignmentOpenedStatusMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ChatAssignmentOpenedStatusMutationFactory();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/chat-assignment-opened-status/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/chat-assignment-opened-status/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.createChatOpenedMutation(
                CHAT_JID, AGENT_ID, true, Instant.ofEpochSecond(1_700_000_000L));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
