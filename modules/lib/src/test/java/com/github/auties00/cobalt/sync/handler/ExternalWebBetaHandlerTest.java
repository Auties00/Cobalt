package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.device.ExternalWebBetaAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.ExternalWebBetaActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link ExternalWebBetaHandler} for the {@code external_web_beta}
 * app-state sync action: metadata, the {@link ABProp#EXTERNAL_BETA_CAN_JOIN}
 * gating, the SET happy path, the malformed-value branch and the REMOVE
 * rejection.
 *
 * <p>The handler is built with a stubbed {@link TestABPropsService} so the
 * gating prop can be flipped per test, and runs against a fresh in-memory
 * {@link DeviceFixtures#temporaryStore} through {@link TestWhatsAppClient} so
 * the {@link LinkedWhatsAppSyncStore#externalWebBeta()} read-back can be asserted directly.
 */
@DisplayName("ExternalWebBetaHandler")
class ExternalWebBetaHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private LinkedWhatsAppClient client;
    private ExternalWebBetaHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder()
                .with(ABProp.EXTERNAL_BETA_CAN_JOIN, true) // open the gate by default
                .build();
        client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
        handler = new ExternalWebBetaHandler(props);
    }

    private static DecryptedMutation.Trusted mutation(boolean optIn, SyncdOperation op, Instant ts) {
        var action = new ExternalWebBetaActionBuilder().isOptIn(optIn).build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .externalWebBetaAction(action)
                .build();
        return new DecryptedMutation.Trusted("[\"external_web_beta\"]", value, op, ts, ExternalWebBetaAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the ExternalWebBetaAction wire constant")
        void actionName() {
            assertEquals(ExternalWebBetaAction.ACTION_NAME, handler.actionName());
            assertEquals("external_web_beta", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(ExternalWebBetaAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (3)")
        void version() {
            assertEquals(ExternalWebBetaAction.ACTION_VERSION, handler.version());
            assertEquals(3, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET (gate open)")
    class ApplySetHappy {
        @Test
        @DisplayName("SET isOptIn=true writes the flag and returns SUCCESS")
        void setsTrue() {
            assertFalse(store.syncStore().externalWebBeta(), "precondition: starts false");
            var result = handler.applyMutation(client,
                    mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(1_700_000_000L)));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.syncStore().externalWebBeta());
        }

        @Test
        @DisplayName("SET isOptIn=false flips the flag back to false")
        void setsFalse() {
            store.syncStore().setExternalWebBeta(true);
            var result = handler.applyMutation(client,
                    mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(1_700_000_000L)));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(store.syncStore().externalWebBeta());
        }
    }

    @Nested
    @DisplayName("applyMutation - gate closed -> UNSUPPORTED")
    class GateClosed {
        @Test
        @DisplayName("when external_beta_can_join is false, every mutation returns UNSUPPORTED")
        void gateClosedReturnsUnsupported() {
            props.set(ABProp.EXTERNAL_BETA_CAN_JOIN, false);
            var result = handler.applyMutation(client,
                    mutation(true, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState(),
                    "WAWebExternalWebBetaSync.applyMutations short-circuits the whole batch when the AB-prop gate is closed");
            assertFalse(store.syncStore().externalWebBeta(), "store must remain untouched when the gate is closed");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("external-web-beta is a global flag; no per-entity orphan path")
        void noOrphan() {
            var result = handler.applyMutation(client,
                    mutation(true, SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SyncActionValue carrying a different action returns MALFORMED")
        void wrongActionIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("[\"external_web_beta\"]", value, SyncdOperation.SET, ts, 3);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("the handler ignores the index shape (global setting)")
        void indexShapeIgnored() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .externalWebBetaAction(new ExternalWebBetaActionBuilder().isOptIn(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted("", value, SyncdOperation.SET, ts, 3);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "the handler does not parse the index; the setting is keyed off the action only");
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE returns UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE is unsupported (gate-open path takes the operation check)")
        void removeIsUnsupported() {
            var result = handler.applyMutation(client,
                    mutation(true, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - inherits default sequential apply")
    class ApplyBatch {
        @Test
        @DisplayName("default batch path applies each mutation in order")
        void sequentialApply() {
            var results = handler.applyMutationBatch(client, List.of(
                    mutation(true, SyncdOperation.SET, Instant.ofEpochSecond(1_000)),
                    mutation(false, SyncdOperation.SET, Instant.ofEpochSecond(2_000))
            ));
            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertFalse(store.syncStore().externalWebBeta());
        }
    }

}
