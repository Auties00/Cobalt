package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingDisableLinkPreviewsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingDisableLinkPreviewsActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link DisableLinkPreviewsHandler} for the
 * {@code setting_disableLinkPreviews} app-state sync action: metadata, the SET
 * happy path, the malformed-value and malformed-index branches, the REMOVE
 * rejection, timestamp-based conflict resolution and the batch path that folds
 * the latest valid value before writing once.
 *
 * <p>Each test runs against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * built through {@link TestWhatsAppClient}, so the
 * {@link LinkedWhatsAppSettingsStore#disableLinkPreviews()}
 * read-back can be asserted directly.
 */
@DisplayName("DisableLinkPreviewsHandler")
class DisableLinkPreviewsHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted mutation(boolean disabled, SyncdOperation op, Instant ts) {
        var action = new PrivacySettingDisableLinkPreviewsActionBuilder()
                .isPreviewsDisabled(disabled)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .privacySettingDisableLinkPreviewsAction(action)
                .build();
        return new DecryptedMutation.Trusted("[\"setting_disableLinkPreviews\"]", value, op, ts,
                PrivacySettingDisableLinkPreviewsAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the PrivacySettingDisableLinkPreviewsAction wire constant")
        void actionName() {
            assertEquals(PrivacySettingDisableLinkPreviewsAction.ACTION_NAME, new DisableLinkPreviewsHandler().actionName());
            assertEquals("setting_disableLinkPreviews", new DisableLinkPreviewsHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(PrivacySettingDisableLinkPreviewsAction.COLLECTION_NAME, new DisableLinkPreviewsHandler().collectionName());
            assertEquals(SyncPatchType.REGULAR, new DisableLinkPreviewsHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (8)")
        void version() {
            assertEquals(PrivacySettingDisableLinkPreviewsAction.ACTION_VERSION, new DisableLinkPreviewsHandler().version());
            assertEquals(8, new DisableLinkPreviewsHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("SET true writes the flag to the store and returns SUCCESS")
        void setsTrue() {
            assertFalse(client.store().settingsStore().disableLinkPreviews(), "precondition: starts false");
            var result = new DisableLinkPreviewsHandler().applyMutation(client,
                    mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(1_700_000_000L)));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().settingsStore().disableLinkPreviews());
        }

        @Test
        @DisplayName("SET false flips the flag back to false")
        void setsFalse() {
            client.store().settingsStore().setDisableLinkPreviews(true);
            var result = new DisableLinkPreviewsHandler().applyMutation(client,
                    mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(1_700_000_000L)));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(client.store().settingsStore().disableLinkPreviews());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("link-preview setting is global; no per-entity orphan path")
        void noOrphan() {
            var result = new DisableLinkPreviewsHandler().applyMutation(client,
                    mutation(true, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SyncActionValue carrying a different action returns MALFORMED")
        void wrongActionIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("[\"setting_disableLinkPreviews\"]", value, SyncdOperation.SET, ts, 8);
            var result = new DisableLinkPreviewsHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("the handler ignores the index shape (global setting)")
        void indexShapeIgnored() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .privacySettingDisableLinkPreviewsAction(
                            new PrivacySettingDisableLinkPreviewsActionBuilder().isPreviewsDisabled(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("", value, SyncdOperation.SET, ts, 8);
            var result = new DisableLinkPreviewsHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "the handler does not parse the index; the setting is keyed off the action only");
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE returns UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE is unsupported per the WA Web fall-through")
        void removeIsUnsupported() {
            var result = new DisableLinkPreviewsHandler().applyMutation(client,
                    mutation(true, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    new DisableLinkPreviewsHandler().resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    new DisableLinkPreviewsHandler().resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - only the last valid SET writes to the store")
    class ApplyBatchOverride {
        @Test
        @DisplayName("an empty batch yields an empty result list")
        void emptyBatchEmptyResult() {
            assertTrue(new DisableLinkPreviewsHandler().applyMutationBatch(client, List.of()).isEmpty());
        }

        @Test
        @DisplayName("only the last SET's value lands in the store; earlier SETs return SUCCESS individually")
        void lastSetWins() {
            var results = new DisableLinkPreviewsHandler().applyMutationBatch(client, List.of(
                    mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(1_000)),
                    mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(2_000))
            ));
            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertFalse(client.store().settingsStore().disableLinkPreviews(),
                    "WAWebDisableLinkPreviewsSync.applyMutations accumulates the last valid value and writes once");
        }

        @Test
        @DisplayName("REMOVE within a batch reports UNSUPPORTED but does not block other SETs")
        void removeInBatchYieldsUnsupported() {
            var results = new DisableLinkPreviewsHandler().applyMutationBatch(client, List.of(
                    mutation(true, SyncdOperation.REMOVE, Instant.ofEpochSecond(1_000)),
                    mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(2_000))
            ));
            assertEquals(SyncActionState.UNSUPPORTED, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertTrue(client.store().settingsStore().disableLinkPreviews());
        }
    }

}
