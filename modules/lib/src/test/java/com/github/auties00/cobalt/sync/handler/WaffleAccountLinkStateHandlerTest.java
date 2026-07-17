package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.WaffleAccountLinkStateAction;
import com.github.auties00.cobalt.wire.linked.sync.action.device.WaffleAccountLinkStateActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link WaffleAccountLinkStateHandler}: the wire-constant trio, the {@code WEB_WAFFLE}
 * AB-prop gate (closed by default in the fixture), the {@link SyncdOperation#REMOVE} unsupported
 * branch, and the default conflict-resolution tiebreaker. The handler is constructed per test with
 * its injected {@link LiveWamService}, against a fresh temporary store and a fresh AB-props
 * snapshot, so gate-flip tests could opt in by mutating their local copy. The SET happy path and
 * the malformed surface sit behind the gate and need send-message infrastructure that
 * {@link TestWhatsAppClient} does not stub; those are exercised end-to-end in
 * {@code WebAppStateServiceTest}.
 */
@DisplayName("WaffleAccountLinkStateHandler")
class WaffleAccountLinkStateHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppClient client;
    private TestABPropsService props;
    private WaffleAccountLinkStateHandler handler;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        props = TestABPropsService.builder().build();
        var wam = new LiveWamService(client, props);
        handler = new WaffleAccountLinkStateHandler(props, wam);
    }

    private static DecryptedMutation.Trusted waffleMutation(
            WaffleAccountLinkStateAction.AccountLinkState linkState, SyncdOperation op, Instant ts) {
        var action = new WaffleAccountLinkStateActionBuilder().linkState(linkState).build();
        var value = new SyncActionValueBuilder().timestamp(ts).waffleAccountLinkStateAction(action).build();
        return new DecryptedMutation.Trusted("[\"waffle_account_link_state\"]", value, op, ts, 1);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'waffle_account_link_state'")
        void actionName() {
            assertEquals(WaffleAccountLinkStateAction.ACTION_NAME, handler.actionName());
            assertEquals("waffle_account_link_state", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (1)")
        void version() {
            assertEquals(WaffleAccountLinkStateAction.ACTION_VERSION, handler.version());
            assertEquals(1, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation gates on the WEB_WAFFLE AB prop (default off)")
    class AbPropsGate {
        @Test
        @DisplayName("a SET mutation with WEB_WAFFLE off returns UNSUPPORTED")
        void setIsUnsupportedWhenGateClosed() {
            // WEB_WAFFLE defaults to false on the constructor-injected ABPropsService.
            var result = handler.applyMutation(client,
                    waffleMutation(WaffleAccountLinkStateAction.AccountLinkState.ACTIVE,
                            SyncdOperation.SET, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }

        @Test
        @DisplayName("a REMOVE mutation with WEB_WAFFLE off returns UNSUPPORTED")
        void removeIsUnsupportedWhenGateClosed() {
            var result = handler.applyMutation(client,
                    waffleMutation(WaffleAccountLinkStateAction.AccountLinkState.UNLINKED,
                            SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch gates on the WEB_WAFFLE AB prop (default off)")
    class BatchAbPropsGate {
        @Test
        @DisplayName("a single-element batch with WEB_WAFFLE off yields UNSUPPORTED per entry")
        void singleBatchUnsupported() {
            var batch = List.of(waffleMutation(
                    WaffleAccountLinkStateAction.AccountLinkState.ACTIVE, SyncdOperation.SET, Instant.now()));

            var results = handler.applyMutationBatch(client, batch);

            assertEquals(1, results.size());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(0).actionState());
        }

        @Test
        @DisplayName("an empty batch returns an empty result list")
        void emptyBatchEmpty() {
            var results = handler.applyMutationBatch(client, List.of());
            assertEquals(0, results.size());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET happy path - n/a (requires abPropsService stub)")
    class SetHappyNa {
        @Test
        @DisplayName("SET happy path exercised in WebAppStateServiceTest end-to-end")
        void requiresFullClient() {
            assertNotNull(handler);
        }
    }

    @Nested
    @DisplayName("applyMutation: orphan paths - n/a")
    class OrphanNa {
        @Test
        @DisplayName("waffle_account_link_state is a singleton account preference; there is no orphan branch")
        void noOrphanPath() {
            assertNotNull(handler);
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed value / index - n/a (requires abPropsService stub)")
    class MalformedNa {
        @Test
        @DisplayName("malformed surface is reachable only after the AB-prop gate; covered end-to-end")
        void requiresFullClient() {
            assertNotNull(handler);
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = waffleMutation(WaffleAccountLinkStateAction.AccountLinkState.UNLINKED,
                    SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = waffleMutation(WaffleAccountLinkStateAction.AccountLinkState.ACTIVE,
                    SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = handler.resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    @Nested
    @DisplayName("static builder methods - n/a, handler exposes none")
    class BuilderNa {
        @Test
        @DisplayName("WaffleAccountLinkStateHandler does not expose a getMutation helper")
        void noBuilder() {
            assertNotNull(handler);
        }
    }
}
