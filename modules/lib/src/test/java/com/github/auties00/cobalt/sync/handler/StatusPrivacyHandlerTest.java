package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacyMode;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StatusPrivacyAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StatusPrivacyActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link StatusPrivacyHandler}: applying an incoming status-privacy
 * mutation and asserting the persisted status-privacy setting,
 * across the per-mode dispatch and the user-JID filter applied to
 * the allow/deny lists. Allow/deny-list tests pass a peer-A, peer-B, and
 * group JID so they can observe the group JID being dropped by the
 * user-only filter.
 */
@DisplayName("StatusPrivacyHandler")
class StatusPrivacyHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER_A = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid PEER_B = Jid.of("12025550101@s.whatsapp.net");
    private static final Jid GROUP = Jid.of("99001112224@g.us");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    // A null mode or userJids omits that field to drive the malformed-value branches.
    private static DecryptedMutation.Trusted statusMutation(StatusPrivacyAction.StatusDistributionMode mode,
                                                            List<Jid> userJids,
                                                            SyncdOperation op,
                                                            Instant ts) {
        var builder = new StatusPrivacyActionBuilder();
        if (mode != null) builder.mode(mode);
        if (userJids != null) builder.userJid(userJids);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .statusPrivacy(builder.build())
                .build();
        return new DecryptedMutation.Trusted("[\"status_privacy\"]", value, op, ts, StatusPrivacyAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the StatusPrivacyAction wire constant")
        void actionName() {
            assertEquals(StatusPrivacyAction.ACTION_NAME, new StatusPrivacyHandler().actionName());
            assertEquals("status_privacy", new StatusPrivacyHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_HIGH")
        void collectionName() {
            assertEquals(StatusPrivacyAction.COLLECTION_NAME, new StatusPrivacyHandler().collectionName());
            assertEquals(SyncPatchType.REGULAR_HIGH, new StatusPrivacyHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared StatusPrivacyAction version (7)")
        void version() {
            assertEquals(StatusPrivacyAction.ACTION_VERSION, new StatusPrivacyHandler().version());
            assertEquals(7, new StatusPrivacyHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("CONTACTS mode persists a status-privacy setting with CONTACTS mode and empty jids")
        void contactsMode() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new StatusPrivacyHandler().applyMutation(client,
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.CONTACTS, List.of(), SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var setting = client.store().settingsStore().statusPrivacy().orElseThrow();
            assertEquals(StatusPrivacyMode.CONTACTS, setting.mode().orElseThrow());
            assertTrue(setting.jids().isEmpty());
        }

        @Test
        @DisplayName("ALLOW_LIST mode filters userJids and persists WHITELIST with the filtered list")
        void allowListMode() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new StatusPrivacyHandler().applyMutation(client,
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.ALLOW_LIST,
                            List.of(PEER_A, GROUP, PEER_B), SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var setting = client.store().settingsStore().statusPrivacy().orElseThrow();
            assertEquals(StatusPrivacyMode.WHITELIST, setting.mode().orElseThrow());
            // Group JID is filtered out by isUser() predicate; only PEER_A and PEER_B survive
            assertEquals(List.of(PEER_A, PEER_B), setting.jids(),
                    "WAWebWid.isUser drops non-user-server JIDs from the allow list");
        }

        @Test
        @DisplayName("DENY_LIST mode filters userJids and persists CONTACTS_EXCEPT with the filtered list")
        void denyListMode() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new StatusPrivacyHandler().applyMutation(client,
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.DENY_LIST,
                            List.of(PEER_A, PEER_B), SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var setting = client.store().settingsStore().statusPrivacy().orElseThrow();
            assertEquals(StatusPrivacyMode.CONTACTS_EXCEPT, setting.mode().orElseThrow());
            assertEquals(List.of(PEER_A, PEER_B), setting.jids());
        }

        @Test
        @DisplayName("CLOSE_FRIENDS mode is a no-op write but returns SUCCESS")
        void closeFriendsIsNoOp() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new StatusPrivacyHandler().applyMutation(client,
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.CLOSE_FRIENDS, List.of(), SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "WA Web's switch breaks out of the CLOSE_FRIENDS branch without writing any IDB entry");
            assertTrue(client.store().settingsStore().statusPrivacy().isEmpty());
        }

        @Test
        @DisplayName("CUSTOM_LIST mode is a no-op write but returns SUCCESS")
        void customListIsNoOp() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new StatusPrivacyHandler().applyMutation(client,
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.CUSTOM_LIST, List.of(), SyncdOperation.SET, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().settingsStore().statusPrivacy().isEmpty());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("status privacy is a global setting; no per-entity orphan path")
        void noOrphan() {
            var result = new StatusPrivacyHandler().applyMutation(client,
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.CONTACTS, List.of(), SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "WAWebStatusPrivacySettingSync has no per-entity target");
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SyncActionValue carrying a different action returns MALFORMED")
        void wrongActionShapeIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("[\"status_privacy\"]", value, SyncdOperation.SET, ts, 7);
            var result = new StatusPrivacyHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a StatusPrivacyAction with no mode returns MALFORMED")
        void nullModeIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new StatusPrivacyHandler().applyMutation(client, statusMutation(null, List.of(), SyncdOperation.SET, ts));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("the status-privacy handler ignores the index shape (global setting)")
        void indexShapeIgnored() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .statusPrivacy(new StatusPrivacyActionBuilder()
                            .mode(StatusPrivacyAction.StatusDistributionMode.CONTACTS)
                            .userJid(List.of())
                            .build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("", value, SyncdOperation.SET, ts, 7);
            var result = new StatusPrivacyHandler().applyMutation(client, mutation);
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
            var result = new StatusPrivacyHandler().applyMutation(client,
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.CONTACTS, List.of(), SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - exactly one mutation required")
    class ApplyBatchOverride {
        @Test
        @DisplayName("an empty batch yields an empty result list")
        void emptyBatchEmptyResult() {
            assertTrue(new StatusPrivacyHandler().applyMutationBatch(client, List.of()).isEmpty(),
                    "WA Web's `t.length !== 1` branch produces a same-length result list, so empty in -> empty out");
        }

        @Test
        @DisplayName("a multi-mutation batch reports every entry as MALFORMED")
        void multiBatchMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var results = new StatusPrivacyHandler().applyMutationBatch(client, List.of(
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.CONTACTS, List.of(), SyncdOperation.SET, ts),
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.ALLOW_LIST, List.of(), SyncdOperation.SET, ts.plusSeconds(1))
            ));
            assertEquals(2, results.size());
            for (var r : results) {
                assertEquals(SyncActionState.MALFORMED, r.actionState(),
                        "WA Web requires exactly one status-privacy mutation per batch");
            }
        }

        @Test
        @DisplayName("a single-mutation batch applies the mutation")
        void singleMutationBatchApplies() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var results = new StatusPrivacyHandler().applyMutationBatch(client, List.of(
                    statusMutation(StatusPrivacyAction.StatusDistributionMode.CONTACTS, List.of(), SyncdOperation.SET, ts)
            ));
            assertEquals(1, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemote() {
            var local = statusMutation(StatusPrivacyAction.StatusDistributionMode.CONTACTS, List.of(), SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = statusMutation(StatusPrivacyAction.StatusDistributionMode.ALLOW_LIST, List.of(PEER_A), SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    new StatusPrivacyHandler().resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemote() {
            var local = statusMutation(StatusPrivacyAction.StatusDistributionMode.CONTACTS, List.of(), SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = statusMutation(StatusPrivacyAction.StatusDistributionMode.ALLOW_LIST, List.of(PEER_A), SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    new StatusPrivacyHandler().resolveConflicts(local, remote).state());
        }
    }

}
