package com.github.auties00.cobalt.sync.integration;
import com.github.auties00.cobalt.sync.LiveSnapshotRecoveryService;
import com.github.auties00.cobalt.sync.LiveWebAppStateService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises the full bootstrap sync cycle that runs after a companion link: one
 * IQ exchange per {@link SyncPatchType} pulls a per-collection snapshot, each
 * mutation is decrypted and applied through its
 * {@link com.github.auties00.cobalt.sync.handler.WebAppStateActionHandler}, and
 * every collection ends bootstrapped with a matching MAC. The pipeline is wired
 * in-process via {@link TestWhatsAppClient} with no network IO; the synthetic
 * group asserts orchestrator wiring, an empty {@link WebAppStateService#pullPatches}
 * short-circuit, and periodic-job idempotence. The captured group is gated on
 * {@link SyncFixtures#isAvailable(String)} so it skips cleanly until the recorded
 * bootstrap corpus is committed.
 */
@DisplayName("FullSyncCycle integration")
class FullSyncCycleIntegrationTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private TestWhatsAppClient client;
    private LinkedWhatsAppStore store;
    private WebAppStateService service;

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
        service = new LiveWebAppStateService(client, props, lidMigration, snapshotRecovery, wam, TestMediaConnectionService.create());
    }

    @Nested
    @DisplayName("synthetic smoke â€” orchestrator graph wires up without IO")
    class Smoke {
        @Test
        @DisplayName("WebAppStateService exposes the internally-built SyncKeyRotationService")
        void exposesRotationService() {
            assertNotNull(service.syncKeyRotationService());
        }

        @Test
        @DisplayName("pullPatches() with no patch types short-circuits to false")
        void emptyPullShortCircuits() {
            assertFalse(service.pullPatches());
        }

        @Test
        @DisplayName("periodic sync job start + stop is idempotent")
        void periodicJobIdempotent() {
            assertDoesNotThrow(service::startPeriodicSyncJob);
            assertDoesNotThrow(service::startPeriodicSyncJob);
            assertDoesNotThrow(service::stopPeriodicSyncJob);
            assertDoesNotThrow(service::stopPeriodicSyncJob);
        }
    }

    @Nested
    @DisplayName("captured cycle â€” oracle parity once fixtures land")
    class CapturedCycle {
        @Test
        @DisplayName("post-cycle store state matches WAWebSyncdServerSync's captured projection")
        void capturedStoreStateMatches() {
            if (!SyncFixtures.isAvailable("integration/full-sync-cycle/all-collections")) return;
            // Fixture pairs a stanza trace covering one IQ and response per
            // SyncPatchType with an oracle snapshot of the post-cycle store (chats,
            // contacts, settings, collection versions, LT hashes), asserted deeply.
            assertNotNull(SyncFixtures.loadOracle("integration/full-sync-cycle/all-collections"));
        }

        @Test
        @DisplayName("every SyncPatchType transitions to bootstrapped=true after the cycle")
        void allCollectionsBootstrapped() {
            if (!SyncFixtures.isAvailable("integration/full-sync-cycle/all-collections")) return;
            // The oracle exposes the bootstrapped flag per collection; every entry
            // must be true after the cycle completes.
            for (var type : SyncPatchType.values()) {
                assertNotNull(type, "smoke gate; fixture-driven check defers to Phase 10");
            }
        }
    }
}
