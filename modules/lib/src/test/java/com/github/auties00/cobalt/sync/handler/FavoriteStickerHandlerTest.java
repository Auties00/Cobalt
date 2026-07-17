package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.preference.StickerBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StickerAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StickerActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.FavoriteStickerMutationFactory;
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
 * Covers the {@link FavoriteStickerHandler} for the {@code favoriteSticker}
 * app-state sync action: metadata, the SET happy path (feature-enabled add and
 * remove), the orphan branch when the {@code favorite_sticker} primary feature
 * is absent, the malformed-value and malformed-index branches, the REMOVE
 * rejection, timestamp-based conflict resolution, the per-item batch dispatch
 * and the {@link FavoriteStickerMutationFactory} builder.
 *
 * <p>Tests run against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * through {@link TestWhatsAppClient} so the
 * {@link LinkedWhatsAppSettingsStore#findFavouriteSticker(String)} read-back can be asserted
 * directly. The {@code "favorite_sticker"} primary feature flag is toggled via
 * the store's primary-features set so the gating branch is deterministic.
 */
@DisplayName("FavoriteStickerHandler")
class FavoriteStickerHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String STICKER_HASH = "stickerhash-abcdef";
    private static final String FAVORITE_STICKER_FEATURE = "favorite_sticker";

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted setMutation(StickerAction action, String index) {
        var ts = Instant.ofEpochSecond(1_700_000_000L);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .stickerAction(action)
                .build();
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, StickerAction.ACTION_VERSION);
    }

    private static DecryptedMutation.Trusted removeMutation() {
        var ts = Instant.ofEpochSecond(1_700_000_000L);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .stickerAction(new StickerActionBuilder().isFavorite(true).build())
                .build();
        var index = "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]";
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.REMOVE, ts, StickerAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire constants")
    class Metadata {
        @Test
        @DisplayName("actionName() returns StickerAction.ACTION_NAME (favoriteSticker)")
        void actionName() {
            assertEquals("favoriteSticker", new FavoriteStickerHandler().actionName());
            assertEquals(StickerAction.ACTION_NAME, new FavoriteStickerHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() returns StickerAction.COLLECTION_NAME (REGULAR_LOW)")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new FavoriteStickerHandler().collectionName());
            assertEquals(StickerAction.COLLECTION_NAME, new FavoriteStickerHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns StickerAction.ACTION_VERSION (7)")
        void version() {
            assertEquals(7, new FavoriteStickerHandler().version());
            assertEquals(StickerAction.ACTION_VERSION, new FavoriteStickerHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET happy path (feature enabled)")
    class HappySet {
        @Test
        @DisplayName("isFavorite=true on a fresh store adds the sticker and reports SUCCESS")
        void favoriteAdd() {
            store.syncStore().setPrimaryFeatures(List.of(FAVORITE_STICKER_FEATURE));
            var action = new StickerActionBuilder()
                    .isFavorite(true)
                    .mimetype("image/webp")
                    .height(512)
                    .width(512)
                    .build();
            var index = "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]";
            var result = new FavoriteStickerHandler().applyMutation(client, setMutation(action, index));

            assertEquals(MutationApplicationResult.success(), result,
                    "feature-enabled SET with non-empty hash must succeed");
            assertTrue(store.settingsStore().findFavouriteSticker(STICKER_HASH).isPresent(),
                    "the sticker must be added to the favourite-stickers store");
        }

        @Test
        @DisplayName("isFavorite=true when the sticker already exists is a no-op SUCCESS (per WA Web addOrUpdate dedup)")
        void favoriteAddIdempotent() {
            store.syncStore().setPrimaryFeatures(List.of(FAVORITE_STICKER_FEATURE));
            store.settingsStore().addFavouriteSticker(STICKER_HASH, new StickerBuilder().build());

            var action = new StickerActionBuilder().isFavorite(true).build();
            var index = "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]";
            var result = new FavoriteStickerHandler().applyMutation(client, setMutation(action, index));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.settingsStore().findFavouriteSticker(STICKER_HASH).isPresent(),
                    "pre-existing entry must still be present after no-op SUCCESS");
        }

        @Test
        @DisplayName("isFavorite=false on an existing sticker removes it and reports SUCCESS")
        void unfavoriteRemove() {
            store.syncStore().setPrimaryFeatures(List.of(FAVORITE_STICKER_FEATURE));
            store.settingsStore().addFavouriteSticker(STICKER_HASH, new StickerBuilder().build());

            var action = new StickerActionBuilder().isFavorite(false).build();
            var index = "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]";
            var result = new FavoriteStickerHandler().applyMutation(client, setMutation(action, index));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.settingsStore().findFavouriteSticker(STICKER_HASH).isEmpty(),
                    "unfavourite must drop the sticker from the favourite-stickers store");
        }

        @Test
        @DisplayName("isFavorite=false on a missing sticker is an idempotent SUCCESS no-op")
        void unfavouriteMissingIsNoop() {
            store.syncStore().setPrimaryFeatures(List.of(FAVORITE_STICKER_FEATURE));
            var action = new StickerActionBuilder().isFavorite(false).build();
            var index = "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]";
            var result = new FavoriteStickerHandler().applyMutation(client, setMutation(action, index));

            // Cobalt's removeFavouriteSticker is idempotent; WA Web pre-checks and short-circuits.
            // Both paths produce SUCCESS for this caller.
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - feature gating reports ORPHAN")
    class FeatureGating {
        @Test
        @DisplayName("missing favorite_sticker primary feature reports ORPHAN with sticker hash as model id")
        void orphanWhenFeatureDisabled() {
            // Default temporary store has no primary features.
            var action = new StickerActionBuilder().isFavorite(true).build();
            var index = "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]";
            var result = new FavoriteStickerHandler().applyMutation(client, setMutation(action, index));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(STICKER_HASH, result.modelId(),
                    "ORPHAN must carry the sticker file hash as the model id");
            assertEquals("FavoriteSticker", result.modelType(),
                    "ORPHAN must tag the model type so the orphan store can match it later");
            assertTrue(store.settingsStore().findFavouriteSticker(STICKER_HASH).isEmpty(),
                    "no state change must occur when the feature is disabled");
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("SyncActionValue carrying a different action sub-message is MALFORMED")
        void wrongActionType() {
            store.syncStore().setPrimaryFeatures(List.of(FAVORITE_STICKER_FEATURE));
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            // No stickerAction set - action().orElse(null) will be null or some other type
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]";
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 7);

            assertEquals(SyncActionState.MALFORMED,
                    new FavoriteStickerHandler().applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("empty sticker hash at index[1] is MALFORMED")
        void emptyHash() {
            var action = new StickerActionBuilder().isFavorite(true).build();
            var result = new FavoriteStickerHandler().applyMutation(client,
                    setMutation(action, "[\"favoriteSticker\",\"\"]"));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("missing sticker hash (single-element index) is MALFORMED")
        void singleElementIndex() {
            var action = new StickerActionBuilder().isFavorite(true).build();
            var result = new FavoriteStickerHandler().applyMutation(client,
                    setMutation(action, "[\"favoriteSticker\"]"));
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("non-JSON index causes the catch-all FAILED branch")
        void nonJsonIndex() {
            var action = new StickerActionBuilder().isFavorite(true).build();
            var result = new FavoriteStickerHandler().applyMutation(client,
                    setMutation(action, "not-json-at-all"));
            // The JSON parser throws, caught by the try/catch -> Failed.
            assertEquals(SyncActionState.FAILED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE short-circuits")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation is UNSUPPORTED (handler is SET-only)")
        void removeUnsupported() {
            assertEquals(SyncActionState.UNSUPPORTED,
                    new FavoriteStickerHandler().applyMutation(client, removeMutation()).actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp tiebreaker")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with later timestamp wins (APPLY_REMOTE_DROP_LOCAL)")
        void remoteLater() {
            var local = setMutation(new StickerActionBuilder().isFavorite(true).build(),
                    "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]");
            var remoteTs = Instant.ofEpochSecond(1_700_000_010L);
            var remoteValue = new SyncActionValueBuilder()
                    .timestamp(remoteTs)
                    .stickerAction(new StickerActionBuilder().isFavorite(false).build())
                    .build();
            var remote = new DecryptedMutation.Trusted(local.index(), remoteValue, SyncdOperation.SET, remoteTs, 7);

            var resolution = new FavoriteStickerHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    resolution.state());
        }

        @Test
        @DisplayName("remote with strictly earlier timestamp is skipped (SKIP_REMOTE)")
        void remoteEarlier() {
            var localTs = Instant.ofEpochSecond(1_700_000_010L);
            var localValue = new SyncActionValueBuilder()
                    .timestamp(localTs)
                    .stickerAction(new StickerActionBuilder().isFavorite(true).build())
                    .build();
            var local = new DecryptedMutation.Trusted(
                    "[\"favoriteSticker\",\"" + STICKER_HASH + "\"]",
                    localValue, SyncdOperation.SET, localTs, 7);
            var remoteTs = Instant.ofEpochSecond(1_700_000_000L);
            var remoteValue = new SyncActionValueBuilder()
                    .timestamp(remoteTs)
                    .stickerAction(new StickerActionBuilder().isFavorite(false).build())
                    .build();
            var remote = new DecryptedMutation.Trusted(local.index(), remoteValue, SyncdOperation.SET, remoteTs, 7);

            var resolution = new FavoriteStickerHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - default per-item dispatch")
    class BatchDispatch {
        @Test
        @DisplayName("default applyMutationBatch is a per-item map of applyMutation results")
        void perItemDispatch() {
            store.syncStore().setPrimaryFeatures(List.of(FAVORITE_STICKER_FEATURE));
            var a = setMutation(new StickerActionBuilder().isFavorite(true).build(),
                    "[\"favoriteSticker\",\"hash-a\"]");
            var b = setMutation(new StickerActionBuilder().isFavorite(true).build(),
                    "[\"favoriteSticker\",\"hash-b\"]");

            var results = new FavoriteStickerHandler().applyMutationBatch(client, List.of(a, b));
            assertEquals(2, results.size(), "batch result list must mirror input arity");
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertTrue(store.settingsStore().findFavouriteSticker("hash-a").isPresent());
            assertTrue(store.settingsStore().findFavouriteSticker("hash-b").isPresent());
        }
    }

    @Nested
    @DisplayName("getFavoriteStickerMutation - pending mutation builder")
    class Builder {
        @Test
        @DisplayName("favorite=true produces a SET pending mutation at the correct index with isFavorite=true")
        void buildsFavorite() {
            var pending = new FavoriteStickerMutationFactory().getFavoriteStickerMutation(STICKER_HASH, true);
            assertNotNull(pending, "builder must never return null");
            var mutation = pending.mutation();
            assertEquals(SyncdOperation.SET, mutation.operation(),
                    "favourite mutations are always SET");
            assertEquals(StickerAction.ACTION_VERSION, mutation.actionVersion());
            assertEquals("[\"favoriteSticker\",\"" + STICKER_HASH + "\"]", mutation.index(),
                    "index must be [\"favoriteSticker\", stickerHash]");
            var action = mutation.value().flatMap(sav -> sav.action()).filter(a -> a instanceof StickerAction).map(a -> (StickerAction) a).orElseThrow(
                    () -> new AssertionError("stickerAction sub-message must be populated"));
            assertTrue(action.isFavorite(),
                    "isFavorite must propagate from the builder argument");
        }

        @Test
        @DisplayName("favorite=false produces a SET pending mutation with isFavorite=false")
        void buildsUnfavorite() {
            var pending = new FavoriteStickerMutationFactory().getFavoriteStickerMutation(STICKER_HASH, false);
            var action = pending.mutation().value().flatMap(sav -> sav.action()).filter(a -> a instanceof StickerAction).map(a -> (StickerAction) a).orElseThrow();
            assertEquals(false, action.isFavorite(),
                    "isFavorite must propagate from the builder argument");
        }

        @Test
        @DisplayName("attemptCount of a freshly-built pending mutation is zero")
        void freshPendingHasZeroAttempts() {
            var pending = new FavoriteStickerMutationFactory().getFavoriteStickerMutation(STICKER_HASH, true);
            assertEquals(0, pending.attemptCount());
        }
    }

}
