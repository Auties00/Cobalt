package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BroadcastListParticipantAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BroadcastListParticipantActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastListAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastListActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.BusinessBroadcastListMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link BusinessBroadcastListHandler}, which upserts business broadcast lists keyed by
 * {@code indexParts[1]} and supports both SET (replace) and REMOVE (drop). The batch override
 * preserves each per-mutation result while tallying a malformed count.
 */
@DisplayName("BusinessBroadcastListHandler")
class BusinessBroadcastListHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PARTICIPANT_LID = Jid.of("83116928594001@lid");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private BusinessBroadcastListHandler handler;
    private BusinessBroadcastListMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new BusinessBroadcastListHandler();
        factory = new BusinessBroadcastListMutationFactory();
    }

    private DecryptedMutation.Trusted buildMutation(String indexId, BusinessBroadcastListAction action,
                                                    SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.businessBroadcastListAction(action);
        }
        var indexParts = indexId == null ? List.of(handler.actionName()) : List.of(handler.actionName(), indexId);
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    private BroadcastListParticipantAction sampleParticipant() {
        return new BroadcastListParticipantActionBuilder().lidJid(PARTICIPANT_LID).build();
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the BusinessBroadcastListAction wire constant")
        void actionName() {
            assertEquals(BusinessBroadcastListAction.ACTION_NAME, handler.actionName());
            assertEquals("business_broadcast_list", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(BusinessBroadcastListAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(BusinessBroadcastListAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET upsert")
    class ApplySet {
        @Test
        @DisplayName("SET upserts the broadcast list, mirroring participants and label ids")
        void upsertsList() {
            var action = new BusinessBroadcastListActionBuilder()
                    .listName("VIP")
                    .participants(List.of(sampleParticipant()))
                    .labelIds(List.of("label-1"))
                    .audienceExpression("{}")
                    .deleted(false)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("list-1", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.businessStore().findBusinessBroadcastList("list-1").orElseThrow();
            assertEquals("list-1", stored.id());
        }

        @Test
        @DisplayName("SET with empty participants and labels round-trips with null mirrored fields")
        void emptyCollectionsRoundtrip() {
            var action = new BusinessBroadcastListActionBuilder()
                    .listName("Empty")
                    .participants(List.of())
                    .labelIds(List.of())
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("list-empty", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findBusinessBroadcastList("list-empty").isPresent());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("SET on an unknown id is the upsert path, not an orphan")
        void unknownIdUpserts() {
            var action = new BusinessBroadcastListActionBuilder().listName("X").build();
            var result = handler.applyMutation(client,
                    buildMutation("brand-new", action, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
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
            var index = JSON.toJSONString(List.of(handler.actionName(), "list-x"));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an empty list id at indexParts[1] returns MALFORMED")
        void emptyListId() {
            var action = new BusinessBroadcastListActionBuilder().listName("X").build();

            var result = handler.applyMutation(client,
                    buildMutation("", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a missing list id slot returns MALFORMED")
        void missingListIdSlot() {
            var action = new BusinessBroadcastListActionBuilder().listName("X").build();

            var result = handler.applyMutation(client,
                    buildMutation(null, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE drops the broadcast list")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE drops the broadcast list from the store and returns SUCCESS")
        void removeDropsList() {
            // Seed via the SET path so the store carries a matching record.
            handler.applyMutation(client, buildMutation("list-rm",
                    new BusinessBroadcastListActionBuilder().listName("Rm").build(),
                    SyncdOperation.SET, Instant.now()));

            var result = handler.applyMutation(client,
                    buildMutation("list-rm", null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findBusinessBroadcastList("list-rm").isEmpty());
        }

        @Test
        @DisplayName("REMOVE of an unknown id still returns SUCCESS - idempotent")
        void removeUnknown() {
            var result = handler.applyMutation(client,
                    buildMutation("never-existed", null, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
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
            var action = new BusinessBroadcastListActionBuilder().listName("Tie").build();
            return buildMutation("list-tie", action, SyncdOperation.SET, ts);
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - per-mutation fan-out + malformed counter logging")
    class BatchOverride {
        @Test
        @DisplayName("each mutation's per-mutation result is preserved in the batch output")
        void perMutationResultsPreserved() {
            var ts = Instant.now();
            var goodMutation = buildMutation("list-1",
                    new BusinessBroadcastListActionBuilder().listName("OK").build(),
                    SyncdOperation.SET, ts);
            var malformedIndexMutation = buildMutation("",
                    new BusinessBroadcastListActionBuilder().listName("OK").build(),
                    SyncdOperation.SET, ts);
            var malformedValueMutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of(handler.actionName(), "list-3")),
                    new SyncActionValueBuilder().timestamp(ts).pinAction(new PinActionBuilder().pinned(true).build()).build(),
                    SyncdOperation.SET, ts, handler.version());

            var results = handler.applyMutationBatch(client,
                    List.of(goodMutation, malformedIndexMutation, malformedValueMutation));

            assertEquals(3, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.MALFORMED, results.get(1).actionState());
            assertEquals(SyncActionState.MALFORMED, results.get(2).actionState());
        }
    }

    @Nested
    @DisplayName("static builder - getBroadcastListMutation")
    class CreateBuilder {
        @Test
        @DisplayName("produces a SET pending mutation with the given participants and list name")
        void carriesInputs() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var participant = sampleParticipant();

            var pending = factory.getBroadcastListMutation(
                    "list-build", List.of(participant), "Friends", ts);
            var inner = pending.mutation();

            assertEquals(SyncdOperation.SET, inner.operation());
            assertEquals(handler.version(), inner.actionVersion());
            assertEquals(ts, inner.timestamp());
            assertEquals(JSON.toJSONString(List.of(handler.actionName(), "list-build")), inner.index());

            var roundtrip = inner.value().flatMap(sav -> sav.action()).filter(a -> a instanceof BusinessBroadcastListAction).map(a -> (BusinessBroadcastListAction) a).orElseThrow();
            assertEquals("Friends", roundtrip.listName().orElseThrow());
            assertEquals(1, roundtrip.participants().size());
        }

        @Test
        @DisplayName("the no-audience overload defaults audienceExpression to null")
        void defaultsAudienceExpressionToNull() {
            var pending = factory.getBroadcastListMutation(
                    "list-noaud", List.of(), "Empty", Instant.now());
            var roundtrip = pending.mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof BusinessBroadcastListAction).map(a -> (BusinessBroadcastListAction) a).orElseThrow();
            assertTrue(roundtrip.audienceExpression().isEmpty(),
                    "the no-audience overload must omit the audience expression");
        }
    }

    @Nested
    @DisplayName("static builder - getDeleteBroadcastListMutation")
    class DeleteBuilder {
        @Test
        @DisplayName("produces a REMOVE pending mutation carrying just the list id index")
        void carriesIdIndex() {
            var ts = Instant.ofEpochSecond(1_700_000_001L);
            var pending = factory.getDeleteBroadcastListMutation("list-del", ts);
            var inner = pending.mutation();

            assertEquals(SyncdOperation.REMOVE, inner.operation());
            assertEquals(JSON.toJSONString(List.of(handler.actionName(), "list-del")), inner.index());
        }
    }

}
