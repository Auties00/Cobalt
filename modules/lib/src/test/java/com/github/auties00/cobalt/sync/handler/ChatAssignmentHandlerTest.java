package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.business.AgentStateBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatAssignmentBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.ChatAssignmentMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ChatAssignmentHandler} - Cobalt's adapter for
 * {@code WAWebChatAssignmentSync}.
 *
 * <p>The handler assigns customer chats to business agents. SET mutations
 * either install a new {@code ChatAssignment} (when the action carries a
 * non-empty {@code deviceAgentID}) or clear the assignment for the chat
 * (when the id is empty). The chat referenced by {@code indexParts[1]}
 * must exist locally, and a non-empty agent id must refer to a known
 * agent. These tests pin down the wire metadata, the SET behaviour
 * (assign / unassign / orphan chat / orphan agent), the malformed-input
 * fallbacks, the default timestamp-based conflict resolution, and the
 * static outbound-mutation builder.
 */
@DisplayName("ChatAssignmentHandler")
class ChatAssignmentHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid CHAT_JID = Jid.of("12345@s.whatsapp.net");

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private ChatAssignmentHandler handler;
    private ChatAssignmentMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new ChatAssignmentHandler();
        factory = new ChatAssignmentMutationFactory();
    }

    /**
     * Builds a trusted mutation whose value carries the given assignment action.
     *
     * @param indexChatJid the chat JID placed in {@code indexParts[1]}, may be {@code null}
     * @param action       the chat assignment action payload, may be {@code null}
     * @param operation    the sync operation
     * @param ts           the mutation timestamp
     * @return the trusted mutation
     */
    private DecryptedMutation.Trusted buildMutation(String indexChatJid, ChatAssignmentAction action, SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.chatAssignment(action);
        }
        var indexParts = indexChatJid == null ? List.of(handler.actionName()) : List.of(handler.actionName(), indexChatJid);
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the ChatAssignmentAction wire constant")
        void actionName() {
            assertEquals(ChatAssignmentAction.ACTION_NAME, handler.actionName());
            assertEquals("agentChatAssignment", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(ChatAssignmentAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared ChatAssignmentAction version")
        void version() {
            assertEquals(ChatAssignmentAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET assigns the chat to the agent")
    class ApplySetAssign {
        @Test
        @DisplayName("a non-empty agent id installs a new ChatAssignment when the agent + chat exist")
        void installsAssignment() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putAgentState(new AgentStateBuilder().agentId("agent-1").name("A").deviceId(0).deleted(false).build());
            var action = new ChatAssignmentActionBuilder().deviceAgentID("agent-1").build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var assignment = store.businessStore().findChatAssignment(CHAT_JID).orElseThrow();
            assertEquals("agent-1", assignment.agentId().orElseThrow());
        }

        @Test
        @DisplayName("an empty agent id clears the assignment for the chat")
        void emptyAgentIdUnassigns() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putChatAssignment(new ChatAssignmentBuilder()
                    .chatJid(CHAT_JID)
                    .agentId("agent-1")
                    .opened(true)
                    .build());
            var action = new ChatAssignmentActionBuilder().deviceAgentID("").build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findChatAssignment(CHAT_JID).isEmpty(),
                    "an empty agent id must clear the assignment for the chat");
        }

        @Test
        @DisplayName("re-assignment preserves the prior 'opened' flag")
        void reassignmentPreservesOpenedFlag() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putAgentState(new AgentStateBuilder().agentId("agent-2").name("B").deviceId(0).deleted(false).build());
            store.businessStore().putChatAssignment(new ChatAssignmentBuilder()
                    .chatJid(CHAT_JID)
                    .agentId("agent-1")
                    .opened(true)
                    .build());
            var action = new ChatAssignmentActionBuilder().deviceAgentID("agent-2").build();

            handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), action, SyncdOperation.SET, Instant.now()));

            var stored = store.businessStore().findChatAssignment(CHAT_JID).orElseThrow();
            assertEquals("agent-2", stored.agentId().orElseThrow());
            assertTrue(stored.opened(), "the prior 'opened' flag is carried into the new assignment");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan outcomes")
    class Orphans {
        @Test
        @DisplayName("a chat absent locally returns ORPHAN with the chat JID as modelId")
        void orphanChat() {
            store.businessStore().putAgentState(new AgentStateBuilder().agentId("agent-1").name("A").deviceId(0).deleted(false).build());
            var action = new ChatAssignmentActionBuilder().deviceAgentID("agent-1").build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(CHAT_JID.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }

        @Test
        @DisplayName("a non-empty agent id with no matching agent returns ORPHAN with the agent id as modelId")
        void orphanAgent() {
            store.chatStore().addNewChat(CHAT_JID);
            var action = new ChatAssignmentActionBuilder().deviceAgentID("missing-agent").build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("missing-agent", result.modelId());
            assertEquals("Agent", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a value carrying the wrong action returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of(handler.actionName(), CHAT_JID.toString()));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            store.chatStore().addNewChat(CHAT_JID);
            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a missing chat JID slot returns MALFORMED")
        void missingChatJidSlot() {
            var action = new ChatAssignmentActionBuilder().deviceAgentID("agent-1").build();

            var result = handler.applyMutation(client,
                    buildMutation(null, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an empty chat JID at indexParts[1] returns MALFORMED")
        void emptyChatJid() {
            var action = new ChatAssignmentActionBuilder().deviceAgentID("agent-1").build();

            var result = handler.applyMutation(client,
                    buildMutation("", action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED")
        void removeUnsupported() {
            store.chatStore().addNewChat(CHAT_JID);
            var action = new ChatAssignmentActionBuilder().deviceAgentID("agent-1").build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote - APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = mutationAt(Instant.ofEpochSecond(1_000));
            var remote = mutationAt(Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("equal timestamps - APPLY_REMOTE_DROP_LOCAL")
        void equalTimestampApplies() {
            var ts = Instant.ofEpochSecond(1_500);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(mutationAt(ts), mutationAt(ts)).state());
        }

        @Test
        @DisplayName("older remote - SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = mutationAt(Instant.ofEpochSecond(2_000));
            var remote = mutationAt(Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private DecryptedMutation.Trusted mutationAt(Instant ts) {
            var action = new ChatAssignmentActionBuilder().deviceAgentID("agent-1").build();
            return buildMutation(CHAT_JID.toString(), action, SyncdOperation.SET, ts);
        }
    }

}
