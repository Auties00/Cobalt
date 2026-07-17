package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.AndroidUnsupportedActions;
import com.github.auties00.cobalt.wire.linked.sync.action.device.AndroidUnsupportedActionsBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
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
 * Tests for {@link AndroidUnsupportedActionsHandler} - Cobalt's adapter for
 * {@code WAWebAndroidUnsupportedActionsSync}.
 *
 * <p>The handler toggles {@code primaryAllowsAllMutations} when the incoming
 * {@code allowed} flag is {@code true}; the flag is sticky (one-way set).
 */
@DisplayName("AndroidUnsupportedActionsHandler")
class AndroidUnsupportedActionsHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted androidMutation(Boolean allowed, SyncdOperation op, Instant ts) {
        var action = new AndroidUnsupportedActionsBuilder().allowed(allowed).build();
        var value = new SyncActionValueBuilder().timestamp(ts).androidUnsupportedActions(action).build();
        return new DecryptedMutation.Trusted("[\"android_unsupported_actions\"]", value, op, ts, 4);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'android_unsupported_actions'")
        void actionName() {
            assertEquals(AndroidUnsupportedActions.ACTION_NAME, new AndroidUnsupportedActionsHandler().actionName());
            assertEquals("android_unsupported_actions", new AndroidUnsupportedActionsHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new AndroidUnsupportedActionsHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (4)")
        void version() {
            assertEquals(AndroidUnsupportedActions.ACTION_VERSION, new AndroidUnsupportedActionsHandler().version());
            assertEquals(4, new AndroidUnsupportedActionsHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET")
    class SetHappy {
        @Test
        @DisplayName("allowed=true sets primaryAllowsAllMutations to true")
        void enables() {
            assertFalse(client.store().syncStore().primaryAllowsAllMutations(), "default is false");

            var result = new AndroidUnsupportedActionsHandler().applyMutation(
                    client, androidMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().syncStore().primaryAllowsAllMutations());
        }

        @Test
        @DisplayName("allowed=false does NOT clear the flag (one-way set)")
        void doesNotClear() {
            client.store().syncStore().setPrimaryAllowsAllMutations(true);

            var result = new AndroidUnsupportedActionsHandler().applyMutation(
                    client, androidMutation(Boolean.FALSE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().syncStore().primaryAllowsAllMutations(),
                    "the handler intentionally never resets the flag to false");
        }

        @Test
        @DisplayName("re-applying allowed=true on a set flag is a SUCCESS no-op")
        void idempotent() {
            client.store().syncStore().setPrimaryAllowsAllMutations(true);

            var result = new AndroidUnsupportedActionsHandler().applyMutation(
                    client, androidMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().syncStore().primaryAllowsAllMutations());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value")
    class Malformed {
        @Test
        @DisplayName("non-AndroidUnsupportedActions value yields MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"android_unsupported_actions\"]", wrongValue, SyncdOperation.SET, Instant.now(), 4);

            var result = new AndroidUnsupportedActionsHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
            assertFalse(client.store().syncStore().primaryAllowsAllMutations(),
                    "malformed mutation must not touch the store");
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index - n/a (singleton index)")
    class MalformedIndexNa {
        @Test
        @DisplayName("the handler does not inspect indexParts[1]")
        void singletonIndex() {
            var result = new AndroidUnsupportedActionsHandler().applyMutation(
                    client, androidMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED before any store write")
        void removeIsUnsupported() {
            var result = new AndroidUnsupportedActionsHandler().applyMutation(
                    client, androidMutation(Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertFalse(client.store().syncStore().primaryAllowsAllMutations());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = androidMutation(Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = androidMutation(Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new AndroidUnsupportedActionsHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - n/a, default implementation is per-item")
    class BatchNa {
        @Test
        @DisplayName("the default applyMutationBatch dispatches per-mutation")
        void perItem() {
            var batch = List.of(
                    androidMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()),
                    androidMutation(Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            var results = new AndroidUnsupportedActionsHandler().applyMutationBatch(client, batch);

            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS,     results.get(0).actionState());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(1).actionState());
        }
    }

}
