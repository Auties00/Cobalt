package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.chat.ChatAssignmentBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentOpenedStatusAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ChatAssignmentOpenedStatusActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.ChatAssignmentOpenedStatusMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ChatAssignmentOpenedStatusHandler} - Cobalt's adapter
 * for {@code WAWebChatAssignmentOpenedStatusSync}.
 *
 * <p>The handler updates the {@code opened} flag on an existing
 * {@code ChatAssignment} after the assigned agent opens or closes the
 * chat. SET mutations require both the chat and the matching chat
 * assignment (by agent id) to be present locally; otherwise the mutation
 * is reported as an orphan. These tests pin down the wire metadata, the
 * SET behaviour, the orphan outcomes, the malformed-input fallbacks, the
 * default timestamp-based conflict resolution, and the static
 * outbound-mutation builder.
 */
@DisplayName("ChatAssignmentOpenedStatusHandler")
class ChatAssignmentOpenedStatusHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid CHAT_JID = Jid.of("12345@s.whatsapp.net");
    private static final String AGENT_ID = "agent-1";

    private LinkedWhatsAppStore store;
    private TestWhatsAppClient client;
    private ChatAssignmentOpenedStatusHandler handler;
    private ChatAssignmentOpenedStatusMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new ChatAssignmentOpenedStatusHandler();
        factory = new ChatAssignmentOpenedStatusMutationFactory();
    }

    /**
     * Builds a trusted mutation whose value carries the given opened-status action.
     *
     * @param indexChatJid the chat JID placed in {@code indexParts[1]}, may be {@code null}
     * @param indexAgentId the agent id placed in {@code indexParts[2]}, may be {@code null}
     * @param action       the opened-status action payload, may be {@code null}
     * @param operation    the sync operation
     * @param ts           the mutation timestamp
     * @return the trusted mutation
     */
    private DecryptedMutation.Trusted buildMutation(String indexChatJid, String indexAgentId,
                                                    ChatAssignmentOpenedStatusAction action,
                                                    SyncdOperation operation, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.chatAssignmentOpenedStatus(action);
        }
        List<String> indexParts;
        if (indexChatJid == null && indexAgentId == null) {
            indexParts = List.of(handler.actionName());
        } else if (indexAgentId == null) {
            indexParts = List.of(handler.actionName(), indexChatJid);
        } else {
            indexParts = List.of(handler.actionName(), indexChatJid == null ? "" : indexChatJid, indexAgentId);
        }
        var index = JSON.toJSONString(indexParts);
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), operation, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the wire constant")
        void actionName() {
            assertEquals(ChatAssignmentOpenedStatusAction.ACTION_NAME, handler.actionName());
            assertEquals("agentChatAssignmentOpenedStatus", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(ChatAssignmentOpenedStatusAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(ChatAssignmentOpenedStatusAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET updates the opened flag")
    class ApplySet {
        @Test
        @DisplayName("a SET with chatOpened=true flips the assignment's opened flag")
        void opensTheAssignment() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putChatAssignment(new ChatAssignmentBuilder()
                    .chatJid(CHAT_JID)
                    .agentId(AGENT_ID)
                    .opened(false)
                    .build());
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), AGENT_ID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.businessStore().findChatAssignment(CHAT_JID).orElseThrow().opened());
        }

        @Test
        @DisplayName("a SET with chatOpened=false clears the opened flag")
        void closesTheAssignment() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putChatAssignment(new ChatAssignmentBuilder()
                    .chatJid(CHAT_JID)
                    .agentId(AGENT_ID)
                    .opened(true)
                    .build());
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(false).build();

            handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), AGENT_ID, action, SyncdOperation.SET, Instant.now()));

            assertTrue(!store.businessStore().findChatAssignment(CHAT_JID).orElseThrow().opened());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan outcomes")
    class Orphans {
        @Test
        @DisplayName("a chat absent locally returns ORPHAN with chat metadata")
        void orphanChat() {
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), AGENT_ID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(CHAT_JID.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
        }

        @Test
        @DisplayName("a chat with no matching assignment returns ORPHAN with ChatAssignment metadata")
        void orphanAssignmentAbsent() {
            store.chatStore().addNewChat(CHAT_JID);
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), AGENT_ID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("ChatAssignment", result.modelType());
        }

        @Test
        @DisplayName("an assignment held by a different agent returns ORPHAN")
        void orphanAssignmentDifferentAgent() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putChatAssignment(new ChatAssignmentBuilder()
                    .chatJid(CHAT_JID)
                    .agentId("someone-else")
                    .opened(false)
                    .build());
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), AGENT_ID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("ChatAssignment", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a value carrying the wrong action returns MALFORMED")
        void wrongActionType() {
            store.chatStore().addNewChat(CHAT_JID);
            store.businessStore().putChatAssignment(new ChatAssignmentBuilder()
                    .chatJid(CHAT_JID)
                    .agentId(AGENT_ID)
                    .opened(false)
                    .build());
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of(handler.actionName(), CHAT_JID.toString(), AGENT_ID));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a missing agent id slot returns MALFORMED")
        void missingAgentSlot() {
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), null, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a missing chat JID slot returns MALFORMED")
        void missingChatSlot() {
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();

            var result = handler.applyMutation(client,
                    buildMutation(null, null, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED")
        void removeUnsupported() {
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();

            var result = handler.applyMutation(client,
                    buildMutation(CHAT_JID.toString(), AGENT_ID, action, SyncdOperation.REMOVE, Instant.now()));

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
            var action = new ChatAssignmentOpenedStatusActionBuilder().chatOpened(true).build();
            return buildMutation(CHAT_JID.toString(), AGENT_ID, action, SyncdOperation.SET, ts);
        }
    }

}
