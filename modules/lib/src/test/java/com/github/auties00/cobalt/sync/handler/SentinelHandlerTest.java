package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyDataBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyIdBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.KeyExpirationAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.KeyExpirationActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.SentinelMutationFactory;
import com.github.auties00.cobalt.sync.key.SyncKeyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link SentinelHandler} and the outgoing builder in
 * {@link SentinelMutationFactory}: applying an incoming sentinel mutation
 * and asserting the app-state-sync key store side-effect. Tests seed sync
 * keys via {@link SyncKeyUtils#buildKeyId(int, int)} so the in-store key id
 * layout matches what the handler reads; the epoch match is exact, so an
 * off-by-one epoch leaves every stored key untouched.
 */
@DisplayName("SentinelHandler")
class SentinelHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    // A null epoch omits expiredKeyEpoch to drive the malformed-value branch.
    private static DecryptedMutation.Trusted sentinelMutation(Integer epoch, SyncdOperation op, Instant ts) {
        var action = new KeyExpirationActionBuilder().expiredKeyEpoch(epoch).build();
        var value = new SyncActionValueBuilder().timestamp(ts).keyExpirationAction(action).build();
        var index = "[\"sentinel\",\"regular\"]";
        return new DecryptedMutation.Trusted(index, value, op, ts, 3);
    }

    private static byte[] syncKeyId(int deviceId, int epoch) {
        return SyncKeyUtils.buildKeyId(deviceId, epoch);
    }

    // The seeded data timestamp is any non-EPOCH instant; the handler only
    // flips it to EPOCH on expiry, so Instant.now() is a valid "before" baseline.
    private void seedSyncKey(int deviceId, int epoch) {
        var keyId = new AppStateSyncKeyIdBuilder().keyId(syncKeyId(deviceId, epoch)).build();
        var keyData = new AppStateSyncKeyDataBuilder()
                .keyData(new byte[32])
                .timestamp(Instant.now())
                .build();
        client.store().syncStore().addWebAppStateKeys(List.of(
                new AppStateSyncKeyBuilder().keyId(keyId).keyData(keyData).build()));
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'sentinel'")
        void actionName() {
            assertEquals(KeyExpirationAction.ACTION_NAME, new SentinelHandler().actionName());
            assertEquals("sentinel", new SentinelHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new SentinelHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (3)")
        void version() {
            assertEquals(KeyExpirationAction.ACTION_VERSION, new SentinelHandler().version());
            assertEquals(3, new SentinelHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET stamps Instant.EPOCH onto the matching sync key")
    class SetHappy {
        @Test
        @DisplayName("expired epoch matching a stored key flips that key's data timestamp to EPOCH")
        void expiresStoredKey() {
            seedSyncKey(0, 5);
            assertEquals(1, client.store().syncStore().appStateKeys().size());

            var result = new SentinelHandler().applyMutation(
                    client, sentinelMutation(5, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(1, client.store().syncStore().appStateKeys().size(),
                    "the key is NOT removed; only its data.timestamp is set to Instant.EPOCH");
            var key = client.store().syncStore().appStateKeys().iterator().next();
            var timestamp = key.keyData().orElseThrow().timestamp().orElseThrow();
            assertEquals(Instant.EPOCH, timestamp,
                    "the matching key's data timestamp must be set to Instant.EPOCH");
        }

        @Test
        @DisplayName("an epoch not matching any stored key leaves the timestamp untouched")
        void noMatchingKey() {
            seedSyncKey(0, 5);
            var beforeTs = client.store().syncStore().appStateKeys().iterator().next()
                    .keyData().orElseThrow().timestamp().orElseThrow();

            var result = new SentinelHandler().applyMutation(
                    client, sentinelMutation(99, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(1, client.store().syncStore().appStateKeys().size(),
                    "no key matches the expired epoch - nothing to expire");
            var afterTs = client.store().syncStore().appStateKeys().iterator().next()
                    .keyData().orElseThrow().timestamp().orElseThrow();
            assertEquals(beforeTs, afterTs,
                    "the unmatched key's timestamp must remain as seeded");
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value")
    class Malformed {
        @Test
        @DisplayName("non-keyExpiration action is MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"sentinel\",\"regular\"]", wrongValue, SyncdOperation.SET, Instant.now(), 3);

            var result = new SentinelHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("missing expiredKeyEpoch field is MALFORMED")
        void missingEpoch() {
            var result = new SentinelHandler().applyMutation(
                    client, sentinelMutation(null, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index - n/a")
    class MalformedIndexNa {
        @Test
        @DisplayName("the handler does not parse indexParts[1]; the index is not part of malformed surface")
        void indexNotValidated() {
            var result = new SentinelHandler().applyMutation(
                    client, sentinelMutation(5, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation does not touch the keystore")
        void removeIsUnsupported() {
            seedSyncKey(0, 5);

            var result = new SentinelHandler().applyMutation(
                    client, sentinelMutation(5, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertEquals(1, client.store().syncStore().appStateKeys().size(),
                    "REMOVE must not expire any key");
        }
    }

    @Nested
    @DisplayName("getSentinelMutations - one pending mutation per collection")
    class SentinelBuilder {
        @Test
        @DisplayName("returns one SET pending mutation per SyncPatchType when a key exists")
        void emitsOnePerCollection() {
            seedSyncKey(7, 12);

            var pending = new SentinelMutationFactory().getSentinelMutations(client);

            assertEquals(SyncPatchType.values().length, pending.size(),
                    "one mutation per collection");
            for (var m : pending) {
                assertEquals(SyncdOperation.SET, m.mutation().operation());
                assertEquals(KeyExpirationAction.ACTION_VERSION, m.mutation().actionVersion());
                var action = (KeyExpirationAction) m.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
                assertEquals(12, action.expiredKeyEpoch().orElseThrow(),
                        "every sentinel mutation must carry the newest key's epoch");
            }
        }

        @Test
        @DisplayName("returns an empty list when no sync keys exist")
        void emptyWhenNoKeys() {
            var pending = new SentinelMutationFactory().getSentinelMutations(client);
            assertTrue(pending.isEmpty(),
                    "no key pairs in the store means no sentinel work");
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = sentinelMutation(5, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = sentinelMutation(6, SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new SentinelHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("local with the strictly later timestamp wins")
        void localWins() {
            var local  = sentinelMutation(6, SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));
            var remote = sentinelMutation(5, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));

            var resolution = new SentinelHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - n/a, default implementation")
    class BatchNa {
        @Test
        @DisplayName("the handler delegates to applyMutation per mutation")
        void perItem() {
            seedSyncKey(0, 5);
            seedSyncKey(0, 6);

            var batch = List.of(
                    sentinelMutation(5, SyncdOperation.SET, Instant.now()),
                    sentinelMutation(99, SyncdOperation.SET, Instant.now()),
                    sentinelMutation(6, SyncdOperation.REMOVE, Instant.now()));

            var results = new SentinelHandler().applyMutationBatch(client, batch);

            assertEquals(3, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(2).actionState());
            assertEquals(2, client.store().syncStore().appStateKeys().size(),
                    "neither SET nor REMOVE removes keys; expireAppStateKeysByEpoch only flips data timestamps");
        }
    }

}
