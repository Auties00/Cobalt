package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppLidMigrationException;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayloadBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link LidMigrationService} state machine: the transition contract for every entry point
 * ({@code initialize}, {@code enableMigration}, {@code disableMigration}, {@code processProtocolMessage},
 * {@code reset}) plus the timeout-future lifecycle. State is observed through the package-private
 * {@link LidMigrationService#state()} test seam so the production API need not leak the
 * {@link LidMigrationState} enum.
 */
@DisplayName("LiveLidMigrationService state machine")
class LidMigrationServiceStateMachineTest {

    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");

    private record Harness(TestWhatsAppClient client, TestABPropsService props, LiveLidMigrationService service) {}

    private static Harness build(TestABPropsService props) {
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);
        return new Harness(client, props, service);
    }

    private static Harness build() {
        return build(TestABPropsService.builder().build());
    }

    @Test
    @DisplayName("initial state is NOT_STARTED")
    void initialState() {
        var h = build();
        assertEquals(LidMigrationState.NOT_STARTED, h.service.state());
        assertFalse(h.service.isLidMigrated());
    }

    @Test
    @DisplayName("initialize advances NOT_STARTED -> WAITING_PROP")
    void initializeFromNotStarted() {
        var h = build();
        h.service.initialize();
        assertEquals(LidMigrationState.WAITING_PROP, h.service.state());
    }

    @Test
    @DisplayName("initialize is idempotent: already-WAITING_PROP stays WAITING_PROP")
    void initializeIdempotent() {
        var h = build();
        h.service.initialize();
        h.service.initialize();
        assertEquals(LidMigrationState.WAITING_PROP, h.service.state());
    }

    @Test
    @DisplayName("initialize is a no-op from non-NOT_STARTED states")
    void initializeNoOpAfterAdvance() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .build();
        var h = build(props);
        h.service.initialize();
        h.service.enableMigration();
        // Now in WAITING_MAPPINGS; initialize must not regress.
        h.service.initialize();
        assertEquals(LidMigrationState.WAITING_MAPPINGS, h.service.state());
    }

    @Test
    @DisplayName("enableMigration advances WAITING_PROP -> WAITING_MAPPINGS")
    void enableMigrationFromWaitingProp() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .build();
        var h = build(props);
        h.service.initialize();
        h.service.enableMigration();
        assertEquals(LidMigrationState.WAITING_MAPPINGS, h.service.state());
    }

    @Test
    @DisplayName("enableMigration is a no-op from NOT_STARTED (initialize was never called)")
    void enableMigrationNoOpBeforeInitialize() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .build();
        var h = build(props);
        h.service.enableMigration();
        assertEquals(LidMigrationState.NOT_STARTED, h.service.state());
    }

    @Test
    @DisplayName("enableMigration with timeout=0 does not arm a timeout (manual sleep would not fire)")
    void enableMigrationTimeoutZeroSkipsScheduling() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .build();
        var h = build(props);
        h.service.initialize();
        h.service.enableMigration();
        // State remains WAITING_MAPPINGS; no timeout fires.
        assertEquals(LidMigrationState.WAITING_MAPPINGS, h.service.state());
        assertTrue(h.client.failures().isEmpty(), "no failure expected without timeout");
    }

    @Test
    @DisplayName("enableMigration with timeout>0 + waiting longer than timeout fires FAILED")
    void enableMigrationTimeoutFiresFailure() throws InterruptedException {
        // 1-second timeout so we can wait it out deterministically.
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 1L)
                .build();
        var h = build(props);
        h.service.initialize();
        h.service.enableMigration();

        // Wait for the scheduled task to fire.
        Thread.sleep(1500);

        assertEquals(LidMigrationState.FAILED, h.service.state());
        var failures = h.client.failures();
        assertEquals(1, failures.size(), "exactly one failure surfaced through the error handler");
        assertInstanceOf(WhatsAppLidMigrationException.FailedToParseMappings.class, failures.getFirst(),
                "timeout surfaces as FailedToParseMappings (matches Cobalt's deliberate choice; WA Web uses PeerMappingsNotReceived but Cobalt re-uses the parse-failed branch)");
    }

    @Test
    @DisplayName("enableMigration timeout does not fire when payload arrives in time")
    void enableMigrationTimeoutCancelledByPayload() throws InterruptedException {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 1L)
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE, true)
                .build();
        var h = build(props);
        h.service.initialize();
        h.service.enableMigration();

        // Deliver an empty payload (accepted by WA Web's parser, advances state to READY then COMPLETE via auto-start).
        var payload = new LIDMigrationMappingSyncPayloadBuilder()
                .pnToLidMappings(List.of())
                .build();
        h.service.processProtocolMessage(payload);

        // Wait past the original timeout window.
        Thread.sleep(1500);

        // State must NOT be FAILED; the timeout future was cancelled when the payload arrived.
        assertFalse(h.service.state() == LidMigrationState.FAILED,
                "payload arriving before timeout cancels the future");
        assertTrue(h.client.failures().isEmpty(), "no failure expected when payload arrives in time");
    }

    @Test
    @DisplayName("disableMigration advances WAITING_PROP -> DISABLED")
    void disableMigrationFromWaitingProp() {
        var h = build();
        h.service.initialize();
        h.service.disableMigration();
        assertEquals(LidMigrationState.DISABLED, h.service.state());
        assertTrue(h.service.state().isTerminal());
    }

    @Test
    @DisplayName("disableMigration is a no-op from NOT_STARTED")
    void disableMigrationNoOpFromNotStarted() {
        var h = build();
        h.service.disableMigration();
        // disableMigration uses compareAndSet(WAITING_PROP, DISABLED) so NOT_STARTED stays put.
        assertEquals(LidMigrationState.NOT_STARTED, h.service.state());
    }

    @Test
    @DisplayName("reset returns non-terminal states to NOT_STARTED")
    void resetFromWaitingProp() {
        var h = build();
        h.service.initialize();
        h.service.reset();
        assertEquals(LidMigrationState.NOT_STARTED, h.service.state());
    }

    @Test
    @DisplayName("reset preserves COMPLETE")
    void resetPreservesComplete() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE, true)
                .build();
        var h = build(props);
        h.service.initialize();
        h.service.enableMigration();
        // Empty payload -> state goes READY -> auto-start -> COMPLETE.
        var payload = new LIDMigrationMappingSyncPayloadBuilder()
                .pnToLidMappings(List.of())
                .build();
        h.service.processProtocolMessage(payload);
        assertEquals(LidMigrationState.COMPLETE, h.service.state());

        h.service.reset();
        assertEquals(LidMigrationState.COMPLETE, h.service.state(),
                "terminal COMPLETE is preserved across reset");
    }

    @Test
    @DisplayName("reset preserves DISABLED")
    void resetPreservesDisabled() {
        var h = build();
        h.service.initialize();
        h.service.disableMigration();
        h.service.reset();
        assertEquals(LidMigrationState.DISABLED, h.service.state());
    }

    @Test
    @DisplayName("reset preserves FAILED")
    void resetPreservesFailed() throws InterruptedException {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 1L)
                .build();
        var h = build(props);
        h.service.initialize();
        h.service.enableMigration();
        Thread.sleep(1500);
        assertEquals(LidMigrationState.FAILED, h.service.state());

        h.service.reset();
        assertEquals(LidMigrationState.FAILED, h.service.state(),
                "terminal FAILED is preserved across reset");
    }

    @Test
    @DisplayName("isLidMigrated is true only in COMPLETE")
    void isLidMigratedOnlyInComplete() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE, true)
                .build();
        var h = build(props);

        assertFalse(h.service.isLidMigrated(), "NOT_STARTED");

        h.service.initialize();
        assertFalse(h.service.isLidMigrated(), "WAITING_PROP");

        h.service.enableMigration();
        assertFalse(h.service.isLidMigrated(), "WAITING_MAPPINGS");

        var payload = new LIDMigrationMappingSyncPayloadBuilder()
                .pnToLidMappings(List.of())
                .build();
        h.service.processProtocolMessage(payload);
        assertTrue(h.service.isLidMigrated(), "COMPLETE after auto-start over empty mappings");
    }

    @Test
    @DisplayName("isSyncdSessionMigrated is constantly false")
    void isSyncdSessionMigratedConstant() {
        var h = build();
        assertFalse(h.service.isSyncdSessionMigrated());
        h.service.initialize();
        assertFalse(h.service.isSyncdSessionMigrated());
    }

    @Test
    @DisplayName("shouldCreatePnChat is constantly false")
    void shouldCreatePnChatConstant() {
        var h = build();
        assertFalse(h.service.shouldCreatePnChat());
    }

    @Test
    @DisplayName("hasStateDiscrepancy is constantly false")
    void hasStateDiscrepancyConstant() {
        var h = build();
        assertFalse(h.service.hasStateDiscrepancy());
    }
}
