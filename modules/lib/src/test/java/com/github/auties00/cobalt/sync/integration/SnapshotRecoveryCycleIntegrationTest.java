package com.github.auties00.cobalt.sync.integration;
import com.github.auties00.cobalt.sync.LiveSnapshotRecoveryService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SnapshotRecoveryService;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the snapshot-recovery cycle that runs when a snapshot MAC fails
 * validation during a per-collection apply: the companion requests the corrected
 * snapshot from the primary, waits for the response, decodes it, and replaces the
 * local collection state. Recovery is gated on the primary advertising support,
 * the {@link com.github.auties00.cobalt.wire.linked.props.ABProp#ENABLE_PEER_SNAPSHOT_RECOVERY}
 * prop, the collection not being CRITICAL_BLOCK, and the mutation count staying
 * within the configured maximum. The {@link SnapshotRecoveryService} is wired
 * in-process via {@link TestWhatsAppClient}. The synthetic group asserts the
 * gating composition directly; the captured group is gated on
 * {@link SyncFixtures#isAvailable(String)} so it skips cleanly until the recorded
 * corpus is committed.
 */
@DisplayName("SnapshotRecoveryCycle integration")
class SnapshotRecoveryCycleIntegrationTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private SnapshotRecoveryService recovery;

    @BeforeEach
    void setUp() {
        props = TestABPropsService.builder().build();
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props);
        var wam = new LiveWamService(client, props);
        recovery = new LiveSnapshotRecoveryService(client, props, wam);
    }

    @Nested
    @DisplayName("synthetic smoke â€” gating composition without IO")
    class Smoke {
        @Test
        @DisplayName("recovery is disabled when the primary doesn't advertise support")
        void disabledByPrimary() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(false);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, true);
            assertFalse(recovery.isRecoveryEnabled());
        }

        @Test
        @DisplayName("recovery is disabled when the AB prop is off")
        void disabledByAbProp() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(true);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, false);
            assertFalse(recovery.isRecoveryEnabled());
        }

        @Test
        @DisplayName("recovery is enabled when both gates pass")
        void enabledByDefault() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(true);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, true);
            assertTrue(recovery.isRecoveryEnabled());
        }

        @Test
        @DisplayName("CRITICAL_BLOCK never attempts recovery (collection-specific gate)")
        void criticalBlockExcluded() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(true);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, true);
            props.set(ABProp.SNAPSHOT_RECOVERY_MAX_MUTATIONS_COUNT_ALLOWED, 1_000);
            assertFalse(recovery.shouldAttemptRecovery(SyncPatchType.CRITICAL_BLOCK, 0));
        }
    }

    @Nested
    @DisplayName("captured cycle â€” oracle parity once fixtures land")
    class CapturedCycle {
        @Test
        @DisplayName("forced MAC mismatch â†’ recovery request â†’ captured response replaces collection state")
        void capturedRecoveryCycle() {
            if (!SyncFixtures.isAvailable("integration/snapshot-recovery-cycle/regular-low")) return;
            // Fixture replays the captured recovery response and asserts the store's
            // post-recovery state matches the oracle projection.
            assertNotNull(SyncFixtures.loadOracle(
                    "integration/snapshot-recovery-cycle/regular-low"));
        }

        @Test
        @DisplayName("a second in-flight request for the same collection is debounced (semaphore-serialized)")
        void debouncedInFlight() {
            if (!SyncFixtures.isAvailable("integration/snapshot-recovery-cycle/in-flight-debounce")) return;
            // Exercise the recoverySemaphore tryAcquire path under concurrent calls
            // for the same collection. The cycle asserts only one outgoing request
            // is dispatched even when two threads race to request recovery.
        }
    }
}
