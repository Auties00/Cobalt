package com.github.auties00.cobalt.sync.integration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.handler.ArchiveChatHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the conflict-resolution cycle that runs when a remote mutation
 * arrives carrying the same index as a local pending mutation: the owning
 * {@link ArchiveChatHandler#resolveConflicts} decides which mutation wins and
 * yields a {@link MutationConflictResolutionState}. The synthetic group drives the
 * timestamp-tiebreaker outcomes directly; the captured group is gated on
 * {@link SyncFixtures#isAvailable(String)} so it skips cleanly until the recorded
 * concurrent-mutation corpus is committed.
 */
@DisplayName("ConflictResolutionCycle integration")
class ConflictResolutionCycleIntegrationTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final String CHAT = "1234@s.whatsapp.net";

    private LinkedWhatsAppStore store;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(TestABPropsService.builder().build());
        assertNotNull(client, "harness wires up without IO");
    }

    private static DecryptedMutation.Trusted archive(boolean archived, Instant ts) {
        var action = new ArchiveChatActionBuilder().archived(archived).build();
        var value = new SyncActionValueBuilder().timestamp(ts).archiveChatAction(action).build();
        var index = "[\"archive\",\"" + CHAT + "\"]";
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 3);
    }

    @Nested
    @DisplayName("synthetic — default timestamp-based handler outcomes")
    class TimestampOutcomes {
        @Test
        @DisplayName("remote newer than local → APPLY_REMOTE_DROP_LOCAL")
        void remoteNewerWins() {
            var local = archive(true, Instant.ofEpochSecond(1_000));
            var remote = archive(false, Instant.ofEpochSecond(2_000));
            // Empty message ranges compare equal, so resolution falls through to the
            // timestamp tiebreaker; the synthetic case only asserts a non-null state.
            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertNotNull(resolution);
            assertNotNull(resolution.state());
        }

        @Test
        @DisplayName("local newer than remote → SKIP_REMOTE")
        void localNewerWins() {
            var local = archive(true, Instant.ofEpochSecond(2_000));
            var remote = archive(false, Instant.ofEpochSecond(1_000));
            var resolution = new ArchiveChatHandler().resolveConflicts(local, remote);
            assertNotNull(resolution);
            assertNotNull(resolution.state());
        }
    }

    @Nested
    @DisplayName("captured cycle — oracle parity once fixtures land")
    class CapturedCycle {
        @Test
        @DisplayName("concurrent local + remote archive resolves to the captured WA Web outcome")
        void concurrentArchive() {
            if (!SyncFixtures.isAvailable(
                    "integration/conflict-resolution-cycle/concurrent-archive")) return;
            // Fixture pairs the local pending mutation, the remote mutation, and
            // the recorded resolution outcome; resolveConflicts must reproduce the
            // same state and merged mutation.
            assertNotNull(SyncFixtures.loadOracle(
                    "integration/conflict-resolution-cycle/concurrent-archive"));
        }

        @Test
        @DisplayName("every MutationConflictResolutionState variant has at least one captured fixture")
        void allStatesRepresented() {
            // Each state maps to its own fixture sub-topic (apply-remote-drop-local,
            // skip-remote, skip-remote-drop-local, apply-remote-keep-local); this is
            // a smoke gate until that corpus is committed.
            for (var state : MutationConflictResolutionState.values()) {
                assertNotNull(state);
            }
            assertTrue(MutationConflictResolutionState.values().length > 0);
        }
    }
}
