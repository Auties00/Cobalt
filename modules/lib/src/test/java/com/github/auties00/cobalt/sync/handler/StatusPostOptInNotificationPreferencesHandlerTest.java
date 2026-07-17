package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.device.StatusPostOptInNotificationPreferencesAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.StatusPostOptInNotificationPreferencesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
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
 * Verifies {@link StatusPostOptInNotificationPreferencesHandler}: applying
 * an incoming opt-in mutation and asserting the boolean flag side-effect on
 * {@link LinkedWhatsAppSettingsStore#setStatusPostOptInNotificationPreferencesEnabled(Boolean)}.
 * The handler is a private-constructor singleton accessed via
 * {@link StatusPostOptInNotificationPreferencesHandler#INSTANCE}, so each
 * test rebuilds the store and client so flag state does not leak.
 */
@DisplayName("StatusPostOptInNotificationPreferencesHandler")
class StatusPostOptInNotificationPreferencesHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private LinkedWhatsAppClient client;
    private StatusPostOptInNotificationPreferencesHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = StatusPostOptInNotificationPreferencesHandler.INSTANCE;
    }

    // A null action omits the payload to drive the missing-action malformed branch.
    private DecryptedMutation.Trusted build(StatusPostOptInNotificationPreferencesAction action,
                                            SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.statusPostOptInNotificationPreferencesAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'status_post_opt_in_notification_preferences_action'")
        void actionName() {
            assertEquals(StatusPostOptInNotificationPreferencesAction.ACTION_NAME, handler.actionName());
            assertEquals("status_post_opt_in_notification_preferences_action", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(StatusPostOptInNotificationPreferencesAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET persists the enabled flag")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with enabled=true persists true on the store")
        void setsEnabled() {
            assertTrue(store.settingsStore().statusPostOptInNotificationPreferencesEnabled().isEmpty(),
                    "precondition: flag is unset");
            var action = new StatusPostOptInNotificationPreferencesActionBuilder()
                    .enabled(true).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.settingsStore().statusPostOptInNotificationPreferencesEnabled().orElseThrow());
        }

        @Test
        @DisplayName("SET with enabled=false persists false on the store")
        void setsDisabled() {
            var action = new StatusPostOptInNotificationPreferencesActionBuilder()
                    .enabled(false).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(store.settingsStore().statusPostOptInNotificationPreferencesEnabled().orElseThrow());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SET with the wrong action type returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"status_post_opt_in_notification_preferences_action\"]",
                    value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE is UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED without touching the store")
        void removeIsUnsupported() {
            var action = new StatusPostOptInNotificationPreferencesActionBuilder()
                    .enabled(true).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.settingsStore().statusPostOptInNotificationPreferencesEnabled().isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ConflictResolution {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var action = new StatusPostOptInNotificationPreferencesActionBuilder()
                    .enabled(true).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var action = new StatusPostOptInNotificationPreferencesActionBuilder()
                    .enabled(true).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
