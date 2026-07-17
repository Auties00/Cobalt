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
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction.NotificationActivitySetting;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingActionBuilder;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link NotificationActivitySettingHandler} for the protobuf-only
 * {@code "notificationActivitySetting"} action: the handler accepts only {@link SyncdOperation#SET}
 * with a non-empty {@link NotificationActivitySettingAction#notificationActivitySetting()} enum,
 * persists it via {@link LinkedWhatsAppSettingsStore#setNotificationActivitySetting}, and rejects a wrong-typed
 * value or an empty enum as {@link SyncActionState#MALFORMED}.
 *
 * <p>No public outgoing-mutation factory exists for this action, so each test drives the handler
 * directly through {@link NotificationActivitySettingHandler#applyMutation} with hand-built
 * {@link DecryptedMutation.Trusted} mutations.
 */
@DisplayName("NotificationActivitySettingHandler")
class NotificationActivitySettingHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private LinkedWhatsAppClient client;
    private NotificationActivitySettingHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new NotificationActivitySettingHandler();
    }

    private DecryptedMutation.Trusted build(NotificationActivitySettingAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.notificationActivitySettingAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'notificationActivitySetting'")
        void actionName() {
            assertEquals(NotificationActivitySettingAction.ACTION_NAME, handler.actionName());
            assertEquals("notificationActivitySetting", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(NotificationActivitySettingAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET persists the activity setting")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with ALL_MESSAGES persists ALL_MESSAGES on the store")
        void setAllMessages() {
            assertTrue(store.settingsStore().notificationActivitySetting().isEmpty(), "precondition: setting is unset");
            var action = new NotificationActivitySettingActionBuilder()
                    .notificationActivitySetting(NotificationActivitySetting.ALL_MESSAGES).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(NotificationActivitySetting.ALL_MESSAGES,
                    store.settingsStore().notificationActivitySetting().orElseThrow());
        }

        @Test
        @DisplayName("SET with HIGHLIGHTS overwrites the prior preference")
        void setHighlightsOverwrites() {
            store.settingsStore().setNotificationActivitySetting(NotificationActivitySetting.ALL_MESSAGES);
            var action = new NotificationActivitySettingActionBuilder()
                    .notificationActivitySetting(NotificationActivitySetting.HIGHLIGHTS).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(NotificationActivitySetting.HIGHLIGHTS,
                    store.settingsStore().notificationActivitySetting().orElseThrow());
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
                    "[\"notificationActivitySetting\"]", value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a SET whose enum is unset returns MALFORMED")
        void emptyEnum() {
            var action = new NotificationActivitySettingActionBuilder().build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE is UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED without touching the store")
        void removeIsUnsupported() {
            var action = new NotificationActivitySettingActionBuilder()
                    .notificationActivitySetting(NotificationActivitySetting.ALL_MESSAGES).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.settingsStore().notificationActivitySetting().isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ConflictResolution {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var action = new NotificationActivitySettingActionBuilder()
                    .notificationActivitySetting(NotificationActivitySetting.ALL_MESSAGES).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var action = new NotificationActivitySettingActionBuilder()
                    .notificationActivitySetting(NotificationActivitySetting.ALL_MESSAGES).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
