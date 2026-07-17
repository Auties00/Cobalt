package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshotRecovery;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshotRecoveryBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the synchronous, store-observable gating of {@link SnapshotRecoveryService}: the AB-prop
 * combination in {@link SnapshotRecoveryService#isRecoveryEnabled()}, the collection plus
 * mutation-count gates in {@link SnapshotRecoveryService#shouldAttemptRecovery}, the store-flag
 * flip in {@link SnapshotRecoveryService#updatePrimaryDeviceSupportsSyncdRecovery}, and the
 * no-pending-future no-op of {@link SnapshotRecoveryService#resolveRecovery}. Each test builds a
 * temporary store via {@link DeviceFixtures#temporaryStore(Jid, Jid)} plus a
 * {@link TestABPropsService} so any single gate can be flipped without booting the syncd stack; the
 * full recovery request/response cycle needs a network round-trip and is out of scope.
 */
@DisplayName("SnapshotRecoveryService")
class SnapshotRecoveryServiceTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;

    private TestABPropsService props;

    private SnapshotRecoveryService recovery;

    @BeforeEach
    void setUp() {
        props = TestABPropsService.builder().build();
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wam = new LiveWamService(client, props);
        recovery = new LiveSnapshotRecoveryService(client, props, wam);
    }

    @Nested
    @DisplayName("isRecoveryEnabled -- gated on both primary support + AB prop")
    class IsRecoveryEnabled {
        @Test
        @DisplayName("primary unsupported is disabled regardless of AB prop")
        void primaryUnsupportedDisabled() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(false);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, true);
            assertFalse(recovery.isRecoveryEnabled());
        }

        @Test
        @DisplayName("AB prop disabled is disabled regardless of primary support")
        void abPropDisabled() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(true);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, false);
            assertFalse(recovery.isRecoveryEnabled());
        }

        @Test
        @DisplayName("both conditions true enables recovery")
        void bothTrueEnables() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(true);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, true);
            assertTrue(recovery.isRecoveryEnabled());
        }
    }

    @Nested
    @DisplayName("shouldAttemptRecovery -- composes isRecoveryEnabled with collection + mutation gates")
    class ShouldAttemptRecovery {
        @BeforeEach
        void enableRecovery() {
            store.syncStore().setPrimaryDeviceSupportsSyncdRecovery(true);
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, true);
            props.set(ABProp.SNAPSHOT_RECOVERY_MAX_MUTATIONS_COUNT_ALLOWED, 100);
        }

        @Test
        @DisplayName("CRITICAL_BLOCK collection is never recovered (returns false)")
        void criticalBlockExcluded() {
            assertFalse(recovery.shouldAttemptRecovery(SyncPatchType.CRITICAL_BLOCK, 0));
        }

        @Test
        @DisplayName("non-critical collection within mutation budget returns true")
        void withinBudgetReturnsTrue() {
            assertTrue(recovery.shouldAttemptRecovery(SyncPatchType.REGULAR, 50));
        }

        @Test
        @DisplayName("non-critical collection at the boundary mutation count returns true")
        void atBoundaryReturnsTrue() {
            assertTrue(recovery.shouldAttemptRecovery(SyncPatchType.REGULAR, 100),
                    "WAWebSyncdSnapshotRecoveryGatingUtils.d: n > a is strict, equal allowed");
        }

        @Test
        @DisplayName("non-critical collection past the mutation budget returns false")
        void pastBudgetReturnsFalse() {
            assertFalse(recovery.shouldAttemptRecovery(SyncPatchType.REGULAR, 101));
        }

        @Test
        @DisplayName("disabled recovery returns false regardless of mutation count")
        void disabledRecoveryShortCircuits() {
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, false);
            assertFalse(recovery.shouldAttemptRecovery(SyncPatchType.REGULAR, 0));
        }
    }

    @Nested
    @DisplayName("updatePrimaryDeviceSupportsSyncdRecovery -- flips the store flag")
    class UpdatePrimarySupport {
        @Test
        @DisplayName("setting to true is observable through isRecoveryEnabled (with AB prop on)")
        void flipsObservably() {
            props.set(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY, true);
            recovery.updatePrimaryDeviceSupportsSyncdRecovery(false);
            assertFalse(recovery.isRecoveryEnabled());
            recovery.updatePrimaryDeviceSupportsSyncdRecovery(true);
            assertTrue(recovery.isRecoveryEnabled());
        }
    }

    @Nested
    @DisplayName("resolveRecovery -- no pending request is a no-op")
    class ResolveRecovery {
        @Test
        @DisplayName("resolving a collection with no pending request does not throw")
        void noPendingNoop() {
            var snapshot = new SyncdSnapshotRecoveryBuilder().build();
            assertDoesNotThrow(() -> recovery.resolveRecovery(SyncPatchType.REGULAR, snapshot),
                    "WAWebRequestSyncdSnapshotRecovery.resolveRecoveryPromise: missing key logs and returns");
        }

        @Test
        @DisplayName("resolving with null snapshot does not throw at the resolveRecovery layer")
        void nullSnapshotIsAcceptedByResolveLayer() {
            // resolveRecovery is a thin dispatch helper, not a payload validator
            assertDoesNotThrow(() ->
                    recovery.resolveRecovery(SyncPatchType.REGULAR, (SyncdSnapshotRecovery) null));
        }
    }
}
