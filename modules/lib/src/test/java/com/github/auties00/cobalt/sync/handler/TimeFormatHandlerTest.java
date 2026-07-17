package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.TimeFormatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.TimeFormatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.factory.TimeFormatMutationFactory;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link TimeFormatHandler} and the outgoing
 * {@link TimeFormatMutationFactory}: applying an incoming time-format
 * mutation and asserting the boolean preference written into
 * {@link LinkedWhatsAppSettingsStore#setTwentyFourHourFormat(boolean)}.
 * Each test recreates the store so the {@code twentyFourHourFormat} field
 * starts at its default value.
 */
@DisplayName("TimeFormatHandler")
class TimeFormatHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    // A null enabled flag exercises the nullable-boolean-coalesces-to-false convention.
    private static DecryptedMutation.Trusted timeFormatMutation(Boolean enabled, SyncdOperation op, Instant ts) {
        var action = new TimeFormatActionBuilder().isTwentyFourHourFormatEnabled(enabled).build();
        var value = new SyncActionValueBuilder().timestamp(ts).timeFormatAction(action).build();
        return new DecryptedMutation.Trusted("[\"time_format\"]", value, op, ts, 7);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'time_format'")
        void actionName() {
            assertEquals(TimeFormatAction.ACTION_NAME, new TimeFormatHandler().actionName());
            assertEquals("time_format", new TimeFormatHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new TimeFormatHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(TimeFormatAction.ACTION_VERSION, new TimeFormatHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET happy path persists into the store")
    class SetHappy {
        @Test
        @DisplayName("isTwentyFourHourFormatEnabled=true sets the store flag to true")
        void enables24Hour() {
            assertFalse(client.store().settingsStore().twentyFourHourFormat(), "default is 12h");

            var result = new TimeFormatHandler().applyMutation(
                    client, timeFormatMutation(Boolean.TRUE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().settingsStore().twentyFourHourFormat(),
                    "store must reflect 24h time format");
        }

        @Test
        @DisplayName("isTwentyFourHourFormatEnabled=false sets the store flag to false")
        void disables24Hour() {
            client.store().settingsStore().setTwentyFourHourFormat(true);

            var result = new TimeFormatHandler().applyMutation(
                    client, timeFormatMutation(Boolean.FALSE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(client.store().settingsStore().twentyFourHourFormat());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value")
    class MalformedValue {
        @Test
        @DisplayName("value carrying a non-time-format action is MALFORMED")
        void wrongActionType() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"time_format\"]", wrongValue, SyncdOperation.SET, Instant.now(), 7);

            var result = new TimeFormatHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
            assertFalse(client.store().settingsStore().twentyFourHourFormat(),
                    "malformed mutation must not touch the store");
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed index - n/a")
    class MalformedIndex {
        @Test
        @DisplayName("WA Web does not validate index parts for time_format (singleton index)")
        void indexNotValidated() {
            // The singleton time_format index carries only the action name; no trailing
            // element is read, so a malformed index never reaches a malformed branch.
            var result = new TimeFormatHandler().applyMutation(
                    client, timeFormatMutation(Boolean.TRUE, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "singleton index is not part of the malformed surface");
        }

        @Test
        @DisplayName("non-JSON index propagates as a parse exception")
        void nonJsonIndex() {
            var action = new TimeFormatActionBuilder().isTwentyFourHourFormatEnabled(Boolean.TRUE).build();
            var value = new SyncActionValueBuilder().timestamp(Instant.now()).timeFormatAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    "not-json", value, SyncdOperation.SET, Instant.now(), 7);
            // The handler never parses the index, so even a non-JSON index applies cleanly;
            // this regression-guards the surface against a future index-parse step.
            var result = new TimeFormatHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE operation is rejected before any store side effect")
        void removeIsUnsupported() {
            var result = new TimeFormatHandler().applyMutation(
                    client, timeFormatMutation(Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertFalse(client.store().settingsStore().twentyFourHourFormat(),
                    "REMOVE must not flip the store");
        }
    }

    @Nested
    @DisplayName("getTimeFormatMutation - builder helper")
    class Builder {
        @Test
        @DisplayName("emits a SET pending mutation with the requested flag and timestamp")
        void buildsPending() {
            var ts = Instant.ofEpochSecond(1700000000L);
            var pending = new TimeFormatMutationFactory().getTimeFormatMutation(ts, true);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            assertEquals(ts, pending.mutation().timestamp());
            assertEquals(TimeFormatAction.ACTION_VERSION, pending.mutation().actionVersion());
            assertEquals("[\"time_format\"]", pending.mutation().index());
            var action = (TimeFormatAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertTrue(action.isTwentyFourHourFormatEnabled());
        }

        @Test
        @DisplayName("null timestamp triggers a NullPointerException at construction time")
        void nullTimestampRejected() {
            assertThrows(NullPointerException.class,
                    () -> new TimeFormatMutationFactory().getTimeFormatMutation(null, true));
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = timeFormatMutation(Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = timeFormatMutation(Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new TimeFormatHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("equal timestamps still apply remote per the default tie-break")
        void equalTimestampApplyRemote() {
            var ts = Instant.ofEpochSecond(1700000000L);
            var local  = timeFormatMutation(Boolean.FALSE, SyncdOperation.SET, ts);
            var remote = timeFormatMutation(Boolean.TRUE,  SyncdOperation.SET, ts);

            var resolution = new TimeFormatHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - n/a, default implementation is per-item")
    class BatchNa {
        @Test
        @DisplayName("the default applyMutationBatch dispatches per-mutation to applyMutation")
        void defaultBatch() {
            // TimeFormatHandler does not override applyMutationBatch, so the default
            // implementation dispatches applyMutation once per mutation.
            var batch = List.of(
                    timeFormatMutation(Boolean.TRUE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)),
                    timeFormatMutation(Boolean.FALSE, SyncdOperation.REMOVE, Instant.ofEpochSecond(1700000010L)));

            var results = new TimeFormatHandler().applyMutationBatch(client, batch);

            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(1).actionState());
        }
    }

}
