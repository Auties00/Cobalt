package com.github.auties00.cobalt.sync;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke tests for the synchronous, no-IO wiring and lifecycle surface of
 * {@link WebAppStateService}: the constructor wires every collaborator without contacting the
 * network, the empty-argument pull short-circuits, start/stop of the periodic jobs is idempotent,
 * the orphan-mutation hooks accept empty input, and {@link WebAppStateService#reset()} closes
 * cleanly. The full sync round-trip (IQ build, dispatch, patch receive, decrypt, apply, ack) needs
 * a live connection and is out of scope. The service runs against {@link TestWhatsAppClient} and an
 * in-memory {@link LinkedWhatsAppStore} from {@link DeviceFixtures#temporaryStore(Jid, Jid)} so every test
 * runs without IO; the named JIDs are fictitious and carry no provenance from a real session. The
 * own JID is pinned via {@link LinkedWhatsAppAccountStore#setJid(Jid)} so paths that consult {@code store.accountStore().jid()}
 * do not face an absent identity.
 */
@DisplayName("WebAppStateService")
class WebAppStateServiceTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private TestWhatsAppClient client;

    private LinkedWhatsAppStore store;

    private TestABPropsService props;

    private WebAppStateService service;

    @BeforeEach
    void setUp() {
        props = TestABPropsService.builder().build();
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        client = TestWhatsAppClient.create().withStore(store);
        var wam = new LiveWamService(client, props);
        var lidMigration = new LiveLidMigrationService(client, props, wam);
        var snapshotRecovery = new LiveSnapshotRecoveryService(client, props, wam);
        service = new LiveWebAppStateService(client, props, lidMigration, snapshotRecovery, wam, TestMediaConnectionService.create());
    }

    @AfterEach
    void tearDown() {
        assertDoesNotThrow(service::stopPeriodicSyncJob,
                "stopPeriodicSyncJob must be safe even when never started");
    }

    @Nested
    @DisplayName("wiring - constructor produces a usable service")
    class ConstructorWiring {
        @Test
        @DisplayName("service exposes the internally-built SyncKeyRotationService")
        void exposesRotationService() {
            assertNotNull(service.syncKeyRotationService(),
                    "SyncKeyRotationService is constructed internally and must be reachable");
        }
    }

    @Nested
    @DisplayName("pullPatches - empty argument is a no-op")
    class PullPatches {
        @Test
        @DisplayName("pullPatches() with no patch types returns false")
        void emptyPull() {
            assertFalse(service.pullPatches(),
                    "pullPatches with an empty argument set must short-circuit (returns false)");
        }
    }

    @Nested
    @DisplayName("periodic sync job - start/stop idempotent")
    class PeriodicSyncJob {
        @Test
        @DisplayName("startPeriodicSyncJob then stopPeriodicSyncJob does not throw")
        void startStop() {
            assertDoesNotThrow(service::startPeriodicSyncJob);
            assertDoesNotThrow(service::stopPeriodicSyncJob);
        }

        @Test
        @DisplayName("a second start is a no-op")
        void startTwiceIsNoop() {
            service.startPeriodicSyncJob();
            assertDoesNotThrow(service::startPeriodicSyncJob,
                    "the periodic job is single-instance per service");
            service.stopPeriodicSyncJob();
        }
    }

    @Nested
    @DisplayName("orphan-mutation hooks - empty input is a no-op")
    class OrphanHooks {
        @Test
        @DisplayName("checkOrphanMessages with empty input does not throw")
        void emptyMessages() {
            assertDoesNotThrow(() -> service.checkOrphanMessages(List.of()));
        }

        @Test
        @DisplayName("checkOrphanChats with empty input does not throw")
        void emptyChats() {
            assertDoesNotThrow(() -> service.checkOrphanChats(List.of()));
        }

        @Test
        @DisplayName("checkOrphanThreads with empty input does not throw")
        void emptyThreads() {
            assertDoesNotThrow(() -> service.checkOrphanThreads(List.of()));
        }

        @Test
        @DisplayName("checkOrphanAgents with empty input does not throw")
        void emptyAgents() {
            assertDoesNotThrow(() -> service.checkOrphanAgents(List.of()));
        }

        @Test
        @DisplayName("checkOrphanChatAssignments with empty input does not throw")
        void emptyChatAssignments() {
            assertDoesNotThrow(() -> service.checkOrphanChatAssignments(List.of()));
        }

        @Test
        @DisplayName("checkOrphanUserStatusMutes with empty input does not throw")
        void emptyUserStatusMutes() {
            assertDoesNotThrow(() -> service.checkOrphanUserStatusMutes(List.of()));
        }

        @Test
        @DisplayName("checkOrphanMutations dispatches each id-bucket to its specialised hook")
        void aggregatedDispatch() {
            assertDoesNotThrow(() ->
                    service.checkOrphanMutations(List.of(), List.of(), List.of()));
        }
    }

    @Nested
    @DisplayName("missing-sync-key follow-ups - scheduling hooks")
    class MissingKeyHooks {
        @Test
        @DisplayName("scheduleAllDevicesRespondedCheck does not throw")
        void scheduleAllDevices() {
            assertDoesNotThrow(service::scheduleAllDevicesRespondedCheck);
        }

        @Test
        @DisplayName("rescheduleMissingSyncKeyTimeout does not throw")
        void rescheduleTimeout() {
            assertDoesNotThrow(service::rescheduleMissingSyncKeyTimeout);
        }
    }

    @Nested
    @DisplayName("reset - clears service state")
    class Reset {
        @Test
        @DisplayName("reset does not throw on a freshly-constructed service")
        void resetFreshly() {
            assertDoesNotThrow(service::reset);
        }

        @Test
        @DisplayName("reset after starting the periodic job is safe")
        void resetAfterStart() {
            service.startPeriodicSyncJob();
            assertDoesNotThrow(service::reset);
        }
    }

    @Nested
    @DisplayName("syncBlockedCollections - no-op when nothing is blocked")
    class SyncBlockedCollections {
        @Test
        @DisplayName("with a fresh store (nothing blocked), syncBlockedCollections is a no-op")
        void freshStoreNoOp() {
            assertDoesNotThrow(service::syncBlockedCollections);
        }
    }
}
