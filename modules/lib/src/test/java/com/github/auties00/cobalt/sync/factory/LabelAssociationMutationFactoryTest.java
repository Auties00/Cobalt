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
 * Verifies that {@link LabelAssociationMutationFactory} encodes byte-for-byte against the captured
 * WhatsApp Web encode oracle under {@code handler/label-association/encode}. The label id, chat JID,
 * labelled flag, and timestamp are pinned to the values the oracle was captured with so byte parity
 * holds. The test skips when the oracle fixture is absent.
 */
@DisplayName("LabelAssociationMutationFactory")
class LabelAssociationMutationFactoryTest {
    // Chat JID used as the third index segment of the mutation.
    private static final Jid CHAT_JID = Jid.of("11110000@s.whatsapp.net");

    private LabelAssociationMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LabelAssociationMutationFactory();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/label-association/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/label-association/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.createLabelAssociationMutation(
                "42", CHAT_JID, true, Instant.ofEpochSecond(1_700_000_000L));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
