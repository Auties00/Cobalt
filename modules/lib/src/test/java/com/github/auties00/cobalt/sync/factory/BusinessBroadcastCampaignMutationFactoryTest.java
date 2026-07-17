package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction.Status;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignActionBuilder;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.wam.TestWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link BusinessBroadcastCampaignMutationFactory} against the captured
 * WhatsApp Web encode oracle for
 * {@code handler/business-broadcast-campaign/encode}. The sample campaign
 * action built by {@code sampleAction()} uses fixed field values that must
 * stay in lockstep with those the oracle was captured under for byte equality
 * to hold. The check is gated on {@link SyncFixtures#isOracleAvailable(String)}
 * so it no-ops cleanly until the fixture is present.
 */
@DisplayName("BusinessBroadcastCampaignMutationFactory")
class BusinessBroadcastCampaignMutationFactoryTest {
    private static final String BROADCAST_JID = "123-1234567890@broadcast";

    private BusinessBroadcastCampaignMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new BusinessBroadcastCampaignMutationFactory(TestWamService.create(TestWhatsAppClient.create()));
    }

    private BusinessBroadcastCampaignAction sampleAction() {
        return new BusinessBroadcastCampaignActionBuilder()
                .deviceId(0)
                .broadcastJid(BROADCAST_JID)
                .name("Promo")
                .msgId("tpl-1")
                .status(Status.SCHEDULED)
                .createTimestamp(1_700_000_000_000L)
                .scheduledTimestamp(1_700_001_000_000L)
                .reservedQuota(100)
                .build();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/business-broadcast-campaign/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/business-broadcast-campaign/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.getCampaignMutation("camp-oracle", sampleAction(),
                Instant.ofEpochSecond(1_700_000_000L));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
