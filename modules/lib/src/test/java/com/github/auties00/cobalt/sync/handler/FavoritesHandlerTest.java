package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionFavoriteBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.device.TimeFormatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.FavoritesMutationFactory;
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
 * Covers the {@link FavoritesHandler} for the {@code favorites} app-state sync
 * action: metadata, the SET happy path, the {@code applyMutationBatch}
 * latest-by-timestamp deduplication, the malformed-value branch, the
 * {@link FavoritesMutationFactory} builder and timestamp-based conflict
 * resolution.
 *
 * <p>Tests run against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * through {@link TestWhatsAppClient} so the
 * {@link LinkedWhatsAppChatStore#favoriteChats()}
 * read-back can be asserted directly without round-tripping through the
 * chat-table cache.
 */
@DisplayName("FavoritesHandler")
class FavoritesHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid FAVORITE_A = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid FAVORITE_B = Jid.of("12025550101@s.whatsapp.net");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted favoritesMutation(List<Jid> jids, SyncdOperation op, Instant ts) {
        var entries = jids.stream()
                .map(jid -> new FavoritesActionFavoriteBuilder().id(jid.toString()).build())
                .toList();
        var action = new FavoritesActionBuilder().favorites(entries).build();
        var value = new SyncActionValueBuilder().timestamp(ts).favoritesAction(action).build();
        return new DecryptedMutation.Trusted("[\"favorites\"]", value, op, ts, 1);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'favorites'")
        void actionName() {
            assertEquals(FavoritesAction.ACTION_NAME, new FavoritesHandler().actionName());
            assertEquals("favorites", new FavoritesHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, new FavoritesHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (1)")
        void version() {
            assertEquals(FavoritesAction.ACTION_VERSION, new FavoritesHandler().version());
            assertEquals(1, new FavoritesHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET replaces the store's favorites list")
    class SetHappy {
        @Test
        @DisplayName("a SET mutation with two entries populates the store list verbatim")
        void replacesFavorites() {
            assertEquals(0, client.store().chatStore().favoriteChats().size(), "store starts empty");

            var result = new FavoritesHandler().applyMutation(
                    client, favoritesMutation(List.of(FAVORITE_A, FAVORITE_B), SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(List.of(FAVORITE_A, FAVORITE_B), client.store().chatStore().favoriteChats(),
                    "the store favorites must match the mutation entries in order");
        }

        @Test
        @DisplayName("an empty favorites list is still a valid SUCCESS - replaces with empty")
        void emptyFavoritesReplaces() {
            client.store().chatStore().setFavoriteChats(List.of(FAVORITE_A));

            var result = new FavoritesHandler().applyMutation(
                    client, favoritesMutation(List.of(), SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(0, client.store().chatStore().favoriteChats().size(),
                    "empty favorites list clears the store");
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed paths")
    class Malformed {
        @Test
        @DisplayName("non-favorites action value is MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .timeFormatAction(new TimeFormatActionBuilder().isTwentyFourHourFormatEnabled(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"favorites\"]", wrongValue, SyncdOperation.SET, Instant.now(), 1);

            var result = new FavoritesHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("REMOVE operation maps to malformed (handler treats non-SET as malformed)")
        void removeIsMalformed() {
            // Per the handler source: applyMutation returns malformedActionValue for non-SET
            // operations rather than UNSUPPORTED. This pins down that specific divergence.
            var result = new FavoritesHandler().applyMutation(
                    client, favoritesMutation(List.of(FAVORITE_A), SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index - n/a (singleton index)")
    class MalformedIndex {
        @Test
        @DisplayName("the singleton 'favorites' index has no second element to validate")
        void singletonIndexNotValidated() {
            // The handler does not read indexParts[1]; the singleton index ["favorites"]
            // is sufficient. Document the n/a outcome.
            var result = new FavoritesHandler().applyMutation(
                    client, favoritesMutation(List.of(FAVORITE_A), SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - keeps only the latest by timestamp")
    class BatchDedup {
        @Test
        @DisplayName("two SET mutations: only the later timestamp's favorites land in the store")
        void latestWins() {
            var earlier = favoritesMutation(List.of(FAVORITE_A), SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var later   = favoritesMutation(List.of(FAVORITE_B), SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var results = new FavoritesHandler().applyMutationBatch(client, List.of(earlier, later));

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(r -> r.actionState() == SyncActionState.SUCCESS),
                    "both well-formed mutations are tagged success");
            assertEquals(List.of(FAVORITE_B), client.store().chatStore().favoriteChats(),
                    "only the latest mutation's favorites land in the store");
        }

        @Test
        @DisplayName("a batch with a malformed mutation still applies the latest valid one")
        void malformedDoesNotBlockLatest() {
            var bad = new DecryptedMutation.Trusted(
                    "[\"favorites\"]",
                    new SyncActionValueBuilder().timestamp(Instant.now())
                            .timeFormatAction(new TimeFormatActionBuilder().build())
                            .build(),
                    SyncdOperation.SET, Instant.ofEpochSecond(1700000000L), 1);
            var ok = favoritesMutation(List.of(FAVORITE_A), SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var results = new FavoritesHandler().applyMutationBatch(client, List.of(bad, ok));

            assertEquals(SyncActionState.MALFORMED, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertEquals(List.of(FAVORITE_A), client.store().chatStore().favoriteChats());
        }

        @Test
        @DisplayName("an empty batch leaves the store untouched and returns an empty list")
        void emptyBatch() {
            client.store().chatStore().setFavoriteChats(List.of(FAVORITE_A));

            var results = new FavoritesHandler().applyMutationBatch(client, List.of());

            assertEquals(0, results.size());
            assertEquals(List.of(FAVORITE_A), client.store().chatStore().favoriteChats(),
                    "empty batch must be a no-op on the store");
        }
    }

    @Nested
    @DisplayName("getFavoritesMutation - builder helper")
    class Builder {
        @Test
        @DisplayName("emits a SET pending mutation with the favorites encoded as Favorite entries")
        void buildsPending() {
            var ts = Instant.ofEpochSecond(1700000000L);

            var pending = new FavoritesMutationFactory().getFavoritesMutation(List.of(FAVORITE_A, FAVORITE_B), ts);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            assertEquals(ts, pending.mutation().timestamp());
            assertEquals("[\"favorites\"]", pending.mutation().index());
            var action = (FavoritesAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertEquals(2, action.favorites().size());
            assertEquals(FAVORITE_A.toString(), action.favorites().get(0).id().orElseThrow());
            assertEquals(FAVORITE_B.toString(), action.favorites().get(1).id().orElseThrow());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = favoritesMutation(List.of(FAVORITE_A), SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = favoritesMutation(List.of(FAVORITE_B), SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new FavoritesHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

}
