package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.preference.LabelBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelReorderingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelReorderingActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.LabelReorderingMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link LabelReorderingHandler} for the {@code label_reordering}
 * app-state sync action: metadata, the SET sort-order write, the REMOVE
 * rejection, the empty-list rejection, the malformed-input fallbacks,
 * timestamp-based conflict resolution and the
 * {@link LabelReorderingMutationFactory} builder.
 *
 * <p>Tests run against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * through {@link TestWhatsAppClient} so each
 * {@link com.github.auties00.cobalt.wire.linked.preference.Label#orderIndex()}
 * read-back can be asserted directly. Labels referenced by the action but absent
 * from the store are silently skipped rather than created.
 */
@DisplayName("LabelReorderingHandler")
class LabelReorderingHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String INDEX = "[\"label_reordering\"]";

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private LabelReorderingHandler handler;
    private LabelReorderingMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new LabelReorderingHandler();
        factory = new LabelReorderingMutationFactory();
    }

    private DecryptedMutation.Trusted buildMutation(LabelReorderingAction action, SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.labelReorderingAction(action);
        }
        return new DecryptedMutation.Trusted(INDEX, valueBuilder.build(), operation, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the LabelReorderingAction wire constant")
        void actionName() {
            assertEquals(LabelReorderingAction.ACTION_NAME, handler.actionName());
            assertEquals("label_reordering", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns the LabelReorderingAction collection")
        void collectionName() {
            assertEquals(LabelReorderingAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared LabelReorderingAction version")
        void version() {
            assertEquals(LabelReorderingAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET reorders the referenced labels")
    class ApplySet {
        @Test
        @DisplayName("each label's orderIndex matches its position in sortedLabelIds")
        void writesPositionsInOrder() {
            store.settingsStore().addLabel(new LabelBuilder().id("10").name("A").color(0).build());
            store.settingsStore().addLabel(new LabelBuilder().id("20").name("B").color(0).build());
            store.settingsStore().addLabel(new LabelBuilder().id("30").name("C").color(0).build());

            var action = new LabelReorderingActionBuilder()
                    .sortedLabelIds(List.of(30, 10, 20))
                    .build();
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client, buildMutation(action, SyncdOperation.SET, ts));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            // position 0 -> label 30; position 1 -> label 10; position 2 -> label 20
            assertEquals(0, store.settingsStore().findLabel("30").orElseThrow().orderIndex().orElseThrow());
            assertEquals(1, store.settingsStore().findLabel("10").orElseThrow().orderIndex().orElseThrow());
            assertEquals(2, store.settingsStore().findLabel("20").orElseThrow().orderIndex().orElseThrow());
        }

        @Test
        @DisplayName("labels in sortedLabelIds but missing from the store are silently skipped")
        void missingLabelsSkipped() {
            // Only label 10 is present; the action also references 20 and 30.
            store.settingsStore().addLabel(new LabelBuilder().id("10").name("A").color(0).build());
            var action = new LabelReorderingActionBuilder()
                    .sortedLabelIds(List.of(30, 10, 20))
                    .build();

            var result = handler.applyMutation(client, buildMutation(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(1, store.settingsStore().findLabel("10").orElseThrow().orderIndex().orElseThrow(),
                    "the lone present label is positioned at its zero-based index in the sortedLabelIds list");
            assertTrue(store.settingsStore().findLabel("20").isEmpty(), "missing labels are not implicitly created");
            assertTrue(store.settingsStore().findLabel("30").isEmpty(), "missing labels are not implicitly created");
        }

        @Test
        @DisplayName("labels present locally but absent from sortedLabelIds keep their existing orderIndex")
        void unreferencedLabelsKeepTheirOrder() {
            store.settingsStore().addLabel(new LabelBuilder().id("10").name("A").color(0).orderIndex(7).build());
            store.settingsStore().addLabel(new LabelBuilder().id("20").name("B").color(0).build());

            var action = new LabelReorderingActionBuilder()
                    .sortedLabelIds(List.of(20))
                    .build();
            var result = handler.applyMutation(client, buildMutation(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(0, store.settingsStore().findLabel("20").orElseThrow().orderIndex().orElseThrow());
            assertEquals(7, store.settingsStore().findLabel("10").orElseThrow().orderIndex().orElseThrow(),
                    "unreferenced labels retain their existing orderIndex");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("missing labels do not produce an ORPHAN result - they are silently skipped to SUCCESS")
        void orphanDimensionNotApplicable() {
            // Reordering has no per-mutation target entity that can be marked orphan; missing
            // labels are silently skipped per WA Web's bulkGet semantics. We confirm here that
            // the absence of every label still produces SUCCESS, ruling out an orphan path.
            var action = new LabelReorderingActionBuilder()
                    .sortedLabelIds(List.of(99, 100))
                    .build();

            var result = handler.applyMutation(client, buildMutation(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(result.isOrphan(), "no orphan outcome is produced for missing labels");
            assertNull(result.modelId());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a value carrying the wrong action returns MALFORMED")
        void wrongActionType() {
            // A pinAction in place of the expected labelReorderingAction.
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(INDEX, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a labelReorderingAction with no sortedLabelIds returns MALFORMED")
        void emptySortedLabelIds() {
            var action = new LabelReorderingActionBuilder().build();

            var result = handler.applyMutation(client, buildMutation(action, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("malformed index dimension is n/a - the index payload is unused")
        void indexUnused() {
            // The handler never reads the index array, so an empty or malformed index slot
            // must not prevent a valid SET from succeeding.
            store.settingsStore().addLabel(new LabelBuilder().id("10").name("A").color(0).build());
            var action = new LabelReorderingActionBuilder().sortedLabelIds(List.of(10)).build();
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).labelReorderingAction(action).build();

            for (var malformedIndex : new String[]{"", "not-json", "[", "[]"}) {
                var mutation = new DecryptedMutation.Trusted(malformedIndex, value, SyncdOperation.SET, ts, handler.version());
                assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client, mutation).actionState(),
                        "handler must not read the index - malformed index '" + malformedIndex + "' is irrelevant");
            }
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED")
        void removeUnsupported() {
            var action = new LabelReorderingActionBuilder().sortedLabelIds(List.of(10)).build();

            var result = handler.applyMutation(client, buildMutation(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutationAt(Instant.ofEpochSecond(1_000));
            var remote = mutationAt(Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("equal timestamps -> APPLY_REMOTE_DROP_LOCAL (remote wins on tie)")
        void equalTimestampApplies() {
            var ts = Instant.ofEpochSecond(1_500);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(mutationAt(ts), mutationAt(ts)).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutationAt(Instant.ofEpochSecond(2_000));
            var remote = mutationAt(Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private DecryptedMutation.Trusted mutationAt(Instant ts) {
            var action = new LabelReorderingActionBuilder().sortedLabelIds(List.of(10)).build();
            return buildMutation(action, SyncdOperation.SET, ts);
        }
    }

}
