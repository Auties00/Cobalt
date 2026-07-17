package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.preference.QuickReplyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.QuickReplyAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.QuickReplyActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.QuickReplyMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link QuickReplyHandler}: a {@link SyncdOperation#SET} with {@code deleted=true}
 * drops the entry by id; a {@code SET} with non-empty {@code shortcut} and {@code message}
 * upserts a {@link com.github.auties00.cobalt.wire.linked.preference.QuickReply} keyed by
 * {@code indexParts[1]}; a missing id, a missing {@link QuickReplyAction} payload, an
 * empty {@code shortcut}, or an empty {@code message} surface as
 * {@link SyncActionState#MALFORMED}; non-{@code SET} operations surface as
 * {@link SyncActionState#UNSUPPORTED}; the default conflict resolution chooses the later
 * timestamp. Both the handler and the {@link QuickReplyMutationFactory} outbound builders
 * are exercised.
 */
@DisplayName("QuickReplyHandler")
class QuickReplyHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private QuickReplyHandler handler;
    private QuickReplyMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new QuickReplyHandler();
        factory = new QuickReplyMutationFactory();
    }

    // indexId == null yields the singleton index ["quick_reply"] (malformed-index branch);
    // action == null omits the quickReplyAction sub-message (malformed-value branch).
    private DecryptedMutation.Trusted buildMutation(String indexId, QuickReplyAction action, SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.quickReplyAction(action);
        }
        var indexParts = indexId == null ? List.of(handler.actionName()) : List.of(handler.actionName(), indexId);
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata â€” wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the QuickReplyAction wire constant")
        void actionName() {
            assertEquals(QuickReplyAction.ACTION_NAME, handler.actionName());
            assertEquals("quick_reply", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(QuickReplyAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared QuickReplyAction version")
        void version() {
            assertEquals(QuickReplyAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” SET upsert (happy path)")
    class ApplySetUpsert {
        @Test
        @DisplayName("a non-deleted action with shortcut + message upserts the quick reply")
        void upsertsNewEntry() {
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/hi")
                    .message("Hi there!")
                    .keywords(List.of("greet"))
                    .count(5)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("qr-1", action, SyncdOperation.SET, Instant.ofEpochSecond(1_700_000_000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var stored = store.settingsStore().findQuickReply("qr-1").orElseThrow();
            assertEquals("qr-1", stored.id());
            assertEquals("/hi", stored.shortcut());
            assertEquals("Hi there!", stored.message());
            assertEquals(List.of("greet"), stored.keywords());
        }

        @Test
        @DisplayName("a SET on an existing id replaces the prior entry")
        void replacesExistingEntry() {
            store.settingsStore().addQuickReply(new QuickReplyBuilder()
                    .id("qr-2")
                    .shortcut("/old")
                    .message("old body")
                    .keywords(List.of())
                    .count(0)
                    .build());
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/new")
                    .message("new body")
                    .keywords(List.of("a", "b"))
                    .count(2)
                    .build();

            handler.applyMutation(client,
                    buildMutation("qr-2", action, SyncdOperation.SET, Instant.now()));

            var stored = store.settingsStore().findQuickReply("qr-2").orElseThrow();
            assertEquals("/new", stored.shortcut());
            assertEquals("new body", stored.message());
            assertEquals(List.of("a", "b"), stored.keywords());
        }

        @Test
        @DisplayName("deleted=true removes the entry from the store")
        void deletedRemovesEntry() {
            store.settingsStore().addQuickReply(new QuickReplyBuilder()
                    .id("qr-3")
                    .shortcut("/x")
                    .message("body")
                    .keywords(List.of())
                    .count(0)
                    .build());
            var action = new QuickReplyActionBuilder()
                    .deleted(true)
                    .shortcut("")
                    .message("")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("qr-3", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.settingsStore().findQuickReply("qr-3").isEmpty(),
                    "deleted=true must remove the entry from the store");
        }
    }

    @Nested
    @DisplayName("applyMutation â€” orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("an upsert of an unknown id succeeds rather than producing an ORPHAN result")
        void unknownIdSucceeds() {
            // Per WAWebQuickRepliesSync.applyMutations, the handler does not validate that the
            // quick reply id exists prior to the upsert (the table call is createOrReplace).
            // SET for a brand-new id is the upsert path, not orphan.
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/u")
                    .message("body")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("unknown-id", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }

        @Test
        @DisplayName("a delete of an unknown id still succeeds â€” remove is idempotent")
        void deleteUnknownIdSucceeds() {
            var action = new QuickReplyActionBuilder()
                    .deleted(true)
                    .shortcut("")
                    .message("")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("never-existed", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a value carrying the wrong action returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of(handler.actionName(), "qr-x"));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a non-deleted action with a blank shortcut returns MALFORMED")
        void blankShortcutMalformed() {
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("")
                    .message("ok")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("qr-blank", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a non-deleted action with a blank message returns MALFORMED")
        void blankMessageMalformed() {
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/ok")
                    .message("")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("qr-blank-msg", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a missing index slot returns MALFORMED")
        void missingIndexSlot() {
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/hi")
                    .message("hi")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation(null, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an empty-string id at indexParts[1] returns MALFORMED")
        void emptyStringId() {
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/hi")
                    .message("hi")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED")
        void removeUnsupported() {
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/x")
                    .message("body")
                    .keywords(List.of())
                    .count(0)
                    .build();

            var result = handler.applyMutation(client,
                    buildMutation("qr-rm", action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts â€” default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote wins â€” APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutationAt(Instant.ofEpochSecond(1_000));
            var remote = mutationAt(Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("equal timestamps â€” APPLY_REMOTE_DROP_LOCAL (remote wins on tie)")
        void equalTimestampApplies() {
            var ts = Instant.ofEpochSecond(1_500);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(mutationAt(ts), mutationAt(ts)).state());
        }

        @Test
        @DisplayName("older remote â€” SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutationAt(Instant.ofEpochSecond(2_000));
            var remote = mutationAt(Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private DecryptedMutation.Trusted mutationAt(Instant ts) {
            var action = new QuickReplyActionBuilder()
                    .deleted(false)
                    .shortcut("/hi")
                    .message("hi")
                    .keywords(List.of())
                    .count(0)
                    .build();
            return buildMutation("qr-tie", action, SyncdOperation.SET, ts);
        }
    }

    @Nested
    @DisplayName("static builder â€” getQuickReplyAddOrEditMutation")
    class AddOrEditBuilder {
        @Test
        @DisplayName("produces a SET pending mutation whose payload mirrors the inputs")
        void carriesInputs() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var pending = factory.getQuickReplyAddOrEditMutation(
                    "qr-9", "/hello", "Hi", 3, List.of("k1", "k2"), ts);
            var inner = pending.mutation();

            assertEquals(SyncdOperation.SET, inner.operation());
            assertEquals(handler.version(), inner.actionVersion());
            assertEquals(ts, inner.timestamp());
            assertEquals(JSON.toJSONString(List.of(handler.actionName(), "qr-9")), inner.index());

            var roundtrip = inner.value().flatMap(sav -> sav.action()).filter(a -> a instanceof QuickReplyAction).map(a -> (QuickReplyAction) a).orElseThrow();
            assertEquals("/hello", roundtrip.shortcut().orElseThrow());
            assertEquals("Hi", roundtrip.message().orElseThrow());
            assertEquals(List.of("k1", "k2"), roundtrip.keywords());
            assertEquals(3, roundtrip.count().orElseThrow());
            assertTrue(!roundtrip.deleted(), "add/edit mutation must carry deleted=false");
        }
    }

    @Nested
    @DisplayName("static builder â€” getQuickReplyDeleteMutation")
    class DeleteBuilder {
        @Test
        @DisplayName("produces a SET pending mutation flagged as deleted")
        void carriesDeletedFlag() {
            var ts = Instant.ofEpochSecond(1_700_000_001L);
            var pending = factory.getQuickReplyDeleteMutation("qr-del", ts);
            var inner = pending.mutation();

            assertEquals(SyncdOperation.SET, inner.operation(),
                    "WA Web emits the deletion as a SET with deleted=true, not as REMOVE");
            assertEquals(ts, inner.timestamp());
            assertEquals(JSON.toJSONString(List.of(handler.actionName(), "qr-del")), inner.index());

            var roundtrip = inner.value().flatMap(sav -> sav.action()).filter(a -> a instanceof QuickReplyAction).map(a -> (QuickReplyAction) a).orElseThrow();
            assertTrue(roundtrip.deleted(), "delete mutation must carry deleted=true");
        }
    }

}
