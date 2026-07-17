package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction.Status;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.BusinessBroadcastCampaignMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link BusinessBroadcastCampaignHandler}, which upserts business broadcast campaigns keyed
 * by {@code indexParts[1]} and supports both SET (replace) and REMOVE (drop). A SET payload is
 * rejected as MALFORMED unless it carries non-null {@code broadcastJid}, {@code deviceId}, and
 * {@code status}.
 */
@DisplayName("BusinessBroadcastCampaignHandler")
class BusinessBroadcastCampaignHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String BROADCAST_JID = "123-1234567890@broadcast";

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private BusinessBroadcastCampaignHandler handler;
    private BusinessBroadcastCampaignMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new BusinessBroadcastCampaignHandler();
        factory = new BusinessBroadcastCampaignMutationFactory(TestWamService.create(client));
    }

    private DecryptedMutation.Trusted buildMutation(String indexId, BusinessBroadcastCampaignAction action,
                                                    SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.businessBroadcastCampaignAction(action);
        }
        var indexParts = indexId == null ? List.of(handler.actionName()) : List.of(handler.actionName(), indexId);
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
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

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the wire constant")
        void actionName() {
            assertEquals(BusinessBroadcastCampaignAction.ACTION_NAME, handler.actionName());
            assertEquals("business_broadcast_campaign", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(BusinessBroadcastCampaignAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(BusinessBroadcastCampaignAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET upsert")
    class ApplySet {
        @Test
        @DisplayName("SET with all required fields upserts the campaign")
        void upsertsCampaign() {
            var result = handler.applyMutation(client,
                    buildMutation("camp-1", sampleAction(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.businessStore().findBusinessBroadcastCampaign("camp-1").orElseThrow();
            assertEquals("camp-1", stored.id());
            assertEquals(Status.SCHEDULED, stored.status().orElseThrow());
            assertEquals(Jid.of(BROADCAST_JID), stored.broadcastJid().orElseThrow());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("SET on an unknown id is the upsert path, not an orphan")
        void unknownIdUpserts() {
            assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client,
                    buildMutation("brand-new", sampleAction(), SyncdOperation.SET, Instant.now()))
                    .actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SET whose value carries the wrong action returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of(handler.actionName(), "camp-x"));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a SET missing broadcastJid returns MALFORMED")
        void missingBroadcastJid() {
            var action = new BusinessBroadcastCampaignActionBuilder()
                    .deviceId(0)
                    .status(Status.SCHEDULED)
                    .build();
            var result = handler.applyMutation(client,
                    buildMutation("camp-bad", action, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a SET missing deviceId returns MALFORMED")
        void missingDeviceId() {
            var action = new BusinessBroadcastCampaignActionBuilder()
                    .broadcastJid(BROADCAST_JID)
                    .status(Status.SCHEDULED)
                    .build();
            var result = handler.applyMutation(client,
                    buildMutation("camp-bad", action, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a SET missing status returns MALFORMED")
        void missingStatus() {
            var action = new BusinessBroadcastCampaignActionBuilder()
                    .broadcastJid(BROADCAST_JID)
                    .deviceId(0)
                    .build();
            var result = handler.applyMutation(client,
                    buildMutation("camp-bad", action, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an empty campaign id returns MALFORMED")
        void emptyId() {
            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client,
                    buildMutation("", sampleAction(), SyncdOperation.SET, Instant.now()))
                    .actionState());
        }

        @Test
        @DisplayName("a missing campaign id slot returns MALFORMED")
        void missingIdSlot() {
            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client,
                    buildMutation(null, sampleAction(), SyncdOperation.SET, Instant.now()))
                    .actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE drops the campaign")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE drops the campaign from the store")
        void removeDropsCampaign() {
            handler.applyMutation(client,
                    buildMutation("camp-rm", sampleAction(), SyncdOperation.SET, Instant.now()));

            var result = handler.applyMutation(client,
                    buildMutation("camp-rm", null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findBusinessBroadcastCampaign("camp-rm").isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote - APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutationAt(Instant.ofEpochSecond(1_000));
            var remote = mutationAt(Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote - SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutationAt(Instant.ofEpochSecond(2_000));
            var remote = mutationAt(Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private DecryptedMutation.Trusted mutationAt(Instant ts) {
            return buildMutation("camp-tie", sampleAction(), SyncdOperation.SET, ts);
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - per-mutation fan-out")
    class BatchOverride {
        @Test
        @DisplayName("each mutation's per-mutation result is preserved in the batch output")
        void preservesResults() {
            var ts = Instant.now();
            var good = buildMutation("camp-batch-1", sampleAction(), SyncdOperation.SET, ts);
            var malformedIndex = buildMutation("", sampleAction(), SyncdOperation.SET, ts);
            var malformedValueAction = new BusinessBroadcastCampaignActionBuilder()
                    .broadcastJid(BROADCAST_JID)
                    .deviceId(0)
                    .build(); // missing status
            var malformedValue = buildMutation("camp-batch-2", malformedValueAction, SyncdOperation.SET, ts);

            var results = handler.applyMutationBatch(client, List.of(good, malformedIndex, malformedValue));

            assertEquals(3, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.MALFORMED, results.get(1).actionState());
            assertEquals(SyncActionState.MALFORMED, results.get(2).actionState());
        }
    }

    @Nested
    @DisplayName("static builder - getCampaignMutation")
    class CreateBuilder {
        @Test
        @DisplayName("produces a SET pending mutation carrying the campaign action and index")
        void carriesInputs() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var action = sampleAction();

            var pending = factory.getCampaignMutation("camp-9", action, ts);
            var inner = pending.mutation();

            assertEquals(SyncdOperation.SET, inner.operation());
            assertEquals(handler.version(), inner.actionVersion());
            assertEquals(ts, inner.timestamp());
            assertEquals(JSON.toJSONString(List.of(handler.actionName(), "camp-9")), inner.index());

            var roundtrip = inner.value().flatMap(sav -> sav.action()).filter(a -> a instanceof BusinessBroadcastCampaignAction).map(a -> (BusinessBroadcastCampaignAction) a).orElseThrow();
            assertEquals(BROADCAST_JID, roundtrip.broadcastJid().orElseThrow());
        }
    }

    @Nested
    @DisplayName("static builder - getDeleteCampaignMutation")
    class DeleteBuilder {
        @Test
        @DisplayName("produces a REMOVE pending mutation carrying just the campaign id")
        void carriesIdIndex() {
            var ts = Instant.ofEpochSecond(1_700_000_001L);
            var pending = factory.getDeleteCampaignMutation("camp-del", ts);
            var inner = pending.mutation();

            assertEquals(SyncdOperation.REMOVE, inner.operation());
            assertEquals(JSON.toJSONString(List.of(handler.actionName(), "camp-del")), inner.index());
        }
    }

}
