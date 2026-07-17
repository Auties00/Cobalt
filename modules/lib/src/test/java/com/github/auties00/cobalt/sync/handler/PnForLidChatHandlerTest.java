package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.PnForLidChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.PnForLidChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers {@link PnForLidChatHandler}: the {@link ABProp#PNH_PN_FOR_LID_CHAT_SYNC} gate,
 * the {@link SyncdOperation#SET} path that registers a bidirectional
 * {@code phoneJid}/{@code lidJid} mapping on the store, the malformed classifications when
 * the index JID is empty or non-LID or when the action payload or its
 * {@link PnForLidChatAction#pnJid()} is missing, the {@link SyncActionState#UNSUPPORTED}
 * classification for {@link SyncdOperation#REMOVE}, and the default timestamp-based
 * conflict resolution. The AB prop is opted in by default in the fixture so happy-path
 * tests do not repeat the gate call.
 */
@DisplayName("PnForLidChatHandler")
class PnForLidChatHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid CONTACT_LID = Jid.of("70000000000000@lid");
    private static final Jid CONTACT_PN = Jid.of("33330000@s.whatsapp.net");

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private LinkedWhatsAppClient client;
    private PnForLidChatHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder()
                .with(ABProp.PNH_PN_FOR_LID_CHAT_SYNC, true)
                .build();
        client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
        handler = new PnForLidChatHandler(props);
    }

    // Passing action == null omits the pnForLidChatAction sub-message so the malformed-value branch can be exercised.
    private DecryptedMutation.Trusted build(Jid lidJid, PnForLidChatAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) valueBuilder.pnForLidChatAction(action);
        var index = JSON.toJSONString(List.of("pnForLidChat", lidJid.toString()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata â€” wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the PnForLidChatAction wire constant")
        void actionName() {
            assertEquals(PnForLidChatAction.ACTION_NAME, handler.actionName());
            assertEquals("pnForLidChat", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns the PnForLidChatAction collection")
        void collectionName() {
            assertEquals(PnForLidChatAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared PnForLidChatAction version")
        void version() {
            assertEquals(PnForLidChatAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("AB-prop gating â€” pnh_pn_for_lid_chat_sync")
    class AbPropGating {
        @Test
        @DisplayName("when the prop is off, the mutation returns UNSUPPORTED")
        void propOffReturnsUnsupported() {
            props.set(ABProp.PNH_PN_FOR_LID_CHAT_SYNC, false);
            var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();

            var result = handler.applyMutation(client, build(CONTACT_LID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("registers the bidirectional phoneJid <-> lidJid mapping on the store")
        void registersMapping() {
            var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();

            var result = handler.applyMutation(client, build(CONTACT_LID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals(CONTACT_PN, store.contactStore().findPhoneByLid(CONTACT_LID).orElseThrow(),
                    "WAWebPnForLidChatSync.applyMutations must persist the LID -> PN mapping");
        }
    }

    @Nested
    @DisplayName("applyMutation â€” orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("the handler writes to the LID-mapping table directly â€” no orphan path")
        void noOrphanPath() {
            var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();
            var result = handler.applyMutation(client, build(CONTACT_LID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(result.isOrphan());
            assertNull(result.modelId());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a value carrying the wrong action returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of("pnForLidChat", CONTACT_LID.toString()));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a pnForLidChatAction with no pnJid returns MALFORMED")
        void missingPnJid() {
            var action = new PnForLidChatActionBuilder().build();

            var result = handler.applyMutation(client, build(CONTACT_LID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an empty lidJid slot returns MALFORMED")
        void emptyLidJid() {
            var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).pnForLidChatAction(action).build();
            var index = JSON.toJSONString(List.of("pnForLidChat", ""));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a non-LID JID in the index returns MALFORMED")
        void nonLidJid() {
            var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).pnForLidChatAction(action).build();
            // The index slot expects a @lid JID; a phone-number JID is rejected.
            var index = JSON.toJSONString(List.of("pnForLidChat", CONTACT_PN.toString()));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a short index missing the lidJid slot returns MALFORMED")
        void shortIndex() {
            var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).pnForLidChatAction(action).build();
            var index = JSON.toJSONString(List.of("pnForLidChat"));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED")
        void removeUnsupported() {
            var action = new PnForLidChatActionBuilder().pnJid(CONTACT_PN).build();

            var result = handler.applyMutation(client, build(CONTACT_LID, action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts â€” default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote â†’ APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = build(CONTACT_LID, action(CONTACT_PN), SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(CONTACT_LID, action(CONTACT_PN), SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("equal timestamps â†’ APPLY_REMOTE_DROP_LOCAL (remote wins on tie)")
        void equalTiesGoToRemote() {
            var ts = Instant.ofEpochSecond(1_500);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(
                            build(CONTACT_LID, action(CONTACT_PN), SyncdOperation.SET, ts),
                            build(CONTACT_LID, action(CONTACT_PN), SyncdOperation.SET, ts)).state());
        }

        @Test
        @DisplayName("older remote â†’ SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = build(CONTACT_LID, action(CONTACT_PN), SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(CONTACT_LID, action(CONTACT_PN), SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private PnForLidChatAction action(Jid pnJid) {
            return new PnForLidChatActionBuilder().pnJid(pnJid).build();
        }
    }

}
