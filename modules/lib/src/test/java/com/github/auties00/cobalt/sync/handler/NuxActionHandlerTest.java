package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.NuxAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.NuxActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.NuxActionMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link NuxActionHandler}: a {@link SyncdOperation#SET} with {@code acknowledged=true} or
 * {@code acknowledged=false} writes a matching {@code dismissed} flag via
 * {@link LinkedWhatsAppSettingsStore#putOnboardingHintState}, a missing
 * {@code nuxAction} on the value coalesces to {@code dismissed=false} and still returns
 * {@link SyncActionState#SUCCESS}, a missing {@code indexParts[1]} surfaces as
 * {@link SyncActionState#MALFORMED}, {@link SyncdOperation#REMOVE} surfaces as
 * {@link SyncActionState#UNSUPPORTED}, and the default {@code resolveConflicts} chooses the later
 * timestamp. The {@link NuxActionMutationFactory} produces a SET pending mutation with the
 * requested key, flag and timestamp, and rejects a {@code null} key or {@code null} timestamp.
 *
 * <p>Inbound mutations are built directly via the local {@code nuxMutation} helper.
 */
@DisplayName("NuxActionHandler")
class NuxActionHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String HINT_KEY = "lockchats_v1";

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted nuxMutation(String hintKey, Boolean acknowledged, SyncdOperation op, Instant ts) {
        var action = new NuxActionBuilder().acknowledged(acknowledged).build();
        var value = new SyncActionValueBuilder().timestamp(ts).nuxAction(action).build();
        var index = "[\"nux\",\"" + hintKey + "\"]";
        return new DecryptedMutation.Trusted(index, value, op, ts, 7);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'nux'")
        void actionName() {
            assertEquals(NuxAction.ACTION_NAME, new NuxActionHandler().actionName());
            assertEquals("nux", new NuxActionHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new NuxActionHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(NuxAction.ACTION_VERSION, new NuxActionHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET writes the hint dismissed flag")
    class SetHappy {
        @Test
        @DisplayName("acknowledged=true persists dismissed=true keyed by nuxKey")
        void acknowledgePersists() {
            var result = new NuxActionHandler().applyMutation(
                    client, nuxMutation(HINT_KEY, Boolean.TRUE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var state = client.store().settingsStore().findOnboardingHintState(HINT_KEY).orElseThrow();
            assertEquals(HINT_KEY, state.hintId());
            assertTrue(state.dismissed());
        }

        @Test
        @DisplayName("acknowledged=false persists dismissed=false (still SUCCESS)")
        void unacknowledgePersists() {
            var result = new NuxActionHandler().applyMutation(
                    client, nuxMutation(HINT_KEY, Boolean.FALSE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var state = client.store().settingsStore().findOnboardingHintState(HINT_KEY).orElseThrow();
            assertFalse(state.dismissed());
        }

        @Test
        @DisplayName("a missing nuxAction defaults to dismissed=false and still returns SUCCESS")
        void missingNuxActionDefaultsFalse() {
            // The handler intentionally falls through to acknowledged=false when nuxAction is absent.
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"nux\",\"" + HINT_KEY + "\"]", value, SyncdOperation.SET, Instant.now(), 7);

            var result = new NuxActionHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var state = client.store().settingsStore().findOnboardingHintState(HINT_KEY).orElseThrow();
            assertFalse(state.dismissed(),
                    "missing nuxAction must coalesce to acknowledged=false on the store record");
        }
    }

    @Nested
    @DisplayName("applyMutation: orphan paths - n/a (handler always writes a hint record)")
    class OrphanNa {
        @Test
        @DisplayName("NUX does not require an existing entity; any nuxKey is acceptable")
        void anyKeyAccepted() {
            var result = new NuxActionHandler().applyMutation(
                    client, nuxMutation("totally_new_key", Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "NUX writes a new hint state row whenever the key is present");
            assertTrue(client.store().settingsStore().findOnboardingHintState("totally_new_key").isPresent());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed paths")
    class Malformed {
        @Test
        @DisplayName("missing nuxKey (index of length 1) yields MALFORMED")
        void missingKey() {
            var action = new NuxActionBuilder().acknowledged(Boolean.TRUE).build();
            var value = new SyncActionValueBuilder().timestamp(Instant.now()).nuxAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"nux\"]", value, SyncdOperation.SET, Instant.now(), 7);

            var result = new NuxActionHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED before any store write")
        void removeIsUnsupported() {
            var result = new NuxActionHandler().applyMutation(
                    client, nuxMutation(HINT_KEY, Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(client.store().settingsStore().findOnboardingHintState(HINT_KEY).isEmpty(),
                    "REMOVE must not create a hint record");
        }
    }

    @Nested
    @DisplayName("getNuxMutation - builder helper")
    class Builder {
        @Test
        @DisplayName("emits a SET pending mutation with the requested key, flag, and timestamp")
        void buildsPending() {
            var ts = Instant.ofEpochSecond(1700000000L);

            var pending = new NuxActionMutationFactory().getNuxMutation(HINT_KEY, ts, true);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            assertEquals(ts, pending.mutation().timestamp());
            assertEquals(NuxAction.ACTION_VERSION, pending.mutation().actionVersion());
            assertEquals("[\"nux\",\"" + HINT_KEY + "\"]", pending.mutation().index());
            var action = (NuxAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertTrue(action.acknowledged());
        }

        @Test
        @DisplayName("null nuxKey is rejected with NullPointerException")
        void nullKeyRejected() {
            assertThrows(NullPointerException.class,
                    () -> new NuxActionMutationFactory().getNuxMutation(null, Instant.now(), true));
        }

        @Test
        @DisplayName("null timestamp is rejected with NullPointerException")
        void nullTimestampRejected() {
            assertThrows(NullPointerException.class,
                    () -> new NuxActionMutationFactory().getNuxMutation(HINT_KEY, null, true));
        }
    }

    @Nested
    @DisplayName("getNuxMutation - acknowledged / un-acknowledged variants")
    class AckHelpers {
        @Test
        @DisplayName("acknowledged=true emits a SET pending mutation with acknowledged=true")
        void acknowledge() {
            var pending = new NuxActionMutationFactory().getNuxMutation(HINT_KEY, Instant.now(), true);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            var action = (NuxAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertTrue(action.acknowledged());
        }

        @Test
        @DisplayName("acknowledged=false emits a SET pending mutation with acknowledged=false")
        void unAcknowledge() {
            var pending = new NuxActionMutationFactory().getNuxMutation(HINT_KEY, Instant.now(), false);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            var action = (NuxAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertFalse(action.acknowledged());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = nuxMutation(HINT_KEY, Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = nuxMutation(HINT_KEY, Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new NuxActionHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

}
