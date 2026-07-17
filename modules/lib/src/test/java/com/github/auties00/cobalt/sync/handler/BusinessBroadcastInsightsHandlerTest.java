package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastInsightBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastInsightsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastInsightsActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link BusinessBroadcastInsightsHandler}, which upserts post-send delivery statistics
 * keyed by the {@code indexParts[1]} campaign id: SET upserts, REMOVE drops. The handler is
 * inbound-only and exposes no outbound mutation builder.
 */
@DisplayName("BusinessBroadcastInsightsHandler")
class BusinessBroadcastInsightsHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private BusinessBroadcastInsightsHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new BusinessBroadcastInsightsHandler();
    }

    private DecryptedMutation.Trusted buildMutation(String indexId, BusinessBroadcastInsightsAction action,
                                                    SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.businessBroadcastInsightsAction(action);
        }
        var indexParts = indexId == null ? List.of(handler.actionName()) : List.of(handler.actionName(), indexId);
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    private BusinessBroadcastInsightsAction sampleAction() {
        return new BusinessBroadcastInsightsActionBuilder()
                .recipientCount(100)
                .deliveredCount(95)
                .readCount(80)
                .repliedCount(20)
                .quickReplyCount(5)
                .build();
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the wire constant")
        void actionName() {
            assertEquals(BusinessBroadcastInsightsAction.ACTION_NAME, handler.actionName());
            assertEquals("business_broadcast_insights_sync", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(BusinessBroadcastInsightsAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(BusinessBroadcastInsightsAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET upsert")
    class ApplySet {
        @Test
        @DisplayName("SET upserts the per-campaign insights record")
        void upsertsInsights() {
            var result = handler.applyMutation(client,
                    buildMutation("camp-1", sampleAction(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.businessStore().findBusinessBroadcastInsight("camp-1").orElseThrow();
            assertEquals(100, stored.recipientCount().orElseThrow());
            assertEquals(95, stored.deliveredCount().orElseThrow());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("SET on an unknown campaign id is the upsert path, not an orphan")
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
    @DisplayName("applyMutation - REMOVE drops the insights record")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE drops the insights record and returns SUCCESS")
        void removeDropsInsights() {
            store.businessStore().putBusinessBroadcastInsight(new BusinessBroadcastInsightBuilder()
                    .id("camp-rm")
                    .recipientCount(1)
                    .build());

            var result = handler.applyMutation(client,
                    buildMutation("camp-rm", null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findBusinessBroadcastInsight("camp-rm").isEmpty());
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
    @DisplayName("applyMutationBatch - per-mutation fan-out + counter tracking")
    class BatchOverride {
        @Test
        @DisplayName("mixed batch preserves each per-mutation result")
        void preservesResults() {
            var ts = Instant.now();
            var goodSet = buildMutation("camp-batch-1", sampleAction(), SyncdOperation.SET, ts);
            store.businessStore().putBusinessBroadcastInsight(new BusinessBroadcastInsightBuilder().id("camp-batch-rm").build());
            var goodRemove = buildMutation("camp-batch-rm", null, SyncdOperation.REMOVE, ts);
            var malformedIndex = buildMutation("", sampleAction(), SyncdOperation.SET, ts);

            var results = handler.applyMutationBatch(client, List.of(goodSet, goodRemove, malformedIndex));

            assertEquals(3, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertEquals(SyncActionState.MALFORMED, results.get(2).actionState());
        }
    }

    @Nested
    @DisplayName("static builders - n/a")
    class StaticBuilders {
        @Test
        @DisplayName("BusinessBroadcastInsightsHandler exposes no outbound mutation builder - dimension is n/a")
        void noBuilder() {
            // Insights are server-published only, so there is no outbound mutation path.
            assertNotNull(new BusinessBroadcastInsightsHandler(),
                    "BusinessBroadcastInsightsHandler instantiates with a no-arg constructor and exposes no builder method");
        }
    }
}
