package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.PrimaryVersionAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.PrimaryVersionActionBuilder;
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

/**
 * Covers {@link PrimaryVersionHandler}: a {@link SyncdOperation#SET} with sub-index
 * {@code "current"} or {@code "session_start"} and a non-empty version string surfaces as
 * {@link SyncActionState#SUCCESS} (no store write); a missing, empty, or unknown
 * sub-index, a wrong-typed value, or a missing version field all surface as
 * {@link SyncActionState#MALFORMED}; {@link SyncdOperation#REMOVE} surfaces as
 * {@link SyncActionState#UNSUPPORTED}; {@link PrimaryVersionHandler#applyMutationBatch}
 * dispatches each mutation independently; the default conflict resolution chooses the
 * later timestamp.
 */
@DisplayName("PrimaryVersionHandler")
class PrimaryVersionHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted primaryVersionMutation(String subIndex, String versionString, SyncdOperation op, Instant ts) {
        var action = new PrimaryVersionActionBuilder().version(versionString).build();
        var value = new SyncActionValueBuilder().timestamp(ts).primaryVersionAction(action).build();
        var index = subIndex == null
                ? "[\"primary_version\"]"
                : "[\"primary_version\",\"" + subIndex + "\"]";
        return new DecryptedMutation.Trusted(index, value, op, ts, 7);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'primary_version'")
        void actionName() {
            assertEquals(PrimaryVersionAction.ACTION_NAME, new PrimaryVersionHandler().actionName());
            assertEquals("primary_version", new PrimaryVersionHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new PrimaryVersionHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(PrimaryVersionAction.ACTION_VERSION, new PrimaryVersionHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET happy paths")
    class SetHappy {
        @Test
        @DisplayName("subIndex 'current' with a version string returns SUCCESS")
        void currentSubIndex() {
            var result = new PrimaryVersionHandler().applyMutation(
                    client, primaryVersionMutation("current", "2.24.0", SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }

        @Test
        @DisplayName("subIndex 'session_start' with a version string returns SUCCESS")
        void sessionStartSubIndex() {
            var result = new PrimaryVersionHandler().applyMutation(
                    client, primaryVersionMutation("session_start", "2.24.0", SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a missing subIndex (singleton array) is MALFORMED")
        void missingSubIndex() {
            var result = new PrimaryVersionHandler().applyMutation(
                    client, primaryVersionMutation(null, "2.24.0", SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an empty subIndex string is MALFORMED")
        void emptySubIndex() {
            var result = new PrimaryVersionHandler().applyMutation(
                    client, primaryVersionMutation("", "2.24.0", SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an unknown subIndex (not 'current' / 'session_start') is MALFORMED")
        void unknownSubIndex() {
            var result = new PrimaryVersionHandler().applyMutation(
                    client, primaryVersionMutation("background_sync", "2.24.0", SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a missing version string in primaryVersionAction is MALFORMED")
        void missingVersionField() {
            var action = new PrimaryVersionActionBuilder().build();
            var value = new SyncActionValueBuilder().timestamp(Instant.now()).primaryVersionAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"primary_version\",\"current\"]", value, SyncdOperation.SET, Instant.now(), 7);

            var result = new PrimaryVersionHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a non-primary-version action in the value is MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"primary_version\",\"current\"]", wrongValue, SyncdOperation.SET, Instant.now(), 7);

            var result = new PrimaryVersionHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation is rejected before any classification")
        void removeIsUnsupported() {
            var result = new PrimaryVersionHandler().applyMutation(
                    client, primaryVersionMutation("current", "2.24.0", SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - overridden but dispatches per-mutation")
    class Batch {
        @Test
        @DisplayName("each mutation in the batch is classified independently")
        void perItemClassification() {
            var ok      = primaryVersionMutation("current", "2.24.0", SyncdOperation.SET, Instant.now());
            var badIdx  = primaryVersionMutation("bogus",   "2.24.0", SyncdOperation.SET, Instant.now());
            var remove  = primaryVersionMutation("current", "2.24.0", SyncdOperation.REMOVE, Instant.now());

            var results = new PrimaryVersionHandler().applyMutationBatch(client, List.of(ok, badIdx, remove));

            assertEquals(3, results.size());
            assertEquals(SyncActionState.SUCCESS,     results.get(0).actionState());
            assertEquals(SyncActionState.MALFORMED,   results.get(1).actionState());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(2).actionState());
        }

        @Test
        @DisplayName("an empty batch returns an empty result list")
        void emptyBatch() {
            assertEquals(0, new PrimaryVersionHandler().applyMutationBatch(client, List.of()).size());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = primaryVersionMutation("current", "2.24.0", SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = primaryVersionMutation("current", "2.24.1", SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new PrimaryVersionHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

}
