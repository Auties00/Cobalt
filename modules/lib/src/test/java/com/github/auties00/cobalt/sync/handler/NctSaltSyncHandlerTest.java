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
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NctSaltSyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NctSaltSyncActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link NctSaltSyncHandler}: a {@link SyncdOperation#SET} with a non-{@code null} salt
 * writes the bytes via
 * {@link LinkedWhatsAppStore#setNotificationContentTokenSalt(byte[])},
 * {@link SyncdOperation#REMOVE} clears it, a SET with the wrong action type or with no salt field
 * surfaces as {@link SyncActionState#MALFORMED}, the default {@code resolveConflicts} chooses the
 * later timestamp, and the default batch path applies each mutation in order.
 *
 * <p>No public outgoing-mutation factory exists for this action, so each test drives the handler
 * directly through {@link NctSaltSyncHandler#applyMutation} with hand-built
 * {@link DecryptedMutation.Trusted} mutations.
 */
@DisplayName("NctSaltSyncHandler")
class NctSaltSyncHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted mutation(byte[] salt, SyncdOperation op, Instant ts) {
        var builder = new NctSaltSyncActionBuilder();
        if (salt != null) builder.salt(salt);
        var value = new SyncActionValueBuilder().timestamp(ts).nctSaltSyncAction(builder.build()).build();
        return new DecryptedMutation.Trusted("[\"nct_salt_sync\"]", value, op, ts, NctSaltSyncAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the NctSaltSyncAction wire constant")
        void actionName() {
            assertEquals(NctSaltSyncAction.ACTION_NAME, new NctSaltSyncHandler().actionName());
            assertEquals("nct_salt_sync", new NctSaltSyncHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_HIGH")
        void collectionName() {
            assertEquals(NctSaltSyncAction.COLLECTION_NAME, new NctSaltSyncHandler().collectionName());
            assertEquals(SyncPatchType.REGULAR_HIGH, new NctSaltSyncHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (1)")
        void version() {
            assertEquals(NctSaltSyncAction.ACTION_VERSION, new NctSaltSyncHandler().version());
            assertEquals(1, new NctSaltSyncHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with a non-null salt writes the bytes into the store and returns SUCCESS")
        void setsSalt() {
            assertTrue(client.store().accountStore().notificationContentTokenSalt().isEmpty(), "precondition: no salt stored");
            var salt = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
            var result = new NctSaltSyncHandler().applyMutation(client,
                    mutation(salt, SyncdOperation.SET, Instant.ofEpochSecond(1_700_000_000L)));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertArrayEquals(salt, client.store().accountStore().notificationContentTokenSalt().orElseThrow(),
                    "WAWebNctSaltSync persists the salt under BACKEND_ONLY_KEYS.NCT_SALT");
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE clears the stored salt")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE wipes the stored salt and returns SUCCESS")
        void removeClearsSalt() {
            client.store().accountStore().setNotificationContentTokenSalt(new byte[]{9, 9, 9});
            var result = new NctSaltSyncHandler().applyMutation(client,
                    mutation(null, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().accountStore().notificationContentTokenSalt().isEmpty(),
                    "WA Web: yield userPrefsIdb.remove(NCT_SALT); Cobalt nulls the store field");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("NCT salt is a global setting; no per-entity orphan path")
        void noOrphan() {
            var result = new NctSaltSyncHandler().applyMutation(client,
                    mutation(new byte[]{1, 2}, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SET mutation whose value is not an NctSaltSyncAction returns MALFORMED (via malformedActionIndex path)")
        void wrongActionShape() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("[\"nct_salt_sync\"]", value, SyncdOperation.SET, ts, 1);
            var result = new NctSaltSyncHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState(),
                    "WA Web: a missing salt causes the malformedActionIndex branch which Cobalt maps to MALFORMED");
        }

        @Test
        @DisplayName("a SET mutation with no salt field returns MALFORMED")
        void nullSaltIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = new NctSaltSyncHandler().applyMutation(client, mutation(null, SyncdOperation.SET, ts));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("the NCT-salt handler ignores the index shape (global setting)")
        void indexShapeIgnored() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .nctSaltSyncAction(new NctSaltSyncActionBuilder().salt(new byte[]{1}).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("", value, SyncdOperation.SET, ts, 1);
            var result = new NctSaltSyncHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - other operations are UNSUPPORTED")
    class OtherOperations {
        @Test
        @DisplayName("operations that are neither SET nor REMOVE cannot be expressed (enum has only SET and REMOVE today); confirmed via SET/REMOVE coverage")
        void onlyKnownOperations() {
            // SyncdOperation has only SET and REMOVE; both are exercised above. This block
            // documents that the WA-Web "neither set nor remove" branch is unreachable in Cobalt.
            assertTrue(SyncdOperation.values().length == 2);
        }
    }

    @Nested
    @DisplayName("resolveConflicts - inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutation(new byte[]{1}, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = mutation(new byte[]{2}, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    new NctSaltSyncHandler().resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutation(new byte[]{1}, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = mutation(new byte[]{2}, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    new NctSaltSyncHandler().resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - inherits default sequential apply")
    class ApplyBatch {
        @Test
        @DisplayName("default batch path applies each mutation in order")
        void sequentialApply() {
            var saltA = new byte[]{1};
            var saltB = new byte[]{2};
            var results = new NctSaltSyncHandler().applyMutationBatch(client, List.of(
                    mutation(saltA, SyncdOperation.SET, Instant.ofEpochSecond(1_000)),
                    mutation(saltB, SyncdOperation.SET, Instant.ofEpochSecond(2_000))
            ));
            assertEquals(2, results.size());
            for (var r : results) {
                assertEquals(SyncActionState.SUCCESS, r.actionState());
            }
            assertArrayEquals(saltB, client.store().accountStore().notificationContentTokenSalt().orElseThrow());
        }
    }

}
