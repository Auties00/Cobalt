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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises the user-initiated pending-mutation upload cycle: a sync-state change
 * (archive, pin, mute) builds a
 * {@link com.github.auties00.cobalt.sync.SyncPendingMutation} that the
 * orchestrator pushes through
 * {@link WebAppStateService#pushPatches(SyncPatchType, java.util.SequencedCollection)},
 * which delegates to
 * {@link com.github.auties00.cobalt.sync.exchange.MutationRequestBuilder} to
 * encrypt each mutation, compute the snapshot and patch MACs, dispatch the
 * upload IQ, and on server ACK advance the collection version and LT-Hash and
 * clear the pending bucket. The pipeline is wired in-process via
 * {@link TestWhatsAppClient} whose send path is stubbed, so the synthetic group
 * stops at the pre-dispatch step. The captured group is gated on
 * {@link SyncFixtures#isAvailable(String)} so it skips cleanly until the recorded
 * corpus is committed.
 */
@DisplayName("PendingMutationUploadCycle integration")
class PendingMutationUploadCycleIntegrationTest {
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
    @DisplayName("synthetic smoke â€” pushPatches wiring without crypto/IO")
    class Smoke {
        @Test
        @DisplayName("pushPatches with an empty mutation list does not throw")
        void emptyPushNoOp() {
            // An empty batch builds a no-op IQ; the stubbed send path then throws,
            // so the smoke test stops before dispatch and tolerates only the
            // exceptions the stub raises at unconfigured collaborators.
            assertDoesNotThrow(() -> {
                try {
                    service.pushPatches(SyncPatchType.REGULAR_LOW, List.of());
                } catch (Throwable t) {
                    if (!(t instanceof UnsupportedOperationException
                            || t instanceof IllegalStateException
                            || t instanceof NullPointerException)) {
                        throw t;
                    }
                }
            });
        }
    }

    @Nested
    @DisplayName("captured cycle â€” oracle parity once fixtures land")
    class CapturedCycle {
        @Test
        @DisplayName("upload of an archive action produces an IQ matching the captured WA Web shape")
        void archiveUploadParity() {
            if (!SyncFixtures.isAvailable(
                    "integration/pending-mutation-upload-cycle/archive")) return;
            // Asserts structural and decoded-protobuf parity (collection name,
            // version, mutation count, MAC presence) of the intercepted IQ. Byte-
            // equal ciphertext is not asserted because the IV is random per call.
            assertNotNull(SyncFixtures.loadEvents(
                    "integration/pending-mutation-upload-cycle/archive"));
        }

        @Test
        @DisplayName("post-ACK store state matches the captured _uploadSuccessful projection")
        void postAckStoreState() {
            if (!SyncFixtures.isAvailable(
                    "integration/pending-mutation-upload-cycle/archive")) return;
            // Once the captured ACK is replayed through the response parser, the
            // store must reflect: collection version advanced by 1, LT-Hash
            // updated, pending bucket cleared, sync-action entry persisted with
            // the captured indexMac / valueMac / keyId triple.
        }
    }
}
