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
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction.PrivateProcessingStatus;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingActionBuilder;
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
 * Covers {@link PrivateProcessingSettingHandler}, which accepts only
 * {@link SyncdOperation#SET} with a non-empty
 * {@link PrivateProcessingSettingAction#privateProcessingStatus()} enum, persists it via
 * {@link LinkedWhatsAppSettingsStore#setPrivateProcessingStatus}, rejects a wrong-typed value or an
 * empty enum as {@link SyncActionState#MALFORMED}, reports
 * {@link SyncActionState#UNSUPPORTED} for {@link SyncdOperation#REMOVE}, and resolves
 * conflicts by timestamp. Mutations are hand-built as {@link DecryptedMutation.Trusted}.
 */
@DisplayName("PrivateProcessingSettingHandler")
class PrivateProcessingSettingHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private LinkedWhatsAppClient client;
    private PrivateProcessingSettingHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new PrivateProcessingSettingHandler();
    }

    private DecryptedMutation.Trusted build(PrivateProcessingSettingAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.privateProcessingSettingAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'private_processing_setting'")
        void actionName() {
            assertEquals(PrivateProcessingSettingAction.ACTION_NAME, handler.actionName());
            assertEquals("private_processing_setting", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(PrivateProcessingSettingAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET persists the private processing status")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with ENABLED persists ENABLED on the store")
        void setEnabled() {
            assertTrue(store.settingsStore().privateProcessingStatus().isEmpty(), "precondition: status is unset");
            var action = new PrivateProcessingSettingActionBuilder()
                    .privateProcessingStatus(PrivateProcessingStatus.ENABLED).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(PrivateProcessingStatus.ENABLED, store.settingsStore().privateProcessingStatus().orElseThrow());
        }

        @Test
        @DisplayName("SET with DISABLED overwrites a prior ENABLED value")
        void setDisabledOverwrites() {
            store.settingsStore().setPrivateProcessingStatus(PrivateProcessingStatus.ENABLED);
            var action = new PrivateProcessingSettingActionBuilder()
                    .privateProcessingStatus(PrivateProcessingStatus.DISABLED).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(PrivateProcessingStatus.DISABLED, store.settingsStore().privateProcessingStatus().orElseThrow());
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
                    "[\"private_processing_setting\"]", value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a SET whose privateProcessingStatus is unset returns MALFORMED")
        void emptyEnum() {
            var action = new PrivateProcessingSettingActionBuilder().build();

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
            var action = new PrivateProcessingSettingActionBuilder()
                    .privateProcessingStatus(PrivateProcessingStatus.ENABLED).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.settingsStore().privateProcessingStatus().isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ConflictResolution {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var action = new PrivateProcessingSettingActionBuilder()
                    .privateProcessingStatus(PrivateProcessingStatus.ENABLED).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var action = new PrivateProcessingSettingActionBuilder()
                    .privateProcessingStatus(PrivateProcessingStatus.ENABLED).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
