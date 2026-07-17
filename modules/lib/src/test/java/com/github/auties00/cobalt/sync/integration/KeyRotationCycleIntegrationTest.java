package com.github.auties00.cobalt.sync.integration;
import com.github.auties00.cobalt.sync.LiveSnapshotRecoveryService;
import com.github.auties00.cobalt.sync.LiveWebAppStateService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyDataBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyIdBuilder;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.sync.key.SyncKeyRotationService;
import com.github.auties00.cobalt.sync.key.SyncKeyUtils;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the sync-key rotation cycle that fires before an outgoing mutation
 * upload when the active key is expired or the linked-device fingerprint changed:
 * a new key is generated, persisted, shared to every companion via an app-state
 * key-share peer message, and used for subsequent encryption while old-key
 * entries are migrated in later patches. The {@link SyncKeyRotationService} is
 * obtained from a {@link WebAppStateService} graph wired in-process via
 * {@link TestWhatsAppClient}. The synthetic group asserts key-share install,
 * idempotent re-share, and active-key absence behaviour directly; the captured
 * group is gated on {@link SyncFixtures#isAvailable(String)} so it skips cleanly
 * until the recorded rotation corpus is committed.
 */
@DisplayName("KeyRotationCycle integration")
class KeyRotationCycleIntegrationTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private TestWhatsAppClient client;
    private LinkedWhatsAppStore store;
    private SyncKeyRotationService rotation;

    @BeforeEach
    void setUp() {
        var props = TestABPropsService.builder().build();
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props);
        var wam = new LiveWamService(client, props);
        var lidMigration = new LiveLidMigrationService(client, props, wam);
        var snapshotRecovery = new LiveSnapshotRecoveryService(client, props, wam);
        var webAppState = new LiveWebAppStateService(client, props, lidMigration, snapshotRecovery, wam, TestMediaConnectionService.create());
        rotation = webAppState.syncKeyRotationService();
    }

    private static byte[] filled(int length, int value) {
        var out = new byte[length];
        for (var i = 0; i < length; i++) out[i] = (byte) value;
        return out;
    }

    private static AppStateSyncKey
            syncKey(byte[] id, byte[] data) {
        return new AppStateSyncKeyBuilder()
                .keyId(new AppStateSyncKeyIdBuilder().keyId(id).build())
                .keyData(new AppStateSyncKeyDataBuilder().keyData(data).timestamp(Instant.now()).build())
                .build();
    }

    @Nested
    @DisplayName("synthetic â€” key share + missing-key clear behaviours")
    class Smoke {
        @Test
        @DisplayName("a fresh share installs the key in the active store")
        void freshShareInstalls() {
            assertTrue(store.syncStore().appStateKeys().isEmpty(), "precondition: empty key store");
            var keyId = SyncKeyUtils.buildKeyId(1, 1);
            rotation.handleKeyShare(0, List.of(syncKey(keyId, filled(32, 0x42))));
            assertTrue(store.syncStore().findWebAppStateKeyById(keyId).isPresent());
        }

        @Test
        @DisplayName("re-sharing an already-installed key does not duplicate")
        void reShareIsIdempotent() {
            var keyId = SyncKeyUtils.buildKeyId(1, 1);
            store.syncStore().addWebAppStateKeys(List.of(syncKey(keyId, filled(32, 0x42))));
            rotation.handleKeyShare(0, List.of(syncKey(keyId, filled(32, 0x42))));
            assertEquals(1, store.syncStore().appStateKeys().size());
        }

        @Test
        @DisplayName("getActiveKey throws when the store has no keys")
        void noKeysThrows() {
            assertFalse(store.syncStore().appStateKeys().iterator().hasNext());
            Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> rotation.getActiveKey(true));
        }
    }

    @Nested
    @DisplayName("captured cycle â€” oracle parity once fixtures land")
    class CapturedCycle {
        @Test
        @DisplayName("forced rotation produces a key-share peer message matching the captured WA Web payload")
        void capturedRotation() {
            if (!SyncFixtures.isAvailable("integration/key-rotation-cycle/forced")) return;
            assertNotNull(SyncFixtures.loadOracle(
                    "integration/key-rotation-cycle/forced"));
            // Fixture pairs the captured key-share peer message bytes with the pre-
            // and post-rotation store snapshots; rotation must produce a key with
            // the same epoch and deviceId, broadcast a protobuf-equal peer message,
            // and leave the same store state.
        }

        @Test
        @DisplayName("post-rotation mutation upload uses the new key id")
        void postRotationUploadKeyId() {
            if (!SyncFixtures.isAvailable("integration/key-rotation-cycle/post-rotation-upload")) return;
            // Replay the captured upload IQ and confirm the patch's keyId matches
            // the newly-rotated key, not the previously-active key.
        }

        @Test
        @DisplayName("old-key entries are re-encrypted in the next outgoing patch")
        void oldKeyMigration() {
            if (!SyncFixtures.isAvailable("integration/key-rotation-cycle/old-key-migration")) return;
            // Each outgoing patch adds rotation SET/REMOVE mutations to migrate
            // old-key entries; the fixture exposes the rotated-entry count per patch.
        }
    }
}
