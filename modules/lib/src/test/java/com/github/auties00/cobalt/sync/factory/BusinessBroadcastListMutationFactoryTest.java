package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BroadcastListParticipantAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BroadcastListParticipantActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link BusinessBroadcastListMutationFactory} against the captured
 * WhatsApp Web encode oracle for {@code handler/business-broadcast-list/encode}.
 * The test drives the four-argument overload (no audience expression) with a
 * single LID participant, matching the way the oracle was captured (null
 * audience). The check is gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so it no-ops cleanly until
 * the fixture is present.
 */
@DisplayName("BusinessBroadcastListMutationFactory")
class BusinessBroadcastListMutationFactoryTest {
    private static final Jid PARTICIPANT_LID = Jid.of("83116928594001@lid");

    private BusinessBroadcastListMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new BusinessBroadcastListMutationFactory();
    }

    private BroadcastListParticipantAction sampleParticipant() {
        return new BroadcastListParticipantActionBuilder().lidJid(PARTICIPANT_LID).build();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/business-broadcast-list/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/business-broadcast-list/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.getBroadcastListMutation(
                "list-oracle", List.of(sampleParticipant()), "Oracle",
                Instant.ofEpochSecond(1_700_000_000L));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
