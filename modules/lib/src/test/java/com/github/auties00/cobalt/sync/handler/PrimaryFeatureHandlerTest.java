package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.PrimaryFeatureAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.PrimaryFeatureActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link PrimaryFeatureHandler}: a {@link SyncdOperation#SET} carrying a non-empty
 * or empty flags list persists the flags via
 * {@link LinkedWhatsAppSyncStore#setPrimaryFeatures(List)}; a
 * wrong-typed value surfaces as {@link SyncActionState#MALFORMED};
 * {@link SyncdOperation#REMOVE} surfaces as {@link SyncActionState#UNSUPPORTED};
 * {@link PrimaryFeatureHandler#applyMutationBatch} writes only the latest-by-timestamp
 * mutation's flags to the store and treats an empty batch as a no-op; the default
 * conflict resolution chooses the later timestamp.
 */
@DisplayName("PrimaryFeatureHandler")
class PrimaryFeatureHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted primaryFeatureMutation(List<String> flags, SyncdOperation op, Instant ts) {
        var action = new PrimaryFeatureActionBuilder().flags(flags).build();
        var value = new SyncActionValueBuilder().timestamp(ts).primaryFeatureAction(action).build();
        return new DecryptedMutation.Trusted("[\"primary_feature\"]", value, op, ts, 7);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'primary_feature'")
        void actionName() {
            assertEquals(PrimaryFeatureAction.ACTION_NAME, new PrimaryFeatureHandler().actionName());
            assertEquals("primary_feature", new PrimaryFeatureHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR, new PrimaryFeatureHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(PrimaryFeatureAction.ACTION_VERSION, new PrimaryFeatureHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET writes flags into the store")
    class SetHappy {
        @Test
        @DisplayName("non-empty flag list lands in the store")
        void writesFlags() {
            var result = new PrimaryFeatureHandler().applyMutation(
                    client, primaryFeatureMutation(List.of("feature_a", "feature_b"), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(List.of("feature_a", "feature_b"), client.store().syncStore().primaryFeatures());
        }

        @Test
        @DisplayName("empty flag list is still SUCCESS - WA Web only rejects null")
        void emptyFlags() {
            var result = new PrimaryFeatureHandler().applyMutation(
                    client, primaryFeatureMutation(List.of(), SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(0, client.store().syncStore().primaryFeatures().size());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value")
    class Malformed {
        @Test
        @DisplayName("non-primary-feature action in value yields MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"primary_feature\"]", wrongValue, SyncdOperation.SET, Instant.now(), 7);

            var result = new PrimaryFeatureHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index - n/a (singleton index)")
    class MalformedIndexNa {
        @Test
        @DisplayName("the handler does not inspect indexParts[1]")
        void singletonIndex() {
            var result = new PrimaryFeatureHandler().applyMutation(
                    client, primaryFeatureMutation(List.of("x"), SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED before any store write")
        void removeIsUnsupported() {
            client.store().syncStore().setPrimaryFeatures(List.of("seed"));

            var result = new PrimaryFeatureHandler().applyMutation(
                    client, primaryFeatureMutation(List.of("ignored"), SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertEquals(List.of("seed"), client.store().syncStore().primaryFeatures(),
                    "REMOVE must not overwrite the store");
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - latest by timestamp wins")
    class BatchDedup {
        @Test
        @DisplayName("among two SET mutations, the later timestamp's flags land in the store")
        void latestWins() {
            var earlier = primaryFeatureMutation(List.of("v1"), SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var later   = primaryFeatureMutation(List.of("v2"), SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var results = new PrimaryFeatureHandler().applyMutationBatch(client, List.of(earlier, later));

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(r -> r.actionState() == SyncActionState.SUCCESS));
            assertEquals(List.of("v2"), client.store().syncStore().primaryFeatures(),
                    "only the latest mutation's flags persist");
        }

        @Test
        @DisplayName("a malformed + a valid mutation: malformed reported, valid still applied")
        void malformedAndValid() {
            var bad = new DecryptedMutation.Trusted(
                    "[\"primary_feature\"]",
                    new SyncActionValueBuilder().timestamp(Instant.now())
                            .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                            .build(),
                    SyncdOperation.SET, Instant.ofEpochSecond(1700000000L), 7);
            var ok = primaryFeatureMutation(List.of("v"), SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var results = new PrimaryFeatureHandler().applyMutationBatch(client, List.of(bad, ok));

            assertEquals(SyncActionState.MALFORMED, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS,   results.get(1).actionState());
            assertEquals(List.of("v"), client.store().syncStore().primaryFeatures());
        }

        @Test
        @DisplayName("an empty batch is a no-op")
        void emptyBatch() {
            client.store().syncStore().setPrimaryFeatures(List.of("seed"));

            var results = new PrimaryFeatureHandler().applyMutationBatch(client, List.of());

            assertEquals(0, results.size());
            assertEquals(List.of("seed"), client.store().syncStore().primaryFeatures());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = primaryFeatureMutation(List.of("a"), SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = primaryFeatureMutation(List.of("b"), SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new PrimaryFeatureHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

}
