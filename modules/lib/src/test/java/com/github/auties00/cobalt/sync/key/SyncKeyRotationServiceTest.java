package com.github.auties00.cobalt.sync.key;
import com.github.auties00.cobalt.sync.LiveSnapshotRecoveryService;
import com.github.auties00.cobalt.sync.LiveWebAppStateService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.device.sync.MissingDeviceSyncKeyBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyDataBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyIdBuilder;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the synchronous, store-observable invariants of {@link SyncKeyRotationService}:
 * {@code handleKeyShare} (insertion, dedup, missing-key clear, malformed-key skipping),
 * {@code getNewestKeyPair} (delegation to {@link SyncKeyUtils#findNewestKey}), and
 * {@code getActiveKey}/{@code ensureActiveKey} (empty-store throw, rotation-suppressed
 * return).
 *
 * <p>Every test obtains its system under test from {@link #build()}, which constructs the
 * full {@link WebAppStateService} dependency chain and pulls the rotation service out, so
 * the wiring (in particular the
 * {@link MissingSyncKeyRequestService}/{@link MissingSyncKeyTimeoutScheduler} cyclic
 * dependency) is not reproduced per test.
 */
@DisplayName("SyncKeyRotationService")
class SyncKeyRotationServiceTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private record Harness(TestWhatsAppClient client, LinkedWhatsAppStore store, SyncKeyRotationService rotation) {
    }

    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        var client = TestWhatsAppClient.create().withStore(store);
        var wam = new LiveWamService(client, props);
        var lidMigration = new LiveLidMigrationService(client, props, wam);
        var snapshotRecovery = new LiveSnapshotRecoveryService(client, props, wam);
        var webAppState = new LiveWebAppStateService(client, props, lidMigration, snapshotRecovery, wam, TestMediaConnectionService.create());
        return new Harness(client, store, webAppState.syncKeyRotationService());
    }

    private static byte[] filled(int length, int value) {
        var out = new byte[length];
        for (var i = 0; i < length; i++) out[i] = (byte) value;
        return out;
    }

    private static AppStateSyncKey syncKey(byte[] id, byte[] data) {
        return new AppStateSyncKeyBuilder()
                .keyId(new AppStateSyncKeyIdBuilder().keyId(id).build())
                .keyData(new AppStateSyncKeyDataBuilder().keyData(data).timestamp(Instant.now()).build())
                .build();
    }

    @Nested
    @DisplayName("handleKeyShare - store / dedup / missing-key removal")
    class HandleKeyShare {
        @Test
        @DisplayName("a key not present in the store is added on receipt")
        void storesNewKey() {
            var h = build();
            assertTrue(h.store.syncStore().appStateKeys().isEmpty(), "precondition: empty key store");

            var keyId = SyncKeyUtils.buildKeyId(1, 1);
            var keyData = filled(32, 0x77);
            h.rotation.handleKeyShare(0, List.of(syncKey(keyId, keyData)));

            var stored = h.store.syncStore().findWebAppStateKeyById(keyId).orElseThrow(
                    () -> new AssertionError("expected key to have been stored"));
            assertArrayEquals(keyId,
                    stored.keyId().orElseThrow().keyId().orElseThrow());
            assertArrayEquals(keyData,
                    stored.keyData().orElseThrow().keyData().orElseThrow());
        }

        @Test
        @DisplayName("an already-stored key with identical key data is not re-added")
        void deduplicatesIdenticalKey() {
            var h = build();
            var keyId = SyncKeyUtils.buildKeyId(1, 1);
            var keyData = filled(32, 0x77);
            h.store.syncStore().addWebAppStateKeys(List.of(syncKey(keyId, keyData)));
            assertEquals(1, h.store.syncStore().appStateKeys().size());

            h.rotation.handleKeyShare(0, List.of(syncKey(keyId, keyData)));
            assertEquals(1, h.store.syncStore().appStateKeys().size(),
                    "dedup keeps the existing entry");
        }

        @Test
        @DisplayName("a shared key without key data does not modify the store")
        void keyWithoutDataIsIgnored() {
            var h = build();
            var keyId = SyncKeyUtils.buildKeyId(1, 1);
            var keyWithoutData = new AppStateSyncKeyBuilder()
                    .keyId(new AppStateSyncKeyIdBuilder().keyId(keyId).build())
                    .build();
            h.rotation.handleKeyShare(0, List.of(keyWithoutData));
            assertTrue(h.store.syncStore().appStateKeys().isEmpty(),
                    "negative response must not be stored as if it were a positive one");
        }

        @Test
        @DisplayName("receiving a previously-missing key removes the tracker entry")
        void resolvesMissingKey() {
            var h = build();
            var keyId = SyncKeyUtils.buildKeyId(2, 5);
            var keyData = filled(32, 0x55);

            h.store.syncStore().addMissingSyncKey(new MissingDeviceSyncKeyBuilder()
                    .keyId(keyId)
                    .timestamp(Instant.now())
                    .askedDevices(Set.of(0))
                    .build());
            assertTrue(h.store.syncStore().findMissingSyncKey(keyId).isPresent(),
                    "precondition: key tracked as missing");

            h.rotation.handleKeyShare(0, List.of(syncKey(keyId, keyData)));

            assertFalse(h.store.syncStore().findMissingSyncKey(keyId).isPresent(),
                    "missing-key tracker must be cleared once the key arrives");
            assertTrue(h.store.syncStore().findWebAppStateKeyById(keyId).isPresent(),
                    "key must be in the active store after the share");
        }

        @Test
        @DisplayName("a batched share with multiple keys stores all of them")
        void batchedShareStoresAll() {
            var h = build();
            var keyA = syncKey(SyncKeyUtils.buildKeyId(1, 1), filled(32, 0x11));
            var keyB = syncKey(SyncKeyUtils.buildKeyId(2, 1), filled(32, 0x22));
            var keyC = syncKey(SyncKeyUtils.buildKeyId(3, 1), filled(32, 0x33));
            h.rotation.handleKeyShare(0, List.of(keyA, keyB, keyC));
            assertEquals(3, h.store.syncStore().appStateKeys().size());
        }

        @Test
        @DisplayName("a share carrying keys without key id is ignored without throwing")
        void absentKeyIdSkipped() {
            var h = build();
            var malformed = new AppStateSyncKeyBuilder()
                    .keyData(new AppStateSyncKeyDataBuilder().keyData(filled(32, 0x00)).build())
                    .build();
            h.rotation.handleKeyShare(0, List.of(malformed));
            assertTrue(h.store.syncStore().appStateKeys().isEmpty(),
                    "share with absent key id must be a no-op, not throw");
        }
    }

    @Nested
    @DisplayName("getNewestKeyPair - delegates to SyncKeyUtils.findNewestKey")
    class GetNewestKeyPair {
        @Test
        @DisplayName("empty key store returns null")
        void emptyReturnsNull() {
            var h = build();
            assertNull(h.rotation.getNewestKeyPair());
        }

        @Test
        @DisplayName("single key is returned regardless of epoch")
        void singleKey() {
            var h = build();
            h.store.syncStore().addWebAppStateKeys(List.of(
                    syncKey(SyncKeyUtils.buildKeyId(1, 7), filled(32, 0x42))));
            var newest = h.rotation.getNewestKeyPair();
            assertNotNull(newest);
            assertEquals(7, SyncKeyUtils.getKeyEpoch(newest));
        }

        @Test
        @DisplayName("highest-epoch key wins across multiple")
        void highestEpochWins() {
            var h = build();
            h.store.syncStore().addWebAppStateKeys(List.of(
                    syncKey(SyncKeyUtils.buildKeyId(1, 1), filled(32, 0x11)),
                    syncKey(SyncKeyUtils.buildKeyId(1, 5), filled(32, 0x55)),
                    syncKey(SyncKeyUtils.buildKeyId(1, 3), filled(32, 0x33))));
            assertEquals(5, SyncKeyUtils.getKeyEpoch(h.rotation.getNewestKeyPair()));
        }
    }

    @Nested
    @DisplayName("getActiveKey")
    class GetActiveKey {
        @Test
        @DisplayName("throws IllegalStateException when no keys are present")
        void throwsOnEmpty() {
            var h = build();
            assertThrows(IllegalStateException.class, () -> h.rotation.getActiveKey(true));
            assertThrows(IllegalStateException.class, () -> h.rotation.getActiveKey(false));
        }

        @Test
        @DisplayName("returns the existing newest key when rotation is suppressed")
        void rotationSuppressedReturnsExisting() {
            var h = build();
            h.store.syncStore().addWebAppStateKeys(List.of(
                    syncKey(SyncKeyUtils.buildKeyId(1, 7), filled(32, 0x42))));
            var active = h.rotation.getActiveKey(false);
            assertNotNull(active);
            assertEquals(7, SyncKeyUtils.getKeyEpoch(active));
        }
    }

    @Nested
    @DisplayName("ensureActiveKey - read-through to getActiveKey")
    class EnsureActiveKey {
        @Test
        @DisplayName("ensureActiveKey(false) on an empty store throws (delegation contract)")
        void throwsOnEmpty() {
            var h = build();
            assertThrows(IllegalStateException.class, () -> h.rotation.ensureActiveKey(false));
        }

        @Test
        @DisplayName("ensureActiveKey(false) is a no-op when a usable key already exists")
        void noopWhenKeyExists() {
            var h = build();
            h.store.syncStore().addWebAppStateKeys(List.of(
                    syncKey(SyncKeyUtils.buildKeyId(1, 1), filled(32, 0x33))));
            h.rotation.ensureActiveKey(false);
            assertEquals(1, h.store.syncStore().appStateKeys().size(),
                    "no rotation requested -> no new key generated");
        }
    }

    @Nested
    @DisplayName("logMissingKeysReceived - gated on critical bootstrap state")
    class LogMissingKeysReceived {
        // Emitted WAM event contents are covered by the WAM package tests; this only
        // smoke-asserts that the bootstrap-state gate routes through cleanly.
        @Test
        @DisplayName("invoking with an unbootstrapped critical_block collection does not throw")
        void smokeRun() {
            var h = build();
            h.rotation.logMissingKeysReceived();
        }
    }
}
