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
import com.github.auties00.cobalt.wire.linked.sync.action.bot.UGCBotAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.UGCBotActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link UGCBotHandler}: applying an incoming {@code ugc_bot}
 * mutation and asserting the {@link UGCBotAction#definition()} bytes
 * persisted on {@link LinkedWhatsAppBusinessStore#setUserCreatedBotDefinition(byte[])}.
 * Each test instantiates a fresh handler against a fresh store, so the
 * user-created bot definition starts unset.
 */
@DisplayName("UGCBotHandler")
class UGCBotHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private LinkedWhatsAppClient client;
    private UGCBotHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new UGCBotHandler();
    }

    // A null action omits the payload to drive the missing-action malformed branch.
    private DecryptedMutation.Trusted build(UGCBotAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.ugcBotAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'ugc_bot'")
        void actionName() {
            assertEquals(UGCBotAction.ACTION_NAME, handler.actionName());
            assertEquals("ugc_bot", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_HIGH")
        void collectionName() {
            assertEquals(UGCBotAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR_HIGH, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(UGCBotAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET persists the bot definition blob")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with a non-empty definition is persisted as-is on the store")
        void setsDefinition() {
            assertTrue(store.businessStore().userCreatedBotDefinition().isEmpty(), "precondition: definition is unset");
            var blob = new byte[]{0x01, 0x02, 0x03};
            var action = new UGCBotActionBuilder().definition(blob).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertArrayEquals(blob, store.businessStore().userCreatedBotDefinition().orElseThrow());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SET with the wrong action type returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"ugc_bot\"]", value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a SET whose definition is unset returns MALFORMED")
        void emptyDefinition() {
            var action = new UGCBotActionBuilder().build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE is UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED without touching the store")
        void removeIsUnsupported() {
            var action = new UGCBotActionBuilder().definition(new byte[]{0x01}).build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.businessStore().userCreatedBotDefinition().isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ConflictResolution {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var action = new UGCBotActionBuilder().definition(new byte[]{0x01}).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var action = new UGCBotActionBuilder().definition(new byte[]{0x01}).build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
