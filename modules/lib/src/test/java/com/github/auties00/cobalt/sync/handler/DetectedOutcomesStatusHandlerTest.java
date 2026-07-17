package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.DetectedOutcomesStatusAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.DetectedOutcomesStatusActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
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
 * Exercises the {@link DetectedOutcomesStatusHandler} adapter for the
 * {@code detected_outcomes_status_action} app-state sync action across metadata,
 * the SET happy path, the malformed-value branch, the REMOVE rejection, the
 * inherited timestamp-based conflict resolution, and the default batch dispatch.
 *
 * <p>Each test runs against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * via {@link TestWhatsAppClient}, so it starts from a clean single-device state and
 * the {@link LinkedWhatsAppBusinessStore#detectedOutcomesEnabled()}
 * read-back can be asserted directly.
 */
@DisplayName("DetectedOutcomesStatusHandler")
class DetectedOutcomesStatusHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted detectedMutation(Boolean enabled, SyncdOperation op, Instant ts) {
        var action = new DetectedOutcomesStatusActionBuilder().isEnabled(enabled).build();
        var value = new SyncActionValueBuilder().timestamp(ts).detectedOutcomesStatusAction(action).build();
        return new DecryptedMutation.Trusted("[\"detected_outcomes_status_action\"]", value, op, ts, 1);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'detected_outcomes_status_action'")
        void actionName() {
            assertEquals(DetectedOutcomesStatusAction.ACTION_NAME, new DetectedOutcomesStatusHandler().actionName());
            assertEquals("detected_outcomes_status_action", new DetectedOutcomesStatusHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR, new DetectedOutcomesStatusHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (1)")
        void version() {
            assertEquals(DetectedOutcomesStatusAction.ACTION_VERSION, new DetectedOutcomesStatusHandler().version());
            assertEquals(1, new DetectedOutcomesStatusHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET persists into the store")
    class SetHappy {
        @Test
        @DisplayName("isEnabled=true sets the store flag to true")
        void enable() {
            assertFalse(client.store().businessStore().detectedOutcomesEnabled(), "default false");

            var result = new DetectedOutcomesStatusHandler().applyMutation(
                    client, detectedMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().businessStore().detectedOutcomesEnabled());
        }

        @Test
        @DisplayName("isEnabled=false flips the store flag to false")
        void disable() {
            client.store().businessStore().setDetectedOutcomesEnabled(true);

            var result = new DetectedOutcomesStatusHandler().applyMutation(
                    client, detectedMutation(Boolean.FALSE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(client.store().businessStore().detectedOutcomesEnabled());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value")
    class Malformed {
        @Test
        @DisplayName("non-detected-outcomes action yields MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"detected_outcomes_status_action\"]", wrongValue, SyncdOperation.SET, Instant.now(), 1);

            var result = new DetectedOutcomesStatusHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index - n/a (singleton index)")
    class MalformedIndexNa {
        @Test
        @DisplayName("the handler does not inspect indexParts[1]")
        void singletonIndex() {
            var result = new DetectedOutcomesStatusHandler().applyMutation(
                    client, detectedMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED before any store write")
        void removeIsUnsupported() {
            var result = new DetectedOutcomesStatusHandler().applyMutation(
                    client, detectedMutation(Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertFalse(client.store().businessStore().detectedOutcomesEnabled());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = detectedMutation(Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = detectedMutation(Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new DetectedOutcomesStatusHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - n/a, default implementation")
    class BatchNa {
        @Test
        @DisplayName("default applyMutationBatch delegates per mutation")
        void perItem() {
            var batch = List.of(
                    detectedMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()),
                    detectedMutation(Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            var results = new DetectedOutcomesStatusHandler().applyMutationBatch(client, batch);

            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS,     results.get(0).actionState());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(1).actionState());
        }
    }

}
